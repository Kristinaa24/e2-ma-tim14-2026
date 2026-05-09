package com.tim14.slagalica;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;

public class HomeActivity extends AppCompatActivity {

    private Button startGameButton;
    private Button koZnaZnaButton;
    private Button spojniceButton;
    private Button korakPoKorakButton;
    private Button mojBrojButton;
    private Button asocijacijeButton;
    private Button skockoButton;
    private TextView notificationsMenuButton;
    private TextView tvProfile, tvFriends, tvRanking;

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private Toolbar homeToolbar;
    private ActionBarDrawerToggle drawerToggle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // UI Components
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
        
        tvProfile = findViewById(R.id.tvProfile);
        tvFriends = findViewById(R.id.tvFriends);
        tvRanking = findViewById(R.id.tvRanking);

        // Setup Toolbar & Drawer
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

        // Click Listeners
        notificationsMenuButton.setOnClickListener(v -> {
            startActivity(new Intent(HomeActivity.this, NotificationsActivity.class));
        });

        tvProfile.setOnClickListener(v -> 
                startActivity(new Intent(HomeActivity.this, ProfileActivity.class)));
        
        // tvFriends and tvRanking could lead to their respective activities once created
        tvFriends.setOnClickListener(v -> Toast.makeText(this, "Otvaram prijatelje...", Toast.LENGTH_SHORT).show());
        tvRanking.setOnClickListener(v -> Toast.makeText(this, "Otvaram rang listu...", Toast.LENGTH_SHORT).show());

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