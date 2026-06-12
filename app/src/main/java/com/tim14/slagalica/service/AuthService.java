package com.tim14.slagalica.service;

import android.content.Context;
import android.util.Patterns;

import com.google.firebase.auth.FirebaseUser;
import com.tim14.slagalica.R;
import com.tim14.slagalica.model.User;
import com.tim14.slagalica.repository.AuthRepository;
import com.tim14.slagalica.repository.FirebaseCallback;
import com.tim14.slagalica.repository.FirestoreRepository;

public class AuthService {

    public interface SimpleCallback {
        void onSuccess();
        void onError(String error);
    }

    public interface LoginCallback {
        void onSuccess(LoginResult result);
        void onError(String error);
    }

    public static final class ValidationResult {
        private final boolean valid;
        private final int messageResId;

        private ValidationResult(boolean valid, int messageResId) {
            this.valid = valid;
            this.messageResId = messageResId;
        }

        public static ValidationResult success() {
            return new ValidationResult(true, 0);
        }

        public static ValidationResult error(int messageResId) {
            return new ValidationResult(false, messageResId);
        }

        public boolean isValid() {
            return valid;
        }

        public int getMessageResId() {
            return messageResId;
        }
    }

    public static final class LoginResult {
        private final FirebaseUser firebaseUser;
        private final User user;
        private final String submittedLoginValue;

        public LoginResult(FirebaseUser firebaseUser, User user, String submittedLoginValue) {
            this.firebaseUser = firebaseUser;
            this.user = user;
            this.submittedLoginValue = submittedLoginValue;
        }

        public FirebaseUser getFirebaseUser() {
            return firebaseUser;
        }

        public User getUser() {
            return user;
        }

        public String getSubmittedLoginValue() {
            return submittedLoginValue;
        }
    }

    private final Context context;
    private final AuthRepository authRepository;
    private final FirestoreRepository firestoreRepository;

    public AuthService(Context context) {
        this(
                context,
                new AuthRepository(context),
                new FirestoreRepository(context)
        );
    }

    public AuthService(
            Context context,
            AuthRepository authRepository,
            FirestoreRepository firestoreRepository
    ) {
        this.context = context.getApplicationContext();
        this.authRepository = authRepository;
        this.firestoreRepository = firestoreRepository;
    }

    public ValidationResult validateLoginInput(String emailOrUsername, String password) {
        if (emailOrUsername == null || emailOrUsername.trim().isEmpty()) {
            return ValidationResult.error(R.string.enter_email_username);
        }

        if (password == null || password.trim().isEmpty()) {
            return ValidationResult.error(R.string.enter_password);
        }

        return ValidationResult.success();
    }

    public ValidationResult validateRegistrationInput(
            String username,
            String email,
            String region,
            String password,
            String repeatPassword,
            String[] allowedRegions
    ) {
        if (isBlank(username) || isBlank(email) || isBlank(region)
                || isBlank(password) || isBlank(repeatPassword)) {
            return ValidationResult.error(R.string.fill_all_fields);
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
            return ValidationResult.error(R.string.invalid_email);
        }

        if (!isValidRegion(region, allowedRegions)) {
            return ValidationResult.error(R.string.select_region_error);
        }

        if (password.trim().length() < 6) {
            return ValidationResult.error(R.string.password_too_short);
        }

        if (!password.trim().equals(repeatPassword.trim())) {
            return ValidationResult.error(R.string.passwords_do_not_match);
        }

        return ValidationResult.success();
    }

    public ValidationResult validateResetEmail(String email) {
        if (isBlank(email)) {
            return ValidationResult.error(R.string.enter_email);
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
            return ValidationResult.error(R.string.invalid_email);
        }

        return ValidationResult.success();
    }

