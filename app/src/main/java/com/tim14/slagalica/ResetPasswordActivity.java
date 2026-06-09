package com.tim14.slagalica;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.tim14.slagalica.repository.AuthRepository;
import com.tim14.slagalica.repository.FirebaseCallback;

public class ResetPasswordActivity extends AppCompatActivity {

    private TextView resetDescriptionText;
    private EditText emailInput;
    private EditText oldPasswordInput;
    private EditText newPasswordInput;
    private EditText repeatNewPasswordInput;
    private Button confirmResetButton;
    private TextView resetErrorText;
    private View changePasswordFieldsGroup;

    private AuthRepository authRepository;
    private boolean loggedInMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        authRepository = new AuthRepository();
        loggedInMode = authRepository.getCurrentUser() != null;

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

        if (email.isEmpty()) {
            showError(R.string.enter_email);
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError(R.string.invalid_email);
            return;
        }

        setLoadingState(true);

        authRepository.sendPasswordResetEmail(email, new FirebaseCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
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

        if (oldPassword.isEmpty() || newPassword.isEmpty() || repeatNewPassword.isEmpty()) {
            showError(R.string.fill_all_fields);
            return;
        }

        if (newPassword.length() < 6) {
            showError(R.string.password_too_short);
            return;
        }

        if (!newPassword.equals(repeatNewPassword)) {
            showError(R.string.new_passwords_do_not_match);
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

        authRepository.changePassword(oldPassword, newPassword, new FirebaseCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
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
}
