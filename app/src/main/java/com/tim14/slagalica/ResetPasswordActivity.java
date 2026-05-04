package com.tim14.slagalica;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class ResetPasswordActivity extends AppCompatActivity {

    private EditText oldPasswordInput;
    private EditText newPasswordInput;
    private EditText repeatNewPasswordInput;
    private Button confirmResetButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        oldPasswordInput = findViewById(R.id.oldPasswordInput);
        newPasswordInput = findViewById(R.id.newPasswordInput);
        repeatNewPasswordInput = findViewById(R.id.repeatNewPasswordInput);
        confirmResetButton = findViewById(R.id.confirmResetButton);

        confirmResetButton.setOnClickListener(v -> {
            String oldPassword = oldPasswordInput.getText().toString();
            String newPassword = newPasswordInput.getText().toString();
            String repeatNewPassword = repeatNewPasswordInput.getText().toString();

            if (oldPassword.isEmpty() || newPassword.isEmpty() || repeatNewPassword.isEmpty()) {
                Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!newPassword.equals(repeatNewPassword)) {
                Toast.makeText(this, "New passwords do not match", Toast.LENGTH_SHORT).show();
                return;
            }

            Toast.makeText(this, "Password changed successfully", Toast.LENGTH_SHORT).show();
            finish();
        });
    }
}