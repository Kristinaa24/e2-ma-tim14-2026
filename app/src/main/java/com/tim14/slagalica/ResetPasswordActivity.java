package com.tim14.slagalica;

import android.app.AlertDialog;
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

            String oldPassword = oldPasswordInput.getText().toString().trim();
            String newPassword = newPasswordInput.getText().toString().trim();
            String repeatNewPassword = repeatNewPasswordInput.getText().toString().trim();

            if (oldPassword.isEmpty()
                    || newPassword.isEmpty()
                    || repeatNewPassword.isEmpty()) {

                resetErrorText.setText(getString(R.string.fill_all_fields));
                resetErrorText.setVisibility(View.VISIBLE);
                return;
            }

            if (!newPassword.equals(repeatNewPassword)) {

                resetErrorText.setText(
                        getString(R.string.new_passwords_do_not_match)
                );

                resetErrorText.setVisibility(View.VISIBLE);
                return;
            }

            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.confirm_password_change))
                    .setMessage(getString(R.string.confirm_password_change_message))

                    .setPositiveButton(getString(R.string.yes), (dialog, which) -> {

                        Toast.makeText(
                                this,
                                getString(R.string.password_changed_successfully),
                                Toast.LENGTH_LONG
                        ).show();

                        finish();
                    })

                    .setNegativeButton(getString(R.string.no), null)
                    .show();
        });
    }
}