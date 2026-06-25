package com.tim14.slagalica;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.tim14.slagalica.fragments.StatisticsFragment;
import com.tim14.slagalica.model.User;
import com.tim14.slagalica.repository.AuthRepository;
import com.tim14.slagalica.repository.FirebaseCallback;
import com.tim14.slagalica.repository.FirestoreRepository;
import com.tim14.slagalica.service.ProfileService;

public class ProfileActivity extends AppCompatActivity {

    private static final String TAG = "ProfileActivity";
    public static final String EXTRA_OPEN_STATISTICS = "open_statistics";

    private FrameLayout avatarFrameContainer;
    private ImageView avatarImage;
    private TextView avatarFrameValue;
    private TextView usernameValue;
    private TextView emailValue;
    private TextView regionValue;
    private TextView tokensValue;
    private TextView starsValue;
    private TextView leagueIconValue;
    private TextView leagueNameValue;
    private TextView qrCodeValue;
    private ImageView qrCodeImage;

    private Button changeAvatarButton;
    private Button logoutButton;
    private Button changePasswordButton;
    private Button viewStatisticsButton;

    private FirestoreRepository firestoreRepository;
    private User currentUser;
    private AuthRepository authRepository;
    private SessionManager sessionManager;
    private ProfileService profileService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        Log.d(TAG, "onCreate");

        firestoreRepository = new FirestoreRepository(this);
        authRepository = new AuthRepository(this);
        sessionManager = new SessionManager(this);
        profileService = new ProfileService();

        if (authRepository.getCurrentUser() == null) {
            openWelcomeScreen();
            return;
        }

        avatarFrameContainer = findViewById(R.id.avatarFrameContainer);
        avatarImage = findViewById(R.id.avatarImage);
        avatarFrameValue = findViewById(R.id.avatarFrameValue);
        usernameValue = findViewById(R.id.usernameValue);
        emailValue = findViewById(R.id.emailValue);
        regionValue = findViewById(R.id.regionValue);
        tokensValue = findViewById(R.id.tokensValue);
        starsValue = findViewById(R.id.starsValue);
        leagueIconValue = findViewById(R.id.leagueIconValue);
        leagueNameValue = findViewById(R.id.leagueNameValue);
        qrCodeImage = findViewById(R.id.qrCodeImage);
        qrCodeValue = findViewById(R.id.qrCodeValue);

        changeAvatarButton = findViewById(R.id.changeAvatarButton);
        logoutButton = findViewById(R.id.logoutButton);
        changePasswordButton = findViewById(R.id.changePasswordButton);
        viewStatisticsButton = findViewById(R.id.viewStatisticsButton);


        setLoadingState();

        loadUserProfile();


        changeAvatarButton.setOnClickListener(v -> showAvatarDialog());
        viewStatisticsButton.setOnClickListener(v -> showStatisticsFragment());

        if (savedInstanceState == null
                && getIntent().getBooleanExtra(EXTRA_OPEN_STATISTICS, false)) {
            viewStatisticsButton.post(this::showStatisticsFragment);
        }

        logoutButton.setOnClickListener(v -> logoutCurrentUser());
      
