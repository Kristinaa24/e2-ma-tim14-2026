package com.tim14.slagalica.repository;

import android.text.TextUtils;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.Transaction;
import com.tim14.slagalica.LeagueUtils;
import com.tim14.slagalica.model.ChallengeParticipant;
import com.tim14.slagalica.model.RegionChallenge;
import com.tim14.slagalica.model.User;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChallengeRepository {

    private static final String USERS_COLLECTION = "users";
    private static final String CHALLENGES_COLLECTION = "challenges";

    public static final int MAX_STAKE_TOKENS = 2;
    public static final int MAX_STAKE_STARS = 10;
    public static final int REQUIRED_PLAYERS = 4;

    private final FirebaseFirestore db;

    public ChallengeRepository() {
        db = FirebaseFirestore.getInstance();
    }

    public ListenerRegistration listenToChallenges(FirebaseCallback<List<RegionChallenge>> callback) {
        return db.collection(CHALLENGES_COLLECTION)
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        callback.onError(error.getMessage());
                        return;
                    }

                    List<RegionChallenge> challenges = new ArrayList<>();
                    if (snapshot != null) {
                        for (DocumentSnapshot document : snapshot.getDocuments()) {
                            RegionChallenge challenge = document.toObject(RegionChallenge.class);
                            if (challenge == null) {
                                continue;
                            }

                            ensureChallengeDefaults(challenge, document.getId());
                            challenges.add(challenge);
                        }
                    }

                    challenges.sort(challengeComparator());
                    callback.onSuccess(challenges);
                });
    }

    public void createChallenge(
            int stakeTokens,
            int stakeStars,
            FirebaseCallback<String> callback
    ) {
        String userId;
        try {
            userId = requireUserId();
        } catch (IllegalStateException exception) {
            callback.onError(exception.getMessage());
            return;
        }

        String validationError = validateStake(stakeTokens, stakeStars);
        if (validationError != null) {
            callback.onError(validationError);
            return;
        }

        DocumentReference challengeReference = db.collection(CHALLENGES_COLLECTION).document();
        DocumentReference userReference = db.collection(USERS_COLLECTION).document(userId);

        db.runTransaction(transaction -> {
            User user = readUser(transaction, userReference, userId);
            applyDailyTokenGrantForTransaction(user);
            assertStakeAvailable(user, stakeTokens, stakeStars);

            long now = System.currentTimeMillis();
            applyStakeDeduction(user, stakeTokens, stakeStars);
            user.lastSeenAt = now;

            RegionChallenge challenge = new RegionChallenge();
            challenge.id = challengeReference.getId();
            challenge.creatorId = userId;
            challenge.creatorName = safeUserName(user);
            challenge.creatorRegion = FirestoreRepository.canonicalRegionName(user.region);
            challenge.stakeTokens = stakeTokens;
            challenge.stakeStars = stakeStars;
            challenge.requiredPlayers = REQUIRED_PLAYERS;
            challenge.status = RegionChallenge.STATUS_OPEN;
            challenge.createdAt = now;
            challenge.updatedAt = now;
            challenge.participants = new ArrayList<>();
            challenge.participants.add(new ChallengeParticipant(userId, safeUserName(user), now));
            challenge.winnerRewardTokens = 0;
            challenge.winnerRewardStars = 0;
            challenge.runnerUpRewardTokens = 0;
            challenge.runnerUpRewardStars = 0;

            transaction.set(userReference, buildUserUpdates(user), com.google.firebase.firestore.SetOptions.merge());
            transaction.set(challengeReference, challenge);
            return challengeReference.getId();
        }).addOnSuccessListener(callback::onSuccess)
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void joinChallenge(String challengeId, FirebaseCallback<Void> callback) {
        String userId;
        try {
            userId = requireUserId();
        } catch (IllegalStateException exception) {
            callback.onError(exception.getMessage());
            return;
        }

        if (TextUtils.isEmpty(challengeId)) {
            callback.onError("Challenge was not found.");
            return;
        }

        DocumentReference challengeReference = db.collection(CHALLENGES_COLLECTION).document(challengeId);
        DocumentReference userReference = db.collection(USERS_COLLECTION).document(userId);

        db.runTransaction(transaction -> {
            RegionChallenge challenge = readChallenge(transaction, challengeReference);
            User user = readUser(transaction, userReference, userId);
            applyDailyTokenGrantForTransaction(user);

            if (!RegionChallenge.STATUS_OPEN.equals(challenge.status)) {
                throw new IllegalStateException("This challenge is no longer accepting players.");
            }

            if (containsParticipant(challenge.participants, userId)) {
                throw new IllegalStateException("You already joined this challenge.");
            }

            if (challenge.getParticipantCount() >= REQUIRED_PLAYERS) {
                throw new IllegalStateException("This challenge is already full.");
            }

            assertStakeAvailable(user, challenge.stakeTokens, challenge.stakeStars);

            long now = System.currentTimeMillis();
            applyStakeDeduction(user, challenge.stakeTokens, challenge.stakeStars);
            user.lastSeenAt = now;

            challenge.participants.add(new ChallengeParticipant(userId, safeUserName(user), now));
            challenge.updatedAt = now;
            challenge.status = challenge.getParticipantCount() >= REQUIRED_PLAYERS
                    ? RegionChallenge.STATUS_READY
                    : RegionChallenge.STATUS_OPEN;

            transaction.set(userReference, buildUserUpdates(user), com.google.firebase.firestore.SetOptions.merge());
            transaction.set(challengeReference, challenge);
            return null;
        }).addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void cancelChallenge(String challengeId, FirebaseCallback<Void> callback) {
        String userId;
        try {
            userId = requireUserId();
        } catch (IllegalStateException exception) {
            callback.onError(exception.getMessage());
            return;
        }

        if (TextUtils.isEmpty(challengeId)) {
            callback.onError("Challenge was not found.");
            return;
        }

        DocumentReference challengeReference = db.collection(CHALLENGES_COLLECTION).document(challengeId);

        db.runTransaction(transaction -> {
            RegionChallenge challenge = readChallenge(transaction, challengeReference);
            if (!userId.equals(challenge.creatorId)) {
                throw new IllegalStateException("Only the creator can cancel this challenge.");
            }

            if (RegionChallenge.STATUS_FINISHED.equals(challenge.status)
                    || RegionChallenge.STATUS_CANCELED.equals(challenge.status)) {
                throw new IllegalStateException("This challenge can no longer be canceled.");
            }

            if (hasPlayedParticipant(challenge.participants)) {
                throw new IllegalStateException("This challenge already started and cannot be canceled.");
            }

            long now = System.currentTimeMillis();
            List<ChallengeParticipant> participants = safeParticipants(challenge.participants);
            Map<String, User> usersToRefund = new HashMap<>();
            for (ChallengeParticipant participant : participants) {
                if (participant == null || TextUtils.isEmpty(participant.userId)) {
                    continue;
                }

                DocumentReference userReference = db.collection(USERS_COLLECTION).document(participant.userId);
                User user = readUser(transaction, userReference, participant.userId);
                usersToRefund.put(participant.userId, user);
            }

            for (ChallengeParticipant participant : participants) {
                if (participant == null || TextUtils.isEmpty(participant.userId)) {
                    continue;
                }

                User user = usersToRefund.get(participant.userId);
                if (user == null) {
                    continue;
                }

                refundStake(user, challenge.stakeTokens, challenge.stakeStars);
                user.lastSeenAt = now;
                DocumentReference userReference = db.collection(USERS_COLLECTION).document(participant.userId);
                transaction.set(userReference, buildUserUpdates(user), com.google.firebase.firestore.SetOptions.merge());
            }

            challenge.status = RegionChallenge.STATUS_CANCELED;
            challenge.updatedAt = now;
            transaction.set(challengeReference, challenge);
            return null;
        }).addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void submitChallengeScore(
            String challengeId,
            int score,
            FirebaseCallback<Void> callback
    ) {
        String userId;
        try {
            userId = requireUserId();
        } catch (IllegalStateException exception) {
            callback.onError(exception.getMessage());
            return;
        }

        if (TextUtils.isEmpty(challengeId)) {
            callback.onError("Challenge was not found.");
            return;
        }

        DocumentReference challengeReference = db.collection(CHALLENGES_COLLECTION).document(challengeId);

        db.runTransaction(transaction -> {
            RegionChallenge challenge = readChallenge(transaction, challengeReference);
            if (!RegionChallenge.STATUS_READY.equals(challenge.status)
                    && !RegionChallenge.STATUS_IN_PROGRESS.equals(challenge.status)) {
                throw new IllegalStateException("This challenge is not ready for score submission.");
            }

            List<ChallengeParticipant> participants = safeParticipants(challenge.participants);
            ChallengeParticipant currentParticipant = findParticipant(participants, userId);
            if (currentParticipant == null) {
                throw new IllegalStateException("You are not part of this challenge.");
            }

            if (currentParticipant.played) {
                throw new IllegalStateException("Your score was already submitted.");
            }

            long now = System.currentTimeMillis();
            currentParticipant.score = Math.max(0, score);
            currentParticipant.played = true;
            currentParticipant.completedAt = now;
            currentParticipant.placement = 0;
            currentParticipant.rewardStars = 0;
            currentParticipant.rewardTokens = 0;

            challenge.updatedAt = now;
            challenge.status = RegionChallenge.STATUS_IN_PROGRESS;

            if (allParticipantsPlayed(participants)) {
                List<ChallengeParticipant> ranking = new ArrayList<>(participants);
                ranking.sort(resultComparator());

                int totalTokens = challenge.stakeTokens * ranking.size();
                int totalStars = challenge.stakeStars * ranking.size();
                int winnerRewardTokens = (totalTokens * 75) / 100;
                int winnerRewardStars = (totalStars * 75) / 100;
                int runnerUpRewardTokens = challenge.stakeTokens;
                int runnerUpRewardStars = challenge.stakeStars;

                for (int index = 0; index < ranking.size(); index++) {
                    ChallengeParticipant participant = ranking.get(index);
                    participant.placement = index + 1;
                    participant.rewardTokens = 0;
                    participant.rewardStars = 0;
                }

                if (!ranking.isEmpty()) {
                    ranking.get(0).rewardTokens = winnerRewardTokens;
                    ranking.get(0).rewardStars = winnerRewardStars;
                }

                if (ranking.size() > 1) {
                    ranking.get(1).rewardTokens = runnerUpRewardTokens;
                    ranking.get(1).rewardStars = runnerUpRewardStars;
                }

                applyRewardsToParticipantUsers(transaction, ranking);

                challenge.status = RegionChallenge.STATUS_FINISHED;
                challenge.winnerRewardTokens = winnerRewardTokens;
                challenge.winnerRewardStars = winnerRewardStars;
                challenge.runnerUpRewardTokens = runnerUpRewardTokens;
                challenge.runnerUpRewardStars = runnerUpRewardStars;
            }

            challenge.participants = participants;
            transaction.set(challengeReference, challenge);
            return null;
        }).addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    private void applyRewardsToParticipantUsers(
            Transaction transaction,
            List<ChallengeParticipant> ranking
    ) {
        long now = System.currentTimeMillis();
        Map<String, User> rewardedUsers = new HashMap<>();

        for (ChallengeParticipant participant : ranking) {
            if (participant.rewardStars <= 0 && participant.rewardTokens <= 0) {
                continue;
            }

            DocumentReference userReference = db.collection(USERS_COLLECTION).document(participant.userId);
            User user = readUser(transaction, userReference, participant.userId);
            rewardedUsers.put(participant.userId, user);
        }

        for (ChallengeParticipant participant : ranking) {
            if (participant.rewardStars <= 0 && participant.rewardTokens <= 0) {
                continue;
            }

            User user = rewardedUsers.get(participant.userId);
            if (user == null) {
                continue;
            }

            applyReward(user, participant.rewardTokens, participant.rewardStars);
            user.lastSeenAt = now;
            DocumentReference userReference = db.collection(USERS_COLLECTION).document(participant.userId);
            transaction.set(userReference, buildUserUpdates(user), com.google.firebase.firestore.SetOptions.merge());
        }
    }

    private User readUser(Transaction transaction, DocumentReference reference, String userId) {
        DocumentSnapshot snapshot;
        try {
            snapshot = transaction.get(reference);
        } catch (com.google.firebase.firestore.FirebaseFirestoreException exception) {
            throw new IllegalStateException(exception.getMessage());
        }
        User user = snapshot.toObject(User.class);
        if (user == null) {
            throw new IllegalStateException("User profile was not found.");
        }

        ensureUserDefaults(user, userId);
        return user;
    }

    private RegionChallenge readChallenge(Transaction transaction, DocumentReference reference) {
        DocumentSnapshot snapshot;
        try {
            snapshot = transaction.get(reference);
        } catch (com.google.firebase.firestore.FirebaseFirestoreException exception) {
            throw new IllegalStateException(exception.getMessage());
        }
        RegionChallenge challenge = snapshot.toObject(RegionChallenge.class);
        if (challenge == null) {
            throw new IllegalStateException("Challenge was not found.");
        }

        ensureChallengeDefaults(challenge, snapshot.getId());
        return challenge;
    }

    private void ensureUserDefaults(User user, String userId) {
        if (TextUtils.isEmpty(user.id)) {
            user.id = userId;
        }

        if (TextUtils.isEmpty(user.username)) {
            user.username = "Player";
        }

        if (TextUtils.isEmpty(user.region)) {
            user.region = "Serbia";
        } else {
            user.region = FirestoreRepository.canonicalRegionName(user.region);
        }

        if (TextUtils.isEmpty(user.lastTokenGrantDate)) {
            user.lastTokenGrantDate = LocalDate.now().toString();
        }

        if (TextUtils.isEmpty(user.lastDailyTokenRewardDate)) {
            user.lastDailyTokenRewardDate = user.lastTokenGrantDate;
        }

        if (user.earnedStarsSinceLastToken < 0) {
            user.earnedStarsSinceLastToken = 0;
        }

        user.tokens = Math.max(0, user.tokens);
        user.stars = Math.max(0, user.stars);
        user.league = LeagueUtils.calculateLeague(user.stars);
    }

    private void ensureChallengeDefaults(RegionChallenge challenge, String challengeId) {
        if (challenge == null) {
            return;
        }

        if (TextUtils.isEmpty(challenge.id)) {
            challenge.id = challengeId;
        }

        if (TextUtils.isEmpty(challenge.creatorName)) {
            challenge.creatorName = "Player";
        }

        if (TextUtils.isEmpty(challenge.creatorRegion)) {
            challenge.creatorRegion = "";
        }

        if (challenge.requiredPlayers <= 0) {
            challenge.requiredPlayers = REQUIRED_PLAYERS;
        }

        if (TextUtils.isEmpty(challenge.status)) {
            challenge.status = RegionChallenge.STATUS_OPEN;
        }

        if (challenge.participants == null) {
            challenge.participants = new ArrayList<>();
        }
    }

    private Map<String, Object> buildUserUpdates(User user) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("tokens", user.tokens);
        updates.put("stars", user.stars);
        updates.put("league", user.league);
        updates.put("earnedStarsSinceLastToken", user.earnedStarsSinceLastToken);
        updates.put("lastTokenGrantDate", user.lastTokenGrantDate);
        updates.put("lastDailyTokenRewardDate", user.lastDailyTokenRewardDate);
        updates.put("lastSeenAt", user.lastSeenAt);
        return updates;
    }

    private void applyStakeDeduction(User user, int stakeTokens, int stakeStars) {
        user.tokens = Math.max(0, user.tokens - stakeTokens);
        user.stars = Math.max(0, user.stars - stakeStars);
        user.league = LeagueUtils.calculateLeague(user.stars);
    }

    private void refundStake(User user, int stakeTokens, int stakeStars) {
        user.tokens += Math.max(0, stakeTokens);
        user.stars += Math.max(0, stakeStars);
        user.league = LeagueUtils.calculateLeague(user.stars);
    }

    private void applyReward(User user, int rewardTokens, int rewardStars) {
        user.tokens += Math.max(0, rewardTokens);

        if (rewardStars > 0) {
            user.stars += rewardStars;
            user.earnedStarsSinceLastToken += rewardStars;
            user.tokens += user.earnedStarsSinceLastToken / 50;
            user.earnedStarsSinceLastToken = user.earnedStarsSinceLastToken % 50;
        }

        user.league = LeagueUtils.calculateLeague(user.stars);
    }

    private void assertStakeAvailable(User user, int stakeTokens, int stakeStars) {
        if (user.tokens < stakeTokens) {
            throw new IllegalStateException("You do not have enough tokens for this challenge.");
        }

        if (user.stars < stakeStars) {
            throw new IllegalStateException("You do not have enough stars for this challenge.");
        }
    }

    private String validateStake(int stakeTokens, int stakeStars) {
        if (stakeTokens < 0 || stakeTokens > MAX_STAKE_TOKENS) {
            return "Challenge stake can use up to 2 tokens.";
        }

        if (stakeStars < 0 || stakeStars > MAX_STAKE_STARS) {
            return "Challenge stake can use up to 10 stars.";
        }

        if (stakeTokens == 0 && stakeStars == 0) {
            return "Enter at least one token or one star for the challenge.";
        }

        return null;
    }

    private String safeUserName(User user) {
        return user == null || TextUtils.isEmpty(user.username)
                ? "Player"
                : user.username.trim();
    }

    private boolean containsParticipant(List<ChallengeParticipant> participants, String userId) {
        return findParticipant(participants, userId) != null;
    }

    private ChallengeParticipant findParticipant(List<ChallengeParticipant> participants, String userId) {
        if (participants == null || TextUtils.isEmpty(userId)) {
            return null;
        }

        for (ChallengeParticipant participant : participants) {
            if (participant != null && userId.equals(participant.userId)) {
                return participant;
            }
        }

        return null;
    }

    private boolean hasPlayedParticipant(List<ChallengeParticipant> participants) {
        for (ChallengeParticipant participant : safeParticipants(participants)) {
            if (participant.played) {
                return true;
            }
        }
        return false;
    }

    private boolean allParticipantsPlayed(List<ChallengeParticipant> participants) {
        if (participants == null || participants.size() < REQUIRED_PLAYERS) {
            return false;
        }

        for (ChallengeParticipant participant : participants) {
            if (participant == null || !participant.played) {
                return false;
            }
        }

        return true;
    }

    private List<ChallengeParticipant> safeParticipants(List<ChallengeParticipant> source) {
        List<ChallengeParticipant> participants = source == null ? new ArrayList<>() : source;
        for (ChallengeParticipant participant : participants) {
            if (participant == null) {
                continue;
            }

            if (TextUtils.isEmpty(participant.username)) {
                participant.username = "Player";
            }
        }
        return participants;
    }

    private Comparator<RegionChallenge> challengeComparator() {
        return (left, right) -> {
            int leftPriority = statusPriority(left.status);
            int rightPriority = statusPriority(right.status);
            if (leftPriority != rightPriority) {
                return Integer.compare(leftPriority, rightPriority);
            }

            return Long.compare(right.updatedAt, left.updatedAt);
        };
    }

    private Comparator<ChallengeParticipant> resultComparator() {
        return (left, right) -> {
            int scoreCompare = Integer.compare(right.score, left.score);
            if (scoreCompare != 0) {
                return scoreCompare;
            }

            int completionCompare = Long.compare(left.completedAt, right.completedAt);
            if (completionCompare != 0) {
                return completionCompare;
            }

            int joinedCompare = Long.compare(left.joinedAt, right.joinedAt);
            if (joinedCompare != 0) {
                return joinedCompare;
            }

            String leftName = left.username == null ? "" : left.username;
            String rightName = right.username == null ? "" : right.username;
            return leftName.compareToIgnoreCase(rightName);
        };
    }

    private int statusPriority(String status) {
        if (RegionChallenge.STATUS_OPEN.equals(status)) {
            return 0;
        }
        if (RegionChallenge.STATUS_READY.equals(status)) {
            return 1;
        }
        if (RegionChallenge.STATUS_IN_PROGRESS.equals(status)) {
            return 2;
        }
        if (RegionChallenge.STATUS_FINISHED.equals(status)) {
            return 3;
        }
        return 4;
    }

    private void applyDailyTokenGrantForTransaction(User user) {
        if (user == null || TextUtils.isEmpty(user.lastTokenGrantDate)) {
            return;
        }

        try {
            LocalDate lastGrantDate = LocalDate.parse(user.lastTokenGrantDate);
            LocalDate today = LocalDate.now();
            long dayDiff = ChronoUnit.DAYS.between(lastGrantDate, today);
            if (dayDiff > 0) {
                user.tokens += (int) (dayDiff * LeagueUtils.getDailyTokenGrant(user.league));
                user.lastTokenGrantDate = today.toString();
                user.lastDailyTokenRewardDate = today.toString();
            }
        } catch (Exception ignored) {
            String today = LocalDate.now().toString();
            user.lastTokenGrantDate = today;
            user.lastDailyTokenRewardDate = today;
        }
    }

    private String requireUserId() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            throw new IllegalStateException("User is not logged in.");
        }

        return FirebaseAuth.getInstance().getCurrentUser().getUid();
    }
}
