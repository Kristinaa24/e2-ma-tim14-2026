package com.tim14.slagalica;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class ProfileActivity extends AppCompatActivity {

    private static final String TAG = "ProfileActivity";

    private TextView profileInfo, statisticsInfo;
    private Button changeAvatarButton, logoutButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        setContentView(R.layout.activity_profile);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        profileInfo = findViewById(R.id.profileInfo);
        statisticsInfo = findViewById(R.id.statisticsInfo);
        changeAvatarButton = findViewById(R.id.changeAvatarButton);
        logoutButton = findViewById(R.id.logoutButton);

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
                        "\nQR code: Available for friend invite"
        );

        statisticsInfo.setText(
                "Statistics:" +
                        "\n\nAverage score per game:" +
                        "\n• Ko zna zna: 34 / 50" +
                        "\n• Spojnice: 14 / 20" +
                        "\n• Asocijacije: 38 / 60" +
                        "\n• Skočko: 25 / 40" +
                        "\n• Korak po korak: 18 / 40" +
                        "\n• Moj broj: 7 / 20" +

                        "\n\nKo zna zna:" +
                        "\n• Correct answers: 42" +
                        "\n• Wrong answers: 18" +

                        "\n\nMoj broj:" +
                        "\n• Exact target number found: 40%" +

                        "\n\nKorak po korak:" +
                        "\n• Step 1: 5%" +
                        "\n• Step 2: 8%" +
                        "\n• Step 3: 12%" +
                        "\n• Step 4: 20%" +
                        "\n• Step 5: 25%" +
                        "\n• Step 6: 18%" +
                        "\n• Step 7: 12%" +

                        "\n\nAsocijacije:" +
                        "\n• Solved: 9" +
                        "\n• Unsolved: 4" +

                        "\n\nSkočko:" +
                        "\n• Attempt 1: 5%" +
                        "\n• Attempt 2: 10%" +
                        "\n• Attempt 3: 20%" +
                        "\n• Attempt 4: 25%" +
                        "\n• Attempt 5: 25%" +
                        "\n• Attempt 6: 15%" +

                        "\n\nSpojnice:" +
                        "\n• Successfully connected pairs: 60%" +

                        "\n\nGames:" +
                        "\n• Total played games: 12" +
                        "\n• Wins: 7" +
                        "\n• Losses: 5" +
                        "\n• Win rate: 58%" +
                        "\n• Loss rate: 42%"
        );

        changeAvatarButton.setOnClickListener(v ->
                Toast.makeText(this, "Avatar change screen placeholder.", Toast.LENGTH_SHORT).show());

        logoutButton.setOnClickListener(v -> {
            SessionManager.currentUser = null;
            Toast.makeText(this, "Logout", Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(ProfileActivity.this, HomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override protected void onStart() { super.onStart(); Log.d(TAG, "onStart"); }
    @Override protected void onRestart() { super.onRestart(); Log.d(TAG, "onRestart"); }
    @Override protected void onResume() { super.onResume(); Log.d(TAG, "onResume"); }
    @Override protected void onPause() { super.onPause(); Log.d(TAG, "onPause"); }
    @Override protected void onStop() { super.onStop(); Log.d(TAG, "onStop"); }
    @Override protected void onDestroy() { super.onDestroy(); Log.d(TAG, "onDestroy"); }
}