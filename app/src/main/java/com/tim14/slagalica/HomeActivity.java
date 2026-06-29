package com.tim14.slagalica;

import android.Manifest;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Bundle;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.text.TextUtils;
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
import android.view.Gravity;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.firestore.ListenerRegistration;
import com.tim14.slagalica.game.GameRound;
import com.tim14.slagalica.model.HomeFriendItem;
import com.tim14.slagalica.model.DailyMissionStatus;
import com.tim14.slagalica.model.HomeRankingItem;
import com.tim14.slagalica.model.RankingCycleInfo;
import com.tim14.slagalica.model.Notification;
import com.tim14.slagalica.model.User;
import com.tim14.slagalica.repository.FirebaseCallback;
import com.tim14.slagalica.repository.FirestoreRepository;
import com.tim14.slagalica.repository.SharedMatchRepository;
import com.tim14.slagalica.service.NotificationHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HomeActivity extends AppCompatActivity {

    private static final String TAG = "HomeActivity";
    private static final int REQUEST_SCAN_FRIEND_QR = 602;
    public static final String EXTRA_SHOW_REWARD_DIALOG = "SHOW_REWARD_DIALOG";

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
    private TextView weeklyRankingCycleText;
    private TextView monthlyRankingCycleText;
    private TextView dailyMissionsProgressText;
    private TextView missionWinMatchText;
    private TextView missionSendChatText;
    private TextView missionFriendlyText;
    private TextView missionTournamentText;
    private TextView tvStatusTokens;
    private TextView tvStatusStars;
    private TextView tvStatusLeague;

    private Button guestLoginButton;
    private Button guestRegisterButton;
    private Button buyTokensButton;
    private Button startTournamentButton;

    private TextView notificationsMenuButton;
    private TextView tvProfile;
    private TextView tvStatistics;
    private TextView tvFriends;
    private TextView tvRegions;

    private LinearLayout memberActionsGroup;
    private LinearLayout guestActionsGroup;
    private ScrollView homeScrollView;
    private View startSection;
    private View rankingSection;
    private View friendsSection;
    private ListView rankingListView;
    private ListView monthlyRankingListView;
    private ListView friendsListView;

    private boolean isGuest;
    private boolean onlineFilterEnabled;
    private boolean rewardDialogShown;
    private FirestoreRepository firestoreRepository;
    private ListenerRegistration friendsListener;
    private HomeRankingAdapter rankingAdapter;
    private HomeRankingAdapter monthlyRankingAdapter;
    private HomeFriendAdapter friendAdapter;
    private final List<HomeRankingItem> rankingItems = new ArrayList<>();
    private final List<HomeRankingItem> monthlyRankingItems = new ArrayList<>();
    private final Handler rankingRefreshHandler = new Handler(Looper.getMainLooper());
    private final Runnable rankingRefreshRunnable = new Runnable() {
        @Override
        public void run() {
            loadRankings();
            rankingRefreshHandler.postDelayed(this, 120000);
        }
    };
    private final List<HomeFriendItem> allFriendItems = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        NotificationHelper.createNotificationChannels(this);
        requestNotificationPermissionIfNeeded();

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
        startTournamentButton = findViewById(R.id.startTournamentButton);

        notificationsMenuButton = findViewById(R.id.notificationsMenuButton);
        tvProfile = findViewById(R.id.tvProfile);
        tvStatistics = findViewById(R.id.tvStatistics);
        tvFriends = findViewById(R.id.tvFriends);
        tvRegions = findViewById(R.id.tvRegions);
        guestRankingHint = findViewById(R.id.guestRankingHint);
        guestFriendsHint = findViewById(R.id.guestFriendsHint);
        weeklyRankingCycleText = findViewById(R.id.weeklyRankingCycleText);
        monthlyRankingCycleText = findViewById(R.id.monthlyRankingCycleText);
        dailyMissionsProgressText = findViewById(R.id.dailyMissionsProgressText);
        missionWinMatchText = findViewById(R.id.missionWinMatchText);
        missionSendChatText = findViewById(R.id.missionSendChatText);
        missionFriendlyText = findViewById(R.id.missionFriendlyText);
        missionTournamentText = findViewById(R.id.missionTournamentText);
        rankingListView = findViewById(R.id.rankingListView);
        monthlyRankingListView = findViewById(R.id.monthlyRankingListView);
        friendsListView = findViewById(R.id.friendsListView);

        memberActionsGroup = findViewById(R.id.memberActionsGroup);
        guestActionsGroup = findViewById(R.id.guestActionsGroup);
        firestoreRepository = new FirestoreRepository(this);
        initializeRegions();

        isGuest = getIntent().getBooleanExtra("IS_GUEST", false);
        configureGuestMode();
        setupHomeLists();
        if (!isGuest) {
            refreshRankingRewards();
            loadUserStatus();
            loadDailyMissions();
            startRankingAutoRefresh();
            startPlayableUsersListener();
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
        rankingTabButton.setOnClickListener(v -> openRankings());
        friendsFilterAllButton.setOnClickListener(v -> applyFriendFilter(false));
        friendsFilterOnlineButton.setOnClickListener(v -> applyFriendFilter(true));

        startGameButton.setOnClickListener(v -> openMatch());
        startTournamentButton.setOnClickListener(v -> openTournament());
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
        tvRegions.setOnClickListener(v -> openRegions());

        notificationsMenuButton.setOnClickListener(v -> openNotifications());
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            return;
        }

        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.POST_NOTIFICATIONS},
                201
        );
    }

    private void initializeRegions() {
        firestoreRepository.initializeRegions(new FirebaseCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                // Region documents are ready for ranking and reward updates.
            }

            @Override
            public void onError(String error) {
                Log.w(TAG, "Region initialization failed: " + error);
            }
        });
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

    private void openRegions() {
        if (isGuest) {
            redirectGuestToLogin();
            return;
        }

        startActivity(new Intent(HomeActivity.this, RegionMapActivity.class));
    }

    private void openRegionalChat() {
        if (isGuest) {
            redirectGuestToLogin();
            return;
        }

        startActivity(new Intent(HomeActivity.this, RegionalChatActivity.class));
    }

    private void openMatch() {
        if (isGuest) {
            startLocalMatch();
            return;
        }

        startCompetitiveMatchmaking();
    }

    private void startLocalMatch() {
        Intent intent = new Intent(HomeActivity.this, GameHostActivity.class);
        intent.putExtra("IS_GUEST", isGuest);
        intent.putExtra(GameHostActivity.EXTRA_FRIENDLY_MATCH, false);
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
        openSharedMatch(matchId, localPlayerNumber, false);
    }

    private void openSharedMatch(String matchId, int localPlayerNumber, boolean friendly) {
        Intent intent = new Intent(HomeActivity.this, GameHostActivity.class);
        intent.putExtra("IS_GUEST", false);
        intent.putExtra(GameHostActivity.EXTRA_REMOTE_MATCH, true);
        intent.putExtra(GameHostActivity.EXTRA_FRIENDLY_MATCH, friendly);
        intent.putExtra(GameHostActivity.EXTRA_REMOTE_MATCH_ID, matchId);
        intent.putExtra(GameHostActivity.EXTRA_LOCAL_PLAYER_NUMBER, localPlayerNumber);
        intent.putExtra(GameHostActivity.EXTRA_START_ROUND, GameRound.MOJ_BROJ);
        startActivity(intent);
    }

    private void openRankings() {
        if (isGuest) {
            redirectGuestToLogin();
            return;
        }
        startActivity(new Intent(HomeActivity.this, RankingActivity.class));
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

        rankingSection.setVisibility(View.GONE);
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
        monthlyRankingItems.clear();

        rankingAdapter = new HomeRankingAdapter(this, rankingItems);
        monthlyRankingAdapter = new HomeRankingAdapter(this, monthlyRankingItems);
        rankingListView.setAdapter(rankingAdapter);
        monthlyRankingListView.setAdapter(monthlyRankingAdapter);
        loadRankings();

        allFriendItems.clear();

        if (isGuest) {
            allFriendItems.add(new HomeFriendItem(
                    1,
                    getString(R.string.friend_initial_1),
                    getString(R.string.friend_name_1),
                    "",
                    "",
                    Integer.parseInt(getString(R.string.friend_score_1)),
                    1,
                    Integer.parseInt(getString(R.string.friend_score_1)),
                    1,
                    true,
                    false
            ));
            allFriendItems.add(new HomeFriendItem(
                    2,
                    getString(R.string.friend_initial_2),
                    getString(R.string.friend_name_2),
                    "",
                    "",
                    Integer.parseInt(getString(R.string.friend_score_2)),
                    2,
                    Integer.parseInt(getString(R.string.friend_score_2)),
                    1,
                    true,
                    false
            ));
            allFriendItems.add(new HomeFriendItem(
                    3,
                    getString(R.string.friend_initial_3),
                    getString(R.string.friend_name_3),
                    "",
                    "",
                    Integer.parseInt(getString(R.string.friend_score_3)),
                    3,
                    Integer.parseInt(getString(R.string.friend_score_3)),
                    0,
                    false,
                    false
            ));
            allFriendItems.add(new HomeFriendItem(
                    4,
                    getString(R.string.friend_initial_4),
                    getString(R.string.friend_name_4),
                    "",
                    "",
                    Integer.parseInt(getString(R.string.friend_score_4)),
                    0,
                    0,
                    0,
                    false,
                    false
            ));
        }

        friendAdapter = new HomeFriendAdapter(this, new ArrayList<>(), item -> {
            if (isGuest) {
                redirectGuestToLogin();
                return;
            }

            if (item.isInviteTile()) {
                showAddFriendOptions();
                return;
            }

            sendFriendlyInvite(item);
        });
        friendsListView.setAdapter(friendAdapter);
        applyFriendFilter(false);
    }
    private void loadRankings() {
        RankingCycleInfo weeklyInfo = firestoreRepository.getCurrentRankingCycleInfo(FirestoreRepository.RANKING_WEEKLY);
        RankingCycleInfo monthlyInfo = firestoreRepository.getCurrentRankingCycleInfo(FirestoreRepository.RANKING_MONTHLY);
        weeklyRankingCycleText.setText(getString(R.string.ranking_cycle_format, weeklyInfo.dateRange));
        monthlyRankingCycleText.setText(getString(R.string.ranking_cycle_format, monthlyInfo.dateRange));
        loadRankingList(FirestoreRepository.RANKING_WEEKLY, rankingAdapter, rankingListView);
        loadRankingList(FirestoreRepository.RANKING_MONTHLY, monthlyRankingAdapter, monthlyRankingListView);
    }

    private void loadRankingList(String type, HomeRankingAdapter adapter, ListView listView) {
        firestoreRepository.getPlayerRanking(type, new FirebaseCallback<List<HomeRankingItem>>() {
            @Override
            public void onSuccess(List<HomeRankingItem> items) {
                adapter.clear();
                if (items == null || items.isEmpty()) {
                    adapter.add(new HomeRankingItem(0, getString(R.string.ranking_empty), "", 0, 0));
                } else {
                    adapter.addAll(items);
                }
                adapter.notifyDataSetChanged();
                listView.post(() -> setListViewHeightBasedOnChildren(listView));
            }

            @Override
            public void onError(String error) {
                Toast.makeText(HomeActivity.this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadDailyMissions() {
        if (isGuest) {
            dailyMissionsProgressText.setText(getString(R.string.guest_progress_message));
            return;
        }

        firestoreRepository.getDailyMissionStatus(new FirebaseCallback<DailyMissionStatus>() {
            @Override
            public void onSuccess(DailyMissionStatus status) {
                dailyMissionsProgressText.setText(getString(
                        R.string.daily_missions_progress_format,
                        status.completedCount()
                ));
                bindMissionRow(missionWinMatchText, status.winMatch, R.string.daily_mission_win_match);
                bindMissionRow(missionSendChatText, status.sendChat, R.string.daily_mission_send_chat);
                bindMissionRow(missionFriendlyText, status.playFriendly, R.string.daily_mission_friendly);
                bindMissionRow(missionTournamentText, status.winTournament, R.string.daily_mission_tournament);

                if (status.bonusClaimed) {
                    dailyMissionsProgressText.setText(R.string.daily_missions_bonus_ready);
                }
            }

            @Override
            public void onError(String error) {
                dailyMissionsProgressText.setText(error);
            }
        });
    }

    private void bindMissionRow(TextView view, boolean completed, int labelResId) {
        view.setText(getString(
                R.string.daily_mission_row_format,
                completed ? String.valueOf((char) 0x2713) : "X",
                getString(labelResId),
                "+3 stars"
        ));
        view.setAlpha(completed ? 1f : 0.82f);
        view.setTextColor(ContextCompat.getColor(
                this,
                completed ? R.color.slagalica_yellow : R.color.white
        ));
    }

    private void openTournament() {
        if (isGuest) {
            redirectGuestToLogin();
            return;
        }

        SharedMatchRepository repository = new SharedMatchRepository();
        repository.startTournamentMatchmaking(new FirebaseCallback<SharedMatchRepository.MatchJoinResult>() {
            @Override
            public void onSuccess(SharedMatchRepository.MatchJoinResult result) {
                if (result.waitingForOpponent) {
                    Toast.makeText(HomeActivity.this, R.string.tournament_waiting_message, Toast.LENGTH_SHORT).show();
                }
                openSharedMatch(result.matchId, result.localPlayerNumber);
            }

            @Override
            public void onError(String error) {
                Toast.makeText(HomeActivity.this, error, Toast.LENGTH_LONG).show();
            }
        });
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

    private void startPlayableUsersListener() {
        startFriendsListener();
    }

    private void startFriendsListener() {
        if (isGuest || friendsListener != null) {
            return;
        }

        friendsListener = firestoreRepository.listenToFriends(new FirebaseCallback<List<User>>() {
            @Override
            public void onSuccess(List<User> users) {
                allFriendItems.clear();

                int rank = 1;
                for (User user : users) {
                    allFriendItems.add(createFriendItem(rank++, user));
                }

                applyFriendFilter(onlineFilterEnabled);
            }

            @Override
            public void onError(String error) {
                Toast.makeText(HomeActivity.this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void stopPlayableUsersListener() {
        if (friendsListener == null) {
            return;
        }

        friendsListener.remove();
        friendsListener = null;
    }

    private HomeFriendItem createFriendItem(int rank, User user) {
        String username = user.username == null ? "Player" : user.username.trim();
        String initial = username.isEmpty()
                ? "?"
                : username.substring(0, 1).toUpperCase();

        return new HomeFriendItem(
                rank,
                initial,
                username,
                user.id,
                user.avatar,
                user.stars,
                user.currentMonthlyRank,
                user.monthlyStars,
                user.league,
                user.loggedIn,
                user.currentMatchId != null && !user.currentMatchId.trim().isEmpty()
        );
    }

    private void startCompetitiveMatchmaking() {
        SharedMatchRepository repository = new SharedMatchRepository();
        repository.startCompetitiveMatchmaking(new FirebaseCallback<SharedMatchRepository.MatchJoinResult>() {
            @Override
            public void onSuccess(SharedMatchRepository.MatchJoinResult result) {
                if (result.waitingForOpponent) {
                    Toast.makeText(
                            HomeActivity.this,
                            R.string.matchmaking_waiting_message,
                            Toast.LENGTH_SHORT
                    ).show();
                }

                openSharedMatch(result.matchId, result.localPlayerNumber);
            }

            @Override
            public void onError(String error) {
                Toast.makeText(HomeActivity.this, error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void sendFriendlyInvite(HomeFriendItem item) {
        if (item == null || item.isInviteTile()) {
            return;
        }

        if (item.isInMatch()) {
            Toast.makeText(this, R.string.friend_busy_in_match_message, Toast.LENGTH_SHORT).show();
            return;
        }

        if (!item.isOnline()) {
            Toast.makeText(this, R.string.friend_offline_invite_message, Toast.LENGTH_SHORT).show();
            return;
        }

        SharedMatchRepository repository = new SharedMatchRepository();
        repository.createFriendlyInvite(item.getUserId(), new FirebaseCallback<SharedMatchRepository.MatchJoinResult>() {
            @Override
            public void onSuccess(SharedMatchRepository.MatchJoinResult result) {
                Toast.makeText(
                        HomeActivity.this,
                        R.string.friendly_invite_sent_message,
                        Toast.LENGTH_SHORT
                ).show();
                openSharedMatch(result.matchId, result.localPlayerNumber, true);
            }

            @Override
            public void onError(String error) {
                Toast.makeText(HomeActivity.this, error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showAddFriendOptions() {
        String[] options = new String[]{
                getString(R.string.friend_add_by_username),
                getString(R.string.friend_add_by_qr)
        };

        new AlertDialog.Builder(this)
                .setTitle(R.string.friend_add_title)
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        showUsernameSearchDialog();
                    } else {
                        openQrScanner();
                    }
                })
                .show();
    }

    private void showUsernameSearchDialog() {
        EditText input = new EditText(this);
        input.setHint(R.string.friend_username_hint);
        input.setSingleLine();

        new AlertDialog.Builder(this)
                .setTitle(R.string.friend_search_title)
                .setView(input)
                .setPositiveButton(R.string.search, (dialog, which) ->
                        searchFriendByUsername(input.getText().toString()))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void searchFriendByUsername(String usernameQuery) {
        firestoreRepository.searchUsersByUsername(usernameQuery, new FirebaseCallback<List<User>>() {
            @Override
            public void onSuccess(List<User> users) {
                if (users.isEmpty()) {
                    Toast.makeText(HomeActivity.this, R.string.friend_search_empty, Toast.LENGTH_SHORT).show();
                    return;
                }

                String[] names = new String[users.size()];
                for (int index = 0; index < users.size(); index++) {
                    User user = users.get(index);
                    names[index] = user.username + " - " + LeagueUtils.getLeagueName(user.league);
                }

                new AlertDialog.Builder(HomeActivity.this)
                        .setTitle(R.string.friend_search_results_title)
                        .setItems(names, (dialog, which) -> addFriend(users.get(which).id))
                        .show();
            }

            @Override
            public void onError(String error) {
                Toast.makeText(HomeActivity.this, error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void addFriend(String friendUserId) {
        firestoreRepository.addFriend(friendUserId, new FirebaseCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                Toast.makeText(HomeActivity.this, R.string.friend_added_message, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String error) {
                Toast.makeText(HomeActivity.this, error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void openQrScanner() {
        Intent intent = new Intent(HomeActivity.this, QRScannerActivity.class);
        startActivityForResult(intent, REQUEST_SCAN_FRIEND_QR);
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode != REQUEST_SCAN_FRIEND_QR || resultCode != RESULT_OK || data == null) {
            return;
        }

        String qrCode = data.getStringExtra(QRScannerActivity.EXTRA_QR_TEXT);
        firestoreRepository.addFriendByQrCode(qrCode, new FirebaseCallback<User>() {
            @Override
            public void onSuccess(User user) {
                Toast.makeText(
                        HomeActivity.this,
                        getString(R.string.friend_added_named_message, user.username),
                        Toast.LENGTH_SHORT
                ).show();
            }

            @Override
            public void onError(String error) {
                Toast.makeText(HomeActivity.this, error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void checkTargetSection(Intent intent) {
        if (intent == null || intent.getExtras() == null) return;
        String section = intent.getStringExtra("TARGET_SECTION");
        boolean showRewardDialog = intent.getBooleanExtra(EXTRA_SHOW_REWARD_DIALOG, false);

        if (section != null) {
            switch (section) {
                case "ranking":
                    openRankings();
                    break;
                case "profile":
                    openProfile();
                    break;
                case "friends":
                    scrollToSection(friendsSection);
                    break;
                case "chat":
                    openRegionalChat();
                    break;
            }
        }

        if (showRewardDialog && !isGuest) {
            homeScrollView.postDelayed(this::showPendingRewardDialog, 350);
        }
    }

    private void refreshRankingRewards() {
        firestoreRepository.refreshRankingRewards(new FirebaseCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                loadRankings();
                loadUserStatus();
                showPendingRewardDialog();
            }

            @Override
            public void onError(String error) {
                Log.w(TAG, "Ranking reward refresh failed: " + error);
            }
        });
    }

    private void showPendingRewardDialog() {
        if (isGuest || rewardDialogShown) {
            return;
        }

        firestoreRepository.getNotifications(new FirebaseCallback<List<Notification>>() {
            @Override
            public void onSuccess(List<Notification> notifications) {
                if (notifications == null) {
                    return;
                }
                for (Notification notification : notifications) {
                    if (notification == null || notification.read) {
                        continue;
                    }
                    boolean rankingReward = "RANKING".equalsIgnoreCase(notification.typeString)
                            && notification.title != null
                            && notification.title.toLowerCase(Locale.US).contains("ranking reward");
                    boolean rewardMessage = notification.message != null
                            && (notification.message.toLowerCase(Locale.US).contains("earned")
                            || notification.message.toLowerCase(Locale.US).contains("tokens"));
                    if (!rankingReward || !rewardMessage) {
                        continue;
                    }
                    rewardDialogShown = true;
                    firestoreRepository.markNotificationAsRead(notification.id);
                    showRewardAnimationDialog(notification.title, notification.message);
                    return;
                }
            }

            @Override
            public void onError(String error) {
                Log.w(TAG, "Reward dialog lookup failed: " + error);
            }
        });
    }

    private void showRewardAnimationDialog(String title, String message) {
        int outerPadding = dpToPx(22);
        LinearLayout rewardPanel = new LinearLayout(this);
        rewardPanel.setOrientation(LinearLayout.VERTICAL);
        rewardPanel.setGravity(Gravity.CENTER_HORIZONTAL);
        rewardPanel.setPadding(outerPadding, dpToPx(24), outerPadding, dpToPx(18));
        rewardPanel.setBackgroundResource(R.drawable.bg_home_panel);

        TextView trophyView = new TextView(this);
        trophyView.setText(getString(R.string.status_tokens_icon));
        trophyView.setTextSize(40f);
        trophyView.setGravity(Gravity.CENTER);
        trophyView.setTextColor(ContextCompat.getColor(this, R.color.slagalica_yellow));
        trophyView.setBackgroundResource(R.drawable.bg_match_timer_circle);
        LinearLayout.LayoutParams trophyParams = new LinearLayout.LayoutParams(dpToPx(82), dpToPx(82));
        rewardPanel.addView(trophyView, trophyParams);

        TextView titleView = new TextView(this);
        titleView.setText(title == null ? "Ranking reward" : title);
        titleView.setTextColor(ContextCompat.getColor(this, R.color.slagalica_yellow));
        titleView.setTextSize(22f);
        titleView.setGravity(Gravity.CENTER);
        titleView.setTypeface(titleView.getTypeface(), android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        titleParams.topMargin = dpToPx(14);
        rewardPanel.addView(titleView, titleParams);

        TextView messageView = new TextView(this);
        messageView.setText(message == null ? "Reward earned." : message);
        messageView.setTextColor(ContextCompat.getColor(this, R.color.white));
        messageView.setTextSize(16f);
        messageView.setGravity(Gravity.CENTER);
        messageView.setLineSpacing(dpToPx(2), 1f);
        LinearLayout.LayoutParams messageParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        messageParams.topMargin = dpToPx(10);
        rewardPanel.addView(messageView, messageParams);

        TextView sparkleView = new TextView(this);
        sparkleView.setText("+ tokens");
        sparkleView.setTextColor(ContextCompat.getColor(this, R.color.slagalica_dark_blue));
        sparkleView.setTextSize(18f);
        sparkleView.setGravity(Gravity.CENTER);
        sparkleView.setTypeface(sparkleView.getTypeface(), android.graphics.Typeface.BOLD);
        sparkleView.setBackgroundResource(R.drawable.bg_button_primary);
        LinearLayout.LayoutParams sparkleParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dpToPx(46)
        );
        sparkleParams.topMargin = dpToPx(18);
        rewardPanel.addView(sparkleView, sparkleParams);

        Button okButton = new Button(this);
        okButton.setAllCaps(false);
        okButton.setText(android.R.string.ok);
        okButton.setTextColor(ContextCompat.getColor(this, R.color.slagalica_dark_blue));
        okButton.setTextSize(15f);
        okButton.setTypeface(okButton.getTypeface(), android.graphics.Typeface.BOLD);
        okButton.setBackgroundResource(R.drawable.bg_button_secondary);
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dpToPx(44)
        );
        buttonParams.topMargin = dpToPx(12);
        rewardPanel.addView(okButton, buttonParams);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(rewardPanel)
                .create();
        okButton.setOnClickListener(v -> dialog.dismiss());
        dialog.setOnShowListener(dialogInterface -> {
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            }
            ToneGenerator toneGenerator = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80);
            toneGenerator.startTone(ToneGenerator.TONE_PROP_ACK, 220);
            rewardPanel.setScaleX(0.72f);
            rewardPanel.setScaleY(0.72f);
            rewardPanel.setAlpha(0.1f);
            trophyView.setRotation(-12f);
            AnimatorSet set = new AnimatorSet();
            set.playTogether(
                    ObjectAnimator.ofFloat(rewardPanel, View.SCALE_X, 0.72f, 1.06f, 1f),
                    ObjectAnimator.ofFloat(rewardPanel, View.SCALE_Y, 0.72f, 1.06f, 1f),
                    ObjectAnimator.ofFloat(rewardPanel, View.ALPHA, 0.1f, 1f),
                    ObjectAnimator.ofFloat(trophyView, View.ROTATION, -12f, 12f, 0f),
                    ObjectAnimator.ofFloat(sparkleView, View.TRANSLATION_Y, dpToPx(12), 0f)
            );
            set.setDuration(750);
            set.start();
            rewardPanel.postDelayed(toneGenerator::release, 500);
        });
        dialog.show();
    }
    private void startRankingAutoRefresh() {
        rankingRefreshHandler.removeCallbacks(rankingRefreshRunnable);
        rankingRefreshHandler.post(rankingRefreshRunnable);
    }

    private void stopRankingAutoRefresh() {
        rankingRefreshHandler.removeCallbacks(rankingRefreshRunnable);
    }
    @Override protected void onStart() { super.onStart(); Log.d(TAG, "onStart"); }
    @Override protected void onRestart() { super.onRestart(); Log.d(TAG, "onRestart"); }
    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        requestNotificationPermissionIfNeeded();
        if (!isGuest) {
            firestoreRepository.markCurrentUserLoggedIn();
            firestoreRepository.markCurrentUserInApp(true);
            firestoreRepository.clearCurrentUserMatch();
            refreshRankingRewards();
            loadUserStatus();
            loadDailyMissions();
            startRankingAutoRefresh();
            startPlayableUsersListener();

            firestoreRepository.markCurrentUserActive(new FirebaseCallback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    // Activity timestamp is used for regional active player counts.
                }

                @Override
                public void onError(String error) {
                    Log.w(TAG, "Active user update failed: " + error);
                }
            });
        }
    }
    @Override
    protected void onPause() {
        if (!isGuest) {
            firestoreRepository.markCurrentUserInApp(false);
            stopPlayableUsersListener();
        }
        stopRankingAutoRefresh();
        super.onPause();
        Log.d(TAG, "onPause");
    }
    @Override protected void onStop() { super.onStop(); Log.d(TAG, "onStop"); }
    @Override
    protected void onDestroy() {
        stopPlayableUsersListener();
        stopRankingAutoRefresh();
        super.onDestroy();
        Log.d(TAG, "onDestroy");
    }
}
