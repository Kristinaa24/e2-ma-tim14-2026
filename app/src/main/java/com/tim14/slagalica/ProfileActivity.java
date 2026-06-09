package com.tim14.slagalica;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.tim14.slagalica.fragments.StatisticsFragment;
import com.tim14.slagalica.model.User;
import com.tim14.slagalica.repository.FirebaseCallback;
import com.tim14.slagalica.repository.FirestoreRepository;

public class ProfileActivity extends AppCompatActivity {

    private static final String TAG = "ProfileActivity";
    public static final String EXTRA_OPEN_STATISTICS = "open_statistics";

    private TextView profileInfo;
    private Button changeAvatarButton;
    private Button logoutButton;
    private Button changePasswordButton;
    private Button viewStatisticsButton;

    private FirestoreRepository firestoreRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        Log.d(TAG, "onCreate");

        firestoreRepository = new FirestoreRepository();

        profileInfo = findViewById(R.id.profileInfo);
        changeAvatarButton = findViewById(R.id.changeAvatarButton);
        logoutButton = findViewById(R.id.logoutButton);
        changePasswordButton = findViewById(R.id.changePasswordButton);
        viewStatisticsButton = findViewById(R.id.viewStatisticsButton);

        profileInfo.setText("Loading profile...");

        loadUserProfile();

        changeAvatarButton.setOnClickListener(v ->
                Toast.makeText(this, "Avatar change screen placeholder.", Toast.LENGTH_SHORT).show()
        );

        viewStatisticsButton.setOnClickListener(v -> showStatisticsFragment());

        if (savedInstanceState == null
                && getIntent().getBooleanExtra(EXTRA_OPEN_STATISTICS, false)) {
            viewStatisticsButton.post(this::showStatisticsFragment);
        }

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

    private void loadUserProfile() {
        firestoreRepository.getCurrentUser(new FirebaseCallback<User>() {
            @Override
            public void onSuccess(User user) {
                SessionManager.currentUser = user;

                profileInfo.setText(
                        "Username: " + user.username +
                                "\nEmail: " + user.email +
                                "\nRegion: " + user.region +
                                "\nTokens: " + user.tokens +
                                "\nStars: " + user.stars +
                                "\nLeague: " + getLeagueName(user.league) +
                                "\nAvatar frame: " + user.avatarFrame +
                                "\nQR code: " + user.qrCode
                );

                Log.d(TAG, "Profile loaded from Firestore: " + user.username);
            }

            @Override
            public void onError(String error) {
                profileInfo.setText("Profile could not be loaded.");
                Toast.makeText(ProfileActivity.this, error, Toast.LENGTH_LONG).show();
                Log.e(TAG, "Error loading profile: " + error);
            }
        });
    }

    private String getLeagueName(int league) {
        switch (league) {
            case 0:
                return "Bronze League 🥉";
            case 1:
                return "Silver League 🥈";
            case 2:
                return "Gold League 🥇";
            case 3:
                return "Platinum League 💎";
            case 4:
                return "Diamond League 💠";
            case 5:
                return "Master League 👑";
            default:
                return "Unknown League";
        }
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
