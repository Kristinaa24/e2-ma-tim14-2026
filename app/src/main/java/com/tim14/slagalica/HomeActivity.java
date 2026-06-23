package com.tim14.slagalica;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.tim14.slagalica.game.GameRound;
import com.tim14.slagalica.model.HomeFriendItem;
import com.tim14.slagalica.model.HomeRankingItem;
import com.tim14.slagalica.model.User;
import com.tim14.slagalica.repository.FirebaseCallback;
import com.tim14.slagalica.repository.FirestoreRepository;
import com.tim14.slagalica.repository.SharedMatchRepository;
import com.tim14.slagalica.service.NotificationHelper;

import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends AppCompatActivity {

    private static final String TAG = "HomeActivity";

    private View startGameButton;
    private View topTabsStrip;
    private View statusBarLayout;
    private View guestProgressCard;

    private TextView playTabButton;
    private TextView inviteFriendsTabButton;
    private TextView buyTokensTabButton;
    private TextView rankingTabButton;
    private TextView friendsFilterAllButton;
    private TextView friendsFilterOnlineButton;
    private TextView guestRankingHint;
    private TextView guestFriendsHint;
    private TextView tvStatusTokens;
    private TextView tvStatusStars;
    private TextView tvStatusLeague;

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
    private ListView rankingListView;
    private ListView friendsListView;

    private boolean isGuest;
    private boolean onlineFilterEnabled;
    private FirestoreRepository firestoreRepository;
    private HomeRankingAdapter rankingAdapter;
    private HomeFriendAdapter friendAdapter;
    private final List<HomeRankingItem> rankingItems = new ArrayList<>();
    private final List<HomeFriendItem> allFriendItems = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        NotificationHelper.createNotificationChannels(this);

        Log.d(TAG, "onCreate");

        homeScrollView = findViewById(R.id.homeScrollView);
        startSection = findViewById(R.id.startSection);
        rankingSection = findViewById(R.id.rankingSection);
        friendsSection = findViewById(R.id.friendsSection);

        startGameButton = findViewById(R.id.startGameButton);
        topTabsStrip = findViewById(R.id.topTabsStrip);
        statusBarLayout = findViewById(R.id.statusBarLayout);
        guestProgressCard = findViewById(R.id.guestProgressCard);
        tvStatusTokens = findViewById(R.id.tvStatusTokens);
        tvStatusStars = findViewById(R.id.tvStatusStars);
        tvStatusLeague = findViewById(R.id.tvStatusLeague);

        playTabButton = findViewById(R.id.playTabButton);
        inviteFriendsTabButton = findViewById(R.id.inviteFriendsTabButton);
        buyTokensTabButton = findViewById(R.id.buyTokensTabButton);
        rankingTabButton = findViewById(R.id.rankingTabButton);
        friendsFilterAllButton = findViewById(R.id.friendsFilterAllButton);
        friendsFilterOnlineButton = findViewById(R.id.friendsFilterOnlineButton);

        guestLoginButton = findViewById(R.id.guestLoginButton);
        guestRegisterButton = findViewById(R.id.guestRegisterButton);
        buyTokensButton = findViewById(R.id.buyTokensButton);

        notificationsMenuButton = findViewById(R.id.notificationsMenuButton);
        tvProfile = findViewById(R.id.tvProfile);
        tvStatistics = findViewById(R.id.tvStatistics);
        tvFriends = findViewById(R.id.tvFriends);
        guestRankingHint = findViewById(R.id.guestRankingHint);
        guestFriendsHint = findViewById(R.id.guestFriendsHint);
        rankingListView = findViewById(R.id.rankingListView);
        friendsListView = findViewById(R.id.friendsListView);

        memberActionsGroup = findViewById(R.id.memberActionsGroup);
        guestActionsGroup = findViewById(R.id.guestActionsGroup);
        firestoreRepository = new FirestoreRepository(this);

        isGuest = getIntent().getBooleanExtra("IS_GUEST", false);
        configureGuestMode();
        setupHomeLists();

        if (!isGuest) {
            loadUserStatus();
        }

        checkTargetSection(getIntent());

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
        friendsFilterAllButton.setOnClickListener(v -> applyFriendFilter(false));
        friendsFilterOnlineButton.setOnClickListener(v -> applyFriendFilter(true));

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
        if (isGuest) {
            startLocalMatch();
            return;
        }

        String[] options = {
                getString(R.string.match_option_local),
                getString(R.string.match_option_create_room),
                getString(R.string.match_option_join_room)
        };

        new AlertDialog.Builder(this)
                .setTitle(R.string.match_mode_dialog_title)
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        startLocalMatch();
                    } else if (which == 1) {
                        createSharedMatch();
                    } else {
                        promptJoinSharedMatch();
                    }
                })
                .show();
    }

    private void startLocalMatch() {
        Intent intent = new Intent(HomeActivity.this, GameHostActivity.class);
        intent.putExtra("IS_GUEST", isGuest);
        intent.putExtra(GameHostActivity.EXTRA_START_ROUND, GameRound.MOJ_BROJ);
        startActivity(intent);
    }

    private void createSharedMatch() {
        SharedMatchRepository repository = new SharedMatchRepository();
        repository.createStudentOneMatch(new FirebaseCallback<SharedMatchRepository.MatchJoinResult>() {
            @Override
            public void onSuccess(SharedMatchRepository.MatchJoinResult result) {
                if (isFinishing()) {
                    return;
                }

                new AlertDialog.Builder(HomeActivity.this)
                        .setTitle(R.string.shared_match_created_title)
                        .setMessage(getString(R.string.shared_match_created_message, result.roomCode))
                        .setPositiveButton(R.string.shared_match_open_room, (dialog, which) ->
                                openSharedMatch(result.matchId, result.localPlayerNumber)
                        )
                        .setCancelable(false)
                        .show();
            }

            @Override
            public void onError(String error) {
                Toast.makeText(HomeActivity.this, error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void promptJoinSharedMatch() {
        EditText input = new EditText(this);
        input.setHint(R.string.shared_match_room_code_hint);
        input.setSingleLine();

        new AlertDialog.Builder(this)
                .setTitle(R.string.shared_match_join_title)
                .setView(input)
                .setPositiveButton(R.string.join, (dialog, which) -> joinSharedMatch(input.getText().toString()))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void joinSharedMatch(String roomCode) {
        SharedMatchRepository repository = new SharedMatchRepository();
        repository.joinStudentOneMatch(roomCode, new FirebaseCallback<SharedMatchRepository.MatchJoinResult>() {
            @Override
            public void onSuccess(SharedMatchRepository.MatchJoinResult result) {
                openSharedMatch(result.matchId, result.localPlayerNumber);
            }

            @Override
            public void onError(String error) {
                Toast.makeText(HomeActivity.this, error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void openSharedMatch(String matchId, int localPlayerNumber) {
        Intent intent = new Intent(HomeActivity.this, GameHostActivity.class);
        intent.putExtra("IS_GUEST", false);
        intent.putExtra(GameHostActivity.EXTRA_REMOTE_MATCH, true);
        intent.putExtra(GameHostActivity.EXTRA_REMOTE_MATCH_ID, matchId);
        intent.putExtra(GameHostActivity.EXTRA_LOCAL_PLAYER_NUMBER, localPlayerNumber);
        intent.putExtra(GameHostActivity.EXTRA_START_ROUND, GameRound.MOJ_BROJ);
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

    private void setupHomeLists() {
        rankingItems.clear();
        rankingItems.add(new HomeRankingItem(
                1,
                getString(R.string.ranking_player_1),
                Integer.parseInt(getString(R.string.ranking_points_1))
        ));
        rankingItems.add(new HomeRankingItem(
                2,
                getString(R.string.ranking_player_2),
                Integer.parseInt(getString(R.string.ranking_points_2))
        ));
        rankingItems.add(new HomeRankingItem(
                3,
                getString(R.string.ranking_player_3),
                Integer.parseInt(getString(R.string.ranking_points_3))
        ));
        rankingItems.add(new HomeRankingItem(
                4,
                getString(R.string.ranking_player_4),
                Integer.parseInt(getString(R.string.ranking_points_4))
        ));

        rankingAdapter = new HomeRankingAdapter(this, rankingItems);
        rankingListView.setAdapter(rankingAdapter);
        rankingListView.post(() -> setListViewHeightBasedOnChildren(rankingListView));

        allFriendItems.clear();
        allFriendItems.add(new HomeFriendItem(
                1,
                getString(R.string.friend_initial_1),
                getString(R.string.friend_name_1),
                Integer.parseInt(getString(R.string.friend_score_1)),
                true
        ));
        allFriendItems.add(new HomeFriendItem(
                2,
                getString(R.string.friend_initial_2),
                getString(R.string.friend_name_2),
                Integer.parseInt(getString(R.string.friend_score_2)),
                true
        ));
        allFriendItems.add(new HomeFriendItem(
                3,
                getString(R.string.friend_initial_3),
                getString(R.string.friend_name_3),
                Integer.parseInt(getString(R.string.friend_score_3)),
                false
        ));
        allFriendItems.add(new HomeFriendItem(
                4,
                getString(R.string.friend_initial_4),
                getString(R.string.friend_name_4),
                Integer.parseInt(getString(R.string.friend_score_4)),
                false
        ));

        friendAdapter = new HomeFriendAdapter(this, new ArrayList<>());
        friendsListView.setAdapter(friendAdapter);
        applyFriendFilter(false);
    }

    private void applyFriendFilter(boolean onlineOnly) {
        onlineFilterEnabled = onlineOnly;

        List<HomeFriendItem> visibleItems = new ArrayList<>();

        for (HomeFriendItem item : allFriendItems) {
            if (!onlineOnly || item.isOnline()) {
                visibleItems.add(item);
            }
        }

        visibleItems.add(HomeFriendItem.createInviteTile());

        friendAdapter.clear();
        friendAdapter.addAll(visibleItems);
        friendAdapter.notifyDataSetChanged();

        updateFriendFilterState();
        friendsListView.post(() -> setListViewHeightBasedOnChildren(friendsListView));
    }

    private void updateFriendFilterState() {
        friendsFilterAllButton.setBackgroundResource(
                onlineFilterEnabled
                        ? R.drawable.bg_lobby_tab_inactive
                        : R.drawable.bg_lobby_tab_active
        );
        friendsFilterOnlineButton.setBackgroundResource(
                onlineFilterEnabled
                        ? R.drawable.bg_lobby_tab_active
                        : R.drawable.bg_lobby_tab_inactive
        );

        friendsFilterAllButton.setTextColor(ContextCompat.getColor(
                this,
                onlineFilterEnabled ? R.color.slagalica_dark_blue : R.color.white
        ));
        friendsFilterOnlineButton.setTextColor(ContextCompat.getColor(
                this,
                onlineFilterEnabled ? R.color.white : R.color.slagalica_dark_blue
        ));
    }

    private void loadUserStatus() {
        firestoreRepository.getCurrentUser(new FirebaseCallback<User>() {
            @Override
            public void onSuccess(User user) {
                tvStatusTokens.setText(String.valueOf(user.tokens));
                tvStatusStars.setText(String.valueOf(user.stars));
                tvStatusLeague.setText(LeagueUtils.getLeagueName(user.league));
            }

            @Override
            public void onError(String error) {
                Toast.makeText(HomeActivity.this, error, Toast.LENGTH_SHORT).show();
            }
        });
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

    private void setListViewHeightBasedOnChildren(ListView listView) {
        ListAdapter adapter = listView.getAdapter();

        if (adapter == null) {
            return;
        }

        int totalHeight = 0;
        int widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(
                listView.getWidth(),
                View.MeasureSpec.AT_MOST
        );

        for (int index = 0; index < adapter.getCount(); index++) {
            View listItem = adapter.getView(index, null, listView);
            listItem.measure(widthMeasureSpec, View.MeasureSpec.UNSPECIFIED);
            totalHeight += listItem.getMeasuredHeight();
        }

        ViewGroup.LayoutParams params = listView.getLayoutParams();
        params.height = totalHeight + (listView.getDividerHeight() * (adapter.getCount() - 1));
        listView.setLayoutParams(params);
        listView.requestLayout();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        checkTargetSection(intent);
    }

    private void checkTargetSection(Intent intent) {
        if (intent == null || intent.getExtras() == null) return;
        String section = intent.getStringExtra("TARGET_SECTION");
        if (section == null) return;

        switch (section) {
            case "ranking":
                scrollToSection(rankingSection);
                break;
            case "profile":
                openProfile();
                break;
            case "friends":
                scrollToSection(friendsSection);
                break;
            case "chat":
                Toast.makeText(this, "Entering Regional Chat Room...", Toast.LENGTH_LONG).show();
                // Future: openChatActivity();
                break;
        }
    }

    @Override protected void onStart() { super.onStart(); Log.d(TAG, "onStart"); }
    @Override protected void onRestart() { super.onRestart(); Log.d(TAG, "onRestart"); }
    @Override protected void onResume() { super.onResume(); Log.d(TAG, "onResume"); }
    @Override protected void onPause() { super.onPause(); Log.d(TAG, "onPause"); }
    @Override protected void onStop() { super.onStop(); Log.d(TAG, "onStop"); }
    @Override protected void onDestroy() { super.onDestroy(); Log.d(TAG, "onDestroy"); }
}
