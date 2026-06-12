package com.tim14.slagalica;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.tim14.slagalica.repository.AuthRepository;

@SuppressLint("CustomSplashScreen")
public class SplashScreenActivity extends AppCompatActivity {

    private static final int SPLASH_TIME_OUT = 3000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        AuthRepository authRepository = new AuthRepository();
        SessionManager sessionManager = new SessionManager(this);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent intent;

            if (authRepository.getCurrentUser() != null
                    && authRepository.isCurrentUserVerified()) {
                intent = new Intent(SplashScreenActivity.this, HomeActivity.class);
                intent.putExtra("IS_GUEST", false);
            } else {
                authRepository.logout();
                sessionManager.logout();
                intent = new Intent(SplashScreenActivity.this, WelcomeActivity.class);
            }

            startActivity(intent);
            finish();
        }, SPLASH_TIME_OUT);
    }
}