        changePasswordButton.setOnClickListener(v ->
                startActivity(new Intent(ProfileActivity.this, ResetPasswordActivity.class))
        );
    }

    private void setLoadingState() {
        usernameValue.setText("...");
        emailValue.setText("...");
        regionValue.setText("...");
        tokensValue.setText("0");
        starsValue.setText("0");
        leagueNameValue.setText("...");
        leagueIconValue.setText("?");
        avatarFrameValue.setText(getString(R.string.region_reward_loading));
        qrCodeValue.setText("...");
        avatarFrameContainer.setBackground(
                createAvatarBackground(profileService.getAvatarFrameColor(getString(R.string.no_avatar_frame)))
        );
        avatarImage.setImageResource(R.drawable.avatar_1);
    }

    private void loadUserProfile() {
        firestoreRepository.getCurrentUser(new FirebaseCallback<User>() {
            @Override
            public void onSuccess(User user) {
                applyRegionRewardFrame(user);
            }

            @Override
            public void onError(String error) {

                Toast.makeText(ProfileActivity.this, error, Toast.LENGTH_LONG).show();
                Log.e(TAG, "Error loading profile: " + error);
            }
        });
    }

    private void applyRegionRewardFrame(User user) {
        firestoreRepository.applyPreviousCycleRegionFrame(user, new FirebaseCallback<User>() {
            @Override
            public void onSuccess(User rewardedUser) {
                currentUser = rewardedUser;
                bindUser(rewardedUser);
                sessionManager.saveUser(rewardedUser);

                Log.d(TAG, "Profile loaded from Firestore: " + rewardedUser.username);
            }

            @Override
            public void onError(String error) {
                currentUser = user;
                bindUser(user);
                sessionManager.saveUser(user);
                Toast.makeText(ProfileActivity.this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void bindUser(User user) {
        ProfileService.ProfileUiData profileUiData = profileService.prepareProfile(user);

        usernameValue.setText(profileUiData.getUsername());
        emailValue.setText(profileUiData.getEmail());
        regionValue.setText(profileUiData.getRegion());
        tokensValue.setText(profileUiData.getTokens());
        starsValue.setText(profileUiData.getStars());

        avatarFrameValue.setText(
                getString(R.string.region_reward_format, profileUiData.getAvatarFrame())
        );
        avatarFrameContainer.setBackground(
                createAvatarBackground(profileUiData.getAvatarFrameColor())
        );
        avatarImage.setImageResource(profileUiData.getAvatarResourceId());

        leagueNameValue.setText(profileUiData.getLeagueNameResId());
        leagueIconValue.setText(profileUiData.getLeagueIcon());

        qrCodeValue.setText(profileUiData.getQrLabel());
        qrCodeImage.setImageBitmap(createQrBitmap(profileUiData.getQrPayload()));
    }

    private void showAvatarDialog() {
        if (currentUser == null) {
            Toast.makeText(this, R.string.profile_loading_message, Toast.LENGTH_SHORT).show();
            return;
        }

        String[] avatars = {"avatar_1", "avatar_2", "avatar_3", "avatar_4", "avatar_5", "avatar_6"};
        GridLayout gridLayout = new GridLayout(this);
        gridLayout.setColumnCount(3);
        gridLayout.setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(6));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.choose_avatar)
                .setView(gridLayout)
                .create();

        for (String avatar : avatars) {
            ImageButton button = new ImageButton(this);
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = dpToPx(78);
            params.height = dpToPx(78);
            params.setMargins(dpToPx(6), dpToPx(6), dpToPx(6), dpToPx(6));
            button.setLayoutParams(params);
            button.setImageResource(profileService.getAvatarResource(avatar));
            button.setBackground(
                    createAvatarBackground(
                            profileService.getAvatarFrameColor(getString(R.string.no_avatar_frame))
                    )
            );
            button.setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8));
            button.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            button.setContentDescription(avatar);
            button.setOnClickListener(v -> {
                updateAvatar(avatar);
                dialog.dismiss();
            });
            gridLayout.addView(button);
        }

        dialog.show();
    }

    private void updateAvatar(String avatar) {
        firestoreRepository.updateAvatar(avatar, new FirebaseCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                currentUser.avatar = avatar;
                bindUser(currentUser);

                Toast.makeText(
                        ProfileActivity.this,
                        R.string.avatar_saved_message,
                        Toast.LENGTH_SHORT
                ).show();
            }

            @Override
            public void onError(String error) {
                Toast.makeText(ProfileActivity.this, error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void logoutCurrentUser() {
        firestoreRepository.markCurrentUserInactive(new FirebaseCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                refreshRegionCountsBeforeLogout();
            }

            @Override
            public void onError(String error) {
                Log.w(TAG, "Inactive user update failed before logout: " + error);
                finishLogout();
            }
        });
    }

    private void refreshRegionCountsBeforeLogout() {
        firestoreRepository.refreshRegionPlayerCounts(new FirebaseCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                finishLogout();
            }

            @Override
            public void onError(String error) {
                Log.w(TAG, "Region player count refresh failed before logout: " + error);
                finishLogout();
            }
        });
    }

    private void finishLogout() {
        authRepository.logout();
        sessionManager.logout();
        openWelcomeScreen();
    }

    private GradientDrawable createAvatarBackground(int avatarFrameColor) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);
        drawable.setColor(Color.TRANSPARENT);
        drawable.setStroke(dpToPx(5), avatarFrameColor);
        return drawable;
    }

    private Bitmap createQrBitmap(String payload) {
        int size = dpToPx(150);

        try {
            BitMatrix matrix = new MultiFormatWriter().encode(
                    payload,
                    BarcodeFormat.QR_CODE,
                    size,
                    size
            );

            Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);

            for (int x = 0; x < size; x++) {
                for (int y = 0; y < size; y++) {
                    bitmap.setPixel(x, y, matrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }

            return bitmap;
        } catch (WriterException e) {
            Log.e(TAG, "QR generation failed", e);
            Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
            bitmap.eraseColor(Color.WHITE);
            return bitmap;
        }
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
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
