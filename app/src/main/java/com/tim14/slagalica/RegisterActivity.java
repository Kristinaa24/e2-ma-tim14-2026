package com.tim14.slagalica;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.tim14.slagalica.service.AuthService;

public class RegisterActivity extends AppCompatActivity {

    private static final String TAG = "RegisterActivity";

    private EditText usernameInput;
    private EditText emailInput;
    private AutoCompleteTextView regionInput;
    private EditText passwordInput;
    private EditText repeatPasswordInput;
    private Button registerButton;
    private TextView registerErrorText;
    private TextView backToLoginText;

    private AuthService authService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        Log.d(TAG, "onCreate");

        authService = new AuthService(this);

        usernameInput = findViewById(R.id.usernameInput);
        emailInput = findViewById(R.id.emailInput);
        regionInput = findViewById(R.id.regionInput);
        passwordInput = findViewById(R.id.passwordInput);
        repeatPasswordInput = findViewById(R.id.repeatPasswordInput);
        registerButton = findViewById(R.id.registerButton);
        registerErrorText = findViewById(R.id.registerErrorText);
        backToLoginText = findViewById(R.id.backToLoginText);

        setupRegionDropdown();
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

        AuthService.ValidationResult validationResult = authService.validateRegistrationInput(
                username,
                email,
                region,
                password,
                repeatPassword,
                getResources().getStringArray(R.array.serbia_regions)
        );

        if (!validationResult.isValid()) {
            showError(validationResult.getMessageResId());
            return;
        }

        setLoadingState(true);

        authService.register(username, email, region, password, new AuthService.SimpleCallback() {
            @Override
            public void onSuccess() {
                openLoginAfterRegistration();
            }

            @Override
            public void onError(String error) {
                setLoadingState(false);
                showError(error);
            }
        });
    }

    private void openLoginAfterRegistration() {
        setLoadingState(false);
        showToast(getString(R.string.registration_verification_sent));

        Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    private void setupRegionDropdown() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.serbia_regions,
                android.R.layout.simple_dropdown_item_1line
        );

        regionInput.setAdapter(adapter);
        regionInput.setOnClickListener(v -> regionInput.showDropDown());
        regionInput.setThreshold(0);
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

    @Override protected void onStart() { super.onStart(); Log.d(TAG, "onStart"); }
    @Override protected void onRestart() { super.onRestart(); Log.d(TAG, "onRestart"); }
    @Override protected void onResume() { super.onResume(); Log.d(TAG, "onResume"); }
    @Override protected void onPause() { super.onPause(); Log.d(TAG, "onPause"); }
    @Override protected void onStop() { super.onStop(); Log.d(TAG, "onStop"); }
    @Override protected void onDestroy() { super.onDestroy(); Log.d(TAG, "onDestroy"); }
}
