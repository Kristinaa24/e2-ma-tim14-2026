package com.tim14.slagalica;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class RegisterActivity extends AppCompatActivity {

    private EditText usernameInput;
    private EditText emailInput;
    private EditText regionInput;
    private EditText passwordInput;
    private EditText repeatPasswordInput;
    private Button registerButton;
    private TextView registerErrorText;
    private TextView backToLoginText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        usernameInput = findViewById(R.id.usernameInput);
        emailInput = findViewById(R.id.emailInput);
        regionInput = findViewById(R.id.regionInput);
        passwordInput = findViewById(R.id.passwordInput);
        repeatPasswordInput = findViewById(R.id.repeatPasswordInput);
        registerButton = findViewById(R.id.registerButton);
        registerErrorText = findViewById(R.id.registerErrorText);
        backToLoginText = findViewById(R.id.backToLoginText);

        registerButton.setOnClickListener(v -> {
            String username = usernameInput.getText().toString();
            String email = emailInput.getText().toString();
            String region = regionInput.getText().toString();
            String password = passwordInput.getText().toString();
            String repeatPassword = repeatPasswordInput.getText().toString();

            registerErrorText.setVisibility(View.GONE);

            if (username.isEmpty() || email.isEmpty() || region.isEmpty() || password.isEmpty() || repeatPassword.isEmpty()) {
                registerErrorText.setText(getString(R.string.fill_all_fields));
                registerErrorText.setVisibility(View.VISIBLE);
                return;
            }

            if (!password.equals(repeatPassword)) {
                registerErrorText.setText(getString(R.string.passwords_do_not_match));
                registerErrorText.setVisibility(View.VISIBLE);
                return;
            }


            Toast.makeText(this, getString(R.string.registration_successful), Toast.LENGTH_LONG).show();

            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });

        backToLoginText.setOnClickListener(v -> {
            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });

    }
}