package com.tim14.slagalica;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.tim14.slagalica.game.GameRound;

public class HomeActivity extends AppCompatActivity {

    private View startGameButton;
    private View topTabsStrip;
    private View statusBarLayout;
    private View guestProgressCard;

    private TextView playTabButton;
    private TextView inviteFriendsTabButton;
    private TextView buyTokensTabButton;
    private TextView rankingTabButton;
    private TextView guestRankingHint;
    private TextView guestFriendsHint;

    private Button guestLoginButton;
    private Button guestRegisterButton;
    private Button buyTokensButton;

    private TextView notificationsMenuButton;
    private TextView tvProfile;
    private TextView tvStatistics;
    private TextView tvFriends;

    private LinearLayout memberActionsGroup;
    private LinearLayout guestActionsGroup;
    private ScrollView homeScrollView;
    private View startSection;
    private View rankingSection;
    private View friendsSection;

    private boolean isGuest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        homeScrollView = findViewById(R.id.homeScrollView);
        startSection = findViewById(R.id.startSection);
        rankingSection = findViewById(R.id.rankingSection);
        friendsSection = findViewById(R.id.friendsSection);

        startGameButton = findViewById(R.id.startGameButton);
        topTabsStrip = findViewById(R.id.topTabsStrip);
        statusBarLayout = findViewById(R.id.statusBarLayout);
        guestProgressCard = findViewById(R.id.guestProgressCard);

        playTabButton = findViewById(R.id.playTabButton);
        inviteFriendsTabButton = findViewById(R.id.inviteFriendsTabButton);
        buyTokensTabButton = findViewById(R.id.buyTokensTabButton);
        rankingTabButton = findViewById(R.id.rankingTabButton);

        guestLoginButton = findViewById(R.id.guestLoginButton);
        guestRegisterButton = findViewById(R.id.guestRegisterButton);
        buyTokensButton = findViewById(R.id.buyTokensButton);

        notificationsMenuButton = findViewById(R.id.notificationsMenuButton);
        tvProfile = findViewById(R.id.tvProfile);
        tvStatistics = findViewById(R.id.tvStatistics);
        tvFriends = findViewById(R.id.tvFriends);
        guestRankingHint = findViewById(R.id.guestRankingHint);
        guestFriendsHint = findViewById(R.id.guestFriendsHint);

        memberActionsGroup = findViewById(R.id.memberActionsGroup);
        guestActionsGroup = findViewById(R.id.guestActionsGroup);

        isGuest = getIntent().getBooleanExtra("IS_GUEST", false);
        configureGuestMode();

        playTabButton.setOnClickListener(v -> scrollToSection(startSection));
        inviteFriendsTabButton.setOnClickListener(v -> scrollToSection(friendsSection));
        buyTokensTabButton.setOnClickListener(v -> {
            if (isGuest) {
                scrollToSection(guestProgressCard);
            } else {
                scrollToSection(startSection);
            }
        });
        rankingTabButton.setOnClickListener(v -> scrollToSection(rankingSection));

        startGameButton.setOnClickListener(v -> openMatch());
        buyTokensButton.setOnClickListener(v -> {
            if (isGuest) {
                redirectGuestToLogin();
            } else {
                showToast(R.string.buy_tokens_placeholder_message);
            }
        });

        guestLoginButton.setOnClickListener(v ->
                startActivity(new Intent(HomeActivity.this, LoginActivity.class))
        );

        guestRegisterButton.setOnClickListener(v ->
                startActivity(new Intent(HomeActivity.this, RegisterActivity.class))
        );

        tvProfile.setOnClickListener(v -> openProfile());

        tvStatistics.setOnClickListener(v -> openStatistics());
        tvFriends.setOnClickListener(v -> scrollToSection(friendsSection));

        notificationsMenuButton.setOnClickListener(v -> openNotifications());
    }

    private void openProfile() {
        if (isGuest) {
            redirectGuestToLogin();
            return;
        }

        startActivity(new Intent(HomeActivity.this, ProfileActivity.class));
    }

    private void openStatistics() {
        if (isGuest) {
            redirectGuestToLogin();
            return;
        }

        Intent intent = new Intent(HomeActivity.this, ProfileActivity.class);
        intent.putExtra(ProfileActivity.EXTRA_OPEN_STATISTICS, true);
        startActivity(intent);
    }

    private void openMatch() {
        Intent intent = new Intent(HomeActivity.this, GameHostActivity.class);
        intent.putExtra("IS_GUEST", isGuest);
        intent.putExtra(GameHostActivity.EXTRA_START_ROUND, GameRound.KO_ZNA_ZNA);
        startActivity(intent);
    }

    private void openNotifications() {
        if (isGuest) {
            redirectGuestToLogin();
            return;
        }

        startActivity(new Intent(HomeActivity.this, NotificationsActivity.class));
    }

    private void scrollToSection(View target) {
        homeScrollView.post(() -> homeScrollView.smoothScrollTo(0, target.getTop()));
    }

    private void showToast(int messageResId) {
        Toast.makeText(this, messageResId, Toast.LENGTH_SHORT).show();
    }

    private void redirectGuestToLogin() {
        Toast.makeText(this, R.string.guest_login_required_message, Toast.LENGTH_SHORT).show();
        startActivity(new Intent(HomeActivity.this, LoginActivity.class));
    }

    private void configureGuestMode() {
        memberActionsGroup.setVisibility(View.VISIBLE);
        guestActionsGroup.setVisibility(isGuest ? View.VISIBLE : View.GONE);

        topTabsStrip.setVisibility(View.VISIBLE);
        statusBarLayout.setVisibility(isGuest ? View.GONE : View.VISIBLE);
        guestProgressCard.setVisibility(isGuest ? View.VISIBLE : View.GONE);

        rankingSection.setVisibility(View.VISIBLE);
        friendsSection.setVisibility(View.VISIBLE);
        buyTokensButton.setVisibility(View.VISIBLE);
        buyTokensButton.setText(isGuest
                ? R.string.guest_buy_tokens_cta
                : R.string.home_buy_tokens_cta
        );

        guestRankingHint.setVisibility(isGuest ? View.VISIBLE : View.GONE);
        guestFriendsHint.setVisibility(isGuest ? View.VISIBLE : View.GONE);
        memberActionsGroup.setAlpha(isGuest ? 0.82f : 1f);
        setTopMargin(guestActionsGroup, 8);
        setTopMargin(startSection, 10);
    }

    private void setTopMargin(View view, int dpValue) {
        ViewGroup.LayoutParams params = view.getLayoutParams();

        if (!(params instanceof ViewGroup.MarginLayoutParams)) {
            return;
        }

        ViewGroup.MarginLayoutParams marginParams = (ViewGroup.MarginLayoutParams) params;
        marginParams.topMargin = dpToPx(dpValue);
        view.setLayoutParams(marginParams);
    }

    private int dpToPx(int dpValue) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dpValue * density);
    }
}
