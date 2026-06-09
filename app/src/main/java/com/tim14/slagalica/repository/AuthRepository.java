package com.tim14.slagalica.repository;

import android.text.TextUtils;
import android.util.Patterns;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.tim14.slagalica.model.User;

public class AuthRepository {

    private static final String USERS_COLLECTION = "users";

    private final FirebaseAuth auth;
    private final FirebaseFirestore db;

    public AuthRepository() {
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
    }

    public void login(String emailOrUsername, String password, FirebaseCallback<FirebaseUser> callback) {
        String trimmedValue = emailOrUsername.trim();

        if (isEmail(trimmedValue)) {
            signInWithEmail(trimmedValue, password, callback);
            return;
        }

        db.collection(USERS_COLLECTION)
                .whereEqualTo("username", trimmedValue)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        callback.onError("User with that username does not exist.");
                        return;
                    }

                    User user = querySnapshot.getDocuments().get(0).toObject(User.class);

                    if (user == null || TextUtils.isEmpty(user.email)) {
                        callback.onError("Email for that username could not be found.");
                        return;
                    }

                    signInWithEmail(user.email, password, callback);
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void register(String email, String password, FirebaseCallback<FirebaseUser> callback) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> callback.onSuccess(result.getUser()))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void sendPasswordResetEmail(String email, FirebaseCallback<Void> callback) {
        auth.sendPasswordResetEmail(email)
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void changePassword(
            String oldPassword,
            String newPassword,
            FirebaseCallback<Void> callback
    ) {
        FirebaseUser currentUser = auth.getCurrentUser();

        if (currentUser == null || TextUtils.isEmpty(currentUser.getEmail())) {
            callback.onError("User must be logged in to change password.");
            return;
        }

        AuthCredential credential = EmailAuthProvider.getCredential(
                currentUser.getEmail(),
                oldPassword
        );

        currentUser.reauthenticate(credential)
                .addOnSuccessListener(unused ->
                        currentUser.updatePassword(newPassword)
                                .addOnSuccessListener(result -> callback.onSuccess(null))
                                .addOnFailureListener(e -> callback.onError(e.getMessage()))
                )
                .addOnFailureListener(e -> callback.onError("Old password is not correct."));
    }

    public FirebaseUser getCurrentUser() {
        return auth.getCurrentUser();
    }

    public void logout() {
        auth.signOut();
    }

    private void signInWithEmail(
            String email,
            String password,
            FirebaseCallback<FirebaseUser> callback
    ) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> callback.onSuccess(result.getUser()))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    private boolean isEmail(String value) {
        return Patterns.EMAIL_ADDRESS.matcher(value).matches();
    }
}
