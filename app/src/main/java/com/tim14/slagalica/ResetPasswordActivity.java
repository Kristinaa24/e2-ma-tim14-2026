package com.tim14.slagalica;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.tim14.slagalica.service.AuthService;

public class ResetPasswordActivity extends AppCompatActivity {

    private static final String TAG = "ResetPasswordActivity";

    private TextView resetDescriptionText;
    private EditText emailInput;
    private EditText oldPasswordInput;
    private EditText newPasswordInput;
    private EditText repeatNewPasswordInput;
    private Button confirmResetButton;
    private TextView resetErrorText;
    private View changePasswordFieldsGroup;

    private AuthService authService;
    private boolean loggedInMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        Log.d(TAG, "onCreate");

        authService = new AuthService(this);
        loggedInMode = authService.isLoggedIn();

        resetDescriptionText = findViewById(R.id.resetDescriptionText);
        emailInput = findViewById(R.id.resetEmailInput);
        oldPasswordInput = findViewById(R.id.oldPasswordInput);
        newPasswordInput = findViewById(R.id.newPasswordInput);
        repeatNewPasswordInput = findViewById(R.id.repeatNewPasswordInput);
        confirmResetButton = findViewById(R.id.confirmResetButton);
        resetErrorText = findViewById(R.id.resetErrorText);
        changePasswordFieldsGroup = findViewById(R.id.changePasswordFieldsGroup);

        configureScreenMode();
        confirmResetButton.setOnClickListener(v -> {
            if (loggedInMode) {
                confirmPasswordChange();
            } else {
                sendResetLink();
            }
        });
    }

    private void configureScreenMode() {
        if (loggedInMode) {
            resetDescriptionText.setText(R.string.change_password_message);
            emailInput.setVisibility(View.GONE);
            changePasswordFieldsGroup.setVisibility(View.VISIBLE);
            confirmResetButton.setText(R.string.confirm);
            return;
        }

        resetDescriptionText.setText(R.string.reset_password_email_message);
        emailInput.setVisibility(View.VISIBLE);
        changePasswordFieldsGroup.setVisibility(View.GONE);
        confirmResetButton.setText(R.string.send_reset_link);
    }

    private void sendResetLink() {
        resetErrorText.setVisibility(View.GONE);

        String email = emailInput.getText().toString().trim();

        AuthService.ValidationResult validationResult = authService.validateResetEmail(email);

        if (!validationResult.isValid()) {
            showError(validationResult.getMessageResId());
            return;
        }

        setLoadingState(true);

        authService.sendResetLink(email, new AuthService.SimpleCallback() {
            @Override
            public void onSuccess() {
                setLoadingState(false);
                Toast.makeText(
                        ResetPasswordActivity.this,
                        getString(R.string.reset_link_sent),
                        Toast.LENGTH_LONG
                ).show();
                finish();
            }

            @Override
            public void onError(String error) {
                setLoadingState(false);
                showError(error);
            }
        });
    }

    private void confirmPasswordChange() {
        resetErrorText.setVisibility(View.GONE);

        String oldPassword = oldPasswordInput.getText().toString().trim();
        String newPassword = newPasswordInput.getText().toString().trim();
        String repeatNewPassword = repeatNewPasswordInput.getText().toString().trim();

        AuthService.ValidationResult validationResult =
                authService.validatePasswordChange(oldPassword, newPassword, repeatNewPassword);

        if (!validationResult.isValid()) {
            showError(validationResult.getMessageResId());
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.confirm_password_change))
                .setMessage(getString(R.string.confirm_password_change_message))
                .setPositiveButton(getString(R.string.yes), (dialog, which) ->
                        changePassword(oldPassword, newPassword)
                )
                .setNegativeButton(getString(R.string.no), null)
                .show();
    }

    private void changePassword(String oldPassword, String newPassword) {
        setLoadingState(true);

        authService.changePassword(oldPassword, newPassword, new AuthService.SimpleCallback() {
            @Override
            public void onSuccess() {
                setLoadingState(false);
                Toast.makeText(
                        ResetPasswordActivity.this,
                        getString(R.string.password_changed_successfully),
                        Toast.LENGTH_LONG
                ).show();
                finish();
            }

            @Override
            public void onError(String error) {
                setLoadingState(false);
                showError(error);
            }
        });
    }

    private void showError(int messageResId) {
        showError(getString(messageResId));
    }

    private void showError(String message) {
        resetErrorText.setText(message);
        resetErrorText.setVisibility(View.VISIBLE);
    }

    private void setLoadingState(boolean isLoading) {
        confirmResetButton.setEnabled(!isLoading);
    }

    @Override protected void onStart() { super.onStart(); Log.d(TAG, "onStart"); }
    @Override protected void onRestart() { super.onRestart(); Log.d(TAG, "onRestart"); }
    @Override protected void onResume() { super.onResume(); Log.d(TAG, "onResume"); }
    @Override protected void onPause() { super.onPause(); Log.d(TAG, "onPause"); }
    @Override protected void onStop() { super.onStop(); Log.d(TAG, "onStop"); }
    @Override protected void onDestroy() { super.onDestroy(); Log.d(TAG, "onDestroy"); }
}
