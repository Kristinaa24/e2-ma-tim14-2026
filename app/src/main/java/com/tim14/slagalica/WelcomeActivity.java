package com.tim14.slagalica;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class WelcomeActivity extends AppCompatActivity {

    private Button loginButton;
    private Button registerButton;
    private Button guestButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        loginButton = findViewById(R.id.loginButton);
        registerButton = findViewById(R.id.registerButton);
        guestButton = findViewById(R.id.guestButton);

        loginButton.setOnClickListener(v -> {
            Intent intent = new Intent(
                    WelcomeActivity.this,
                    LoginActivity.class
            );
            startActivity(intent);
        });

        registerButton.setOnClickListener(v -> {
            Intent intent = new Intent(
                    WelcomeActivity.this,
                    RegisterActivity.class
            );
            startActivity(intent);
        });

        guestButton.setOnClickListener(v -> {
            Intent intent = new Intent(
                    WelcomeActivity.this,
                    HomeActivity.class
            );

            intent.putExtra("IS_GUEST", true);

            startActivity(intent);
            finish();
        });
    }
}