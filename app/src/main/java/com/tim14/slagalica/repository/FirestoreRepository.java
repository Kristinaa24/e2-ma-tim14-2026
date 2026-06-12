package com.tim14.slagalica.repository;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.tim14.slagalica.R;
import com.google.firebase.firestore.SetOptions;
import com.tim14.slagalica.model.KoZnaZnaQuestion;
import com.tim14.slagalica.model.PlayerStatistics;
import com.tim14.slagalica.model.SpojniceRound;
import com.tim14.slagalica.model.User;

import java.util.ArrayList;
import java.util.List;

public class FirestoreRepository {

    private static final String TAG = "REZ_DB";
    public static final String TEST_USER_ID = "test_user_1";

    private static final String USERS_COLLECTION = "users";
    private static final String STATISTICS_COLLECTION = "statistics";
    private static final String KO_ZNA_ZNA_COLLECTION = "koZnaZnaQuestions";
    private static final String SPOJNICE_COLLECTION = "spojniceRounds";

    private final FirebaseFirestore db;
    private final Context context;

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
        User user = new User(uid, username, email, region, 200, 0, 0, "None", uid);
        user.avatar = "avatar_1";

        PlayerStatistics statistics = new PlayerStatistics(uid);

        db.collection(USERS_COLLECTION)
                .document(uid)
                .set(user)
                .addOnSuccessListener(unused ->
                        db.collection(STATISTICS_COLLECTION)
                                .document(uid)
                                .set(statistics)
                                .addOnSuccessListener(result -> callback.onSuccess(null))
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

                    db.collection(USERS_COLLECTION)
                            .document(userId)
                            .set(user, SetOptions.merge());

                    callback.onSuccess(user);
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
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
        }

        if (TextUtils.isEmpty(user.avatar)) {
            user.avatar = "avatar_1";
        }

        if (TextUtils.isEmpty(user.avatarFrame)
                || user.avatarFrame.equalsIgnoreCase("Diamond")
                || user.avatarFrame.equalsIgnoreCase("Platinum")
                || user.avatarFrame.equalsIgnoreCase("Master")) {
            user.avatarFrame = "None";
        }

        if (TextUtils.isEmpty(user.qrCode)
                || user.qrCode.equals("QR pending")
                || user.qrCode.equals("Available for friend invite")) {
            user.qrCode = user.id;
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
