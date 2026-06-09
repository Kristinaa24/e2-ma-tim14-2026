package com.tim14.slagalica;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseUser;
import com.tim14.slagalica.repository.AuthRepository;
import com.tim14.slagalica.repository.FirebaseCallback;
import com.tim14.slagalica.repository.FirestoreRepository;

public class RegisterActivity extends AppCompatActivity {

    private EditText usernameInput;
    private EditText emailInput;
    private EditText regionInput;
    private EditText passwordInput;
    private EditText repeatPasswordInput;
    private Button registerButton;
    private TextView registerErrorText;
    private TextView backToLoginText;

    private AuthRepository authRepository;
    private FirestoreRepository firestoreRepository;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        authRepository = new AuthRepository();
        firestoreRepository = new FirestoreRepository();
        sessionManager = new SessionManager(this);

        usernameInput = findViewById(R.id.usernameInput);
        emailInput = findViewById(R.id.emailInput);
        regionInput = findViewById(R.id.regionInput);
        passwordInput = findViewById(R.id.passwordInput);
        repeatPasswordInput = findViewById(R.id.repeatPasswordInput);
        registerButton = findViewById(R.id.registerButton);
        registerErrorText = findViewById(R.id.registerErrorText);
        backToLoginText = findViewById(R.id.backToLoginText);

        registerButton.setOnClickListener(v -> registerUser());

        backToLoginText.setOnClickListener(v -> {
            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void registerUser() {
        String username = usernameInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();
        String region = regionInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();
        String repeatPassword = repeatPasswordInput.getText().toString().trim();

        registerErrorText.setVisibility(View.GONE);

        if (username.isEmpty() || email.isEmpty() || region.isEmpty()
                || password.isEmpty() || repeatPassword.isEmpty()) {
            showError(R.string.fill_all_fields);
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError(R.string.invalid_email);
            return;
        }

        if (password.length() < 6) {
            showError(R.string.password_too_short);
            return;
        }

        if (!password.equals(repeatPassword)) {
            showError(R.string.passwords_do_not_match);
            return;
        }

        setLoadingState(true);

        firestoreRepository.isUsernameTaken(username, new FirebaseCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean usernameTaken) {
                if (Boolean.TRUE.equals(usernameTaken)) {
                    setLoadingState(false);
                    showError(R.string.username_taken);
                    return;
                }

                authRepository.register(email, password, new FirebaseCallback<FirebaseUser>() {
                    @Override
                    public void onSuccess(FirebaseUser firebaseUser) {
                        firestoreRepository.createUserProfile(
                                firebaseUser.getUid(),
                                username,
                                email,
                                region,
                                new FirebaseCallback<Void>() {
                                    @Override
                                    public void onSuccess(Void result) {
                                        sessionManager.saveLogin(firebaseUser.getUid(), email, username);
                                        showToast(getString(R.string.registration_success));
                                        openHome();
                                    }

                                    @Override
                                    public void onError(String error) {
                                        setLoadingState(false);
                                        showError(getString(R.string.profile_setup_failed) + " " + error);
                                    }
                                }
                        );
                    }

                    @Override
                    public void onError(String error) {
                        setLoadingState(false);
                        showError(error);
                    }
                });
            }

            @Override
            public void onError(String error) {
                setLoadingState(false);
                showError(error);
            }
        });
    }

    private void openHome() {
        setLoadingState(false);

        Intent intent = new Intent(RegisterActivity.this, HomeActivity.class);
        intent.putExtra("IS_GUEST", false);
        startActivity(intent);
        finish();
    }

    private void showError(int messageResId) {
        showError(getString(messageResId));
    }

    private void showError(String message) {
        registerErrorText.setText(message);
        registerErrorText.setVisibility(View.VISIBLE);
    }

    private void setLoadingState(boolean isLoading) {
        registerButton.setEnabled(!isLoading);
        backToLoginText.setEnabled(!isLoading);
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}
