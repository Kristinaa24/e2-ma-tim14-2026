package com.tim14.slagalica.repository;

import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Transaction;
import com.tim14.slagalica.game.GameRound;
import com.tim14.slagalica.model.AsocijacijeRound;
import com.tim14.slagalica.model.KorakPoKorakRound;
import com.tim14.slagalica.model.KoZnaZnaQuestion;
import com.tim14.slagalica.model.Notification;
import com.tim14.slagalica.model.PlayerStatistics;
import com.tim14.slagalica.model.SharedAsocijacijeRound;
import com.tim14.slagalica.model.SharedKorakPoKorakRound;
import com.tim14.slagalica.model.SharedMatchState;
import com.tim14.slagalica.model.SharedMojBrojRound;
import com.tim14.slagalica.model.SharedSkockoRound;
import com.tim14.slagalica.model.SharedSpojniceRound;
import com.tim14.slagalica.model.SpojniceRound;
import com.tim14.slagalica.model.User;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SharedMatchRepository {

    private static final String MATCHES_COLLECTION = "sharedMatches";
    private static final String USERS_COLLECTION = "users";
    private static final String STATISTICS_COLLECTION = "statistics";
    private static final long FRIENDLY_INVITE_EXPIRATION_MS = 24L * 60L * 60L * 1000L;

    private final FirebaseFirestore db;
    private final FirestoreRepository firestoreRepository;
    private final LocalGameRepository localGameRepository;

    public SharedMatchRepository() {
        db = FirebaseFirestore.getInstance();
        firestoreRepository = new FirestoreRepository();
        localGameRepository = new LocalGameRepository();
    }

    public void startCompetitiveMatchmaking(FirebaseCallback<MatchJoinResult> callback) {
        String userId;
        try {
            userId = requireUserId();
        } catch (IllegalStateException e) {
            callback.onError(e.getMessage());
            return;
        }

        firestoreRepository.getCurrentUser(new FirebaseCallback<User>() {
            @Override
            public void onSuccess(User user) {
                if (user.tokens <= 0) {
                    callback.onError("You do not have enough tokens for a ranked match.");
                    return;
                }

                db.collection(MATCHES_COLLECTION)
                        .whereEqualTo("status", SharedMatchState.STATUS_WAITING)
                        .whereEqualTo("matchType", SharedMatchState.MATCH_TYPE_COMPETITIVE)
                        .limit(10)
                        .get()
                        .addOnSuccessListener(querySnapshot -> {
                            for (DocumentSnapshot snapshot : querySnapshot.getDocuments()) {
                                SharedMatchState state = snapshot.toObject(SharedMatchState.class);
                                if (state == null) {
                                    continue;
                                }

                                if (userId.equals(state.playerOneId)
                                        || !TextUtils.isEmpty(state.playerTwoId)) {
                                    continue;
                                }

                                joinCompetitiveWaitingMatch(snapshot, user, callback);
                                return;
                            }

                            createCompetitiveWaitingMatch(user, callback);
                        })
                        .addOnFailureListener(e -> callback.onError(e.getMessage()));
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    public void createFriendlyInvite(
            String targetUserId,
            FirebaseCallback<MatchJoinResult> callback
    ) {
        String currentUserId;
        try {
            currentUserId = requireUserId();
        } catch (IllegalStateException e) {
            callback.onError(e.getMessage());
            return;
        }

        if (TextUtils.isEmpty(targetUserId) || currentUserId.equals(targetUserId)) {
            callback.onError("Select a valid friend.");
            return;
        }

        firestoreRepository.getCurrentUser(new FirebaseCallback<User>() {
            @Override
            public void onSuccess(User sender) {
                firestoreRepository.getUserById(targetUserId, new FirebaseCallback<User>() {
                    @Override
                    public void onSuccess(User targetUser) {
                        if (!targetUser.loggedIn) {
                            callback.onError("This friend is not logged in right now.");
                            return;
                        }

                        if (!TextUtils.isEmpty(targetUser.currentMatchId)) {
                            callback.onError("This friend is already in a match.");
                            return;
                        }

                        DocumentReference reference = db.collection(MATCHES_COLLECTION).document();
                        SharedMatchState state = buildInitialState(
                                reference.getId(),
                                currentUserId,
                                safeUserName(sender),
                                SharedMatchState.MATCH_TYPE_FRIENDLY
                        );
                        state.playerTwoId = targetUserId;
                        state.playerTwoName = safeUserName(targetUser);
                        state.phaseMessage = "Waiting for your friend to accept the invite.";

                        reference.set(state)
                                .addOnSuccessListener(unused -> {
                                    Map<String, Object> extras = new HashMap<>();
                                    extras.put("senderId", currentUserId);
                                    extras.put("senderName", safeUserName(sender));
                                    extras.put("relatedMatchId", reference.getId());
                                    extras.put("invitationStatus", "PENDING");
                                    extras.put("friendlyMatch", true);
                                    extras.put("expiresAt", System.currentTimeMillis() + FRIENDLY_INVITE_EXPIRATION_MS);

                                    firestoreRepository.saveNotificationForUser(
                                            targetUserId,
                                            "Friendly match invite",
                                            safeUserName(sender) + " invited you to a friendly match.",
                                            "INVITE",
                                            extras
                                    );

                                    callback.onSuccess(new MatchJoinResult(
                                            reference.getId(),
                                            1,
                                            state.roomCode,
                                            true,
                                            true
                                    ));
                                })
                                .addOnFailureListener(e -> callback.onError(e.getMessage()));
                    }

                    @Override
                    public void onError(String error) {
                        callback.onError(error);
                    }
                });
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    public void acceptFriendlyInvite(
            Notification notification,
            FirebaseCallback<MatchJoinResult> callback
    ) {
        String currentUserId;
        try {
            currentUserId = requireUserId();
        } catch (IllegalStateException e) {
            callback.onError(e.getMessage());
            return;
        }

        if (notification == null || !notification.isPendingInvite()) {
            callback.onError("This invite is no longer available.");
            return;
        }

        DocumentReference matchReference =
                db.collection(MATCHES_COLLECTION).document(notification.relatedMatchId);
        DocumentReference notificationReference =
                db.collection("notifications").document(notification.id);
        DocumentReference invitedUserReference =
                db.collection(USERS_COLLECTION).document(currentUserId);

        db.runTransaction((Transaction.Function<MatchJoinResult>) transaction -> {
            DocumentSnapshot matchSnapshot = transaction.get(matchReference);
            SharedMatchState state = matchSnapshot.toObject(SharedMatchState.class);

            if (state == null
                    || !SharedMatchState.MATCH_TYPE_FRIENDLY.equals(state.matchType)
                    || !SharedMatchState.STATUS_WAITING.equals(state.status)
                    || !currentUserId.equals(state.playerTwoId)) {
                throw new IllegalStateException("This invite has already expired.");
            }

            if (notification.expiresAt > 0 && System.currentTimeMillis() > notification.expiresAt) {
                transaction.update(matchReference, buildCanceledUpdates(
                        SharedMatchState.STATUS_DECLINED,
                        "The invite expired before it was accepted."
                ));
                transaction.update(notificationReference,
                        new HashMap<String, Object>() {{
                            put("invitationStatus", "EXPIRED");
                            put("read", true);
                        }});
                throw new IllegalStateException("This invite has expired.");
            }

            DocumentSnapshot invitedUserSnapshot = transaction.get(invitedUserReference);
            User invitedUser = invitedUserSnapshot.toObject(User.class);
            if (invitedUser == null) {
                throw new IllegalStateException("Your profile could not be loaded.");
            }

            ensureUserDefaultsForTransaction(invitedUser, currentUserId);
            if (!TextUtils.isEmpty(invitedUser.currentMatchId)
                    && !notification.relatedMatchId.equals(invitedUser.currentMatchId)) {
                throw new IllegalStateException("You are already in another match.");
            }

            invitedUser.currentMatchId = notification.relatedMatchId;
            invitedUser.lastSeenAt = System.currentTimeMillis();
            transaction.set(invitedUserReference, invitedUser);
            transaction.update(matchReference, buildJoinUpdates(currentUserId, safeUserName(invitedUser)));
            transaction.update(notificationReference,
                    new HashMap<String, Object>() {{
                        put("invitationStatus", "ACCEPTED");
                        put("read", true);
                    }});

            return new MatchJoinResult(
                    notification.relatedMatchId,
                    2,
                    state.roomCode,
                    false,
                    true
            );
        }).addOnSuccessListener(callback::onSuccess)
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void declineFriendlyInvite(Notification notification) {
        if (notification == null
                || TextUtils.isEmpty(notification.id)
                || TextUtils.isEmpty(notification.relatedMatchId)) {
            return;
        }

        firestoreRepository.updateNotificationInviteStatus(notification.id, "DECLINED", true);
        db.collection(MATCHES_COLLECTION)
                .document(notification.relatedMatchId)
                .update(buildCanceledUpdates(
                        SharedMatchState.STATUS_DECLINED,
                        "The friendly match invite was declined."
                ));
    }

    public void cancelPendingFriendlyInvite(String matchId) {
        if (TextUtils.isEmpty(matchId)) {
            return;
        }

        db.collection(MATCHES_COLLECTION)
                .document(matchId)
                .update(buildCanceledUpdates(
                        SharedMatchState.STATUS_CANCELED,
                        "The friendly match invite was canceled."
                ));
    }

    public void cancelWaitingCompetitiveMatch(String matchId) {
        if (TextUtils.isEmpty(matchId)) {
            return;
        }

        db.collection(MATCHES_COLLECTION)
                .document(matchId)
                .update(buildCanceledUpdates(
                        SharedMatchState.STATUS_CANCELED,
                        "The ranked search was canceled."
                ));
    }

    public void finalizeMatchIfNeeded(
            String matchId,
            FirebaseCallback<MatchFinalizationResult> callback
    ) {
        if (TextUtils.isEmpty(matchId)) {
            callback.onError("Match was not found.");
            return;
        }

        DocumentReference matchReference = db.collection(MATCHES_COLLECTION).document(matchId);

        db.runTransaction((Transaction.Function<MatchFinalizationResult>) transaction -> {
            DocumentSnapshot matchSnapshot = transaction.get(matchReference);
            SharedMatchState state = matchSnapshot.toObject(SharedMatchState.class);

            if (state == null) {
                throw new IllegalStateException("Match was not found.");
            }

            if (state.resultApplied) {
                return new MatchFinalizationResult(false, SharedMatchState.MATCH_TYPE_FRIENDLY.equals(state.matchType));
            }

            if (SharedMatchState.MATCH_TYPE_FRIENDLY.equals(state.matchType)) {
                transaction.update(matchReference, new HashMap<String, Object>() {{
                    put("resultApplied", true);
                    put("updatedAt", System.currentTimeMillis());
                }});
                return new MatchFinalizationResult(true, true);
            }

            DocumentReference playerOneUserReference =
                    db.collection(USERS_COLLECTION).document(state.playerOneId);
            DocumentReference playerTwoUserReference =
                    db.collection(USERS_COLLECTION).document(state.playerTwoId);
            DocumentReference playerOneStatsReference =
                    db.collection(STATISTICS_COLLECTION).document(state.playerOneId);
            DocumentReference playerTwoStatsReference =
                    db.collection(STATISTICS_COLLECTION).document(state.playerTwoId);

            User playerOne = transaction.get(playerOneUserReference).toObject(User.class);
            User playerTwo = transaction.get(playerTwoUserReference).toObject(User.class);
            PlayerStatistics playerOneStatistics =
                    transaction.get(playerOneStatsReference).toObject(PlayerStatistics.class);
            PlayerStatistics playerTwoStatistics =
                    transaction.get(playerTwoStatsReference).toObject(PlayerStatistics.class);

            if (playerOne == null || playerTwo == null) {
                throw new IllegalStateException("Players could not be loaded.");
            }

            ensureUserDefaultsForTransaction(playerOne, state.playerOneId);
            ensureUserDefaultsForTransaction(playerTwo, state.playerTwoId);
            applyDailyTokenGrantForTransaction(playerOne);
            applyDailyTokenGrantForTransaction(playerTwo);

            if (playerOneStatistics == null) {
                playerOneStatistics = new PlayerStatistics(state.playerOneId);
            }

            if (playerTwoStatistics == null) {
                playerTwoStatistics = new PlayerStatistics(state.playerTwoId);
            }

            boolean playerOneWon = state.playerOneScore > state.playerTwoScore;
            boolean playerTwoWon = state.playerTwoScore > state.playerOneScore;

            if (state.forfeitedPlayer == 1) {
                playerOneWon = false;
                playerTwoWon = true;
            } else if (state.forfeitedPlayer == 2) {
                playerOneWon = true;
                playerTwoWon = false;
            }

            boolean draw = !playerOneWon && !playerTwoWon;

            int playerOneStarDelta = calculateStarDelta(
                    state.playerOneScore,
                    playerOneWon,
                    !draw && !playerOneWon,
                    state.forfeitedPlayer == 1
            );
            int playerTwoStarDelta = calculateStarDelta(
                    state.playerTwoScore,
                    playerTwoWon,
                    !draw && !playerTwoWon,
                    state.forfeitedPlayer == 2
            );

            applyCompetitiveOutcome(playerOne, playerOneStarDelta);
            applyCompetitiveOutcome(playerTwo, playerTwoStarDelta);
            playerOne.currentMatchId = "";
            playerTwo.currentMatchId = "";

            playerOneStatistics.gamesPlayed++;
            playerTwoStatistics.gamesPlayed++;

            if (playerOneWon) {
                playerOneStatistics.wins++;
                playerTwoStatistics.losses++;
            } else if (playerTwoWon) {
                playerTwoStatistics.wins++;
                playerOneStatistics.losses++;
            }

            transaction.set(playerOneUserReference, playerOne);
            transaction.set(playerTwoUserReference, playerTwo);
            transaction.set(playerOneStatsReference, playerOneStatistics);
            transaction.set(playerTwoStatsReference, playerTwoStatistics);
            transaction.update(matchReference, new HashMap<String, Object>() {{
                put("resultApplied", true);
                put("updatedAt", System.currentTimeMillis());
            }});

            return new MatchFinalizationResult(true, false);
        }).addOnSuccessListener(callback::onSuccess)
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void createStudentOneMatch(FirebaseCallback<MatchJoinResult> callback) {
        String userId;
        try {
            userId = requireUserId();
        } catch (IllegalStateException e) {
            callback.onError(e.getMessage());
            return;
        }

        firestoreRepository.getCurrentUser(new FirebaseCallback<User>() {
            @Override
            public void onSuccess(User user) {
                DocumentReference reference = db.collection(MATCHES_COLLECTION).document();
                SharedMatchState state = buildInitialState(
                        reference.getId(),
                        userId,
                        safeUserName(user),
                        SharedMatchState.MATCH_TYPE_COMPETITIVE
                );

                reference.set(state)
                        .addOnSuccessListener(unused -> callback.onSuccess(
                                new MatchJoinResult(reference.getId(), 1, state.roomCode, true, false)
                        ))
                        .addOnFailureListener(e -> callback.onError(e.getMessage()));
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    public void joinStudentOneMatch(String rawRoomCode, FirebaseCallback<MatchJoinResult> callback) {
        String userId;
        try {
            userId = requireUserId();
        } catch (IllegalStateException e) {
            callback.onError(e.getMessage());
            return;
        }
        String roomCode = rawRoomCode == null ? "" : rawRoomCode.trim().toUpperCase(Locale.US);

        if (TextUtils.isEmpty(roomCode)) {
            callback.onError("Enter a room code.");
            return;
        }

        firestoreRepository.getCurrentUser(new FirebaseCallback<User>() {
            @Override
            public void onSuccess(User user) {
                db.collection(MATCHES_COLLECTION)
                        .whereEqualTo("roomCode", roomCode)
                        .limit(1)
                        .get()
                        .addOnSuccessListener(querySnapshot -> {
                            if (querySnapshot.isEmpty()) {
                                callback.onError("Room not found.");
                                return;
                            }

                            DocumentSnapshot snapshot = querySnapshot.getDocuments().get(0);
                            SharedMatchState state = snapshot.toObject(SharedMatchState.class);

                            if (state == null) {
                                callback.onError("Room data is invalid.");
                                return;
                            }

                            if (!TextUtils.isEmpty(state.playerTwoId)
                                    && !userId.equals(state.playerTwoId)
                                    && !userId.equals(state.playerOneId)) {
                                callback.onError("Room is already full.");
                                return;
                            }

                            int localPlayerNumber = userId.equals(state.playerOneId) ? 1 : 2;
                            if (localPlayerNumber == 2) {
                                updateMatchState(snapshot.getId(), buildJoinUpdates(userId, safeUserName(user)))
                                        .addOnSuccessListener(unused -> callback.onSuccess(
                                                new MatchJoinResult(snapshot.getId(), 2, state.roomCode, false, false)
                                        ))
                                        .addOnFailureListener(e -> callback.onError(e.getMessage()));
                                return;
                            }

                            callback.onSuccess(new MatchJoinResult(snapshot.getId(), 1, state.roomCode, false, false));
                        })
                        .addOnFailureListener(e -> callback.onError(e.getMessage()));
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    public ListenerRegistration listenToMatch(
            String matchId,
            FirebaseCallback<SharedMatchState> callback
    ) {
        return db.collection(MATCHES_COLLECTION)
                .document(matchId)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        callback.onError(error.getMessage());
                        return;
                    }

                    if (snapshot == null || !snapshot.exists()) {
                        callback.onError("Match was not found.");
                        return;
                    }

                    SharedMatchState state = snapshot.toObject(SharedMatchState.class);
                    if (state == null) {
                        callback.onError("Match data is invalid.");
                        return;
                    }

                    callback.onSuccess(state);
                });
    }

    public com.google.android.gms.tasks.Task<Void> updateMatchState(
            String matchId,
            @NonNull Map<String, Object> updates
    ) {
        return db.collection(MATCHES_COLLECTION)
                .document(matchId)
                .update(updates);
    }

    public void resetStudentOneMatch(String matchId, SharedMatchState previousState) {
        if (previousState == null) {
            return;
        }

        SharedMatchState resetState = buildInitialState(
                matchId,
                previousState.playerOneId,
                previousState.playerOneName,
                previousState.matchType
        );

        resetState.playerTwoId = previousState.playerTwoId;
        resetState.playerTwoName = previousState.playerTwoName;
        resetState.status = SharedMatchState.STATUS_ACTIVE;
        resetState.currentRound = GameRound.KO_ZNA_ZNA.name();
        resetState.phase = SharedMatchState.PHASE_KZZ_QUESTION;
        resetState.activePlayer = 0;
        resetState.currentTurnIndex = 0;
        resetState.phaseStartedAt = System.currentTimeMillis();
        resetState.phaseDurationSeconds = 5;
        resetState.phaseMessage = "Question 1/5. Both players can answer.";
        resetState.updatedAt = System.currentTimeMillis();

        db.collection(MATCHES_COLLECTION)
                .document(matchId)
                .set(resetState);
    }

    public void submitKoZnaZnaAnswer(
            String matchId,
            int questionIndex,
            int playerNumber,
            int selectedIndex,
            FirebaseCallback<KoZnaZnaAnswerOutcome> callback
    ) {
        DocumentReference matchReference = db.collection(MATCHES_COLLECTION).document(matchId);

        db.runTransaction((Transaction.Function<KoZnaZnaAnswerOutcome>) transaction -> {
            DocumentSnapshot snapshot = transaction.get(matchReference);
            SharedMatchState state = snapshot.toObject(SharedMatchState.class);

            if (state == null
                    || !GameRound.KO_ZNA_ZNA.name().equals(state.currentRound)
                    || !SharedMatchState.PHASE_KZZ_QUESTION.equals(state.phase)
                    || state.currentTurnIndex != questionIndex
                    || state.quizQuestions == null
                    || questionIndex < 0
                    || questionIndex >= state.quizQuestions.size()) {
                return new KoZnaZnaAnswerOutcome(false, false, 0);
            }

            KoZnaZnaQuestion question = state.quizQuestions.get(questionIndex);
            boolean correct = question.correctIndex == selectedIndex;
            int points = correct ? 10 : -5;
            long now = System.currentTimeMillis();
            Map<String, Object> updates = new HashMap<>();

            if (playerNumber == 1) {
                updates.put("playerOneScore", state.playerOneScore + points);
            } else {
                updates.put("playerTwoScore", state.playerTwoScore + points);
            }

            updates.put("phase", SharedMatchState.PHASE_KZZ_REVEAL);
            updates.put("activePlayer", 0);
            updates.put("answeredByPlayer", playerNumber);
            updates.put("selectedAnswerIndex", selectedIndex);
            updates.put("phaseStartedAt", now);
            updates.put("phaseDurationSeconds", 1);
            updates.put(
                    "phaseMessage",
                    correct
                            ? "Question resolved. Player " + playerNumber + " answered correctly."
                            : "Question resolved. Player " + playerNumber + " answered incorrectly."
            );
            updates.put("updatedAt", now);
            transaction.update(matchReference, updates);
            return new KoZnaZnaAnswerOutcome(true, correct, points);
        }).addOnSuccessListener(callback::onSuccess)
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void resolveKoZnaZnaTimeout(
            String matchId,
            int questionIndex,
            FirebaseCallback<Boolean> callback
    ) {
        DocumentReference matchReference = db.collection(MATCHES_COLLECTION).document(matchId);

        db.runTransaction((Transaction.Function<Boolean>) transaction -> {
            DocumentSnapshot snapshot = transaction.get(matchReference);
            SharedMatchState state = snapshot.toObject(SharedMatchState.class);

            if (state == null
                    || !GameRound.KO_ZNA_ZNA.name().equals(state.currentRound)
                    || !SharedMatchState.PHASE_KZZ_QUESTION.equals(state.phase)
                    || state.currentTurnIndex != questionIndex) {
                return false;
            }

            long now = System.currentTimeMillis();
            Map<String, Object> updates = new HashMap<>();
            updates.put("phase", SharedMatchState.PHASE_KZZ_REVEAL);
            updates.put("activePlayer", 0);
            updates.put("answeredByPlayer", 0);
            updates.put("selectedAnswerIndex", -1);
            updates.put("phaseStartedAt", now);
            updates.put("phaseDurationSeconds", 1);
            updates.put("phaseMessage", "Question resolved. No one answered in time.");
            updates.put("updatedAt", now);
            transaction.update(matchReference, updates);
            return true;
        }).addOnSuccessListener(callback::onSuccess)
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    private SharedMatchState buildInitialState(
            String documentId,
            String playerOneId,
            String playerOneName,
            String matchType
    ) {
        SharedMatchState state = new SharedMatchState();
        state.roomCode = documentId.substring(0, Math.min(6, documentId.length())).toUpperCase(Locale.US);
        state.status = SharedMatchState.STATUS_WAITING;
        state.matchType = matchType;
        state.currentRound = GameRound.KO_ZNA_ZNA.name();
        state.phase = SharedMatchState.PHASE_WAITING;
        state.playerOneId = playerOneId;
        state.playerOneName = playerOneName;
        state.playerTwoId = "";
        state.playerTwoName = "Waiting...";
        state.activePlayer = 0;
        state.currentTurnIndex = 0;
        state.playerOneScore = 0;
        state.playerTwoScore = 0;
        state.phaseStartedAt = System.currentTimeMillis();
        state.phaseDurationSeconds = 0;
        state.phaseMessage = "Waiting for player 2 to join.";
        state.revealedAnswer = "";
        state.playerOneExpression = "";
        state.playerTwoExpression = "";
        state.answeredByPlayer = 0;
        state.selectedAnswerIndex = -1;
        state.resultApplied = false;
        state.forfeitedPlayer = 0;
        state.rematchRequestedBy = 0;
        state.rematchDeclinedBy = 0;
        state.matchStartedAt = 0L;
        state.updatedAt = System.currentTimeMillis();

        for (KoZnaZnaQuestion question : localGameRepository.getKoZnaZnaMatchQuestions()) {
            state.quizQuestions.add(new KoZnaZnaQuestion(
                    question.id,
                    question.question,
                    new ArrayList<>(question.answers),
                    question.correctIndex
            ));
        }

        List<SpojniceRound> spojniceSourceRounds = localGameRepository.getSpojniceMatchRounds();
        for (int roundIndex = 0; roundIndex < spojniceSourceRounds.size(); roundIndex++) {
            SpojniceRound round = spojniceSourceRounds.get(roundIndex);
            SharedSpojniceRound sharedRound = new SharedSpojniceRound();
            sharedRound.title = round.getTitle();
            sharedRound.leftItems = new ArrayList<>(round.getLeftItems());
            sharedRound.correctRightItems = new ArrayList<>(round.getCorrectRightItems());
            sharedRound.displayedRightItems = new ArrayList<>(round.getDisplayedRightItems());
            sharedRound.starterPlayer = roundIndex == 0 ? 1 : 2;
            sharedRound.currentPlayer = sharedRound.starterPlayer;
            state.spojniceRounds.add(sharedRound);
        }

        for (int roundIndex = 0; roundIndex < 2; roundIndex++) {
            SharedSkockoRound sharedRound = new SharedSkockoRound();
            sharedRound.secretCombination = new ArrayList<>(
                    localGameRepository.generateSkockoSecretCombination()
            );
            sharedRound.starterPlayer = roundIndex == 0 ? 1 : 2;
            state.skockoRounds.add(sharedRound);
        }

        List<AsocijacijeRound> asocijacijeSourceRounds = localGameRepository.getAsocijacijeRounds();
        for (int roundIndex = 0; roundIndex < asocijacijeSourceRounds.size(); roundIndex++) {
            AsocijacijeRound sourceRound = asocijacijeSourceRounds.get(roundIndex);
            SharedAsocijacijeRound sharedRound = new SharedAsocijacijeRound();
            sharedRound.columnASolution = sourceRound.columnA_solution;
            sharedRound.columnAClues = new ArrayList<>(sourceRound.columnA_clues);
            sharedRound.columnBSolution = sourceRound.columnB_solution;
            sharedRound.columnBClues = new ArrayList<>(sourceRound.columnB_clues);
            sharedRound.columnCSolution = sourceRound.columnC_solution;
            sharedRound.columnCClues = new ArrayList<>(sourceRound.columnC_clues);
            sharedRound.columnDSolution = sourceRound.columnD_solution;
            sharedRound.columnDClues = new ArrayList<>(sourceRound.columnD_clues);
            sharedRound.finalSolution = sourceRound.finalSolution;
            sharedRound.currentPlayer = roundIndex == 0 ? 1 : 2;

            for (int columnIndex = 0; columnIndex < 4; columnIndex++) {
                sharedRound.columnSolved.add(false);
                sharedRound.columnSolvers.add(0);
                sharedRound.openedFields.put(
                        String.valueOf(columnIndex),
                        new ArrayList<>(Arrays.asList(false, false, false, false))
                );
            }

            state.asocijacijeRounds.add(sharedRound);
        }

        for (KorakPoKorakRound round : localGameRepository.getKorakPoKorakMatchRounds()) {
            state.korakRounds.add(new SharedKorakPoKorakRound(
                    round.getAnswer(),
                    Arrays.asList(round.getClues())
            ));
        }

        for (int roundIndex = 0; roundIndex < 2; roundIndex++) {
            int targetNumber = localGameRepository.generateTargetNumber();
            int[] offeredNumbers = localGameRepository.generateOfferedNumbers();
            List<Integer> offered = new ArrayList<>();
            for (int number : offeredNumbers) {
                offered.add(number);
            }
            state.myNumberRounds.add(new SharedMojBrojRound(targetNumber, offered));
        }

        return state;
    }

    private Map<String, Object> buildJoinUpdates(String playerTwoId, String playerTwoName) {
        return new HashMap<String, Object>() {{
            put("playerTwoId", playerTwoId);
            put("playerTwoName", playerTwoName);
            put("status", SharedMatchState.STATUS_ACTIVE);
            put("currentRound", GameRound.KO_ZNA_ZNA.name());
            put("phase", SharedMatchState.PHASE_KZZ_QUESTION);
            put("activePlayer", 0);
            put("currentTurnIndex", 0);
            put("phaseStartedAt", System.currentTimeMillis());
            put("phaseDurationSeconds", 5);
            put("phaseMessage", "Question 1/5. Both players can answer.");
            put("revealedAnswer", "");
            put("playerOneExpression", "");
            put("playerTwoExpression", "");
            put("answeredByPlayer", 0);
            put("selectedAnswerIndex", -1);
            put("resultApplied", false);
            put("forfeitedPlayer", 0);
            put("rematchRequestedBy", 0);
            put("rematchDeclinedBy", 0);
            put("matchStartedAt", System.currentTimeMillis());
            put("updatedAt", System.currentTimeMillis());
        }};
    }

    private void createCompetitiveWaitingMatch(
            User user,
            FirebaseCallback<MatchJoinResult> callback
    ) {
        DocumentReference reference = db.collection(MATCHES_COLLECTION).document();
        SharedMatchState state = buildInitialState(
                reference.getId(),
                user.id,
                safeUserName(user),
                SharedMatchState.MATCH_TYPE_COMPETITIVE
        );
        state.phaseMessage = "Searching for an opponent...";

        reference.set(state)
                .addOnSuccessListener(unused -> callback.onSuccess(
                        new MatchJoinResult(reference.getId(), 1, state.roomCode, true, false)
                ))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    private void joinCompetitiveWaitingMatch(
            DocumentSnapshot waitingSnapshot,
            User joiningUser,
            FirebaseCallback<MatchJoinResult> callback
    ) {
        String joiningUserId = joiningUser == null ? "" : joiningUser.id;
        DocumentReference matchReference = waitingSnapshot.getReference();
        DocumentReference waitingPlayerReference =
                db.collection(USERS_COLLECTION).document(waitingSnapshot.getString("playerOneId"));
        DocumentReference joiningPlayerReference =
                db.collection(USERS_COLLECTION).document(joiningUserId);

        db.runTransaction((Transaction.Function<MatchJoinResult>) transaction -> {
            DocumentSnapshot latestMatchSnapshot = transaction.get(matchReference);
            SharedMatchState latestState = latestMatchSnapshot.toObject(SharedMatchState.class);

            if (latestState == null
                    || !SharedMatchState.STATUS_WAITING.equals(latestState.status)
                    || !SharedMatchState.MATCH_TYPE_COMPETITIVE.equals(latestState.matchType)
                    || !TextUtils.isEmpty(latestState.playerTwoId)
                    || joiningUserId.equals(latestState.playerOneId)) {
                throw new IllegalStateException("That ranked match is no longer available.");
            }

            User waitingPlayer = transaction.get(waitingPlayerReference).toObject(User.class);
            User currentJoiningPlayer = transaction.get(joiningPlayerReference).toObject(User.class);

            if (waitingPlayer == null || currentJoiningPlayer == null) {
                throw new IllegalStateException("Player data could not be loaded.");
            }

            ensureUserDefaultsForTransaction(waitingPlayer, latestState.playerOneId);
            ensureUserDefaultsForTransaction(currentJoiningPlayer, joiningUserId);
            applyDailyTokenGrantForTransaction(waitingPlayer);
            applyDailyTokenGrantForTransaction(currentJoiningPlayer);

            if (waitingPlayer.tokens <= 0) {
                throw new IllegalStateException("The opponent no longer has tokens for a ranked match.");
            }

            if (currentJoiningPlayer.tokens <= 0) {
                throw new IllegalStateException("You do not have enough tokens for a ranked match.");
            }

            waitingPlayer.tokens -= 1;
            currentJoiningPlayer.tokens -= 1;
            waitingPlayer.currentMatchId = latestMatchSnapshot.getId();
            currentJoiningPlayer.currentMatchId = latestMatchSnapshot.getId();
            waitingPlayer.lastSeenAt = System.currentTimeMillis();
            currentJoiningPlayer.lastSeenAt = System.currentTimeMillis();

            transaction.set(waitingPlayerReference, waitingPlayer);
            transaction.set(joiningPlayerReference, currentJoiningPlayer);
            transaction.update(matchReference, buildJoinUpdates(joiningUserId, safeUserName(currentJoiningPlayer)));

            return new MatchJoinResult(
                    latestMatchSnapshot.getId(),
                    2,
                    latestState.roomCode,
                    false,
                    false
            );
        }).addOnSuccessListener(callback::onSuccess)
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    private Map<String, Object> buildCanceledUpdates(String status, String message) {
        return new HashMap<String, Object>() {{
            put("status", status);
            put("phase", SharedMatchState.PHASE_RESULT);
            put("phaseMessage", message);
            put("updatedAt", System.currentTimeMillis());
        }};
    }

    private void ensureUserDefaultsForTransaction(User user, String userId) {
        if (user == null) {
            return;
        }

        if (TextUtils.isEmpty(user.id)) {
            user.id = userId;
        }

        if (TextUtils.isEmpty(user.username)) {
            user.username = "Player";
        }

        if (TextUtils.isEmpty(user.region)) {
            user.region = "Serbia";
        }

        if (TextUtils.isEmpty(user.avatar)) {
            user.avatar = "avatar_1";
        }

        if (TextUtils.isEmpty(user.qrCode)) {
            user.qrCode = userId;
        }

        if (TextUtils.isEmpty(user.lastTokenGrantDate)) {
            user.lastTokenGrantDate = java.time.LocalDate.now().toString();
        }

        if (user.earnedStarsSinceLastToken < 0) {
            user.earnedStarsSinceLastToken = 0;
        }

        if (user.currentMatchId == null) {
            user.currentMatchId = "";
        }
    }

    private int calculateStarDelta(
            int playerScore,
            boolean won,
            boolean lost,
            boolean forfeited
    ) {
        if (forfeited) {
            return 0;
        }

        int scoreBonus = Math.max(0, playerScore / 40);
        if (won) {
            return 10 + scoreBonus;
        }

        if (lost) {
            return -10 + scoreBonus;
        }

        return scoreBonus;
    }

    private void applyCompetitiveOutcome(User user, int starDelta) {
        user.stars = Math.max(0, user.stars + starDelta);

        if (starDelta > 0) {
            user.earnedStarsSinceLastToken += starDelta;
            user.tokens += user.earnedStarsSinceLastToken / 50;
            user.earnedStarsSinceLastToken = user.earnedStarsSinceLastToken % 50;
        }

        user.lastSeenAt = System.currentTimeMillis();
    }

    private void applyDailyTokenGrantForTransaction(User user) {
        if (user == null || TextUtils.isEmpty(user.lastTokenGrantDate)) {
            return;
        }

        try {
            java.time.LocalDate lastGrantDate = java.time.LocalDate.parse(user.lastTokenGrantDate);
            java.time.LocalDate today = java.time.LocalDate.now();
            long dayDiff = java.time.temporal.ChronoUnit.DAYS.between(lastGrantDate, today);
            if (dayDiff > 0) {
                user.tokens += (int) (dayDiff * 5);
                user.lastTokenGrantDate = today.toString();
            }
        } catch (Exception ignored) {
            user.lastTokenGrantDate = java.time.LocalDate.now().toString();
        }
    }

    private String requireUserId() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            throw new IllegalStateException("User is not logged in.");
        }
        return FirebaseAuth.getInstance().getCurrentUser().getUid();
    }

    private String safeUserName(User user) {
        if (user == null || TextUtils.isEmpty(user.username)) {
            return "Player";
        }
        return user.username;
    }

    public static final class KoZnaZnaAnswerOutcome {
        public final boolean accepted;
        public final boolean correct;
        public final int points;

        public KoZnaZnaAnswerOutcome(boolean accepted, boolean correct, int points) {
            this.accepted = accepted;
            this.correct = correct;
            this.points = points;
        }
    }

    public static final class MatchJoinResult {
        public final String matchId;
        public final int localPlayerNumber;
        public final String roomCode;
        public final boolean waitingForOpponent;
        public final boolean friendlyMatch;

        public MatchJoinResult(
                String matchId,
                int localPlayerNumber,
                String roomCode,
                boolean waitingForOpponent,
                boolean friendlyMatch
        ) {
            this.matchId = matchId;
            this.localPlayerNumber = localPlayerNumber;
            this.roomCode = roomCode;
            this.waitingForOpponent = waitingForOpponent;
            this.friendlyMatch = friendlyMatch;
        }
    }

    public static final class MatchFinalizationResult {
        public final boolean applied;
        public final boolean friendlyMatch;

        public MatchFinalizationResult(boolean applied, boolean friendlyMatch) {
            this.applied = applied;
            this.friendlyMatch = friendlyMatch;
        }
    }
}