    public ValidationResult validatePasswordChange(
            String oldPassword,
            String newPassword,
            String repeatNewPassword
    ) {
        if (isBlank(oldPassword) || isBlank(newPassword) || isBlank(repeatNewPassword)) {
            return ValidationResult.error(R.string.fill_all_fields);
        }

        if (newPassword.trim().length() < 6) {
            return ValidationResult.error(R.string.password_too_short);
        }

        if (!newPassword.trim().equals(repeatNewPassword.trim())) {
            return ValidationResult.error(R.string.new_passwords_do_not_match);
        }

        return ValidationResult.success();
    }

    public void login(String emailOrUsername, String password, LoginCallback callback) {
        String trimmedLogin = emailOrUsername.trim();

        authRepository.login(trimmedLogin, password.trim(), new FirebaseCallback<FirebaseUser>() {
            @Override
            public void onSuccess(FirebaseUser firebaseUser) {
                if (firebaseUser == null || !firebaseUser.isEmailVerified()) {
                    authRepository.logout();
                    callback.onError(text(R.string.verify_email_before_login));
                    return;
                }

                firestoreRepository.getCurrentUser(new FirebaseCallback<User>() {
                    @Override
                    public void onSuccess(User user) {
                        callback.onSuccess(new LoginResult(firebaseUser, user, trimmedLogin));
                    }

                    @Override
                    public void onError(String error) {
                        callback.onSuccess(new LoginResult(firebaseUser, null, trimmedLogin));
                    }
                });
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    public void register(
            String username,
            String email,
            String region,
            String password,
            SimpleCallback callback
    ) {
        firestoreRepository.isUsernameTaken(username.trim(), new FirebaseCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean usernameTaken) {
                if (Boolean.TRUE.equals(usernameTaken)) {
                    callback.onError(text(R.string.username_taken));
                    return;
                }

                authRepository.register(email.trim(), password.trim(), new FirebaseCallback<FirebaseUser>() {
                    @Override
                    public void onSuccess(FirebaseUser firebaseUser) {
                        if (firebaseUser == null) {
                            callback.onError(text(R.string.auth_error_user_not_created));
                            return;
                        }

                        firestoreRepository.createUserProfile(
                                firebaseUser.getUid(),
                                username.trim(),
                                email.trim(),
                                region.trim(),
                                new FirebaseCallback<Void>() {
                                    @Override
                                    public void onSuccess(Void result) {
                                        authRepository.sendEmailVerification(
                                                firebaseUser,
                                                new FirebaseCallback<Void>() {
                                                    @Override
                                                    public void onSuccess(Void verificationResult) {
                                                        authRepository.logout();
                                                        callback.onSuccess();
                                                    }

                                                    @Override
                                                    public void onError(String error) {
                                                        callback.onError(error);
                                                    }
                                                }
                                        );
                                    }

                                    @Override
                                    public void onError(String error) {
                                        callback.onError(
                                                text(R.string.profile_setup_failed) + " " + error
                                        );
                                    }
                                }
                        );
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

    public void sendResetLink(String email, SimpleCallback callback) {
        authRepository.sendPasswordResetEmail(email.trim(), new FirebaseCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                callback.onSuccess();
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    public void changePassword(String oldPassword, String newPassword, SimpleCallback callback) {
        authRepository.changePassword(
                oldPassword.trim(),
                newPassword.trim(),
                new FirebaseCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        callback.onSuccess();
                    }

                    @Override
                    public void onError(String error) {
                        callback.onError(error);
                    }
                }
        );
    }

    public boolean isLoggedIn() {
        return authRepository.getCurrentUser() != null;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private boolean isValidRegion(String region, String[] allowedRegions) {
        if (allowedRegions == null) {
            return false;
        }

        String trimmedRegion = region == null ? "" : region.trim();

        for (String allowedRegion : allowedRegions) {
            if (allowedRegion.equals(trimmedRegion)) {
                return true;
            }
        }

        return false;
    }

    private String text(int stringResId) {
        return context.getString(stringResId);
    }
}
