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
import com.tim14.slagalica.repository.FirebaseCallback;
import com.tim14.slagalica.repository.FirestoreRepository;

import java.util.Locale;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        Log.d(TAG, "onCreate");

        firestoreRepository = new FirestoreRepository();

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

        logoutButton.setOnClickListener(v -> {
            SessionManager.currentUser = null;
            startActivity(new Intent(ProfileActivity.this, LoginActivity.class));
            finish();
        });

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
        avatarFrameContainer.setBackground(createAvatarBackground(getString(R.string.no_avatar_frame)));
        avatarImage.setImageResource(R.drawable.avatar_1);
    }

    private void loadUserProfile() {
        firestoreRepository.getCurrentUser(new FirebaseCallback<User>() {
            @Override
            public void onSuccess(User user) {
                currentUser = user;
                SessionManager.currentUser = user;
                bindUser(user);
                Log.d(TAG, "Profile loaded from Firestore: " + user.username);
            }

            @Override
            public void onError(String error) {
                Toast.makeText(ProfileActivity.this, error, Toast.LENGTH_LONG).show();
                Log.e(TAG, "Error loading profile: " + error);
            }
        });
    }

    private void bindUser(User user) {
        usernameValue.setText(valueOrFallback(user.username, getString(R.string.profile_default_username)));
        emailValue.setText(valueOrFallback(user.email, getString(R.string.profile_default_email)));
        regionValue.setText(valueOrFallback(user.region, getString(R.string.profile_default_region)));
        tokensValue.setText(String.valueOf(user.tokens));
        starsValue.setText(String.valueOf(user.stars));

        String avatarFrame = sanitizeAvatarFrame(user.avatarFrame);
        avatarFrameValue.setText(getString(R.string.region_reward_format, avatarFrame));
        avatarFrameContainer.setBackground(createAvatarBackground(avatarFrame));
        avatarImage.setImageResource(getAvatarResource(valueOrFallback(user.avatar, "avatar_1")));

        LeagueUi leagueUi = getLeagueUi(user.league);
        leagueNameValue.setText(leagueUi.name);
        leagueIconValue.setText(leagueUi.icon);

        String qrPayload = valueOrFallback(user.qrCode, valueOrFallback(user.id, FirestoreRepository.TEST_USER_ID));
        qrCodeValue.setText(valueOrFallback(user.username, getString(R.string.player_id_label)));
        qrCodeImage.setImageBitmap(createQrBitmap(qrPayload));
    }

    private String valueOrFallback(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }

        return value;
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
            button.setImageResource(getAvatarResource(avatar));
            button.setBackground(createAvatarBackground(getString(R.string.no_avatar_frame)));
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
                SessionManager.currentUser = currentUser;
                bindUser(currentUser);
                Toast.makeText(ProfileActivity.this, R.string.avatar_saved_message, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String error) {
                Toast.makeText(ProfileActivity.this, error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private int getAvatarResource(String avatar) {
        switch (valueOrFallback(avatar, "avatar_1")) {
            case "avatar_2":
                return R.drawable.avatar_2;
            case "avatar_3":
                return R.drawable.avatar_3;
            case "avatar_4":
                return R.drawable.avatar_4;
            case "avatar_5":
                return R.drawable.avatar_5;
            case "avatar_6":
                return R.drawable.avatar_6;
            case "avatar_1":
            default:
                return R.drawable.avatar_1;
        }
    }

    private GradientDrawable createAvatarBackground(String avatarFrame) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);
        drawable.setColor(Color.TRANSPARENT);
        drawable.setStroke(dpToPx(5), getAvatarFrameColor(avatarFrame));
        return drawable;
    }

    private int getAvatarFrameColor(String avatarFrame) {
        switch (sanitizeAvatarFrame(avatarFrame).toLowerCase(Locale.US)) {
            case "bronze":
                return Color.rgb(176, 105, 45);
            case "silver":
                return Color.rgb(192, 192, 192);
            case "gold":
                return Color.rgb(255, 196, 32);
            case "none":
            default:
                return Color.TRANSPARENT;
        }
    }

    private String sanitizeAvatarFrame(String avatarFrame) {
        String normalized = valueOrFallback(avatarFrame, getString(R.string.no_avatar_frame));

        if (normalized.equalsIgnoreCase(getString(R.string.bronze_frame))
                || normalized.equalsIgnoreCase(getString(R.string.silver_frame))
                || normalized.equalsIgnoreCase(getString(R.string.gold_frame))) {
            return normalized.substring(0, 1).toUpperCase(Locale.US)
                    + normalized.substring(1).toLowerCase(Locale.US);
        }

        return getString(R.string.no_avatar_frame);
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

    private LeagueUi getLeagueUi(int league) {
        switch (league) {
            case 0:
                return new LeagueUi(getString(R.string.bronze_league), "B");
            case 1:
                return new LeagueUi(getString(R.string.silver_league), "S");
            case 2:
                return new LeagueUi(getString(R.string.gold_league), "G");
            case 3:
                return new LeagueUi(getString(R.string.platinum_league), "P");
            case 4:
                return new LeagueUi(getString(R.string.diamond_league), "D");
            case 5:
                return new LeagueUi(getString(R.string.master_league), "M");
            default:
                return new LeagueUi(getString(R.string.bronze_league), "B");
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

    private static class LeagueUi {
        final String name;
        final String icon;

        LeagueUi(String name, String icon) {
            this.name = name;
            this.icon = icon;
        }
    }

    @Override protected void onStart() { super.onStart(); Log.d(TAG, "onStart"); }
    @Override protected void onRestart() { super.onRestart(); Log.d(TAG, "onRestart"); }
    @Override protected void onResume() { super.onResume(); Log.d(TAG, "onResume"); }
    @Override protected void onPause() { super.onPause(); Log.d(TAG, "onPause"); }
    @Override protected void onStop() { super.onStop(); Log.d(TAG, "onStop"); }
    @Override protected void onDestroy() { super.onDestroy(); Log.d(TAG, "onDestroy"); }
}
