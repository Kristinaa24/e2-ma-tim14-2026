package com.tim14.slagalica.repository;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;
import com.tim14.slagalica.LeagueUtils;
import com.tim14.slagalica.R;
import com.tim14.slagalica.model.AsocijacijeRound;
import com.tim14.slagalica.model.KoZnaZnaQuestion;
import com.tim14.slagalica.model.Notification;
import com.tim14.slagalica.model.PlayerStatistics;
import com.tim14.slagalica.model.Region;
import com.tim14.slagalica.model.SpojniceRound;
import com.tim14.slagalica.model.User;
import com.tim14.slagalica.service.NotificationHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class FirestoreRepository {

    private static final String TAG = "REZ_DB";

    private static final String USERS_COLLECTION = "users";
    private static final String STATISTICS_COLLECTION = "statistics";
    private static final String KO_ZNA_ZNA_COLLECTION = "koZnaZnaQuestions";
    private static final String SPOJNICE_COLLECTION = "spojniceRounds";
    private static final String ASOCIJACIJE_COLLECTION = "asocijacijeRounds";
    private static final String NOTIFICATIONS_COLLECTION = "notifications";
    private static final String REGIONS_COLLECTION = "regions";
    private static final long ACTIVE_PLAYER_WINDOW_MS = 10 * 60 * 1000;
    private static final int MONTHLY_PLAYER_RANKING_PLACES = 3;

    private static final Region[] DEFAULT_REGIONS = new Region[]{
            new Region("VO", "Vojvodina", "\uD83C\uDF3E"),
            new Region("BG", "Belgrade", "\uD83C\uDFD9"),
            new Region("SW", "Sumadija and Western Serbia", "\uD83C\uDF32"),
            new Region("SE", "Southern and Eastern Serbia", "\u26F0"),
            new Region("KM", "Kosovo i Metohija", "\uD83C\uDFFB")
    };

    private final FirebaseFirestore db;
    private final Context context;

    public static final class MatchRewardResult {
        public final int earnedRegionStars;
        public final int previousLeague;
        public final int currentLeague;
        public final boolean leagueChanged;

        private MatchRewardResult(int earnedRegionStars, int previousLeague, int currentLeague) {
            this.earnedRegionStars = earnedRegionStars;
            this.previousLeague = previousLeague;
            this.currentLeague = currentLeague;
            this.leagueChanged = previousLeague != currentLeague;
        }
    }

    public FirestoreRepository() {
        this(null);
    }

    public FirestoreRepository(Context context) {
        db = FirebaseFirestore.getInstance();
        this.context = context != null ? context.getApplicationContext() : null;
    }

    public void isUsernameTaken(String username, FirebaseCallback<Boolean> callback) {
        db.collection(USERS_COLLECTION)
                .whereEqualTo("username", username.trim())
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> callback.onSuccess(!querySnapshot.isEmpty()))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void createUserProfile(
            String uid,
            String username,
            String email,
            String region,
            FirebaseCallback<Void> callback
    ) {
        User user = new User(
                uid,
                username,
                email,
                canonicalRegionName(region),
                LeagueUtils.INITIAL_REGISTRATION_TOKENS,
                0,
                0,
                "None",
                uid
        );
        user.avatar = "avatar_1";
        user.lastDailyTokenRewardDate = currentDailyRewardDate();

        PlayerStatistics statistics = new PlayerStatistics(uid);

        db.collection(USERS_COLLECTION)
                .document(uid)
                .set(user)
                .addOnSuccessListener(unused ->
                        db.collection(STATISTICS_COLLECTION)
                                .document(uid)
                                .set(statistics)
                                .addOnSuccessListener(result ->
                                        incrementRegionRegisteredPlayers(user.region, callback))
                                .addOnFailureListener(e -> callback.onError(e.getMessage()))
                )
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void getCurrentUser(FirebaseCallback<User> callback) {
        String userId;

        try {
            userId = requireUserId();
        } catch (IllegalStateException e) {
            callback.onError(e.getMessage());
            return;
        }

        db.collection(USERS_COLLECTION)
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    User user = documentSnapshot.toObject(User.class);

                    if (user == null) {
                        callback.onError(text(
                                R.string.firestore_error_profile_not_found,
                                "User profile was not found."
                        ));
                        return;
                    }

                    ensureUserDefaults(user, userId);
                    boolean dailyTokensApplied = applyDailyTokenGrant(user);

                    db.collection(USERS_COLLECTION)
                            .document(userId)
                            .set(user, SetOptions.merge())
                            .addOnFailureListener(e -> Log.w(TAG, "Error saving user defaults.", e));

                    if (dailyTokensApplied) {
                        Log.i(TAG, "Daily token grant applied for user=" + userId
                                + ", league=" + user.league
                                + ", tokens=" + user.tokens);
                    }

                    callback.onSuccess(user);
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void getAllUsers(FirebaseCallback<List<User>> callback) {
        db.collection(USERS_COLLECTION)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<User> users = new ArrayList<>();
                    WriteBatch batch = db.batch();

                    for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                        User user = document.toObject(User.class);
                        if (user == null) {
                            continue;
                        }

                        ensureUserDefaults(user, document.getId());
                        users.add(user);
                        batch.set(
                                db.collection(USERS_COLLECTION).document(document.getId()),
                                user,
                                SetOptions.merge()
                        );
                    }

                    if (users.isEmpty()) {
                        callback.onSuccess(users);
                        return;
                    }

                    batch.commit()
                            .addOnSuccessListener(unused -> callback.onSuccess(users))
                            .addOnFailureListener(e -> callback.onError(e.getMessage()));
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void getRegions(FirebaseCallback<List<Region>> callback) {
        ensureDefaultRegions(new FirebaseCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                db.collection(REGIONS_COLLECTION)
                        .get()
                        .addOnSuccessListener(querySnapshot -> {
                            List<Region> regions = new ArrayList<>();

                            for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                                Region region = document.toObject(Region.class);
                                if (region == null) {
                                    continue;
                                }

                                ensureRegionDefaults(region, document.getId());
                                regions.add(region);
                            }

                            callback.onSuccess(regions);
                        })
                        .addOnFailureListener(e -> callback.onError(e.getMessage()));
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    public void markCurrentUserActive(FirebaseCallback<Void> callback) {
        String userId;

        try {
            userId = requireUserId();
        } catch (IllegalStateException e) {
            callback.onError(e.getMessage());
            return;
        }

        db.collection(USERS_COLLECTION)
                .document(userId)
                .update(
                        "lastActiveAt", System.currentTimeMillis(),
                        "loggedIn", true
                )
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void markCurrentUserInactive(FirebaseCallback<Void> callback) {
        String userId;

        try {
            userId = requireUserId();
        } catch (IllegalStateException e) {
            callback.onError(e.getMessage());
            return;
        }

        db.collection(USERS_COLLECTION)
                .document(userId)
                .update(
                        "lastActiveAt", 0,
                        "loggedIn", false
                )
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void updateCurrentUserAfterRegularMatch(
            int playerScore,
            boolean won,
            boolean lost,
            FirebaseCallback<MatchRewardResult> callback
    ) {
        String userId;

        try {
            userId = requireUserId();
        } catch (IllegalStateException e) {
            callback.onError(e.getMessage());
            return;
        }

        db.collection(USERS_COLLECTION)
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    User user = documentSnapshot.toObject(User.class);
                    if (user == null) {
                        callback.onError(text(
                                R.string.firestore_error_profile_not_found,
                                "User profile was not found."
                        ));
                        return;
                    }

                    ensureUserDefaults(user, userId);

                    int previousLeague = user.league;
                    int starDelta = calculateMatchStarDelta(playerScore, won, lost);
                    user.stars = Math.max(0, user.stars + starDelta);
                    user.league = LeagueUtils.calculateLeague(user.stars);
                    Log.i(TAG, "Applying match rewards for user=" + userId
                            + ", region=" + user.region
                            + ", regionCode=" + regionCodeForName(user.region)
                            + ", score=" + playerScore
                            + ", won=" + won
                            + ", lost=" + lost
                            + ", starDelta=" + starDelta
                            + ", newStars=" + user.stars
                            + ", newLeague=" + user.league);

                    String regionCode = regionCodeForName(user.region);
                    int earnedRegionStars = Math.max(0, starDelta);
                    user.monthlyStars += earnedRegionStars;
                    WriteBatch batch = db.batch();
                    batch.set(
                            db.collection(REGIONS_COLLECTION).document(regionCode),
                            createDefaultRegionData(regionCode, earnedRegionStars),
                            SetOptions.merge()
                    );

                    Map<String, Object> userUpdates = new HashMap<>();
                    userUpdates.put("stars", user.stars);
                    userUpdates.put("monthlyStars", user.monthlyStars);
                    userUpdates.put("league", user.league);
                    batch.update(db.collection(USERS_COLLECTION).document(userId), userUpdates);

                    if (previousLeague != user.league) {
                        addLeagueChangeNotification(
                                batch,
                                userId,
                                previousLeague,
                                user.league
                        );
                    }

                    batch.commit()
                            .addOnSuccessListener(unused -> callback.onSuccess(
                                    new MatchRewardResult(earnedRegionStars, previousLeague, user.league)
                            ))
                            .addOnFailureListener(e -> callback.onError(e.getMessage()));
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void resetMonthlyRegionRanking(FirebaseCallback<Void> callback) {
        getRegions(new FirebaseCallback<List<Region>>() {
            @Override
            public void onSuccess(List<Region> regions) {
                regions.sort((left, right) -> Integer.compare(right.monthlyStars, left.monthlyStars));

                if (regions.isEmpty() || regions.get(0).monthlyStars <= 0) {
                    callback.onSuccess(null);
                    return;
                }

                WriteBatch batch = db.batch();
                for (int index = 0; index < regions.size(); index++) {
                    Region region = regions.get(index);
                    int rank = index < 3 ? index + 1 : 0;
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("previousMonthlyRank", rank);
                    updates.put("monthlyStars", 0);

                    if (rank == 1) {
                        updates.put("firstPlaces", region.firstPlaces + 1);
                    } else if (rank == 2) {
                        updates.put("secondPlaces", region.secondPlaces + 1);
                    } else if (rank == 3) {
                        updates.put("thirdPlaces", region.thirdPlaces + 1);
                    }

                    batch.set(
                            db.collection(REGIONS_COLLECTION).document(region.code),
                            updates,
                            SetOptions.merge()
                    );
                }

                batch.commit()
                        .addOnSuccessListener(unused -> callback.onSuccess(null))
                        .addOnFailureListener(e -> callback.onError(e.getMessage()));
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    public void resetMonthlyPlayerRanking(FirebaseCallback<Void> callback) {
        db.collection(USERS_COLLECTION)
                .get()
                .addOnSuccessListener(usersSnapshot -> {
                    List<DocumentSnapshot> rankedUsers = new ArrayList<>(usersSnapshot.getDocuments());
                    rankedUsers.sort((left, right) -> {
                        User leftUser = left.toObject(User.class);
                        User rightUser = right.toObject(User.class);
                        int leftMonthlyStars = leftUser != null ? Math.max(0, leftUser.monthlyStars) : 0;
                        int rightMonthlyStars = rightUser != null ? Math.max(0, rightUser.monthlyStars) : 0;
                        return Integer.compare(rightMonthlyStars, leftMonthlyStars);
                    });

                    WriteBatch batch = db.batch();
                    applyMonthlyPlayerRankingReset(batch, rankedUsers);
                    batch.commit()
                            .addOnSuccessListener(unused -> callback.onSuccess(null))
                            .addOnFailureListener(e -> callback.onError(e.getMessage()));
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void updateRegionPlayerCounts(
            Map<String, Integer> registeredCountsByRegionName,
            Map<String, Integer> activeCountsByRegionName,
            FirebaseCallback<Void> callback
    ) {
        ensureDefaultRegions(new FirebaseCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                WriteBatch batch = db.batch();

                for (Region region : DEFAULT_REGIONS) {
                    Map<String, Object> data = createDefaultRegionData(region.code, 0);
                    data.put("registeredPlayers", registeredCountsByRegionName.getOrDefault(region.name, 0));
                    data.put("activePlayers", activeCountsByRegionName.getOrDefault(region.name, 0));
                    batch.set(
                            db.collection(REGIONS_COLLECTION).document(region.code),
                            data,
                            SetOptions.merge()
                    );
                }

                batch.commit()
                        .addOnSuccessListener(unused -> callback.onSuccess(null))
                        .addOnFailureListener(e -> callback.onError(e.getMessage()));
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    public void refreshRegionPlayerCounts(FirebaseCallback<Void> callback) {
        getAllUsers(new FirebaseCallback<List<User>>() {
            @Override
            public void onSuccess(List<User> users) {
                Map<String, Integer> registeredCounts = new HashMap<>();
                Map<String, Integer> activeCounts = new HashMap<>();

                for (User user : users) {
                    String canonicalRegion = canonicalRegionName(user.region);
                    if (TextUtils.isEmpty(canonicalRegion)) {
                        continue;
                    }

                    registeredCounts.put(
                            canonicalRegion,
                            registeredCounts.getOrDefault(canonicalRegion, 0) + 1
                    );

                    if (isRecentlyActive(user)) {
                        activeCounts.put(
                                canonicalRegion,
                                activeCounts.getOrDefault(canonicalRegion, 0) + 1
                        );
                    }
                }

                updateRegionPlayerCounts(registeredCounts, activeCounts, callback);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    public static boolean isRecentlyActive(User user) {
        return user != null
                && user.loggedIn
                && user.lastActiveAt > 0
                && System.currentTimeMillis() - user.lastActiveAt <= ACTIVE_PLAYER_WINDOW_MS;
    }

    public void initializeRegions(FirebaseCallback<Void> callback) {
        ensureDefaultRegions(callback);
    }

    public void applyPreviousCycleRegionFrame(User user, FirebaseCallback<User> callback) {
        if (user == null || TextUtils.isEmpty(user.region)) {
            callback.onSuccess(user);
            return;
        }

        ensureDefaultRegions(new FirebaseCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                String code = regionCodeForName(user.region);
                db.collection(REGIONS_COLLECTION)
                        .document(code)
                        .get()
                        .addOnSuccessListener(documentSnapshot -> {
                            Region region = documentSnapshot.toObject(Region.class);
                            if (region == null) {
                                callback.onSuccess(user);
                                return;
                            }

                            ensureRegionDefaults(region, documentSnapshot.getId());
                            String frame = frameForPreviousRank(region.previousMonthlyRank);
                            if (frame == null
                                    || frame.equals(user.avatarFrame)
                                    || framePriority(user.avatarFrame) >= framePriority(frame)) {
                                callback.onSuccess(user);
                                return;
                            }

                            user.avatarFrame = frame;
                            db.collection(USERS_COLLECTION)
                                    .document(user.id)
                                    .update("avatarFrame", frame)
                                    .addOnSuccessListener(unused -> callback.onSuccess(user))
                                    .addOnFailureListener(e -> callback.onError(e.getMessage()));
                        })
                        .addOnFailureListener(e -> callback.onError(e.getMessage()));
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    public static String canonicalRegionName(String regionValue) {
        if (TextUtils.isEmpty(regionValue)) {
            return "";
        }

        String value = regionValue.trim();
        if ("VO".equalsIgnoreCase(value) || "Vojvodina".equalsIgnoreCase(value)) {
            return "Vojvodina";
        }
        if ("BG".equalsIgnoreCase(value)
                || "Belgrade".equalsIgnoreCase(value)
                || "Beograd".equalsIgnoreCase(value)) {
            return "Belgrade";
        }
        if ("SW".equalsIgnoreCase(value)
                || "Sumadija and Western Serbia".equalsIgnoreCase(value)
                || "Šumadija and Western Serbia".equalsIgnoreCase(value)
                || "Sumadija i Zapadna Srbija".equalsIgnoreCase(value)
                || "Šumadija i Zapadna Srbija".equalsIgnoreCase(value)) {
            return "Sumadija and Western Serbia";
        }
        if ("SE".equalsIgnoreCase(value)
                || "Southern and Eastern Serbia".equalsIgnoreCase(value)
                || "Juzna and Eastern Serbia".equalsIgnoreCase(value)
                || "Južna and Eastern Serbia".equalsIgnoreCase(value)
                || "Juzna i Istocna Srbija".equalsIgnoreCase(value)
                || "Južna i Istočna Srbija".equalsIgnoreCase(value)) {
            return "Southern and Eastern Serbia";
        }
        if ("KM".equalsIgnoreCase(value)
                || "Kosovo i Metohija".equalsIgnoreCase(value)
                || "Kosovo and Metohija".equalsIgnoreCase(value)) {
            return "Kosovo i Metohija";
        }
        return value;
    }

    public static String regionCodeForName(String regionName) {
        String canonicalRegionName = canonicalRegionName(regionName);
        if ("Vojvodina".equals(canonicalRegionName)) {
            return "VO";
        }
        if ("Belgrade".equals(canonicalRegionName)) {
            return "BG";
        }
        if ("Sumadija and Western Serbia".equals(canonicalRegionName)) {
            return "SW";
        }
        if ("Southern and Eastern Serbia".equals(canonicalRegionName)) {
            return "SE";
        }
        if ("Kosovo i Metohija".equals(canonicalRegionName)) {
            return "KM";
        }
        return "RS";
    }

    private void ensureUserDefaults(User user, String userId) {
        if (TextUtils.isEmpty(user.id)) {
            user.id = userId;
        }

        if (TextUtils.isEmpty(user.username)) {
            user.username = "Player";
        }

        if (TextUtils.isEmpty(user.email)) {
            user.email = "";
        }

        if (TextUtils.isEmpty(user.region)) {
            user.region = "Serbia";
        } else {
            user.region = canonicalRegionName(user.region);
        }

        if (TextUtils.isEmpty(user.avatar)) {
            user.avatar = "avatar_1";
        }

        if (TextUtils.isEmpty(user.avatarFrame)) {
            user.avatarFrame = "None";
        }

        if (TextUtils.isEmpty(user.qrCode)
                || user.qrCode.equals("QR pending")
                || user.qrCode.equals("Available for friend invite")) {
            user.qrCode = user.id;
        }

        user.stars = Math.max(0, user.stars);
        user.monthlyStars = Math.max(0, user.monthlyStars);
        user.league = LeagueUtils.calculateLeague(user.stars);
        migrateLegacyInitialTokens(user);
        if (TextUtils.isEmpty(user.lastDailyTokenRewardDate)) {
            user.lastDailyTokenRewardDate = "";
        }
    }

    private void migrateLegacyInitialTokens(User user) {
        if (user.tokens >= 200 && user.stars < LeagueUtils.getRequiredStars(1)) {
            user.tokens = LeagueUtils.INITIAL_REGISTRATION_TOKENS;
            user.lastDailyTokenRewardDate = currentDailyRewardDate();
        }
    }

    private boolean applyDailyTokenGrant(User user) {
        String today = currentDailyRewardDate();
        if (today.equals(user.lastDailyTokenRewardDate)) {
            return false;
        }

        user.tokens += LeagueUtils.getDailyTokenGrant(user.league);
        user.lastDailyTokenRewardDate = today;
        return true;
    }

    private String currentDailyRewardDate() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
    }

    private void applyMonthlyPlayerRankingReset(WriteBatch batch, List<DocumentSnapshot> rankedUsers) {
        boolean hasMonthlyPlayerResults = false;
        for (DocumentSnapshot document : rankedUsers) {
            User user = document.toObject(User.class);
            if (user != null && user.monthlyStars > 0) {
                hasMonthlyPlayerResults = true;
                break;
            }
        }

        for (int index = 0; index < rankedUsers.size(); index++) {
            DocumentSnapshot document = rankedUsers.get(index);
            User user = document.toObject(User.class);
            if (user == null) {
                continue;
            }

            ensureUserDefaults(user, document.getId());
            boolean placed = index < MONTHLY_PLAYER_RANKING_PLACES && user.monthlyStars > 0;
            Map<String, Object> userUpdates = new HashMap<>();
            userUpdates.put("monthlyStars", 0);

            if (hasMonthlyPlayerResults && !placed) {
                int previousStars = Math.max(0, user.stars);
                int previousLeague = user.league;
                int newStars = (int) Math.floor(previousStars * 0.7);
                int newLeague = LeagueUtils.calculateLeague(newStars);

                userUpdates.put("stars", newStars);
                userUpdates.put("league", newLeague);

                if (previousStars != newStars || previousLeague != newLeague) {
                    addMonthlyRankingPenaltyNotification(
                            batch,
                            document.getId(),
                            previousStars,
                            newStars,
                            previousLeague,
                            newLeague
                    );
                }
            }

            batch.update(db.collection(USERS_COLLECTION).document(document.getId()), userUpdates);
        }
    }

    private void ensureDefaultRegions(FirebaseCallback<Void> callback) {
        WriteBatch batch = db.batch();

        for (Region region : DEFAULT_REGIONS) {
            batch.set(
                    db.collection(REGIONS_COLLECTION).document(region.code),
                    createDefaultRegionData(region.code, 0),
                    SetOptions.merge()
            );
        }

        batch.commit()
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    private Map<String, Object> createDefaultRegionData(String regionCode, int monthlyStarsIncrement) {
        Region defaultRegion = defaultRegionForCode(regionCode);
        Map<String, Object> data = new HashMap<>();
        data.put("id", defaultRegion.code);
        data.put("code", defaultRegion.code);
        data.put("name", defaultRegion.name);
        data.put("icon", defaultRegion.icon);
        data.put("monthlyStars", FieldValue.increment(monthlyStarsIncrement));
        data.put("firstPlaces", FieldValue.increment(0));
        data.put("secondPlaces", FieldValue.increment(0));
        data.put("thirdPlaces", FieldValue.increment(0));
        data.put("activePlayers", FieldValue.increment(0));
        data.put("registeredPlayers", FieldValue.increment(0));
        data.put("previousMonthlyRank", FieldValue.increment(0));
        return data;
    }

    private Region defaultRegionForCode(String code) {
        for (Region region : DEFAULT_REGIONS) {
            if (region.code.equals(code)) {
                return region;
            }
        }
        return new Region("RS", "Serbia", "RS");
    }

    private void ensureRegionDefaults(Region region, String documentId) {
        if (TextUtils.isEmpty(region.id)) {
            region.id = documentId;
        }

        if (TextUtils.isEmpty(region.code)) {
            region.code = documentId;
        }

        if (TextUtils.isEmpty(region.name)) {
            region.name = regionNameForCode(region.code);
        }

        if (TextUtils.isEmpty(region.icon)) {
            region.icon = region.code;
        }
    }

    private String regionNameForCode(String code) {
        for (Region region : DEFAULT_REGIONS) {
            if (region.code.equals(code)) {
                return region.name;
            }
        }
        return "Serbia";
    }

    private void incrementRegionRegisteredPlayers(String regionName, FirebaseCallback<Void> callback) {
        ensureDefaultRegions(new FirebaseCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                db.collection(REGIONS_COLLECTION)
                        .document(regionCodeForName(regionName))
                        .update("registeredPlayers", FieldValue.increment(1))
                        .addOnSuccessListener(unused -> callback.onSuccess(null))
                        .addOnFailureListener(e -> callback.onError(e.getMessage()));
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    private int calculateMatchStarDelta(int playerScore, boolean won, boolean lost) {
        int scoreBonus = Math.max(0, playerScore) / 40;

        if (won) {
            return 10 + scoreBonus;
        }

        if (lost) {
            return scoreBonus - 10;
        }

        return scoreBonus;
    }

    private String frameForPreviousRank(int previousMonthlyRank) {
        if (previousMonthlyRank == 1) {
            return "Gold";
        }
        if (previousMonthlyRank == 2) {
            return "Silver";
        }
        if (previousMonthlyRank == 3) {
            return "Bronze";
        }
        return null;
    }

    private int framePriority(String avatarFrame) {
        if (avatarFrame == null) {
            return 0;
        }

        switch (avatarFrame.toLowerCase(Locale.US)) {
            case "bronze":
                return 1;
            case "silver":
                return 2;
            case "gold":
                return 3;
            case "platinum":
                return 4;
            case "diamond":
                return 5;
            case "master":
                return 6;
            default:
                return 0;
        }
    }

    public void getStatistics(FirebaseCallback<PlayerStatistics> callback) {
        String userId;

        try {
            userId = requireUserId();
        } catch (IllegalStateException e) {
            callback.onError(e.getMessage());
            return;
        }

        db.collection(STATISTICS_COLLECTION)
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    try {
                        PlayerStatistics statistics = documentSnapshot.toObject(PlayerStatistics.class);

                        if (statistics == null) {
                            statistics = new PlayerStatistics(userId);
                            db.collection(STATISTICS_COLLECTION).document(userId).set(statistics);
                        }

                        callback.onSuccess(statistics);
                    } catch (Exception e) {
                        Log.e(TAG, "Serialization error in getStatistics", e);
                        callback.onSuccess(new PlayerStatistics(userId));
                    }
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void updateMatchStatistics(boolean won, boolean lost) {
        String userId;

        try {
            userId = requireUserId();
        } catch (IllegalStateException e) {
            Log.w(TAG, e.getMessage());
            return;
        }

        db.collection(STATISTICS_COLLECTION)
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    try {
                        PlayerStatistics statistics = documentSnapshot.toObject(PlayerStatistics.class);

                        if (statistics == null) {
                            statistics = new PlayerStatistics(userId);
                        }

                        statistics.gamesPlayed++;

                        if (won) {
                            statistics.wins++;
                        }

                        if (lost) {
                            statistics.losses++;
                        }

                        db.collection(STATISTICS_COLLECTION)
                                .document(userId)
                                .set(statistics)
                                .addOnSuccessListener(unused ->
                                        Log.d(TAG, "Match statistics updated."))
                                .addOnFailureListener(e ->
                                        Log.w(TAG, "Error updating match statistics.", e));
                    } catch (Exception e) {
                        Log.e(TAG, "Serialization error in updateMatchStatistics", e);
                    }
                })
                .addOnFailureListener(e ->
                        Log.w(TAG, "Error reading statistics.", e));
    }

    public void updateAvatar(String avatar, FirebaseCallback<Void> callback) {
        Log.i(TAG, "updateAvatar");

        String userId;

        try {
            userId = requireUserId();
        } catch (IllegalStateException e) {
            callback.onError(e.getMessage());
            return;
        }

        db.collection(USERS_COLLECTION)
                .document(userId)
                .update("avatar", avatar)
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void getKoZnaZnaQuestions(FirebaseCallback<List<KoZnaZnaQuestion>> callback) {
        Log.i(TAG, "getKoZnaZnaQuestions");

        db.collection(KO_ZNA_ZNA_COLLECTION)
                .limit(5)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots ->
                        callback.onSuccess(queryDocumentSnapshots.toObjects(KoZnaZnaQuestion.class))
                )
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void getSpojniceRounds(FirebaseCallback<List<SpojniceRound>> callback) {
        Log.i(TAG, "getSpojniceRounds");

        db.collection(SPOJNICE_COLLECTION)
                .document("round1")
                .get()
                .addOnSuccessListener(round1Document -> {
                    SpojniceRound round1 = createSpojniceRoundFromDocument(round1Document);

                    db.collection(SPOJNICE_COLLECTION)
                            .document("round2")
                            .get()
                            .addOnSuccessListener(round2Document -> {
                                SpojniceRound round2 = createSpojniceRoundFromDocument(round2Document);

                                List<SpojniceRound> rounds = new ArrayList<>();
                                rounds.add(round1);
                                rounds.add(round2);

                                callback.onSuccess(rounds);
                            })
                            .addOnFailureListener(e -> callback.onError(e.getMessage()));
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void updateKoZnaZnaStatistics(int correctAnswers, int wrongAnswers, int score) {
        String userId;

        try {
            userId = requireUserId();
        } catch (IllegalStateException e) {
            Log.w(TAG, e.getMessage());
            return;
        }

        db.collection(STATISTICS_COLLECTION)
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    try {
                        PlayerStatistics statistics = documentSnapshot.toObject(PlayerStatistics.class);

                        if (statistics == null) {
                            statistics = new PlayerStatistics(userId);
                        }

                        statistics.koZnaZnaCorrect += correctAnswers;
                        statistics.koZnaZnaWrong += wrongAnswers;
                        statistics.koZnaZnaTotalScore += score;

                        db.collection(STATISTICS_COLLECTION)
                                .document(userId)
                                .set(statistics)
                                .addOnSuccessListener(unused ->
                                        Log.d(TAG, "Ko zna zna statistics updated."))
                                .addOnFailureListener(e ->
                                        Log.w(TAG, "Error updating Ko zna zna statistics.", e));
                    } catch (Exception e) {
                        Log.e(TAG, "Serialization error in updateKoZnaZnaStatistics", e);
                    }
                })
                .addOnFailureListener(e ->
                        Log.w(TAG, "Error reading statistics.", e));
    }

    public void updateSpojniceStatistics(int correctPairs, int totalPairs, int score) {
        String userId;

        try {
            userId = requireUserId();
        } catch (IllegalStateException e) {
            Log.w(TAG, e.getMessage());
            return;
        }

        db.collection(STATISTICS_COLLECTION)
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    try {
                        PlayerStatistics statistics = documentSnapshot.toObject(PlayerStatistics.class);

                        if (statistics == null) {
                            statistics = new PlayerStatistics(userId);
                        }

                        statistics.spojnicaCorrectPairs += correctPairs;
                        statistics.spojnicaTotalPairs += totalPairs;
                        statistics.spojnicaTotalScore += score;

                        db.collection(STATISTICS_COLLECTION)
                                .document(userId)
                                .set(statistics)
                                .addOnSuccessListener(unused ->
                                        Log.d(TAG, "Spojnice statistics updated."))
                                .addOnFailureListener(e ->
                                        Log.w(TAG, "Error updating Spojnice statistics.", e));
                    } catch (Exception e) {
                        Log.e(TAG, "Serialization error in updateSpojniceStatistics", e);
                    }
                })
                .addOnFailureListener(e ->
                        Log.w(TAG, "Error reading statistics.", e));
    }

    public void updateKorakPoKorakStatistics(int openedClues, int score, boolean solved) {
        String userId;

        try {
            userId = requireUserId();
        } catch (IllegalStateException e) {
            Log.w(TAG, e.getMessage());
            return;
        }

        db.collection(STATISTICS_COLLECTION)
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    try {
                        PlayerStatistics statistics = documentSnapshot.toObject(PlayerStatistics.class);

                        if (statistics == null) {
                            statistics = new PlayerStatistics(userId);
                        }

                        statistics.korakPoKorakTotalRounds++;

                        if (solved) {
                            statistics.korakPoKorakSolved++;

                            if (isKorakPoKorakStep(openedClues)) {
                                incrementKorakPoKorakStepHit(statistics, openedClues);

                                if (statistics.korakPoKorakBestStep == 0
                                        || openedClues < statistics.korakPoKorakBestStep) {
                                    statistics.korakPoKorakBestStep = openedClues;
                                }
                            }
                        }

                        statistics.korakPoKorakTotalScore += score;

                        db.collection(STATISTICS_COLLECTION)
                                .document(userId)
                                .set(statistics)
                                .addOnSuccessListener(unused ->
                                        Log.d(TAG, "Korak po korak statistics updated."))
                                .addOnFailureListener(e ->
                                        Log.w(TAG, "Error updating Korak po korak statistics.", e));

                    } catch (Exception e) {
                        Log.e(TAG, "Serialization error in updateKorakPoKorakStatistics", e);
                    }
                })
                .addOnFailureListener(e ->
                        Log.w(TAG, "Error reading statistics.", e));
    }

    private boolean isKorakPoKorakStep(int openedClues) {
        return openedClues >= 1 && openedClues <= 7;
    }

    private void incrementKorakPoKorakStepHit(PlayerStatistics statistics, int openedClues) {
        switch (openedClues) {
            case 1:
                statistics.korakPoKorakStep1Hits++;
                break;
            case 2:
                statistics.korakPoKorakStep2Hits++;
                break;
            case 3:
                statistics.korakPoKorakStep3Hits++;
                break;
            case 4:
                statistics.korakPoKorakStep4Hits++;
                break;
            case 5:
                statistics.korakPoKorakStep5Hits++;
                break;
            case 6:
                statistics.korakPoKorakStep6Hits++;
                break;
            case 7:
                statistics.korakPoKorakStep7Hits++;
                break;
            default:
                Log.w(TAG, "Invalid Korak po korak step: " + openedClues);
                break;
        }
    }

    public void updateMojBrojStatistics(int difference, int score) {
        String userId;

        try {
            userId = requireUserId();
        } catch (IllegalStateException e) {
            Log.w(TAG, e.getMessage());
            return;
        }

        db.collection(STATISTICS_COLLECTION)
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    try {
                        PlayerStatistics statistics = documentSnapshot.toObject(PlayerStatistics.class);

                        if (statistics == null) {
                            statistics = new PlayerStatistics(userId);
                        }

                    statistics.mojBrojTotalRounds++;

                    if (difference == 0) {
                        statistics.mojBrojExactHits++;
                    } else if (score > 0) {
                        statistics.mojBrojCloseHits++;
                    }

                        statistics.mojBrojTotalScore += score;

                        db.collection(STATISTICS_COLLECTION)
                                .document(userId)
                                .set(statistics)
                                .addOnSuccessListener(unused ->
                                        Log.d(TAG, "Moj broj statistics updated."))
                                .addOnFailureListener(e ->
                                        Log.w(TAG, "Error updating Moj broj statistics.", e));
                    } catch (Exception e) {
                        Log.e(TAG, "Serialization error in updateMojBrojStatistics", e);
                    }
                })
                .addOnFailureListener(e ->
                        Log.w(TAG, "Error reading statistics.", e));
    }

    public void updateSkockoStatistics(int attemptIndex, int score, boolean solved) {
        String userId;
        try {
            userId = requireUserId();
        } catch (IllegalStateException e) {
            return;
        }

        db.collection(STATISTICS_COLLECTION).document(userId).get().addOnSuccessListener(documentSnapshot -> {
            try {
                PlayerStatistics statistics = documentSnapshot.toObject(PlayerStatistics.class);
                if (statistics == null) statistics = new PlayerStatistics(userId);

                if (solved && attemptIndex >= 0 && attemptIndex < 6) {
                    statistics.skockoSolvedCount++;
                    ensureSkockoAttempts(statistics);
                    int currentVal = statistics.skockoAttemptsCount.get(attemptIndex);
                    statistics.skockoAttemptsCount.set(attemptIndex, currentVal + 1);
                }
                statistics.skockoTotalRounds++;
                statistics.skockoTotalScore += score;

                db.collection(STATISTICS_COLLECTION).document(userId).set(statistics);
            } catch (Exception e) {
                Log.e(TAG, "Serialization error in updateSkockoStatistics", e);
            }
        });
    }

    private void ensureSkockoAttempts(PlayerStatistics statistics) {
        if (statistics.skockoAttemptsCount == null) {
            statistics.skockoAttemptsCount = new ArrayList<>();
        }

        while (statistics.skockoAttemptsCount.size() < 6) {
            statistics.skockoAttemptsCount.add(0);
        }
    }

    public void updateAsocijacijeStatistics(boolean solved, int score) {
        String userId;
        try {
            userId = requireUserId();
        } catch (IllegalStateException e) {
            return;
        }

        db.collection(STATISTICS_COLLECTION).document(userId).get().addOnSuccessListener(documentSnapshot -> {
            try {
                PlayerStatistics statistics = documentSnapshot.toObject(PlayerStatistics.class);
                if (statistics == null) statistics = new PlayerStatistics(userId);

                if (solved) {
                    statistics.asocijacijeSolved++;
                } else {
                    statistics.asocijacijeUnsolved++;
                }
                statistics.asocijacijeTotalScore += score;

                db.collection(STATISTICS_COLLECTION).document(userId).set(statistics);
            } catch (Exception e) {
                Log.e(TAG, "Serialization error in updateAsocijacijeStatistics", e);
            }
        });
    }

    // ── Asocijacije ──────────────────────────────────────────────────────────

    public void getAsocijacijeRounds(FirebaseCallback<List<AsocijacijeRound>> callback) {
        db.collection(ASOCIJACIJE_COLLECTION)
                .limit(10)
                .get()
                .addOnSuccessListener(querySnapshot ->
                        callback.onSuccess(querySnapshot.toObjects(AsocijacijeRound.class)))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    // ── Notifications ─────────────────────────────────────────────────────────

    public void saveNotification(String title, String message, String type) {
        String userId;
        try {
            userId = requireUserId();
        } catch (IllegalStateException e) {
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("userId", userId);
        data.put("title", title);
        data.put("message", message);
        data.put("typeString", type != null ? type.toUpperCase() : "OTHER");
        data.put("timestamp", new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date()));
        data.put("read", false);
        data.put("createdAt", System.currentTimeMillis());

        db.collection(NOTIFICATIONS_COLLECTION)
                .add(data)
                .addOnFailureListener(e -> Log.w(TAG, "Error saving notification", e));
    }

    private void addLeagueChangeNotification(
            WriteBatch batch,
            String userId,
            int previousLeague,
            int currentLeague
    ) {
        boolean promoted = currentLeague > previousLeague;
        String title = promoted ? "League promotion" : "League demotion";
        String message = promoted
                ? "You advanced to " + LeagueUtils.getLeagueName(currentLeague) + " League."
                : "You dropped to " + LeagueUtils.getLeagueName(currentLeague) + " League.";

        batch.set(
                db.collection(NOTIFICATIONS_COLLECTION).document(),
                notificationData(userId, title, message, "REWARD")
        );
        showLeagueSystemNotification(title, message, "REWARD");
    }

    private void addMonthlyRankingPenaltyNotification(
            WriteBatch batch,
            String userId,
            int previousStars,
            int newStars,
            int previousLeague,
            int newLeague
    ) {
        String message = "You did not place in the monthly player ranking. Stars reduced from "
                + previousStars + " to " + newStars + ".";
        if (previousLeague != newLeague) {
            message += " Current league: " + LeagueUtils.getLeagueName(newLeague) + " League.";
        }

        batch.set(
                db.collection(NOTIFICATIONS_COLLECTION).document(),
                notificationData(userId, "Monthly ranking update", message, "RANKING")
        );

        if (previousLeague != newLeague) {
            showLeagueSystemNotification("Monthly ranking update", message, "RANKING");
        }
    }

    private void showLeagueSystemNotification(String title, String message, String type) {
        if (context == null) {
            return;
        }

        NotificationHelper.createNotificationChannels(context);
        NotificationHelper.showSystemNotification(context, title, message, type, false);
    }

    private Map<String, Object> notificationData(String userId, String title, String message, String type) {
        Map<String, Object> data = new HashMap<>();
        data.put("userId", userId);
        data.put("title", title);
        data.put("message", message);
        data.put("typeString", type != null ? type.toUpperCase() : "OTHER");
        data.put("timestamp", new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date()));
        data.put("read", false);
        data.put("createdAt", System.currentTimeMillis());
        return data;
    }

    public void getNotifications(FirebaseCallback<List<Notification>> callback) {
        String userId;
        try {
            userId = requireUserId();
        } catch (IllegalStateException e) {
            callback.onError(e.getMessage());
            return;
        }

        db.collection(NOTIFICATIONS_COLLECTION)
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Notification> notifications = new ArrayList<>();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        notifications.add(notificationFromDocument(doc));
                    }
                    notifications.sort((a, b) -> Long.compare(b.createdAt, a.createdAt));
                    callback.onSuccess(notifications);
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void markNotificationAsRead(String notificationId) {
        try {
            requireUserId();
        } catch (IllegalStateException e) {
            return;
        }

        db.collection(NOTIFICATIONS_COLLECTION)
                .document(notificationId)
                .update("read", true)
                .addOnFailureListener(e -> Log.w(TAG, "Error marking notification as read", e));
    }

    private Notification notificationFromDocument(com.google.firebase.firestore.DocumentSnapshot doc) {
        Notification n = new Notification();
        n.id = doc.getId();
        n.title = doc.getString("title");
        n.message = doc.getString("message");
        n.timestamp = doc.getString("timestamp");
        Boolean readVal = doc.getBoolean("read");
        n.read = readVal != null && readVal;
        n.typeString = doc.getString("typeString");
        n.type = Notification.typeFromString(n.typeString);
        Long createdAt = doc.getLong("createdAt");
        n.createdAt = createdAt != null ? createdAt : 0L;
        n.userId = doc.getString("userId");
        return n;
    }

    private String requireUserId() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            throw new IllegalStateException(text(
                    R.string.firestore_error_user_not_logged_in,
                    "User is not logged in."
            ));
        }

        return FirebaseAuth.getInstance().getCurrentUser().getUid();
    }

    @SuppressWarnings("unchecked")
    private SpojniceRound createSpojniceRoundFromDocument(DocumentSnapshot document) {
        SpojniceRound round = new SpojniceRound();

        round.setId(document.getId());
        round.setTitle(document.getString("title"));
        round.setLeftItems((List<String>) document.get("leftItems"));
        round.setCorrectRightItems((List<String>) document.get("correctRightItems"));
        round.setDisplayedRightItems((List<String>) document.get("displayedRightItems"));

        Log.d(TAG, "Loaded round: " + round.getId());
        return round;
    }

    private String text(int resId, String fallback) {
        return context != null ? context.getString(resId) : fallback;
    }
}
