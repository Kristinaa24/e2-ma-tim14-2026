package com.tim14.slagalica;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseUser;
import com.tim14.slagalica.model.User;
import com.tim14.slagalica.repository.AuthRepository;
import com.tim14.slagalica.repository.FirebaseCallback;
import com.tim14.slagalica.repository.FirestoreRepository;

public class LoginActivity extends AppCompatActivity {

    private EditText emailInput;
    private EditText passwordInput;
    private Button loginButton;
    private Button registerButton;
    private TextView forgotPasswordText;
    private TextView errorText;

    private AuthRepository authRepository;
    private FirestoreRepository firestoreRepository;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        authRepository = new AuthRepository();
        firestoreRepository = new FirestoreRepository();
        sessionManager = new SessionManager(this);

        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        loginButton = findViewById(R.id.loginButton);
        registerButton = findViewById(R.id.registerButton);
        forgotPasswordText = findViewById(R.id.forgotPasswordText);
        errorText = findViewById(R.id.errorText);

        loginButton.setOnClickListener(v -> loginUser());
        registerButton.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class))
        );
        forgotPasswordText.setOnClickListener(v ->
                startActivity(new Intent(this, ResetPasswordActivity.class))
        );
    }

    private void loginUser() {
        String emailOrUsername = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        errorText.setVisibility(View.GONE);

        if (emailOrUsername.isEmpty()) {
            errorText.setText(getString(R.string.enter_email_username));
            errorText.setVisibility(View.VISIBLE);
            return;
        }

        if (password.isEmpty()) {
            errorText.setText(getString(R.string.enter_password));
            errorText.setVisibility(View.VISIBLE);
            return;
        }

        setLoadingState(true);

        authRepository.login(emailOrUsername, password, new FirebaseCallback<FirebaseUser>() {
            @Override
            public void onSuccess(FirebaseUser result) {
                firestoreRepository.getCurrentUser(new FirebaseCallback<User>() {
                    @Override
                    public void onSuccess(User user) {
                        sessionManager.saveUser(user);
                        showToast(getString(R.string.login_successful));
                        openHome();
                    }

                    @Override
                    public void onError(String error) {
                        sessionManager.saveLogin(
                                result.getUid(),
                                result.getEmail(),
                                emailOrUsername
                        );
                        showToast(getString(R.string.login_successful));
                        openHome();
                    }
                });
            }

            @Override
            public void onError(String error) {
                setLoadingState(false);
                errorText.setText(error);
                errorText.setVisibility(View.VISIBLE);
            }
        });
    }

    private void openHome() {
        setLoadingState(false);

        Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
        intent.putExtra("IS_GUEST", false);
        startActivity(intent);
        finish();
    }

    private void setLoadingState(boolean isLoading) {
        loginButton.setEnabled(!isLoading);
        registerButton.setEnabled(!isLoading);
        forgotPasswordText.setEnabled(!isLoading);
    }

    private void showToast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_LONG).show();
    }
}
