package com.tim14.slagalica;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.tim14.slagalica.fragments.StatisticsFragment;

import androidx.appcompat.app.AppCompatActivity;

public class ProfileActivity extends AppCompatActivity {

    private static final String TAG = "ProfileActivity";

    private TextView profileInfo;
    private Button changeAvatarButton, logoutButton, changePasswordButton, viewStatisticsButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        Log.d(TAG, "onCreate");

        profileInfo = findViewById(R.id.profileInfo);
        changeAvatarButton = findViewById(R.id.changeAvatarButton);
        logoutButton = findViewById(R.id.logoutButton);
        changePasswordButton = findViewById(R.id.changePasswordButton);
        viewStatisticsButton = findViewById(R.id.viewStatisticsButton);

        if (SessionManager.currentUser == null) {
            SessionManager.currentUser = new User("Kristina", "kristinadivnic2003@gmail.com", "Vojvodina", 5, 120, 2);
        }

        User u = SessionManager.currentUser;

        profileInfo.setText(
                "Username: " + u.username +
                        "\nEmail: " + u.email +
                        "\nRegion: " + u.region +
                        "\nTokens: " + u.tokens +
                        "\nStars: " + u.stars +
                        "\nLeague: Silver League 🥈" +
                        "\nAvatar frame: Silver" +
                        "\nQR code: Available for friend invite"
        );

        changeAvatarButton.setOnClickListener(v ->
                Toast.makeText(this, "Avatar change screen placeholder.", Toast.LENGTH_SHORT).show());

        viewStatisticsButton.setOnClickListener(v -> showStatisticsFragment());

        logoutButton.setOnClickListener(v -> {

            SessionManager.currentUser = null;

            Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);

            startActivity(intent);

            finish();
        });

        changePasswordButton.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, ResetPasswordActivity.class);
            startActivity(intent);
        });
    }

    private void showStatisticsFragment() {
        StatisticsFragment fragment = new StatisticsFragment();

        getSupportFragmentManager()
                .beginTransaction()
                .setTransition(androidx.fragment.app.FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .replace(R.id.statisticsFragmentContainer, fragment)
                .addToBackStack(null)
                .commit();
    }

    @Override protected void onStart() { super.onStart(); Log.d(TAG, "onStart"); }
    @Override protected void onRestart() { super.onRestart(); Log.d(TAG, "onRestart"); }
    @Override protected void onResume() { super.onResume(); Log.d(TAG, "onResume"); }
    @Override protected void onPause() { super.onPause(); Log.d(TAG, "onPause"); }
    @Override protected void onStop() { super.onStop(); Log.d(TAG, "onStop"); }
    @Override protected void onDestroy() { super.onDestroy(); Log.d(TAG, "onDestroy"); }
}