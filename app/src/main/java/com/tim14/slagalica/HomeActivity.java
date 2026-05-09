package com.tim14.slagalica;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import android.view.View;
import android.widget.LinearLayout;

import com.google.android.material.navigation.NavigationView;

public class HomeActivity extends AppCompatActivity {

    private Button startGameButton, koZnaZnaButton, spojniceButton;
    private Button korakPoKorakButton, mojBrojButton, asocijacijeButton, skockoButton;
    private Button logoutButton;
    private TextView notificationsMenuButton;
    private TextView tvProfile, tvFriends, tvRanking;
    private LinearLayout statusBarLayout;

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private Toolbar homeToolbar;
    private ActionBarDrawerToggle drawerToggle;

    private boolean isGuest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);
        homeToolbar = findViewById(R.id.homeToolbar);

        startGameButton = findViewById(R.id.startGameButton);
        koZnaZnaButton = findViewById(R.id.koZnaZnaButton);
        spojniceButton = findViewById(R.id.spojniceButton);
        korakPoKorakButton = findViewById(R.id.korakPoKorakButton);
        mojBrojButton = findViewById(R.id.mojBrojButton);
        asocijacijeButton = findViewById(R.id.asocijacijeButton);
        skockoButton = findViewById(R.id.skockoButton);
        notificationsMenuButton = findViewById(R.id.notificationsMenuButton);
        logoutButton = findViewById(R.id.logoutButton);
        statusBarLayout = findViewById(R.id.statusBarLayout);

        tvProfile = findViewById(R.id.tvProfile);
        tvFriends = findViewById(R.id.tvFriends);
        tvRanking = findViewById(R.id.tvRanking);

        isGuest = getIntent().getBooleanExtra("IS_GUEST", false);

        if (isGuest) {
            tvProfile.setVisibility(View.GONE);
            tvFriends.setVisibility(View.GONE);
            tvRanking.setVisibility(View.GONE);
            notificationsMenuButton.setVisibility(View.GONE);
            logoutButton.setVisibility(View.GONE);
            statusBarLayout.setVisibility(View.GONE);

            navigationView.getMenu().findItem(R.id.nav_profile).setVisible(false);
        } else {
            navigationView.getMenu().findItem(R.id.nav_login).setVisible(false);
            navigationView.getMenu().findItem(R.id.nav_register).setVisible(false);
            logoutButton.setVisibility(View.VISIBLE);
        }

        setSupportActionBar(homeToolbar);

        drawerToggle = new ActionBarDrawerToggle(
                this,
                drawerLayout,
                homeToolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close
        );

        drawerLayout.addDrawerListener(drawerToggle);
        drawerToggle.syncState();

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_profile) {
                startActivity(new Intent(HomeActivity.this, ProfileActivity.class));
            } else if (id == R.id.nav_login) {
                startActivity(new Intent(HomeActivity.this, LoginActivity.class));
            } else if (id == R.id.nav_register) {
                startActivity(new Intent(HomeActivity.this, RegisterActivity.class));
            }

            drawerLayout.closeDrawers();
            return true;
        });

        notificationsMenuButton.setOnClickListener(v ->
                startActivity(new Intent(HomeActivity.this, NotificationsActivity.class))
        );

        tvProfile.setOnClickListener(v ->
                startActivity(new Intent(HomeActivity.this, ProfileActivity.class))
        );

        tvFriends.setOnClickListener(v ->
                Toast.makeText(this, "Otvaram prijatelje...", Toast.LENGTH_SHORT).show()
        );

        tvRanking.setOnClickListener(v ->
                Toast.makeText(this, "Otvaram rang listu...", Toast.LENGTH_SHORT).show()
        );

        startGameButton.setOnClickListener(v ->
                openGameActivity(KoZnaZnaActivity.class)
        );

        koZnaZnaButton.setOnClickListener(v ->
                openGameActivity(KoZnaZnaActivity.class)
        );

        spojniceButton.setOnClickListener(v ->
                openGameActivity(SpojniceActivity.class)
        );

        korakPoKorakButton.setOnClickListener(v ->
                openGameActivity(KorakPoKorakActivity.class)
        );

        mojBrojButton.setOnClickListener(v ->
                openGameActivity(MojBrojActivity.class)
        );

        asocijacijeButton.setOnClickListener(v ->
                openGameActivity(AsocijacijeActivity.class)
        );

        skockoButton.setOnClickListener(v ->
                openGameActivity(SkockoActivity.class)
        );

        logoutButton.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, WelcomeActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void openGameActivity(Class<?> activityClass) {
        Intent intent = new Intent(HomeActivity.this, activityClass);
        intent.putExtra("IS_GUEST", isGuest);
        startActivity(intent);
    }
}