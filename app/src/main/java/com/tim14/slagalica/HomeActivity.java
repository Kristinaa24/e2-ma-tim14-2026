package com.tim14.slagalica;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class HomeActivity extends AppCompatActivity {

    private TextView userInfoText;
    private Button profileButton;
    private Button koZnaZnaButton;
    private Button spojniceButton;

    private Button korakPoKorakButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        userInfoText = findViewById(R.id.userInfoText);
        profileButton = findViewById(R.id.profileButton);
        koZnaZnaButton = findViewById(R.id.koZnaZnaButton);
        spojniceButton = findViewById(R.id.spojniceButton);
        korakPoKorakButton = findViewById(R.id.korakPoKorakButton);

        if (SessionManager.currentUser == null) {
            SessionManager.currentUser =
                    new User("TestPlayer", "test@mail.com", "Vojvodina", 5, 120, 2);
        }

        User user = SessionManager.currentUser;

        userInfoText.setText(
                "User: " + user.username +
                        "\nEmail: " + user.email +
                        "\nRegion: " + user.region +
                        "\nTokens: " + user.tokens +
                        "\nStars: " + user.stars +
                        "\nLeague: " + user.league
        );

        profileButton.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, ProfileActivity.class);
            startActivity(intent);
        });

        koZnaZnaButton.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, KoZnaZnaActivity.class);
            startActivity(intent);
        });

        spojniceButton.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, SpojniceActivity.class);
            startActivity(intent);
        });

        korakPoKorakButton.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, KorakPoKorakActivity.class);
            startActivity(intent);
        });
    }
}