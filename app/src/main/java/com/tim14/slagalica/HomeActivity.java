package com.tim14.slagalica;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class HomeActivity extends AppCompatActivity {

    private Button avatarProfileButton;
    private Button startGameButton;
    private Button koZnaZnaButton;
    private Button spojniceButton;
    private Button korakPoKorakButton;
    private Button mojBrojButton;
    private Button asocijacijeButton;
    private Button skockoButton;
    private Button notificationsMenuButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        avatarProfileButton = findViewById(R.id.avatarProfileButton);
        startGameButton = findViewById(R.id.startGameButton);
        koZnaZnaButton = findViewById(R.id.koZnaZnaButton);
        spojniceButton = findViewById(R.id.spojniceButton);
        korakPoKorakButton = findViewById(R.id.korakPoKorakButton);
        mojBrojButton = findViewById(R.id.mojBrojButton);
        asocijacijeButton = findViewById(R.id.asocijacijeButton);
        skockoButton = findViewById(R.id.skockoButton);
        notificationsMenuButton = findViewById(R.id.notificationsMenuButton);

        if (SessionManager.currentUser == null) {
            SessionManager.currentUser =
                    new User("TestPlayer", "test@mail.com", "Vojvodina", 5, 120, 2);
        }

        avatarProfileButton.setOnClickListener(v ->
                startActivity(new Intent(HomeActivity.this, ProfileActivity.class))
        );

        notificationsMenuButton.setOnClickListener(v ->
                startActivity(new Intent(HomeActivity.this, NotificationsActivity.class))
        );

        startGameButton.setOnClickListener(v ->
                startActivity(new Intent(HomeActivity.this, KoZnaZnaActivity.class))
        );

        koZnaZnaButton.setOnClickListener(v ->
                startActivity(new Intent(HomeActivity.this, KoZnaZnaActivity.class))
        );

        spojniceButton.setOnClickListener(v ->
                startActivity(new Intent(HomeActivity.this, SpojniceActivity.class))
        );

        korakPoKorakButton.setOnClickListener(v ->
                startActivity(new Intent(HomeActivity.this, KorakPoKorakActivity.class))
        );

        mojBrojButton.setOnClickListener(v ->
                startActivity(new Intent(HomeActivity.this, MojBrojActivity.class))
        );

        asocijacijeButton.setOnClickListener(v ->
                startActivity(new Intent(HomeActivity.this, AsocijacijeActivity.class))
        );

        skockoButton.setOnClickListener(v ->
                startActivity(new Intent(HomeActivity.this, SkockoActivity.class))
        );
    }
}