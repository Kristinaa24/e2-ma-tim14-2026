package com.tim14.slagalica;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.view.View;
import android.widget.TextView;
import com.tim14.slagalica.model.User;
import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {


    private EditText emailInput, passwordInput;
    private Button loginButton, registerButton;
    private TextView errorText;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        loginButton = findViewById(R.id.loginButton);
        registerButton = findViewById(R.id.registerButton);
        errorText = findViewById(R.id.errorText);



        loginButton.setOnClickListener(v -> {
            String emailOrUsername = emailInput.getText().toString().trim();
            String password = passwordInput.getText().toString();

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

            SessionManager.currentUser = new User(
                    "TestPlayer",
                    emailOrUsername,
                    "Vojvodina",
                    5,
                    120,
                    2
            );

            showToast(getString(R.string.login_successful));

            Intent intent = new Intent(
                    LoginActivity.this,
                    HomeActivity.class
            );

            intent.putExtra("IS_GUEST", false);

            startActivity(intent);
            finish();
        });

        registerButton.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class))
        );





    }
    private void showToast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_LONG).show();
    }

}
