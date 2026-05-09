package com.tim14.slagalica;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class ResetPasswordActivity extends AppCompatActivity {

    private EditText oldPasswordInput;
    private EditText newPasswordInput;
    private EditText repeatNewPasswordInput;
    private Button confirmResetButton;
    private TextView resetErrorText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        oldPasswordInput = findViewById(R.id.oldPasswordInput);
        newPasswordInput = findViewById(R.id.newPasswordInput);
        repeatNewPasswordInput = findViewById(R.id.repeatNewPasswordInput);
        confirmResetButton = findViewById(R.id.confirmResetButton);
        resetErrorText = findViewById(R.id.resetErrorText);

        confirmResetButton.setOnClickListener(v -> {
            resetErrorText.setVisibility(View.GONE);

            String oldPassword = oldPasswordInput.getText().toString();
            String newPassword = newPasswordInput.getText().toString();
            String repeatNewPassword = repeatNewPasswordInput.getText().toString();

            if (oldPassword.isEmpty() || newPassword.isEmpty() || repeatNewPassword.isEmpty()) {
                resetErrorText.setText(getString(R.string.fill_all_fields));
                resetErrorText.setVisibility(View.VISIBLE);
                return;
            }

            if (!newPassword.equals(repeatNewPassword)) {
                resetErrorText.setText(getString(R.string.new_passwords_do_not_match));
                resetErrorText.setVisibility(View.VISIBLE);
                return;
            }

            Toast.makeText(this, getString(R.string.password_changed_successfully), Toast.LENGTH_LONG).show();
            finish();
        });
    }
}