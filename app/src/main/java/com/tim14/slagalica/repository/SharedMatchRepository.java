package com.tim14.slagalica.repository;

import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.tim14.slagalica.game.GameRound;
import com.tim14.slagalica.model.KorakPoKorakRound;
import com.tim14.slagalica.model.SharedKorakPoKorakRound;
import com.tim14.slagalica.model.SharedMatchState;
import com.tim14.slagalica.model.SharedMojBrojRound;
import com.tim14.slagalica.model.User;

import java.util.ArrayList;
import java.util.Arrays;
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

    private SharedMatchState buildInitialState(String documentId, String playerOneId, String playerOneName) {
        SharedMatchState state = new SharedMatchState();
        state.roomCode = documentId.substring(0, Math.min(6, documentId.length())).toUpperCase(Locale.US);
        state.status = SharedMatchState.STATUS_WAITING;
        state.currentRound = GameRound.KORAK_PO_KORAK.name();
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
        state.updatedAt = System.currentTimeMillis();

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
        return new java.util.HashMap<String, Object>() {{
            put("playerTwoId", playerTwoId);
            put("playerTwoName", playerTwoName);
            put("status", SharedMatchState.STATUS_ACTIVE);
            put("currentRound", GameRound.KORAK_PO_KORAK.name());
            put("phase", SharedMatchState.PHASE_KPP_STARTER);
            put("activePlayer", 1);
            put("currentTurnIndex", 0);
            put("phaseStartedAt", System.currentTimeMillis());
            put("phaseDurationSeconds", 70);
            put("phaseMessage", "Round 1/2. Player 1 is solving.");
            put("revealedAnswer", "");
            put("playerOneExpression", "");
            put("playerTwoExpression", "");
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
