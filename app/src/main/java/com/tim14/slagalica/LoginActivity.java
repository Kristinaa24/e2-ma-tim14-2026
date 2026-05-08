package com.tim14.slagalica;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {


    private EditText emailInput, passwordInput;
    private Button loginButton, registerButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        loginButton = findViewById(R.id.loginButton);
        registerButton = findViewById(R.id.registerButton);


        loginButton.setOnClickListener(v -> {
            String emailOrUsername = emailInput.getText().toString();
            String password = passwordInput.getText().toString();

            if (emailOrUsername.isEmpty()) {
                showToast(getString(R.string.enter_email_username));
                return;
            }

            if (password.isEmpty()) {
                showToast(getString(R.string.enter_password));
                return;
            }

            SessionManager.currentUser = new User(
                    "TestPlayer",
                    emailOrUsername,
                    "Vojvodina",
                    5,
                    120,
                    2
            );

            showToast(getString(R.string.login_successful));
            startActivity(new Intent(this, HomeActivity.class));
        });

        registerButton.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class))
        );




    }
    private void showToast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

}
