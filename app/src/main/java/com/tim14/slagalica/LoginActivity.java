package com.tim14.slagalica;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseUser;
import com.tim14.slagalica.model.User;
import com.tim14.slagalica.service.AuthService;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    private EditText emailInput;
    private EditText passwordInput;
    private Button loginButton;
    private Button registerButton;
    private TextView forgotPasswordText;
    private TextView errorText;

    private AuthService authService;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        Log.d(TAG, "onCreate");

        authService = new AuthService(this);
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

        AuthService.ValidationResult validationResult =
                authService.validateLoginInput(emailOrUsername, password);

        if (!validationResult.isValid()) {
            errorText.setText(getString(validationResult.getMessageResId()));
            errorText.setVisibility(View.VISIBLE);
            return;
        }

        setLoadingState(true);

        authService.login(emailOrUsername, password, new AuthService.LoginCallback() {
            @Override
            public void onSuccess(AuthService.LoginResult result) {
                User user = result.getUser();
                FirebaseUser firebaseUser = result.getFirebaseUser();

                if (user != null) {
                    sessionManager.saveUser(user);
                } else {
                    sessionManager.saveLogin(
                            firebaseUser.getUid(),
                            firebaseUser.getEmail(),
                            result.getSubmittedLoginValue()
                    );
                }

                showToast(getString(R.string.login_successful));
                openHome();
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

    @Override protected void onStart() { super.onStart(); Log.d(TAG, "onStart"); }
    @Override protected void onRestart() { super.onRestart(); Log.d(TAG, "onRestart"); }
    @Override protected void onResume() { super.onResume(); Log.d(TAG, "onResume"); }
    @Override protected void onPause() { super.onPause(); Log.d(TAG, "onPause"); }
    @Override protected void onStop() { super.onStop(); Log.d(TAG, "onStop"); }
    @Override protected void onDestroy() { super.onDestroy(); Log.d(TAG, "onDestroy"); }
}
