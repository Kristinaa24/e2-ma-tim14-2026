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
    private Button mojBrojButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        userInfoText = findViewById(R.id.userInfoText);
        profileButton = findViewById(R.id.profileButton);
        koZnaZnaButton = findViewById(R.id.koZnaZnaButton);
        spojniceButton = findViewById(R.id.spojniceButton);
        korakPoKorakButton = findViewById(R.id.korakPoKorakButton);
        mojBrojButton = findViewById(R.id.mojBrojButton);

        if (SessionManager.currentUser == null) {
            SessionManager.currentUser =
                    new User("TestPlayer", "test@mail.com", "Vojvodina", 5, 120, 2);
        }

        User user = SessionManager.currentUser;

        userInfoText.setText(getString(
                R.string.user_info_format,
                user.username,
                user.email,
                user.region,
                user.tokens,
                user.stars,
                user.league
        ));

        profileButton.setOnClickListener(v ->
                startActivity(new Intent(HomeActivity.this, ProfileActivity.class))
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
    }
}