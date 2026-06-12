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
import com.tim14.slagalica.repository.AuthRepository;
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
    private AuthRepository authRepository;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        Log.d(TAG, "onCreate");

        firestoreRepository = new FirestoreRepository(this);
        authRepository = new AuthRepository(this);
        sessionManager = new SessionManager(this);

        if (authRepository.getCurrentUser() == null) {
            openWelcomeScreen();
            return;
        }

        profileInfo = findViewById(R.id.profileInfo);
        changeAvatarButton = findViewById(R.id.changeAvatarButton);
        logoutButton = findViewById(R.id.logoutButton);
        changePasswordButton = findViewById(R.id.changePasswordButton);
        viewStatisticsButton = findViewById(R.id.viewStatisticsButton);

        profileInfo.setText(getString(R.string.loading_profile));
        loadUserProfile();

        changeAvatarButton.setOnClickListener(v ->
                Toast.makeText(this, getString(R.string.avatar_change_placeholder), Toast.LENGTH_SHORT).show()
        );

        viewStatisticsButton.setOnClickListener(v -> showStatisticsFragment());

        if (savedInstanceState == null
                && getIntent().getBooleanExtra(EXTRA_OPEN_STATISTICS, false)) {
            viewStatisticsButton.post(this::showStatisticsFragment);
        }

        logoutButton.setOnClickListener(v -> {
            authRepository.logout();
            sessionManager.logout();
            openWelcomeScreen();
        });

        changePasswordButton.setOnClickListener(v ->
                startActivity(new Intent(ProfileActivity.this, ResetPasswordActivity.class))
        );
    }

    private void loadUserProfile() {
        firestoreRepository.getCurrentUser(new FirebaseCallback<User>() {
            @Override
            public void onSuccess(User user) {
                sessionManager.saveUser(user);

                profileInfo.setText(getString(
                        R.string.profile_info_format,
                        user.username,
                        user.email,
                        user.region,
                        user.tokens,
                        user.stars,
                        LeagueUtils.getLeagueName(user.league),
                        user.avatarFrame,
                        user.qrCode
                ));

                Log.d(TAG, "Profile loaded from Firestore: " + user.username);
            }

            @Override
            public void onError(String error) {
                profileInfo.setText(getString(R.string.profile_load_failed));
                Toast.makeText(ProfileActivity.this, error, Toast.LENGTH_LONG).show();
                Log.e(TAG, "Error loading profile: " + error);
            }
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

    private void openWelcomeScreen() {
        Intent intent = new Intent(ProfileActivity.this, WelcomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override protected void onStart() { super.onStart(); Log.d(TAG, "onStart"); }
    @Override protected void onRestart() { super.onRestart(); Log.d(TAG, "onRestart"); }
    @Override protected void onResume() { super.onResume(); Log.d(TAG, "onResume"); }
    @Override protected void onPause() { super.onPause(); Log.d(TAG, "onPause"); }
    @Override protected void onStop() { super.onStop(); Log.d(TAG, "onStop"); }
    @Override protected void onDestroy() { super.onDestroy(); Log.d(TAG, "onDestroy"); }
}
