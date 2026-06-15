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

    private final FirebaseFirestore db;
    private final FirestoreRepository firestoreRepository;
    private final LocalGameRepository localGameRepository;

    public SharedMatchRepository() {
        db = FirebaseFirestore.getInstance();
        firestoreRepository = new FirestoreRepository();
        localGameRepository = new LocalGameRepository();
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
                SharedMatchState state = buildInitialState(reference.getId(), userId, safeUserName(user));

                reference.set(state)
                        .addOnSuccessListener(unused -> callback.onSuccess(
                                new MatchJoinResult(reference.getId(), 1, state.roomCode)
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
                                                new MatchJoinResult(snapshot.getId(), 2, state.roomCode)
                                        ))
                                        .addOnFailureListener(e -> callback.onError(e.getMessage()));
                                return;
                            }

                            callback.onSuccess(new MatchJoinResult(snapshot.getId(), 1, state.roomCode));
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
                previousState.playerOneName
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

    private SharedMatchState buildInitialState(String documentId, String playerOneId, String playerOneName) {
        SharedMatchState state = new SharedMatchState();
        state.roomCode = documentId.substring(0, Math.min(6, documentId.length())).toUpperCase(Locale.US);
        state.status = SharedMatchState.STATUS_WAITING;
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
                sharedRound.openedFields.add(new ArrayList<>(Arrays.asList(false, false, false, false)));
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
            put("updatedAt", System.currentTimeMillis());
        }};
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

        public MatchJoinResult(String matchId, int localPlayerNumber, String roomCode) {
            this.matchId = matchId;
            this.localPlayerNumber = localPlayerNumber;
            this.roomCode = roomCode;
        }
    }
}
