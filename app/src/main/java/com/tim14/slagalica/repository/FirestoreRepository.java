package com.tim14.slagalica.repository;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.tim14.slagalica.model.User;
import com.tim14.slagalica.model.PlayerStatistics;
import com.tim14.slagalica.model.SpojniceRound;
import com.tim14.slagalica.model.KoZnaZnaQuestion;

import java.util.List;

public class FirestoreRepository {

    private static final String TAG = "REZ_DB";

    private static final String USERS_COLLECTION = "users";
    private static final String STATISTICS_COLLECTION = "statistics";
    private static final String KO_ZNA_ZNA_COLLECTION = "koZnaZnaQuestions";
    private static final String SPOJNICE_COLLECTION = "spojniceRounds";

    // Privremeno dok Student 1 ne poveže Firebase Auth.
    // Kasnije ćemo ovo zameniti pravim ID-em ulogovanog korisnika.
    public static final String TEST_USER_ID = "test_user_1";

    private final FirebaseFirestore db;

    public FirestoreRepository() {
        db = FirebaseFirestore.getInstance();
    }

    public void createTestUserIfNeeded() {
        Log.i(TAG, "createTestUserIfNeeded");

        DocumentReference userRef = db.collection(USERS_COLLECTION).document(TEST_USER_ID);

        userRef.get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        User user = new User(
                                TEST_USER_ID,
                                "Test User",
                                "test@example.com",
                                "Serbia",
                                200,
                                12,
                                1,
                                "None",
                                TEST_USER_ID
                        );
                        user.avatar = "avatar_1";

                        userRef.set(user)
                                .addOnSuccessListener(aVoid ->
                                        Log.d(TAG, "Test user successfully created."))
                                .addOnFailureListener(e ->
                                        Log.w(TAG, "Error creating test user.", e));

                        PlayerStatistics statistics = new PlayerStatistics(TEST_USER_ID);

                        db.collection(STATISTICS_COLLECTION)
                                .document(TEST_USER_ID)
                                .set(statistics)
                                .addOnSuccessListener(aVoid ->
                                        Log.d(TAG, "Test statistics successfully created."))
                                .addOnFailureListener(e ->
                                        Log.w(TAG, "Error creating test statistics.", e));
                    } else {
                        Log.d(TAG, "Test user already exists.");
                    }
                })
                .addOnFailureListener(e ->
                        Log.w(TAG, "Error checking test user.", e));
    }

    public void getCurrentUser(FirebaseCallback<User> callback) {
        Log.i(TAG, "getCurrentUser");

        db.collection(USERS_COLLECTION)
                .document(TEST_USER_ID)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    User user = documentSnapshot.toObject(User.class);

                    if (user == null) {
                        callback.onError("User not found.");
                    } else {
                        ensureUserDefaults(user);
                        db.collection(USERS_COLLECTION)
                                .document(TEST_USER_ID)
                                .set(user, SetOptions.merge());
                        callback.onSuccess(user);
                    }
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    private void ensureUserDefaults(User user) {
        if (user.id == null || user.id.trim().isEmpty()) {
            user.id = TEST_USER_ID;
        }

        if (user.username == null || user.username.trim().isEmpty()) {
            user.username = "Test User";
        }

        if (user.email == null || user.email.trim().isEmpty()) {
            user.email = "test@example.com";
        }

        if (user.region == null || user.region.trim().isEmpty()) {
            user.region = "Serbia";
        }

        if (user.avatar == null || user.avatar.trim().isEmpty()) {
            user.avatar = "avatar_1";
        }

        if (user.avatarFrame == null || user.avatarFrame.trim().isEmpty()
                || user.avatarFrame.equalsIgnoreCase("Diamond")
                || user.avatarFrame.equalsIgnoreCase("Platinum")
                || user.avatarFrame.equalsIgnoreCase("Master")) {
            user.avatarFrame = "None";
        }

        if (user.qrCode == null || user.qrCode.trim().isEmpty()
                || user.qrCode.equals("QR pending")
                || user.qrCode.equals("Available for friend invite")) {
            user.qrCode = user.id;
        }
    }

    public void getStatistics(FirebaseCallback<PlayerStatistics> callback) {
        Log.i(TAG, "getStatistics");

        db.collection(STATISTICS_COLLECTION)
                .document(TEST_USER_ID)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    PlayerStatistics statistics = documentSnapshot.toObject(PlayerStatistics.class);

                    if (statistics == null) {
                        statistics = new PlayerStatistics(TEST_USER_ID);

                        db.collection(STATISTICS_COLLECTION)
                                .document(TEST_USER_ID)
                                .set(statistics);
                    }

                    db.collection(STATISTICS_COLLECTION)
                            .document(TEST_USER_ID)
                            .set(statistics, SetOptions.merge());

                    callback.onSuccess(statistics);
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void updateAvatar(String avatar, FirebaseCallback<Void> callback) {
        Log.i(TAG, "updateAvatar");

        db.collection(USERS_COLLECTION)
                .document(TEST_USER_ID)
                .update("avatar", avatar)
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void getKoZnaZnaQuestions(FirebaseCallback<List<KoZnaZnaQuestion>> callback) {
        Log.i(TAG, "getKoZnaZnaQuestions");

        db.collection(KO_ZNA_ZNA_COLLECTION)
                .limit(5)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<KoZnaZnaQuestion> questions =
                            queryDocumentSnapshots.toObjects(KoZnaZnaQuestion.class);

                    callback.onSuccess(questions);
                })
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

                                List<SpojniceRound> rounds = new java.util.ArrayList<>();
                                rounds.add(round1);
                                rounds.add(round2);

                                callback.onSuccess(rounds);
                            })
                            .addOnFailureListener(e -> callback.onError(e.getMessage()));
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    private SpojniceRound createSpojniceRoundFromDocument(
            com.google.firebase.firestore.DocumentSnapshot document
    ) {
        SpojniceRound round = new SpojniceRound();

        round.setId(document.getId());
        round.setTitle(document.getString("title"));
        round.setLeftItems((List<String>) document.get("leftItems"));
        round.setCorrectRightItems((List<String>) document.get("correctRightItems"));
        round.setDisplayedRightItems((List<String>) document.get("displayedRightItems"));

        Log.d(TAG, "Loaded round: " + round.getId());
        Log.d(TAG, "Left items: " + round.getLeftItems());
        Log.d(TAG, "Correct right items: " + round.getCorrectRightItems());
        Log.d(TAG, "Displayed right items: " + round.getDisplayedRightItems());

        return round;
    }

    public void updateKoZnaZnaStatistics(int correctAnswers, int wrongAnswers, int score) {
        Log.i(TAG, "updateKoZnaZnaStatistics");

        db.collection(STATISTICS_COLLECTION)
                .document(TEST_USER_ID)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    PlayerStatistics statistics = documentSnapshot.toObject(PlayerStatistics.class);

                    if (statistics == null) {
                        statistics = new PlayerStatistics(TEST_USER_ID);
                    }

                    statistics.koZnaZnaCorrect += correctAnswers;
                    statistics.koZnaZnaWrong += wrongAnswers;
                    statistics.koZnaZnaTotalScore += score;

                    db.collection(STATISTICS_COLLECTION)
                            .document(TEST_USER_ID)
                            .set(statistics)
                            .addOnSuccessListener(aVoid ->
                                    Log.d(TAG, "Ko zna zna statistics updated."))
                            .addOnFailureListener(e ->
                                    Log.w(TAG, "Error updating Ko zna zna statistics.", e));
                })
                .addOnFailureListener(e ->
                        Log.w(TAG, "Error reading statistics.", e));
    }

    public void updateSpojniceStatistics(int correctPairs, int totalPairs, int score) {
        Log.i(TAG, "updateSpojniceStatistics");

        db.collection(STATISTICS_COLLECTION)
                .document(TEST_USER_ID)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    PlayerStatistics statistics = documentSnapshot.toObject(PlayerStatistics.class);

                    if (statistics == null) {
                        statistics = new PlayerStatistics(TEST_USER_ID);
                    }

                    statistics.spojnicaCorrectPairs += correctPairs;
                    statistics.spojnicaTotalPairs += totalPairs;
                    statistics.spojnicaTotalScore += score;

                    db.collection(STATISTICS_COLLECTION)
                            .document(TEST_USER_ID)
                            .set(statistics)
                            .addOnSuccessListener(aVoid ->
                                    Log.d(TAG, "Spojnice statistics updated."))
                            .addOnFailureListener(e ->
                                    Log.w(TAG, "Error updating Spojnice statistics.", e));
                })
                .addOnFailureListener(e ->
                        Log.w(TAG, "Error reading statistics.", e));
    }
}
