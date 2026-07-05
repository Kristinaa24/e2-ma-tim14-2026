package com.tim14.slagalica.repository;

import android.text.TextUtils;
import android.os.Handler;
import android.os.Looper;

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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SharedMatchRepository {

    private static final String MATCHES_COLLECTION = "sharedMatches";
    private static final String USERS_COLLECTION = "users";
    private static final String STATISTICS_COLLECTION = "statistics";
    private static final int REQUIRED_SHARED_QUIZ_QUESTIONS = 5;
    private static final int REQUIRED_SHARED_ROUNDS = 2;
    private static final long FRIENDLY_INVITE_EXPIRATION_MS = 10L * 1000L;
    private static final long COMPETITIVE_WAITING_EXPIRATION_MS = 2L * 60L * 1000L;
    private static final long TOURNAMENT_WAITING_EXPIRATION_MS = 10L * 60L * 1000L;


    private final FirebaseFirestore db;
    private final FirestoreRepository firestoreRepository;
    private final LocalGameRepository localGameRepository;

    private static final class MatchContentBundle {
        final List<KoZnaZnaQuestion> quizQuestions;
        final List<SpojniceRound> spojniceRounds;
        final List<AsocijacijeRound> asocijacijeRounds;
        final List<KorakPoKorakRound> korakRounds;

        private MatchContentBundle(
                List<KoZnaZnaQuestion> quizQuestions,
                List<SpojniceRound> spojniceRounds,
                List<AsocijacijeRound> asocijacijeRounds,
                List<KorakPoKorakRound> korakRounds
        ) {
            this.quizQuestions = quizQuestions == null ? new ArrayList<>() : quizQuestions;
            this.spojniceRounds = spojniceRounds == null ? new ArrayList<>() : spojniceRounds;
            this.asocijacijeRounds = asocijacijeRounds == null ? new ArrayList<>() : asocijacijeRounds;
            this.korakRounds = korakRounds == null ? new ArrayList<>() : korakRounds;
        }
    }

    public SharedMatchRepository() {
        db = FirebaseFirestore.getInstance();
        firestoreRepository = new FirestoreRepository();
        localGameRepository = new LocalGameRepository();
    }

    private MatchContentBundle createLocalMatchContentBundle() {
        return new MatchContentBundle(
                localGameRepository.getKoZnaZnaMatchQuestions(),
                localGameRepository.getSpojniceMatchRounds(),
                localGameRepository.getAsocijacijeRounds(),
                localGameRepository.getKorakPoKorakMatchRounds()
        );
    }

    private void loadMatchContentBundle(FirebaseCallback<MatchContentBundle> callback) {
        loadKoZnaZnaMatchQuestions(new FirebaseCallback<List<KoZnaZnaQuestion>>() {
            @Override
            public void onSuccess(List<KoZnaZnaQuestion> quizQuestions) {
                loadSpojniceMatchRounds(new FirebaseCallback<List<SpojniceRound>>() {
                    @Override
                    public void onSuccess(List<SpojniceRound> spojniceRounds) {
                        loadAsocijacijeMatchRounds(new FirebaseCallback<List<AsocijacijeRound>>() {
                            @Override
                            public void onSuccess(List<AsocijacijeRound> asocijacijeRounds) {
                                loadKorakPoKorakMatchRounds(new FirebaseCallback<List<KorakPoKorakRound>>() {
                                    @Override
                                    public void onSuccess(List<KorakPoKorakRound> korakRounds) {
                                        callback.onSuccess(new MatchContentBundle(
                                                quizQuestions,
                                                spojniceRounds,
                                                asocijacijeRounds,
                                                korakRounds
                                        ));
                                    }

                                    @Override
                                    public void onError(String error) {
                                        callback.onSuccess(createLocalMatchContentBundle());
                                    }
                                });
                            }

                            @Override
                            public void onError(String error) {
                                callback.onSuccess(createLocalMatchContentBundle());
                            }
                        });
                    }

                    @Override
                    public void onError(String error) {
                        callback.onSuccess(createLocalMatchContentBundle());
                    }
                });
            }

            @Override
            public void onError(String error) {
                callback.onSuccess(createLocalMatchContentBundle());
            }
        });
    }

    private void loadKoZnaZnaMatchQuestions(FirebaseCallback<List<KoZnaZnaQuestion>> callback) {
        firestoreRepository.getKoZnaZnaQuestions(new FirebaseCallback<List<KoZnaZnaQuestion>>() {
            @Override
            public void onSuccess(List<KoZnaZnaQuestion> result) {
                callback.onSuccess(selectKoZnaZnaMatchQuestions(result));
            }

            @Override
            public void onError(String error) {
                callback.onSuccess(localGameRepository.getKoZnaZnaMatchQuestions());
            }
        });
    }

    private List<KoZnaZnaQuestion> selectKoZnaZnaMatchQuestions(List<KoZnaZnaQuestion> sourceQuestions) {
        List<KoZnaZnaQuestion> availableQuestions = createKoZnaZnaQuestionCopies(sourceQuestions);
        if (availableQuestions.size() < REQUIRED_SHARED_QUIZ_QUESTIONS) {
            return localGameRepository.getKoZnaZnaMatchQuestions();
        }

        Collections.shuffle(availableQuestions);
        return new ArrayList<>(availableQuestions.subList(0, REQUIRED_SHARED_QUIZ_QUESTIONS));
    }

    private List<KoZnaZnaQuestion> createKoZnaZnaQuestionCopies(List<KoZnaZnaQuestion> sourceQuestions) {
        List<KoZnaZnaQuestion> copies = new ArrayList<>();
        if (sourceQuestions == null) {
            return copies;
        }

        for (KoZnaZnaQuestion question : sourceQuestions) {
            if (question == null
                    || TextUtils.isEmpty(question.id)
                    || TextUtils.isEmpty(question.question)
                    || question.answers == null
                    || question.answers.size() < 2
                    || question.correctIndex < 0
                    || question.correctIndex >= question.answers.size()) {
                continue;
            }

            copies.add(new KoZnaZnaQuestion(
                    question.id,
                    question.question,
                    new ArrayList<>(question.answers),
                    question.correctIndex
            ));
        }

        return copies;
    }

    private void loadSpojniceMatchRounds(FirebaseCallback<List<SpojniceRound>> callback) {
        firestoreRepository.getSpojniceRounds(new FirebaseCallback<List<SpojniceRound>>() {
            @Override
            public void onSuccess(List<SpojniceRound> result) {
                callback.onSuccess(selectSpojniceMatchRounds(result));
            }

            @Override
            public void onError(String error) {
                callback.onSuccess(localGameRepository.getSpojniceMatchRounds());
            }
        });
    }

    private List<SpojniceRound> selectSpojniceMatchRounds(List<SpojniceRound> sourceRounds) {
        List<SpojniceRound> availableRounds = createSpojniceRoundCopies(sourceRounds);
        if (availableRounds.size() < REQUIRED_SHARED_ROUNDS) {
            return localGameRepository.getSpojniceMatchRounds();
        }

        Collections.shuffle(availableRounds);
        return new ArrayList<>(availableRounds.subList(0, REQUIRED_SHARED_ROUNDS));
    }

    private List<SpojniceRound> createSpojniceRoundCopies(List<SpojniceRound> sourceRounds) {
        List<SpojniceRound> copies = new ArrayList<>();
        if (sourceRounds == null) {
            return copies;
        }

        for (SpojniceRound round : sourceRounds) {
            if (round == null
                    || TextUtils.isEmpty(round.getId())
                    || TextUtils.isEmpty(round.getTitle())
                    || round.getLeftItems() == null
                    || round.getLeftItems().isEmpty()
                    || round.getCorrectRightItems() == null
                    || round.getCorrectRightItems().isEmpty()
                    || round.getDisplayedRightItems() == null
                    || round.getDisplayedRightItems().isEmpty()) {
                continue;
            }

            copies.add(new SpojniceRound(
                    round.getId(),
                    round.getTitle(),
                    new ArrayList<>(round.getLeftItems()),
                    new ArrayList<>(round.getCorrectRightItems()),
                    new ArrayList<>(round.getDisplayedRightItems())
            ));
        }

        return copies;
    }

    private void loadAsocijacijeMatchRounds(FirebaseCallback<List<AsocijacijeRound>> callback) {
        firestoreRepository.getAsocijacijeRounds(new FirebaseCallback<List<AsocijacijeRound>>() {
            @Override
            public void onSuccess(List<AsocijacijeRound> result) {
                callback.onSuccess(selectAsocijacijeMatchRounds(result));
            }

            @Override
            public void onError(String error) {
                callback.onSuccess(localGameRepository.getAsocijacijeRounds());
            }
        });
    }

    private List<AsocijacijeRound> selectAsocijacijeMatchRounds(List<AsocijacijeRound> sourceRounds) {
        List<AsocijacijeRound> availableRounds = createAsocijacijeRoundCopies(sourceRounds);
        if (availableRounds.size() < REQUIRED_SHARED_ROUNDS) {
            return localGameRepository.getAsocijacijeRounds();
        }

        Collections.shuffle(availableRounds);
        return new ArrayList<>(availableRounds.subList(0, REQUIRED_SHARED_ROUNDS));
    }

    private List<AsocijacijeRound> createAsocijacijeRoundCopies(List<AsocijacijeRound> sourceRounds) {
        List<AsocijacijeRound> copies = new ArrayList<>();
        if (sourceRounds == null) {
            return copies;
        }

        for (AsocijacijeRound round : sourceRounds) {
            if (round == null
                    || TextUtils.isEmpty(round.finalSolution)
                    || TextUtils.isEmpty(round.columnA_solution)
                    || TextUtils.isEmpty(round.columnB_solution)
                    || TextUtils.isEmpty(round.columnC_solution)
                    || TextUtils.isEmpty(round.columnD_solution)
                    || round.columnA_clues == null
                    || round.columnA_clues.isEmpty()
                    || round.columnB_clues == null
                    || round.columnB_clues.isEmpty()
                    || round.columnC_clues == null
                    || round.columnC_clues.isEmpty()
                    || round.columnD_clues == null
                    || round.columnD_clues.isEmpty()) {
                continue;
            }

            copies.add(new AsocijacijeRound(
                    round.finalSolution,
                    round.columnA_solution,
                    new ArrayList<>(round.columnA_clues),
                    round.columnB_solution,
                    new ArrayList<>(round.columnB_clues),
                    round.columnC_solution,
                    new ArrayList<>(round.columnC_clues),
                    round.columnD_solution,
                    new ArrayList<>(round.columnD_clues)
            ));
        }

        return copies;
    }

    private void loadKorakPoKorakMatchRounds(FirebaseCallback<List<KorakPoKorakRound>> callback) {
        firestoreRepository.getKorakPoKorakRounds(new FirebaseCallback<List<KorakPoKorakRound>>() {
            @Override
            public void onSuccess(List<KorakPoKorakRound> result) {
                callback.onSuccess(selectKorakPoKorakMatchRounds(result));
            }

            @Override
            public void onError(String error) {
                callback.onSuccess(localGameRepository.getKorakPoKorakMatchRounds());
            }
        });
    }

    private List<KorakPoKorakRound> selectKorakPoKorakMatchRounds(List<KorakPoKorakRound> sourceRounds) {
        List<KorakPoKorakRound> availableRounds = createKorakPoKorakRoundCopies(sourceRounds);
        if (availableRounds.size() < REQUIRED_SHARED_ROUNDS) {
            return localGameRepository.getKorakPoKorakMatchRounds();
        }

        Collections.shuffle(availableRounds);
        return new ArrayList<>(availableRounds.subList(0, REQUIRED_SHARED_ROUNDS));
    }

    private List<KorakPoKorakRound> createKorakPoKorakRoundCopies(List<KorakPoKorakRound> sourceRounds) {
        List<KorakPoKorakRound> copies = new ArrayList<>();
        if (sourceRounds == null) {
            return copies;
        }

        for (KorakPoKorakRound round : sourceRounds) {
            if (round == null
                    || TextUtils.isEmpty(round.getAnswer())
                    || round.getCluesList().isEmpty()) {
                continue;
            }
            copies.add(new KorakPoKorakRound(round.getAnswer(), round.getCluesList()));
        }
        return copies;
    }

    private List<SharedKorakPoKorakRound> copySharedKorakRounds(List<SharedKorakPoKorakRound> sourceRounds) {
        List<SharedKorakPoKorakRound> copies = new ArrayList<>();
        if (sourceRounds == null) {
            return copies;
        }

        for (SharedKorakPoKorakRound round : sourceRounds) {
            if (round == null) {
                continue;
            }
            copies.add(new SharedKorakPoKorakRound(
                    round.answer,
                    round.clues == null ? new ArrayList<>() : new ArrayList<>(round.clues)
            ));
        }
        return copies;
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
                if (!isGuestUser(user) && user.tokens <= 0) {
                    callback.onError("You do not have enough tokens for a ranked match.");
                    return;
                }

                db.collection(MATCHES_COLLECTION)
                        .whereEqualTo("status", SharedMatchState.STATUS_WAITING)
                        .whereEqualTo("matchType", SharedMatchState.MATCH_TYPE_COMPETITIVE)
                        .limit(25)
                        .get()
                        .addOnSuccessListener(querySnapshot -> {
                            List<DocumentSnapshot> waitingMatches = new ArrayList<>(querySnapshot.getDocuments());
                            waitingMatches.sort((left, right) ->
                                    Long.compare(readUpdatedAt(right), readUpdatedAt(left)));

                            for (DocumentSnapshot snapshot : waitingMatches) {
                                SharedMatchState state = snapshot.toObject(SharedMatchState.class);
                                if (state == null) {
                                    continue;
                                }

                                if (!isFreshCompetitiveWaitingState(state)
                                        || userId.equals(state.playerOneId)
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

    public void startTournamentMatchmaking(FirebaseCallback<MatchJoinResult> callback) {
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
                if (user.tokens < 3) {
                    callback.onError("You need 3 tokens to enter a tournament.");
                    return;
                }

                db.collection(MATCHES_COLLECTION)
                        .whereEqualTo("status", SharedMatchState.STATUS_WAITING)
                        .whereEqualTo("matchType", SharedMatchState.MATCH_TYPE_TOURNAMENT)
                        .limit(20)
                        .get()
                        .addOnSuccessListener(waitingSnapshot -> {
                            for (DocumentSnapshot snapshot : waitingSnapshot.getDocuments()) {
                                SharedMatchState state = snapshot.toObject(SharedMatchState.class);
                                if (state == null
                                        || !isFreshTournamentWaitingState(state)
                                        || userId.equals(state.playerOneId)
                                        || userId.equals(state.playerTwoId)
                                        || !TextUtils.isEmpty(state.playerTwoId)) {
                                    continue;
                                }
                                joinTournamentWaitingMatch(snapshot, user, callback);
                                return;
                            }

                            for (DocumentSnapshot snapshot : waitingSnapshot.getDocuments()) {
                                SharedMatchState state = snapshot.toObject(SharedMatchState.class);
                                if (state == null
                                        || state.tournamentSemiNumber != 1
                                        || TextUtils.isEmpty(state.playerTwoId)
                                        || !TextUtils.isEmpty(state.siblingMatchId)
                                        || !isFreshTournamentWaitingState(state)
                                        || userId.equals(state.playerOneId)
                                        || userId.equals(state.playerTwoId)) {
                                    continue;
                                }
                                createTournamentSecondSemiMatch(snapshot, state, user, callback);
                                return;
                            }

                            createTournamentWaitingMatch(user, callback);
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

                        loadMatchContentBundle(new FirebaseCallback<MatchContentBundle>() {
                            @Override
                            public void onSuccess(MatchContentBundle matchContent) {
                                DocumentReference reference = db.collection(MATCHES_COLLECTION).document();
                                SharedMatchState state = buildInitialState(
                                        reference.getId(),
                                        currentUserId,
                                        safeUserName(sender),
                                        SharedMatchState.MATCH_TYPE_FRIENDLY,
                                        matchContent
                                );
                                state.playerTwoId = targetUserId;
                                state.playerTwoName = safeUserName(targetUser);
                                state.phaseMessage = "Waiting for your friend to accept the invite.";

                                reference.set(state)
                                        .addOnSuccessListener(unused -> {
                                            long now = System.currentTimeMillis();
                                            long expiresAt = now + FRIENDLY_INVITE_EXPIRATION_MS;

                                            Map<String, Object> inviteTimingUpdates = new HashMap<>();
                                            inviteTimingUpdates.put("friendlyInviteExpiresAt", expiresAt);
                                            inviteTimingUpdates.put("phaseStartedAt", now);
                                            inviteTimingUpdates.put(
                                                    "phaseDurationSeconds",
                                                    (int) (FRIENDLY_INVITE_EXPIRATION_MS / 1000L)
                                            );
                                            inviteTimingUpdates.put("updatedAt", now);

                                            reference.update(inviteTimingUpdates)
                                                    .addOnSuccessListener(timingUpdated -> {
                                                        Map<String, Object> extras = new HashMap<>();
                                                        extras.put("senderId", currentUserId);
                                                        extras.put("senderName", safeUserName(sender));
                                                        extras.put("relatedMatchId", reference.getId());
                                                        extras.put("invitationStatus", "PENDING");
                                                        extras.put("friendlyMatch", true);
                                                        extras.put("expiresAt", expiresAt);

                                                        firestoreRepository.saveNotificationForUser(
                                                                targetUserId,
                                                                "Friendly match invite",
                                                                safeUserName(sender) + " invited you to a friendly match.",
                                                                "INVITE",
                                                                extras
                                                        );
                                                        schedulePendingFriendlyInviteExpiration(reference.getId(), expiresAt);

                                                        callback.onSuccess(new MatchJoinResult(
                                                                reference.getId(),
                                                                1,
                                                                state.roomCode,
                                                                true,
                                                                true
                                                        ));
                                                    })
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

            long effectiveExpiresAt = notification.expiresAt > 0
                    ? notification.expiresAt
                    : state.friendlyInviteExpiresAt;
            if (effectiveExpiresAt > 0 && System.currentTimeMillis() > effectiveExpiresAt) {
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
                ))
                .addOnSuccessListener(unused -> updatePendingInviteNotifications(matchId, "CANCELED", true));
    }

    private void schedulePendingFriendlyInviteExpiration(String matchId, long expiresAt) {
        long delayMs = Math.max(0L, expiresAt - System.currentTimeMillis());
        new Handler(Looper.getMainLooper()).postDelayed(
                () -> expirePendingFriendlyInvite(matchId),
                delayMs + 250L
        );
    }

    private void expirePendingFriendlyInvite(String matchId) {
        if (TextUtils.isEmpty(matchId)) {
            return;
        }

        DocumentReference matchReference = db.collection(MATCHES_COLLECTION).document(matchId);
        db.runTransaction((Transaction.Function<Boolean>) transaction -> {
            DocumentSnapshot snapshot = transaction.get(matchReference);
            SharedMatchState state = snapshot.toObject(SharedMatchState.class);

            if (state == null
                    || !SharedMatchState.MATCH_TYPE_FRIENDLY.equals(state.matchType)
                    || !SharedMatchState.STATUS_WAITING.equals(state.status)) {
                return false;
            }

            if (state.friendlyInviteExpiresAt > 0
                    && System.currentTimeMillis() < state.friendlyInviteExpiresAt) {
                return false;
            }

            transaction.update(matchReference, buildCanceledUpdates(
                    SharedMatchState.STATUS_DECLINED,
                    "The friendly match invite expired."
            ));
            return true;
        }).addOnSuccessListener(expired -> {
            if (!expired) {
                return;
            }

            updatePendingInviteNotifications(matchId, "EXPIRED", true);
        });
    }

    private void updatePendingInviteNotifications(String matchId, String status, boolean read) {
        if (TextUtils.isEmpty(matchId)) {
            return;
        }

        db.collection("notifications")
                .whereEqualTo("relatedMatchId", matchId)
                .whereEqualTo("invitationStatus", "PENDING")
                .get()
                .addOnSuccessListener(snapshot -> {
                    for (DocumentSnapshot document : snapshot.getDocuments()) {
                        document.getReference().update(new HashMap<String, Object>() {{
                            put("invitationStatus", status);
                            put("read", read);
                        }});
                    }
                });
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

    public void leaveTournamentWaitingMatch(String matchId) {
        String userId;
        try {
            userId = requireUserId();
        } catch (IllegalStateException e) {
            return;
        }
        if (TextUtils.isEmpty(matchId)) {
            return;
        }

        DocumentReference matchReference = db.collection(MATCHES_COLLECTION).document(matchId);
        DocumentReference userReference = db.collection(USERS_COLLECTION).document(userId);

        db.runTransaction(transaction -> {
            DocumentSnapshot matchSnapshot = transaction.get(matchReference);
            SharedMatchState state = matchSnapshot.toObject(SharedMatchState.class);
            User user = transaction.get(userReference).toObject(User.class);
            DocumentReference siblingReference = null;
            SharedMatchState siblingState = null;
            if (state != null
                    && SharedMatchState.MATCH_TYPE_TOURNAMENT.equals(state.matchType)
                    && SharedMatchState.STATUS_WAITING.equals(state.status)
                    && !TextUtils.isEmpty(state.siblingMatchId)) {
                siblingReference = db.collection(MATCHES_COLLECTION).document(state.siblingMatchId);
                DocumentSnapshot siblingSnapshot = transaction.get(siblingReference);
                siblingState = siblingSnapshot.toObject(SharedMatchState.class);
            }

            if (user != null) {
                ensureUserDefaultsForTransaction(user, userId);
                user.currentMatchId = "";
                user.lastSeenAt = System.currentTimeMillis();
                transaction.set(userReference, user);
            }

            if (state == null
                    || !SharedMatchState.MATCH_TYPE_TOURNAMENT.equals(state.matchType)
                    || !SharedMatchState.STATUS_WAITING.equals(state.status)) {
                return null;
            }

            boolean leavingPlayerOne = userId.equals(state.playerOneId);
            boolean leavingPlayerTwo = userId.equals(state.playerTwoId);
            if (!leavingPlayerOne && !leavingPlayerTwo) {
                return null;
            }

            Map<String, Object> updates = new HashMap<>();
            updates.put("updatedAt", System.currentTimeMillis());

            if (leavingPlayerTwo) {
                updates.put("playerTwoId", "");
                updates.put("playerTwoName", "");
                updates.put("phaseMessage", "Waiting for one more tournament player...");
                transaction.update(matchReference, updates);
                return null;
            }

            if (!TextUtils.isEmpty(state.playerTwoId)) {
                updates.put("playerOneId", state.playerTwoId);
                updates.put("playerOneName", state.playerTwoName);
                updates.put("playerTwoId", "");
                updates.put("playerTwoName", "");
                updates.put("phaseMessage", "Waiting for one more tournament player...");
                transaction.update(matchReference, updates);
                return null;
            }

            transaction.update(matchReference, buildCanceledUpdates(
                    SharedMatchState.STATUS_CANCELED,
                    "The tournament search was canceled."
            ));
            if (siblingReference != null && siblingState != null
                    && SharedMatchState.STATUS_WAITING.equals(siblingState.status)) {
                Map<String, Object> siblingUpdates = new HashMap<>();
                siblingUpdates.put("siblingMatchId", "");
                siblingUpdates.put("updatedAt", System.currentTimeMillis());
                transaction.update(siblingReference, siblingUpdates);
            }
            return null;
        });
    }
    public void finalizeMatchIfNeeded(
            String matchId,
            FirebaseCallback<MatchFinalizationResult> callback
    ) {
        String currentUserId;
        try {
            currentUserId = requireUserId();
        } catch (IllegalStateException e) {
            callback.onError(e.getMessage());
            return;
        }

        if (TextUtils.isEmpty(matchId)) {
            callback.onError("Match was not found.");
            return;
        }

        DocumentReference matchReference = db.collection(MATCHES_COLLECTION).document(matchId);
        loadMatchContentBundle(new FirebaseCallback<MatchContentBundle>() {
            @Override
            public void onSuccess(MatchContentBundle matchContent) {
                db.runTransaction((Transaction.Function<MatchFinalizationResult>) transaction -> {
                    DocumentSnapshot matchSnapshot = transaction.get(matchReference);
                    SharedMatchState state = matchSnapshot.toObject(SharedMatchState.class);

                    if (state == null) {
                        throw new IllegalStateException("Match was not found.");
                    }

                    if (state.resultApplied) {
                        return buildFinalizationResult(false, state, currentUserId);
                    }

                    if (SharedMatchState.MATCH_TYPE_FRIENDLY.equals(state.matchType)) {
                        DocumentReference playerOneUserReference =
                                db.collection(USERS_COLLECTION).document(state.playerOneId);
                        DocumentReference playerTwoUserReference =
                                db.collection(USERS_COLLECTION).document(state.playerTwoId);
                        User playerOne = transaction.get(playerOneUserReference).toObject(User.class);
                        User playerTwo = transaction.get(playerTwoUserReference).toObject(User.class);
                        if (playerOne == null || playerTwo == null) {
                            throw new IllegalStateException("Players could not be loaded.");
                        }

                        ensureUserDefaultsForTransaction(playerOne, state.playerOneId);
                        ensureUserDefaultsForTransaction(playerTwo, state.playerTwoId);
                        int playerOnePreviousLeague = com.tim14.slagalica.LeagueUtils.calculateLeague(playerOne.stars);
                        int playerTwoPreviousLeague = com.tim14.slagalica.LeagueUtils.calculateLeague(playerTwo.stars);

                        boolean playerOneWon = state.playerOneScore > state.playerTwoScore;
                        boolean playerTwoWon = state.playerTwoScore > state.playerOneScore;
                        if (state.forfeitedPlayer == 1) {
                            playerOneWon = false;
                            playerTwoWon = true;
                        } else if (state.forfeitedPlayer == 2) {
                            playerOneWon = true;
                            playerTwoWon = false;
                        }

                        completeDailyMissionForTransaction(playerOne, FirestoreRepository.MISSION_PLAY_FRIENDLY);
                        completeDailyMissionForTransaction(playerTwo, FirestoreRepository.MISSION_PLAY_FRIENDLY);

                        String winnerId = playerOneWon ? state.playerOneId : (playerTwoWon ? state.playerTwoId : "");
                        int playerOneCurrentLeague = com.tim14.slagalica.LeagueUtils.calculateLeague(playerOne.stars);
                        int playerTwoCurrentLeague = com.tim14.slagalica.LeagueUtils.calculateLeague(playerTwo.stars);
                        playerOne.currentMatchId = "";
                        playerTwo.currentMatchId = "";
                        transaction.set(playerOneUserReference, playerOne);
                        transaction.set(playerTwoUserReference, playerTwo);
                        transaction.update(matchReference, new HashMap<String, Object>() {{
                            put("resultApplied", true);
                            put("winnerId", winnerId);
                            put("playerOnePreviousLeague", playerOnePreviousLeague);
                            put("playerOneCurrentLeague", playerOneCurrentLeague);
                            put("playerTwoPreviousLeague", playerTwoPreviousLeague);
                            put("playerTwoCurrentLeague", playerTwoCurrentLeague);
                            put("updatedAt", System.currentTimeMillis());
                        }});
                        if (currentUserId.equals(state.playerOneId)) {
                            return new MatchFinalizationResult(
                                    true,
                                    true,
                                    "",
                                    winnerId,
                                    playerOnePreviousLeague,
                                    playerOneCurrentLeague
                            );
                        }

                        if (currentUserId.equals(state.playerTwoId)) {
                            return new MatchFinalizationResult(
                                    true,
                                    true,
                                    "",
                                    winnerId,
                                    playerTwoPreviousLeague,
                                    playerTwoCurrentLeague
                            );
                        }

                        return new MatchFinalizationResult(true, true, "", winnerId);
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
                    boolean playerOneGuest = isGuestUser(playerOne);
                    boolean playerTwoGuest = isGuestUser(playerTwo);
                    if (!playerOneGuest) {
                        applyDailyTokenGrantForTransaction(playerOne);
                    }
                    if (!playerTwoGuest) {
                        applyDailyTokenGrantForTransaction(playerTwo);
                    }
                    int playerOnePreviousLeague = playerOneGuest
                            ? 0
                            : com.tim14.slagalica.LeagueUtils.calculateLeague(playerOne.stars);
                    int playerTwoPreviousLeague = playerTwoGuest
                            ? 0
                            : com.tim14.slagalica.LeagueUtils.calculateLeague(playerTwo.stars);

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
                    boolean tournamentMatch = SharedMatchState.MATCH_TYPE_TOURNAMENT.equals(state.matchType);
                    if (!playerOneGuest) {
                        markRankingGamePlayedForTransaction(playerOne);
                    }
                    if (!playerTwoGuest) {
                        markRankingGamePlayedForTransaction(playerTwo);
                    }

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

                    if (tournamentMatch) {
                        boolean finalMatch = "FINAL".equals(state.tournamentStage);
                        if (finalMatch) {
                            if (playerOneWon) {
                                if (!playerOneGuest) {
                                    applyTournamentFinalWinnerOutcome(playerOne, playerOneStarDelta);
                                    completeTournamentWinMissionsForTransaction(playerOne);
                                    playerOneStatistics.wins++;
                                }
                                if (!playerTwoGuest) {
                                    applyCompetitiveOutcome(playerTwo, playerTwoStarDelta);
                                    playerTwoStatistics.losses++;
                                }
                            } else if (playerTwoWon) {
                                if (!playerTwoGuest) {
                                    applyTournamentFinalWinnerOutcome(playerTwo, playerTwoStarDelta);
                                    completeTournamentWinMissionsForTransaction(playerTwo);
                                    playerTwoStatistics.wins++;
                                }
                                if (!playerOneGuest) {
                                    applyCompetitiveOutcome(playerOne, playerOneStarDelta);
                                    playerOneStatistics.losses++;
                                }
                            }
                        } else if (playerOneWon) {
                            if (!playerOneGuest) {
                                applyTournamentSemiWinnerOutcome(playerOne, playerOneStarDelta);
                                completeTournamentWinMissionsForTransaction(playerOne);
                                playerOneStatistics.wins++;
                            }
                            if (!playerTwoGuest) {
                                playerTwoStatistics.losses++;
                            }
                        } else if (playerTwoWon) {
                            if (!playerTwoGuest) {
                                applyTournamentSemiWinnerOutcome(playerTwo, playerTwoStarDelta);
                                completeTournamentWinMissionsForTransaction(playerTwo);
                                playerTwoStatistics.wins++;
                            }
                            if (!playerOneGuest) {
                                playerOneStatistics.losses++;
                            }
                        }
                    } else {
                        if (!playerOneGuest) {
                            applyCompetitiveOutcome(playerOne, playerOneStarDelta);
                            if (playerOneWon) {
                                completeDailyMissionForTransaction(playerOne, FirestoreRepository.MISSION_WIN_MATCH);
                            }
                        }
                        if (!playerTwoGuest) {
                            applyCompetitiveOutcome(playerTwo, playerTwoStarDelta);
                            if (playerTwoWon) {
                                completeDailyMissionForTransaction(playerTwo, FirestoreRepository.MISSION_WIN_MATCH);
                            }
                        }
                    }
                    playerOne.currentMatchId = "";
                    playerTwo.currentMatchId = "";

                    if (!playerOneGuest) {
                        playerOneStatistics.gamesPlayed++;
                    }
                    if (!playerTwoGuest) {
                        playerTwoStatistics.gamesPlayed++;
                    }

                    if (!tournamentMatch) {
                        if (playerOneWon) {
                            if (!playerOneGuest) {
                                playerOneStatistics.wins++;
                            }
                            if (!playerTwoGuest) {
                                playerTwoStatistics.losses++;
                            }
                        } else if (playerTwoWon) {
                            if (!playerTwoGuest) {
                                playerTwoStatistics.wins++;
                            }
                            if (!playerOneGuest) {
                                playerOneStatistics.losses++;
                            }
                        }
                    }
                    String winnerId = playerOneWon ? state.playerOneId : (playerTwoWon ? state.playerTwoId : "");
                    String finalMatchId = "";
                    int playerOneCurrentLeague = playerOneGuest
                            ? 0
                            : com.tim14.slagalica.LeagueUtils.calculateLeague(playerOne.stars);
                    int playerTwoCurrentLeague = playerTwoGuest
                            ? 0
                            : com.tim14.slagalica.LeagueUtils.calculateLeague(playerTwo.stars);
                    Map<String, Object> resultUpdates = new HashMap<>();
                    resultUpdates.put("resultApplied", true);
                    resultUpdates.put("winnerId", winnerId);
                    resultUpdates.put("playerOnePreviousLeague", playerOnePreviousLeague);
                    resultUpdates.put("playerOneCurrentLeague", playerOneCurrentLeague);
                    resultUpdates.put("playerTwoPreviousLeague", playerTwoPreviousLeague);
                    resultUpdates.put("playerTwoCurrentLeague", playerTwoCurrentLeague);
                    resultUpdates.put("updatedAt", System.currentTimeMillis());
                    if (tournamentMatch && "SEMI".equals(state.tournamentStage) && !TextUtils.isEmpty(winnerId)) {
                        finalMatchId = createTournamentFinalIfReady(
                                transaction,
                                matchReference,
                                state,
                                winnerId,
                                matchContent
                        );
                        if (!TextUtils.isEmpty(finalMatchId)) {
                            resultUpdates.put("finalMatchId", finalMatchId);
                            if (winnerId.equals(state.playerOneId)) {
                                playerOne.currentMatchId = finalMatchId;
                            } else if (winnerId.equals(state.playerTwoId)) {
                                playerTwo.currentMatchId = finalMatchId;
                            }
                        }
                    }

                    transaction.set(playerOneUserReference, playerOne);
                    transaction.set(playerTwoUserReference, playerTwo);
                    if (!playerOneGuest) {
                        transaction.set(playerOneStatsReference, playerOneStatistics);
                    }
                    if (!playerTwoGuest) {
                        transaction.set(playerTwoStatsReference, playerTwoStatistics);
                    }
                    transaction.update(matchReference, resultUpdates);

                    if (currentUserId.equals(state.playerOneId)) {
                        return new MatchFinalizationResult(
                                true,
                                false,
                                finalMatchId,
                                winnerId,
                                playerOnePreviousLeague,
                                playerOneCurrentLeague
                        );
                    }

                    if (currentUserId.equals(state.playerTwoId)) {
                        return new MatchFinalizationResult(
                                true,
                                false,
                                finalMatchId,
                                winnerId,
                                playerTwoPreviousLeague,
                                playerTwoCurrentLeague
                        );
                    }

                    return new MatchFinalizationResult(true, false, finalMatchId, winnerId);
                }).addOnSuccessListener(callback::onSuccess)
                        .addOnFailureListener(e -> callback.onError(e.getMessage()));
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
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
                loadMatchContentBundle(new FirebaseCallback<MatchContentBundle>() {
                    @Override
                    public void onSuccess(MatchContentBundle matchContent) {
                        DocumentReference reference = db.collection(MATCHES_COLLECTION).document();
                        SharedMatchState state = buildInitialState(
                                reference.getId(),
                                userId,
                                safeUserName(user),
                                SharedMatchState.MATCH_TYPE_COMPETITIVE,
                                matchContent
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

    public void getMatchState(String matchId, FirebaseCallback<SharedMatchState> callback) {
        if (TextUtils.isEmpty(matchId)) {
            callback.onError("Match was not found.");
            return;
        }

        db.collection(MATCHES_COLLECTION)
                .document(matchId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    SharedMatchState state = snapshot == null ? null : snapshot.toObject(SharedMatchState.class);
                    if (state == null) {
                        callback.onError("Match was not found.");
                        return;
                    }
                    callback.onSuccess(state);
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
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

        loadMatchContentBundle(new FirebaseCallback<MatchContentBundle>() {
            @Override
            public void onSuccess(MatchContentBundle matchContent) {
                SharedMatchState resetState = buildInitialState(
                        matchId,
                        previousState.playerOneId,
                        previousState.playerOneName,
                        previousState.matchType,
                        matchContent
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

            @Override
            public void onError(String error) {
                // Ignore reset errors here to preserve existing flow.
            }
        });
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
        return buildInitialState(
                documentId,
                playerOneId,
                playerOneName,
                matchType,
                createLocalMatchContentBundle()
        );
    }

    private SharedMatchState buildInitialState(
            String documentId,
            String playerOneId,
            String playerOneName,
            String matchType,
            MatchContentBundle matchContent
    ) {
        MatchContentBundle safeMatchContent =
                matchContent == null ? createLocalMatchContentBundle() : matchContent;
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

        for (KoZnaZnaQuestion question : safeMatchContent.quizQuestions) {
            state.quizQuestions.add(new KoZnaZnaQuestion(
                    question.id,
                    question.question,
                    new ArrayList<>(question.answers),
                    question.correctIndex
            ));
        }

        List<SpojniceRound> spojniceSourceRounds = safeMatchContent.spojniceRounds;
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

        List<AsocijacijeRound> asocijacijeSourceRounds = safeMatchContent.asocijacijeRounds;
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

        for (KorakPoKorakRound round : safeMatchContent.korakRounds) {
            state.korakRounds.add(new SharedKorakPoKorakRound(
                    round.getAnswer(),
                    round.getCluesList()
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

    private Map<String, Object> buildTournamentPairWaitingUpdates(String playerTwoId, String playerTwoName) {
        return new HashMap<String, Object>() {{
            put("playerTwoId", playerTwoId);
            put("playerTwoName", playerTwoName);
            put("playerTwoEntryPaid", true);
            put("status", SharedMatchState.STATUS_WAITING);
            put("phase", SharedMatchState.PHASE_WAITING);
            put("phaseMessage", "Waiting for two more tournament players...");
            put("updatedAt", System.currentTimeMillis());
        }};
    }

    private Map<String, Object> buildTournamentJoinAndStartUpdates(
            String playerTwoId,
            String playerTwoName,
            String message
    ) {
        Map<String, Object> updates = buildTournamentStartUpdates(message);
        updates.put("playerTwoId", playerTwoId);
        updates.put("playerTwoName", playerTwoName);
        updates.put("playerTwoEntryPaid", true);
        return updates;
    }

    private Map<String, Object> buildTournamentStartUpdates(String message) {
        return new HashMap<String, Object>() {{
            put("status", SharedMatchState.STATUS_ACTIVE);
            put("currentRound", GameRound.KO_ZNA_ZNA.name());
            put("phase", SharedMatchState.PHASE_KZZ_QUESTION);
            put("activePlayer", 0);
            put("currentTurnIndex", 0);
            put("phaseStartedAt", System.currentTimeMillis());
            put("phaseDurationSeconds", 5);
            put("phaseMessage", message);
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

    private boolean isFreshTournamentWaitingState(SharedMatchState state) {
        if (state == null || state.updatedAt <= 0L) {
            return true;
        }
        return System.currentTimeMillis() - state.updatedAt <= TOURNAMENT_WAITING_EXPIRATION_MS;
    }

    private boolean isFreshCompetitiveWaitingState(SharedMatchState state) {
        if (state == null || state.updatedAt <= 0L) {
            return false;
        }
        return System.currentTimeMillis() - state.updatedAt <= COMPETITIVE_WAITING_EXPIRATION_MS;
    }

    private long readUpdatedAt(DocumentSnapshot snapshot) {
        Long updatedAt = snapshot == null ? null : snapshot.getLong("updatedAt");
        return updatedAt == null ? 0L : updatedAt;
    }

    private void createTournamentWaitingMatch(
            User user,
            FirebaseCallback<MatchJoinResult> callback
    ) {
        loadMatchContentBundle(new FirebaseCallback<MatchContentBundle>() {
            @Override
            public void onSuccess(MatchContentBundle matchContent) {
                DocumentReference reference = db.collection(MATCHES_COLLECTION).document();
                DocumentReference userReference = db.collection(USERS_COLLECTION).document(user.id);

                db.runTransaction((Transaction.Function<MatchJoinResult>) transaction -> {
                    User currentUser = transaction.get(userReference).toObject(User.class);
                    if (currentUser == null) {
                        throw new IllegalStateException("Player data could not be loaded.");
                    }

                    ensureUserDefaultsForTransaction(currentUser, user.id);
                    applyDailyTokenGrantForTransaction(currentUser);
                    if (currentUser.tokens < 3) {
                        throw new IllegalStateException("You need 3 tokens to enter a tournament.");
                    }

                    currentUser.tokens -= 3;
                    currentUser.currentMatchId = reference.getId();
                    currentUser.lastSeenAt = System.currentTimeMillis();

                    SharedMatchState state = buildInitialState(
                            reference.getId(),
                            user.id,
                            safeUserName(currentUser),
                            SharedMatchState.MATCH_TYPE_TOURNAMENT,
                            matchContent
                    );
                    state.phaseMessage = "Waiting for three more tournament players...";
                    state.tournamentId = reference.getId();
                    state.tournamentStage = "SEMI";
                    state.tournamentSemiNumber = 1;
                    state.playerOneEntryPaid = true;

                    transaction.set(userReference, currentUser);
                    transaction.set(reference, state);
                    return new MatchJoinResult(reference.getId(), 1, state.roomCode, true, false);
                }).addOnSuccessListener(callback::onSuccess)
                        .addOnFailureListener(e -> callback.onError(e.getMessage()));
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }
    private void joinTournamentWaitingMatch(
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
                    || !SharedMatchState.MATCH_TYPE_TOURNAMENT.equals(latestState.matchType)
                    || !TextUtils.isEmpty(latestState.playerTwoId)
                    || joiningUserId.equals(latestState.playerOneId)) {
                throw new IllegalStateException("That tournament match is no longer available.");
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

            boolean chargeWaitingPlayer = !latestState.playerOneEntryPaid;
            boolean chargeJoiningPlayer = !latestState.playerTwoEntryPaid;
            if ((chargeWaitingPlayer && waitingPlayer.tokens < 3)
                    || (chargeJoiningPlayer && currentJoiningPlayer.tokens < 3)) {
                throw new IllegalStateException("Both players need 3 tokens for a tournament.");
            }

            boolean startsTournament = latestState.tournamentSemiNumber == 2 && !TextUtils.isEmpty(latestState.siblingMatchId);
            DocumentReference firstSemiReference = null;
            if (startsTournament) {
                firstSemiReference = db.collection(MATCHES_COLLECTION).document(latestState.siblingMatchId);
                DocumentSnapshot firstSemiSnapshot = transaction.get(firstSemiReference);
                SharedMatchState firstSemiState = firstSemiSnapshot.toObject(SharedMatchState.class);
                if (firstSemiState == null
                        || !SharedMatchState.STATUS_WAITING.equals(firstSemiState.status)
                        || TextUtils.isEmpty(firstSemiState.playerTwoId)) {
                    throw new IllegalStateException("Tournament players are no longer ready.");
                }
            }

            if (chargeWaitingPlayer) {
                waitingPlayer.tokens -= 3;
            }
            if (chargeJoiningPlayer) {
                currentJoiningPlayer.tokens -= 3;
            }
            waitingPlayer.currentMatchId = latestMatchSnapshot.getId();
            currentJoiningPlayer.currentMatchId = latestMatchSnapshot.getId();
            waitingPlayer.lastSeenAt = System.currentTimeMillis();
            currentJoiningPlayer.lastSeenAt = System.currentTimeMillis();

            transaction.set(waitingPlayerReference, waitingPlayer);
            transaction.set(joiningPlayerReference, currentJoiningPlayer);

            if (startsTournament) {
                transaction.update(firstSemiReference, buildTournamentStartUpdates("Tournament semifinal 1. Question 1/5. Both players can answer."));
                transaction.update(matchReference, buildTournamentJoinAndStartUpdates(joiningUserId, safeUserName(currentJoiningPlayer), "Tournament semifinal 2. Question 1/5. Both players can answer."));
                return new MatchJoinResult(
                        latestMatchSnapshot.getId(),
                        2,
                        latestState.roomCode,
                        false,
                        false
                );
            }

            transaction.update(matchReference, buildTournamentPairWaitingUpdates(joiningUserId, safeUserName(currentJoiningPlayer)));
            return new MatchJoinResult(
                    latestMatchSnapshot.getId(),
                    2,
                    latestState.roomCode,
                    true,
                    false
            );
        }).addOnSuccessListener(callback::onSuccess)
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }
    private void createTournamentSecondSemiMatch(
            DocumentSnapshot firstSemiSnapshot,
            SharedMatchState firstSemiState,
            User user,
            FirebaseCallback<MatchJoinResult> callback
    ) {
        loadMatchContentBundle(new FirebaseCallback<MatchContentBundle>() {
            @Override
            public void onSuccess(MatchContentBundle matchContent) {
                DocumentReference secondReference = db.collection(MATCHES_COLLECTION).document();
                DocumentReference userReference = db.collection(USERS_COLLECTION).document(user.id);

                db.runTransaction((Transaction.Function<MatchJoinResult>) transaction -> {
                    DocumentSnapshot latestFirstSnapshot = transaction.get(firstSemiSnapshot.getReference());
                    SharedMatchState latestFirstState = latestFirstSnapshot.toObject(SharedMatchState.class);
                    User currentUser = transaction.get(userReference).toObject(User.class);

                    if (latestFirstState == null
                            || !SharedMatchState.MATCH_TYPE_TOURNAMENT.equals(latestFirstState.matchType)
                            || !SharedMatchState.STATUS_WAITING.equals(latestFirstState.status)
                            || latestFirstState.tournamentSemiNumber != 1
                            || TextUtils.isEmpty(latestFirstState.playerTwoId)
                            || !TextUtils.isEmpty(latestFirstState.siblingMatchId)
                            || currentUser == null) {
                        throw new IllegalStateException("That tournament is no longer accepting players.");
                    }

                    ensureUserDefaultsForTransaction(currentUser, user.id);
                    applyDailyTokenGrantForTransaction(currentUser);
                    if (currentUser.tokens < 3) {
                        throw new IllegalStateException("You need 3 tokens to enter a tournament.");
                    }

                    currentUser.tokens -= 3;
                    currentUser.currentMatchId = secondReference.getId();
                    currentUser.lastSeenAt = System.currentTimeMillis();

                    SharedMatchState secondState = buildInitialState(
                            secondReference.getId(),
                            user.id,
                            safeUserName(currentUser),
                            SharedMatchState.MATCH_TYPE_TOURNAMENT,
                            matchContent
                    );
                    secondState.phaseMessage = "Waiting for one more tournament player...";
                    secondState.tournamentId = latestFirstState.tournamentId;
                    secondState.tournamentStage = "SEMI";
                    secondState.tournamentSemiNumber = 2;
                    secondState.playerOneEntryPaid = true;
                    secondState.siblingMatchId = latestFirstSnapshot.getId();

                    Map<String, Object> firstUpdates = new HashMap<>();
                    firstUpdates.put("siblingMatchId", secondReference.getId());
                    firstUpdates.put("updatedAt", System.currentTimeMillis());

                    transaction.set(userReference, currentUser);
                    transaction.set(secondReference, secondState);
                    transaction.update(firstSemiSnapshot.getReference(), firstUpdates);

                    return new MatchJoinResult(secondReference.getId(), 1, secondState.roomCode, true, false);
                }).addOnSuccessListener(callback::onSuccess)
                        .addOnFailureListener(e -> callback.onError(e.getMessage()));
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }
    private void createCompetitiveWaitingMatch(
            User user,
            FirebaseCallback<MatchJoinResult> callback
    ) {
        loadMatchContentBundle(new FirebaseCallback<MatchContentBundle>() {
            @Override
            public void onSuccess(MatchContentBundle matchContent) {
                DocumentReference reference = db.collection(MATCHES_COLLECTION).document();
                SharedMatchState state = buildInitialState(
                        reference.getId(),
                        user.id,
                        safeUserName(user),
                        SharedMatchState.MATCH_TYPE_COMPETITIVE,
                        matchContent
                );
                state.phaseMessage = "Searching for an opponent...";

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
                    || !isFreshCompetitiveWaitingState(latestState)
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
            if (!isGuestUser(waitingPlayer)) {
                applyDailyTokenGrantForTransaction(waitingPlayer);
            }
            if (!isGuestUser(currentJoiningPlayer)) {
                applyDailyTokenGrantForTransaction(currentJoiningPlayer);
            }

            if (!isGuestUser(waitingPlayer) && waitingPlayer.tokens <= 0) {
                throw new IllegalStateException("The opponent no longer has tokens for a ranked match.");
            }

            if (!isGuestUser(currentJoiningPlayer) && currentJoiningPlayer.tokens <= 0) {
                throw new IllegalStateException("You do not have enough tokens for a ranked match.");
            }

            if (!isGuestUser(waitingPlayer)) {
                waitingPlayer.tokens -= 1;
            }
            if (!isGuestUser(currentJoiningPlayer)) {
                currentJoiningPlayer.tokens -= 1;
            }
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

    private String createTournamentFinalIfReady(
            Transaction transaction,
            DocumentReference currentMatchReference,
            SharedMatchState currentState,
            String currentWinnerId,
            MatchContentBundle matchContent
    ) throws com.google.firebase.firestore.FirebaseFirestoreException {
        if (TextUtils.isEmpty(currentState.siblingMatchId)) {
            return "";
        }

        DocumentReference siblingReference = db.collection(MATCHES_COLLECTION).document(currentState.siblingMatchId);
        DocumentSnapshot siblingSnapshot = transaction.get(siblingReference);
        SharedMatchState siblingState = siblingSnapshot.toObject(SharedMatchState.class);
        if (siblingState == null
                || !siblingState.resultApplied
                || TextUtils.isEmpty(siblingState.winnerId)) {
            return "";
        }

        if (!TextUtils.isEmpty(currentState.finalMatchId)) {
            return currentState.finalMatchId;
        }
        if (!TextUtils.isEmpty(siblingState.finalMatchId)) {
            return siblingState.finalMatchId;
        }

        String firstWinnerId = currentState.tournamentSemiNumber == 1 ? currentWinnerId : siblingState.winnerId;
        String secondWinnerId = currentState.tournamentSemiNumber == 1 ? siblingState.winnerId : currentWinnerId;
        String firstWinnerName = currentState.tournamentSemiNumber == 1
                ? winnerNameForState(currentState, currentWinnerId)
                : winnerNameForState(siblingState, siblingState.winnerId);
        String secondWinnerName = currentState.tournamentSemiNumber == 1
                ? winnerNameForState(siblingState, siblingState.winnerId)
                : winnerNameForState(currentState, currentWinnerId);

        DocumentReference finalReference = db.collection(MATCHES_COLLECTION).document();
        SharedMatchState finalState = buildInitialState(
                finalReference.getId(),
                firstWinnerId,
                firstWinnerName,
                SharedMatchState.MATCH_TYPE_TOURNAMENT,
                matchContent
        );
        finalState.korakRounds = currentState.tournamentSemiNumber == 1
                ? copySharedKorakRounds(currentState.korakRounds)
                : copySharedKorakRounds(siblingState.korakRounds);
        finalState.playerTwoId = secondWinnerId;
        finalState.playerTwoName = secondWinnerName;
        finalState.tournamentId = TextUtils.isEmpty(currentState.tournamentId) ? currentMatchReference.getId() : currentState.tournamentId;
        finalState.tournamentStage = "FINAL";
        finalState.tournamentSemiNumber = 0;
        finalState.status = SharedMatchState.STATUS_ACTIVE;
        finalState.phase = SharedMatchState.PHASE_KZZ_QUESTION;
        finalState.phaseStartedAt = System.currentTimeMillis();
        finalState.phaseDurationSeconds = 5;
        finalState.phaseMessage = "Tournament final. Question 1/5. Both players can answer.";
        finalState.matchStartedAt = System.currentTimeMillis();
        finalState.updatedAt = System.currentTimeMillis();
        Map<String, Object> finalWinnerUpdates = new HashMap<>();
        finalWinnerUpdates.put("currentMatchId", finalReference.getId());
        finalWinnerUpdates.put("lastSeenAt", System.currentTimeMillis());

        Map<String, Object> siblingUpdates = new HashMap<>();
        siblingUpdates.put("finalMatchId", finalReference.getId());
        siblingUpdates.put("updatedAt", System.currentTimeMillis());

        transaction.set(finalReference, finalState);
        transaction.update(db.collection(USERS_COLLECTION).document(firstWinnerId), finalWinnerUpdates);
        transaction.update(db.collection(USERS_COLLECTION).document(secondWinnerId), finalWinnerUpdates);
        transaction.update(siblingReference, siblingUpdates);
        return finalReference.getId();
    }

    private String winnerNameForState(SharedMatchState state, String winnerId) {
        if (state == null || TextUtils.isEmpty(winnerId)) {
            return "Player";
        }
        if (winnerId.equals(state.playerOneId)) {
            return TextUtils.isEmpty(state.playerOneName) ? "Player" : state.playerOneName;
        }
        if (winnerId.equals(state.playerTwoId)) {
            return TextUtils.isEmpty(state.playerTwoName) ? "Player" : state.playerTwoName;
        }
        return "Player";
    }
    private void ensureUserDefaultsForTransaction(User user, String userId) {
        if (user == null) {
            return;
        }

        if (TextUtils.isEmpty(user.id)) {
            user.id = userId;
        }

        if (TextUtils.isEmpty(user.username)) {
            user.username = user.guest ? guestUsernameForId(userId) : "Player";
        }

        if (TextUtils.isEmpty(user.region)) {
            user.region = user.guest ? "" : "Serbia";
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

        user.weeklyGames = Math.max(0, user.weeklyGames);
        user.monthlyGames = Math.max(0, user.monthlyGames);
    }

    private int calculateStarDelta(
            int playerScore,
            boolean won,
            boolean lost,
            boolean forfeited
    ) throws com.google.firebase.firestore.FirebaseFirestoreException {
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

    private void ensureCycleKeysForTransaction(User user) {
        if (user == null) {
            return;
        }
        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.temporal.WeekFields weekFields = java.time.temporal.WeekFields.ISO;
        String currentWeeklyCycleKey = today.getYear() + "-W" + String.format(Locale.US, "%02d", today.get(weekFields.weekOfWeekBasedYear()));
        String currentMonthlyCycleKey = new java.text.SimpleDateFormat("yyyy-MM", Locale.US).format(new java.util.Date());
        if (!currentWeeklyCycleKey.equals(user.weeklyCycleKey)
                && canMoveToNewRankingCycle(user.weeklyCycleKey, user.lastWeeklyRewardCycle, user.weeklyGames)) {
            user.weeklyCycleKey = currentWeeklyCycleKey;
            user.weeklyStars = 0;
            user.weeklyGames = 0;
        }
        if (!currentMonthlyCycleKey.equals(user.monthlyCycleKey)
                && canMoveToNewRankingCycle(user.monthlyCycleKey, user.lastMonthlyRewardCycle, user.monthlyGames)) {
            user.monthlyCycleKey = currentMonthlyCycleKey;
            user.monthlyStars = 0;
            user.monthlyGames = 0;
        }
        if (TextUtils.isEmpty(user.dailyMissionDate)
                || !user.dailyMissionDate.equals(new java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new java.util.Date()))) {
            user.dailyMissionDate = new java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new java.util.Date());
            user.dailyMissionWinMatch = false;
            user.dailyMissionSendChat = false;
            user.dailyMissionPlayFriendly = false;
            user.dailyMissionWinTournament = false;
            user.dailyMissionBonusClaimed = false;
        }
    }
    private boolean canMoveToNewRankingCycle(String cycleKey, String lastRewardCycle, int cycleGames) {
        return TextUtils.isEmpty(cycleKey) || cycleGames <= 0 || cycleKey.equals(lastRewardCycle);
    }

    private void completeTournamentWinMissionsForTransaction(User user) {
        completeDailyMissionForTransaction(user, FirestoreRepository.MISSION_WIN_MATCH);
        completeDailyMissionForTransaction(user, FirestoreRepository.MISSION_WIN_TOURNAMENT);
    }
    private void completeDailyMissionForTransaction(User user, String mission) {
        if (user == null || TextUtils.isEmpty(mission)) {
            return;
        }
        ensureCycleKeysForTransaction(user);
        boolean newlyCompleted = false;
        if (FirestoreRepository.MISSION_WIN_MATCH.equals(mission) && !user.dailyMissionWinMatch) {
            user.dailyMissionWinMatch = true;
            newlyCompleted = true;
        } else if (FirestoreRepository.MISSION_SEND_CHAT.equals(mission) && !user.dailyMissionSendChat) {
            user.dailyMissionSendChat = true;
            newlyCompleted = true;
        } else if (FirestoreRepository.MISSION_PLAY_FRIENDLY.equals(mission) && !user.dailyMissionPlayFriendly) {
            user.dailyMissionPlayFriendly = true;
            newlyCompleted = true;
        } else if (FirestoreRepository.MISSION_WIN_TOURNAMENT.equals(mission) && !user.dailyMissionWinTournament) {
            user.dailyMissionWinTournament = true;
            newlyCompleted = true;
        }

        int starDelta = newlyCompleted ? 3 : 0;
        if (newlyCompleted
                && !user.dailyMissionBonusClaimed
                && user.dailyMissionWinMatch
                && user.dailyMissionSendChat
                && user.dailyMissionPlayFriendly
                && user.dailyMissionWinTournament) {
            user.dailyMissionBonusClaimed = true;
            user.tokens += 2;
            starDelta += 3;
        }
        applyDailyMissionStarsForTransaction(user, starDelta);
    }

    private void markRankingGamePlayedForTransaction(User user) {
        if (user == null) {
            return;
        }
        ensureCycleKeysForTransaction(user);
        user.weeklyGames++;
        user.monthlyGames++;
    }
    private void applyDailyMissionStarsForTransaction(User user, int starDelta) {
        if (user == null || starDelta <= 0) {
            return;
        }
        ensureCycleKeysForTransaction(user);
        user.stars = Math.max(0, user.stars + starDelta);
        user.weeklyStars += starDelta;
        user.monthlyStars += starDelta;
        user.league = com.tim14.slagalica.LeagueUtils.calculateLeague(user.stars);
    }
    private void applyTournamentSemiWinnerOutcome(User user, int starDelta) {
        int safeStarDelta = Math.max(0, starDelta);
        applyCompetitiveOutcome(user, safeStarDelta);
        user.tokens += 2;
    }
    private void applyTournamentFinalWinnerOutcome(User user, int starDelta) {
        int safeStarDelta = Math.max(0, starDelta) + 10;
        applyCompetitiveOutcome(user, safeStarDelta);
        user.tokens += 3;
    }
    private void applyCompetitiveOutcome(User user, int starDelta) {
        user.stars = Math.max(0, user.stars + starDelta);
        if (starDelta > 0) {
            ensureCycleKeysForTransaction(user);
            user.weeklyStars += starDelta;
            user.monthlyStars += starDelta;
        }
        user.league = com.tim14.slagalica.LeagueUtils.calculateLeague(user.stars);

        if (starDelta > 0) {
            user.earnedStarsSinceLastToken += starDelta;
            user.tokens += user.earnedStarsSinceLastToken / 50;
            user.earnedStarsSinceLastToken = user.earnedStarsSinceLastToken % 50;
        }

        user.lastSeenAt = System.currentTimeMillis();
    }

    private void applyDailyTokenGrantForTransaction(User user) {
        if (user == null || user.guest || TextUtils.isEmpty(user.lastTokenGrantDate)) {
            return;
        }

        try {
            java.time.LocalDate lastGrantDate = java.time.LocalDate.parse(user.lastTokenGrantDate);
            java.time.LocalDate today = java.time.LocalDate.now();
            long dayDiff = java.time.temporal.ChronoUnit.DAYS.between(lastGrantDate, today);
            if (dayDiff > 0) {
                user.tokens += (int) (dayDiff * com.tim14.slagalica.LeagueUtils.getDailyTokenGrant(user.league));
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

    private boolean isGuestUser(User user) {
        return user != null && user.guest;
    }

    private String guestUsernameForId(String userId) {
        if (TextUtils.isEmpty(userId)) {
            return "Guest";
        }

        String suffix = userId.substring(0, Math.min(6, userId.length())).toUpperCase(Locale.US);
        return "Guest-" + suffix;
    }

    private MatchFinalizationResult buildFinalizationResult(
            boolean applied,
            SharedMatchState state,
            String currentUserId
    ) {
        boolean friendlyMatch = state != null
                && SharedMatchState.MATCH_TYPE_FRIENDLY.equals(state.matchType);
        if (state == null) {
            return new MatchFinalizationResult(applied, friendlyMatch, "", "");
        }

        if (currentUserId.equals(state.playerOneId)) {
            return new MatchFinalizationResult(
                    applied,
                    friendlyMatch,
                    state.finalMatchId,
                    state.winnerId,
                    state.playerOnePreviousLeague,
                    state.playerOneCurrentLeague
            );
        }

        if (currentUserId.equals(state.playerTwoId)) {
            return new MatchFinalizationResult(
                    applied,
                    friendlyMatch,
                    state.finalMatchId,
                    state.winnerId,
                    state.playerTwoPreviousLeague,
                    state.playerTwoCurrentLeague
            );
        }

        return new MatchFinalizationResult(
                applied,
                friendlyMatch,
                state.finalMatchId,
                state.winnerId
        );
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
        public final String finalMatchId;
        public final String winnerId;
        public final int previousLeague;
        public final int currentLeague;
        public final boolean leagueChanged;

        public MatchFinalizationResult(boolean applied, boolean friendlyMatch, String finalMatchId, String winnerId) {
            this(applied, friendlyMatch, finalMatchId, winnerId, 0, 0);
        }

        public MatchFinalizationResult(
                boolean applied,
                boolean friendlyMatch,
                String finalMatchId,
                String winnerId,
                int previousLeague,
                int currentLeague
        ) {
            this.applied = applied;
            this.friendlyMatch = friendlyMatch;
            this.finalMatchId = finalMatchId == null ? "" : finalMatchId;
            this.winnerId = winnerId == null ? "" : winnerId;
            this.previousLeague = previousLeague;
            this.currentLeague = currentLeague;
            this.leagueChanged = previousLeague != currentLeague;
        }
    }
}
