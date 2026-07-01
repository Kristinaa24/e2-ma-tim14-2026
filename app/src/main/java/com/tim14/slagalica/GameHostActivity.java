package com.tim14.slagalica;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.firebase.firestore.ListenerRegistration;
import com.tim14.slagalica.fragments.AsocijacijeFragment;
import com.tim14.slagalica.fragments.KoZnaZnaFragment;
import com.tim14.slagalica.fragments.KorakPoKorakFragment;
import com.tim14.slagalica.fragments.MatchResultFragment;
import com.tim14.slagalica.fragments.MojBrojFragment;
import com.tim14.slagalica.fragments.PlaceholderRoundFragment;
import com.tim14.slagalica.fragments.SkockoFragment;
import com.tim14.slagalica.fragments.SpojniceFragment;
import com.tim14.slagalica.game.GameNavigator;
import com.tim14.slagalica.game.GameRound;
import com.tim14.slagalica.model.KoZnaZnaQuestion;
import com.tim14.slagalica.model.SharedAsocijacijeRound;
import com.tim14.slagalica.model.SharedKorakPoKorakRound;
import com.tim14.slagalica.model.SharedMatchState;
import com.tim14.slagalica.model.SharedMojBrojRound;
import com.tim14.slagalica.model.SharedSkockoRound;
import com.tim14.slagalica.model.SharedSpojniceRound;
import com.tim14.slagalica.model.User;
import com.tim14.slagalica.repository.ChallengeRepository;
import com.tim14.slagalica.repository.FirebaseCallback;
import com.tim14.slagalica.repository.FirestoreRepository;
import com.tim14.slagalica.repository.SharedMatchRepository;
import com.tim14.slagalica.service.ProfileService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GameHostActivity extends AppCompatActivity implements GameNavigator {

    public static final String EXTRA_START_ROUND = "start_round";
    public static final String EXTRA_REMOTE_MATCH = "remote_match";
    public static final String EXTRA_REMOTE_MATCH_ID = "remote_match_id";
    public static final String EXTRA_LOCAL_PLAYER_NUMBER = "local_player_number";
    public static final String EXTRA_FRIENDLY_MATCH = "friendly_match";
    public static final String EXTRA_CHALLENGE_MODE = "challenge_mode";
    public static final String EXTRA_CHALLENGE_ID = "challenge_id";

    private TextView tvRoundTimer;
    private TextView tvPhaseText;
    private TextView tvMatchMeta;
    private TextView tvPlayerOneScore;
    private TextView tvPlayerTwoScore;
    private TextView tvPlayerOneName;
    private TextView tvPlayerTwoName;
    private TextView tvPlayerOneChatBubble;
    private TextView tvPlayerTwoChatBubble;
    private TextView tvPlayerOneChatBadge;
    private TextView tvPlayerTwoChatBadge;
    private TextView tvStatusTokens;
    private TextView tvStatusStars;
    private TextView tvStatusLeague;
    private Button btnQuitMatch;
    private View matchPlayersHeader;
    private View matchPhaseHeader;
    private View matchContentArea;
    private View tournamentMatchingPanel;
    private TextView tournamentMatchingTitle;
    private TextView tournamentMatchingStatus;
    private TextView tournamentMatchingSubtitle;
    private ImageView[] tournamentSlotAvatars;
    private TextView[] tournamentSlotNames;
    private TextView[] tournamentSlotLeagues;

    private int playerOneScore;
    private int playerTwoScore;
    private GameRound currentRound;
    private boolean isGuest;
    private boolean friendlyMatch;
    private boolean matchResultRecorded;
    private FirestoreRepository firestoreRepository;
    private ChallengeRepository challengeRepository;
    private SharedMatchRepository sharedMatchRepository;
    private ListenerRegistration sharedMatchListener;
    private SharedMatchState sharedMatchState;
    private boolean remoteMatchMode;
    private String remoteMatchId;
    private int localPlayerNumber;
    private long lastAppliedRemoteUpdatedAt;
    private long lastObservedRemoteMatchStartedAt;
    private int lastObservedForfeitedPlayer;
    private String currentUserRegion = "";
    private String opponentRegion = "";
    private int unreadChatCount;
    private ListenerRegistration unreadChatListener;
    private boolean challengeMode;
    private String challengeId;
    private boolean tournamentStartAnimationRunning;
    private final ProfileService profileService = new ProfileService();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_host);

        tvRoundTimer = findViewById(R.id.tvRoundTimer);
        tvPhaseText = findViewById(R.id.tvPhaseText);
        tvMatchMeta = findViewById(R.id.tvMatchMeta);
        tvPlayerOneScore = findViewById(R.id.tvPlayerOneScore);
        tvPlayerTwoScore = findViewById(R.id.tvPlayerTwoScore);
        tvPlayerOneName = findViewById(R.id.tvPlayerOneName);
        tvPlayerTwoName = findViewById(R.id.tvPlayerTwoName);
        tvPlayerOneChatBubble = findViewById(R.id.tvPlayerOneChatBubble);
        tvPlayerTwoChatBubble = findViewById(R.id.tvPlayerTwoChatBubble);
        tvPlayerOneChatBadge = findViewById(R.id.tvPlayerOneChatBadge);
        tvPlayerTwoChatBadge = findViewById(R.id.tvPlayerTwoChatBadge);
        tvStatusTokens = findViewById(R.id.tvStatusTokens);
        tvStatusStars = findViewById(R.id.tvStatusStars);
        tvStatusLeague = findViewById(R.id.tvStatusLeague);
        btnQuitMatch = findViewById(R.id.btnQuitMatch);
        matchPlayersHeader = findViewById(R.id.matchPlayersHeader);
        matchPhaseHeader = findViewById(R.id.matchPhaseHeader);
        matchContentArea = findViewById(R.id.matchContentArea);
        tournamentMatchingPanel = findViewById(R.id.tournamentMatchingPanel);
        tournamentMatchingTitle = findViewById(R.id.tournamentMatchingTitle);
        tournamentMatchingStatus = findViewById(R.id.tournamentMatchingStatus);
        tournamentMatchingSubtitle = findViewById(R.id.tournamentMatchingSubtitle);
        setupTournamentMatchingSlots();
        firestoreRepository = new FirestoreRepository();
        challengeRepository = new ChallengeRepository();
        sharedMatchRepository = new SharedMatchRepository();
        isGuest = getIntent().getBooleanExtra("IS_GUEST", false);
        friendlyMatch = getIntent().getBooleanExtra(EXTRA_FRIENDLY_MATCH, false);
        remoteMatchMode = getIntent().getBooleanExtra(EXTRA_REMOTE_MATCH, false);
        remoteMatchId = getIntent().getStringExtra(EXTRA_REMOTE_MATCH_ID);
        localPlayerNumber = getIntent().getIntExtra(EXTRA_LOCAL_PLAYER_NUMBER, 1);
        challengeMode = getIntent().getBooleanExtra(EXTRA_CHALLENGE_MODE, false);
        challengeId = getIntent().getStringExtra(EXTRA_CHALLENGE_ID);

        btnQuitMatch.setOnClickListener(v -> surrenderMatch());
        tvPlayerOneChatBubble.setOnClickListener(v -> openRegionalChat());
        tvPlayerTwoChatBubble.setOnClickListener(v -> openRegionalChat());
        bindLocalPlayerChatBubble();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                surrenderMatch();
            }
        });

        setScores(0, 0);
        setTimerValue(0);
        setPhaseText(getString(R.string.phase_waiting_for_round));
        tvMatchMeta.setText(challengeMode ? getString(R.string.challenge_match_meta) : "");
        if (challengeMode) {
            tvPlayerOneName.setText(R.string.challenge_player_label);
            tvPlayerTwoName.setText(R.string.challenge_opponent_label);
        }

        if (!isGuest) {
            loadUserStatus();
        }

        if (remoteMatchMode) {
            startListeningToRemoteMatch();
            return;
        }

        if (savedInstanceState == null) {
            GameRound startRound =
                    (GameRound) getIntent().getSerializableExtra(EXTRA_START_ROUND);

            if (startRound == null) {
                startRound = challengeMode ? GameRound.KO_ZNA_ZNA : GameRound.MOJ_BROJ;
            }

            goToRound(startRound, null);
        }
    }

    @Override
    public void setPhaseText(String text) {
        tvPhaseText.setText(text);
    }

    @Override
    public void setTimerValue(int seconds) {
        tvRoundTimer.setText(String.valueOf(seconds));
    }

    @Override
    public void setScores(int playerOne, int playerTwo) {
        playerOneScore = playerOne;
        playerTwoScore = playerTwo;
        tvPlayerOneScore.setText(String.valueOf(playerOne));
        tvPlayerTwoScore.setText(String.valueOf(playerTwo));
    }

    @Override
    public int getPlayerOneScore() {
        return playerOneScore;
    }

    @Override
    public int getPlayerTwoScore() {
        return playerTwoScore;
    }

    @Override
    public void goToRound(GameRound round, Bundle args) {
        markCurrentUserActive();
        currentRound = round;
        btnQuitMatch.setVisibility(round == GameRound.RESULT ? View.GONE : View.VISIBLE);

        if (round == GameRound.RESULT) {
            recordMatchResult();
        }

        Fragment fragment;

        switch (round) {
            case KO_ZNA_ZNA:
                fragment = new KoZnaZnaFragment();
                break;
            case SPOJNICE:
                fragment = new SpojniceFragment();
                break;
            case SKOCKO:
                fragment = new SkockoFragment();
                break;
            case ASOCIJACIJE:
                fragment = new AsocijacijeFragment();
                break;
            case KORAK_PO_KORAK:
                fragment = new KorakPoKorakFragment();
                break;
            case MOJ_BROJ:
                fragment = new MojBrojFragment();
                break;
            case RESULT:
                fragment = new MatchResultFragment();
                break;
            default:
                fragment = PlaceholderRoundFragment.newInstance(round.name());
                break;
        }

        if (args != null) {
            fragment.setArguments(args);
        }

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.gameContentContainer, fragment)
                .commitAllowingStateLoss();
    }

    @Override
    public void goToNextRound() {
        if (remoteMatchMode) {
            goToNextRemoteRound();
            return;
        }

        if (currentRound == null) {
            finishMatch();
            return;
        }

        GameRound nextRound = currentRound.nextInMatch();

        if (nextRound == null) {
            finishMatch();
            return;
        }

        goToRound(nextRound, null);
    }

    @Override
    public void recordMatchResult() {
        if (matchResultRecorded || isGuest) {
            return;
        }

        if (challengeMode) {
            submitChallengeScore(false);
            return;
        }

        if (remoteMatchMode) {
            matchResultRecorded = true;


            sharedMatchRepository.finalizeMatchIfNeeded(remoteMatchId, new FirebaseCallback<SharedMatchRepository.MatchFinalizationResult>() {
                @Override
                public void onSuccess(SharedMatchRepository.MatchFinalizationResult result) {
                    if (result != null && result.leagueChanged) {
                        saveLeagueChangeAlert(result.previousLeague, result.currentLeague);
                        showLeagueChangeDialog(result.previousLeague, result.currentLeague);
                    }
                    completeMatchMissions(result);
                    loadUserStatus();
                }

                @Override
                public void onError(String error) {
                    matchResultRecorded = false;
                    Toast.makeText(GameHostActivity.this, error, Toast.LENGTH_SHORT).show();
                }
            });
            return;
        }

        recordMatchStatistics(
                playerOneScore > playerTwoScore,
                playerOneScore < playerTwoScore,
                !friendlyMatch
        );
    }

    @Override
    public void restartMatch() {
        if (challengeMode) {
            finishMatch();
            return;
        }

        if (remoteMatchMode) {
            restartRemoteMatch();
            return;
        }

        matchResultRecorded = false;
        setScores(0, 0);
        goToRound(GameRound.KO_ZNA_ZNA, null);
    }

    @Override
    public void finishMatch() {
        if (remoteMatchMode) {
            if (sharedMatchState != null
                    && SharedMatchState.STATUS_WAITING.equals(sharedMatchState.status)
                    && localPlayerNumber == 1) {
                cancelPendingRemoteMatch();
            }

            if (sharedMatchState != null
                    && currentRound == GameRound.RESULT
                    && SharedMatchState.STATUS_FINISHED.equals(sharedMatchState.status)) {
                if (sharedMatchState.rematchRequestedBy == localPlayerNumber) {
                    cancelPendingRemoteRematchRequest();
                } else if (sharedMatchState.rematchRequestedBy != 0) {
                    declinePendingRemoteRematchRequest();
                }
            }

            firestoreRepository.clearCurrentUserMatch();
        }

        if (sharedMatchListener != null) {
            sharedMatchListener.remove();
            sharedMatchListener = null;
        }
        finish();
    }

    public void finishMatchAndReturnHome() {
        finishMatch();
        Intent intent = new Intent(this, HomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
    }
    private void loadUserStatus() {
        firestoreRepository.getCurrentUser(new FirebaseCallback<User>() {
            @Override
            public void onSuccess(User user) {
                tvStatusTokens.setText(String.valueOf(user.tokens));
                tvStatusStars.setText(String.valueOf(user.stars));
                tvStatusLeague.setText(LeagueUtils.getLeagueName(user.league));
                currentUserRegion = FirestoreRepository.canonicalRegionName(user.region);
                if (challengeMode) {
                    tvPlayerOneName.setText(TextUtils.isEmpty(user.username)
                            ? getString(R.string.challenge_player_label)
                            : user.username);
                    tvPlayerTwoName.setText(R.string.challenge_opponent_label);
                    tvMatchMeta.setText(getString(R.string.challenge_match_meta));
                }
                resolveOpponentRegion();
            }

            @Override
            public void onError(String error) {
                Toast.makeText(GameHostActivity.this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openRegionalChat() {
        if (isGuest) {
            Toast.makeText(this, R.string.guest_login_required_message, Toast.LENGTH_SHORT).show();
            return;
        }

        if (remoteMatchMode && !canPlayersUseMatchChat()) {
            Toast.makeText(this, R.string.chat_match_region_restricted_message, Toast.LENGTH_LONG).show();
            return;
        }

        if (getSupportFragmentManager().findFragmentByTag("regional_chat_dialog") != null) {
            return;
        }

        RegionalChatDialogFragment.newInstance().show(
                getSupportFragmentManager(),
                "regional_chat_dialog"
        );
    }

    private void bindLocalPlayerChatBubble() {
        if (challengeMode) {
            tvPlayerOneChatBubble.setVisibility(View.GONE);
            tvPlayerTwoChatBubble.setVisibility(View.GONE);
            tvPlayerOneChatBadge.setVisibility(View.GONE);
            tvPlayerTwoChatBadge.setVisibility(View.GONE);
            return;
        }

        boolean showPlayerTwoBubble = remoteMatchMode && localPlayerNumber == 2;
        tvPlayerOneChatBubble.setVisibility(showPlayerTwoBubble ? View.GONE : View.VISIBLE);
        tvPlayerTwoChatBubble.setVisibility(showPlayerTwoBubble ? View.VISIBLE : View.GONE);
        tvPlayerOneChatBadge.setVisibility(View.GONE);
        tvPlayerTwoChatBadge.setVisibility(View.GONE);
        startUnreadChatBadgeListener();
        updateChatBubbleEnabledState();
    }

    private void startUnreadChatBadgeListener() {
        if (isGuest || firestoreRepository == null) {
            return;
        }

        if (unreadChatListener != null) {
            return;
        }

        unreadChatListener = firestoreRepository.listenToUnreadChatNotificationCount(new FirebaseCallback<Integer>() {
            @Override
            public void onSuccess(Integer unreadCount) {
                unreadChatCount = unreadCount == null ? 0 : unreadCount;
                updateUnreadChatBadge(unreadChatCount);
            }

            @Override
            public void onError(String error) {
                unreadChatCount = 0;
                updateUnreadChatBadge(0);
            }
        });
    }

    private void updateUnreadChatBadge(int unreadCount) {
        boolean usePlayerTwo = remoteMatchMode && localPlayerNumber == 2;
        TextView activeBadge = usePlayerTwo ? tvPlayerTwoChatBadge : tvPlayerOneChatBadge;
        TextView hiddenBadge = usePlayerTwo ? tvPlayerOneChatBadge : tvPlayerTwoChatBadge;

        hiddenBadge.setVisibility(View.GONE);

        if (unreadCount <= 0 || (remoteMatchMode && !canPlayersUseMatchChat())) {
            activeBadge.setVisibility(View.GONE);
            return;
        }

        activeBadge.setText(String.valueOf(Math.min(unreadCount, 99)));
        activeBadge.setVisibility(View.VISIBLE);
    }

    private void resolveOpponentRegion() {
        if (!remoteMatchMode || sharedMatchState == null) {
            opponentRegion = "";
            updateChatBubbleEnabledState();
            return;
        }

        String opponentId = localPlayerNumber == 1
                ? sharedMatchState.playerTwoId
                : sharedMatchState.playerOneId;
        if (TextUtils.isEmpty(opponentId)) {
            opponentRegion = "";
            updateChatBubbleEnabledState();
            return;
        }

        firestoreRepository.getUserById(opponentId, new FirebaseCallback<User>() {
            @Override
            public void onSuccess(User user) {
                opponentRegion = FirestoreRepository.canonicalRegionName(user.region);
                updateChatBubbleEnabledState();
                updateUnreadChatBadge(unreadChatCount);
            }

            @Override
            public void onError(String error) {
                opponentRegion = "";
                updateChatBubbleEnabledState();
            }
        });
    }

    private boolean canPlayersUseMatchChat() {
        if (!remoteMatchMode) {
            return true;
        }

        if (sharedMatchState == null
                || TextUtils.isEmpty(sharedMatchState.playerOneId)
                || TextUtils.isEmpty(sharedMatchState.playerTwoId)) {
            return true;
        }

        if (TextUtils.isEmpty(currentUserRegion) || TextUtils.isEmpty(opponentRegion)) {
            return false;
        }

        return currentUserRegion.equals(opponentRegion);
    }

    private void updateChatBubbleEnabledState() {
        boolean enabled = !remoteMatchMode || canPlayersUseMatchChat();
        boolean showPlayerTwoBubble = remoteMatchMode && localPlayerNumber == 2;
        TextView activeBubble = showPlayerTwoBubble ? tvPlayerTwoChatBubble : tvPlayerOneChatBubble;
        activeBubble.setAlpha(enabled ? 1f : 0.55f);
    }

    private void surrenderMatch() {
        boolean cancelingPendingInvite = !challengeMode && isWaitingForFriendlyInvite();
        int titleRes = cancelingPendingInvite
                ? R.string.cancel_invite_confirmation_title
                : R.string.quit_confirmation_title;
        int messageRes = cancelingPendingInvite
                ? R.string.cancel_invite_confirmation_message
                : (challengeMode
                ? R.string.challenge_quit_confirmation_message
                : R.string.quit_confirmation_message);
        int positiveActionRes = cancelingPendingInvite
                ? R.string.cancel_invite_action
                : R.string.yes;

        new AlertDialog.Builder(this)
                .setTitle(titleRes)
                .setMessage(messageRes)
                .setPositiveButton(positiveActionRes, (dialog, which) -> {
                    if (challengeMode) {
                        submitChallengeScore(true);
                        return;
                    }

                    if (remoteMatchMode) {
                        if (sharedMatchState != null
                                && SharedMatchState.STATUS_WAITING.equals(sharedMatchState.status)) {
                            cancelPendingRemoteMatch();
                            finishMatch();
                            return;
                        }

                        surrenderRemoteMatch();
                        return;
                    }
                    recordMatchStatistics(false, true, false);
                    Toast.makeText(this, R.string.match_surrendered_message, Toast.LENGTH_SHORT).show();
                    finishMatch();
                })
                .setNegativeButton(R.string.no, null)
                .show();
    }

    private void submitChallengeScore(boolean finishAfterSubmit) {
        if (matchResultRecorded || isGuest) {
            if (finishAfterSubmit) {
                finishMatch();
            }
            return;
        }

        if (TextUtils.isEmpty(challengeId)) {
            Toast.makeText(this, "Challenge was not found.", Toast.LENGTH_SHORT).show();
            finishMatch();
            return;
        }

        matchResultRecorded = true;
        challengeRepository.submitChallengeScore(challengeId, playerOneScore, new FirebaseCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                loadUserStatus();
                Toast.makeText(GameHostActivity.this, R.string.challenge_score_submitted_message, Toast.LENGTH_SHORT).show();
                if (finishAfterSubmit) {
                    finishMatch();
                }
            }

            @Override
            public void onError(String error) {
                matchResultRecorded = false;
                Toast.makeText(GameHostActivity.this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void recordMatchStatistics(boolean won, boolean lost, boolean awardStars) {
        if (matchResultRecorded || isGuest) {
            return;
        }

        matchResultRecorded = true;
        firestoreRepository.updateMatchStatistics(won, lost);
        if (won) {
            completeDailyMission(FirestoreRepository.MISSION_WIN_MATCH);
        }
        if (friendlyMatch) {
            completeDailyMission(FirestoreRepository.MISSION_PLAY_FRIENDLY);
        }

        if (!awardStars) {
            return;
        }

        int localScore = getLocalPlayerScore();
        firestoreRepository.updateCurrentUserAfterRegularMatch(localScore, won, lost, new FirebaseCallback<FirestoreRepository.MatchRewardResult>() {
            @Override
            public void onSuccess(FirestoreRepository.MatchRewardResult rewardResult) {
                loadUserStatus();
                if (rewardResult.leagueChanged) {
                    showLeagueChangeDialog(rewardResult.previousLeague, rewardResult.currentLeague);
                }
            }

            @Override
            public void onError(String error) {
                Toast.makeText(GameHostActivity.this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void openTournamentFinalMatch() {
        if (sharedMatchState == null || TextUtils.isEmpty(sharedMatchState.finalMatchId)) {
            Toast.makeText(this, "Waiting for the other semifinal to finish.", Toast.LENGTH_SHORT).show();
            return;
        }
        openTournamentFinalMatch(sharedMatchState.finalMatchId, getTournamentFinalPlayerNumber());
    }

    private int getTournamentFinalPlayerNumber() {
        if (sharedMatchState == null) {
            return 1;
        }
        if ("SEMI".equals(sharedMatchState.tournamentStage)) {
            return sharedMatchState.tournamentSemiNumber == 2 ? 2 : 1;
        }
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            return 1;
        }
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        return currentUserId.equals(sharedMatchState.playerTwoId) ? 2 : 1;
    }

    private void openTournamentFinalMatch(String finalMatchId, int finalPlayerNumber) {
        Intent intent = new Intent(GameHostActivity.this, GameHostActivity.class);
        intent.putExtra("IS_GUEST", false);
        intent.putExtra(EXTRA_REMOTE_MATCH, true);
        intent.putExtra(EXTRA_FRIENDLY_MATCH, false);
        intent.putExtra(EXTRA_REMOTE_MATCH_ID, finalMatchId);
        intent.putExtra(EXTRA_LOCAL_PLAYER_NUMBER, finalPlayerNumber);
        intent.putExtra(EXTRA_START_ROUND, GameRound.MOJ_BROJ);
        startActivity(intent);
        finish();
    }
    private boolean isCurrentUserMatchWinner(SharedMatchRepository.MatchFinalizationResult result) {
        return result != null
                && FirebaseAuth.getInstance().getCurrentUser() != null
                && !TextUtils.isEmpty(result.winnerId)
                && result.winnerId.equals(FirebaseAuth.getInstance().getCurrentUser().getUid());
    }
    private void completeMatchMissions(SharedMatchRepository.MatchFinalizationResult result) {
        if (isGuest || sharedMatchState == null || result == null) {
            return;
        }
        if (result.friendlyMatch || isFriendlyMatch()) {
            return;
        }

        String currentUserId = FirebaseAuth.getInstance().getCurrentUser() == null
                ? ""
                : FirebaseAuth.getInstance().getCurrentUser().getUid();
        boolean localWon = !TextUtils.isEmpty(result.winnerId) && result.winnerId.equals(currentUserId);
        if (!localWon) {
            return;
        }
        if (SharedMatchState.MATCH_TYPE_TOURNAMENT.equals(sharedMatchState.matchType)) {
            completeDailyMission(FirestoreRepository.MISSION_WIN_TOURNAMENT);
        } else {
            completeDailyMission(FirestoreRepository.MISSION_WIN_MATCH);
        }
    }

    private void completeFriendlyMatchMissions() {
        if (isLocalWinner()) {
            completeDailyMission(
                    FirestoreRepository.MISSION_WIN_MATCH,
                    () -> completeDailyMission(FirestoreRepository.MISSION_PLAY_FRIENDLY)
            );
            return;
        }
        completeDailyMission(FirestoreRepository.MISSION_PLAY_FRIENDLY);
    }

    private boolean isLocalWinner() {
        return (localPlayerNumber == 1 && playerOneScore > playerTwoScore)
                || (localPlayerNumber == 2 && playerTwoScore > playerOneScore)
                || hasOpponentForfeited();
    }

    private void completeDailyMission(String mission) {
        completeDailyMission(mission, null);
    }

    private void completeDailyMission(String mission, Runnable afterSuccess) {
        firestoreRepository.completeDailyMissionWithResult(mission, new FirebaseCallback<FirestoreRepository.DailyMissionRewardResult>() {
            @Override
            public void onSuccess(FirestoreRepository.DailyMissionRewardResult result) {
                loadUserStatus();
                if (result != null && result.leagueChanged) {
                    showLeagueChangeDialog(result.previousLeague, result.currentLeague);
                }
                if (afterSuccess != null) {
                    afterSuccess.run();
                }
            }

            @Override
            public void onError(String error) {
                if (afterSuccess != null) {
                    afterSuccess.run();
                }
                // Daily missions should not interrupt match flow.
            }
        });
    }
    private void showLeagueChangeDialog(int previousLeague, int currentLeague) {
        boolean promoted = currentLeague > previousLeague;
        String message = promoted
                ? getString(R.string.league_promoted_message, LeagueUtils.getLeagueName(currentLeague))
                : getString(R.string.league_demoted_message, LeagueUtils.getLeagueName(currentLeague));

        new AlertDialog.Builder(this)
                .setTitle(promoted ? R.string.league_promoted_title : R.string.league_demoted_title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private void saveLeagueChangeAlert(int previousLeague, int currentLeague) {
        boolean promoted = currentLeague > previousLeague;
        String title = promoted ? "League promotion" : "League demotion";
        String message = promoted
                ? "You advanced to " + LeagueUtils.getLeagueName(currentLeague) + " League."
                : "You dropped to " + LeagueUtils.getLeagueName(currentLeague) + " League.";
        firestoreRepository.saveNotification(title, message, "REWARD");
    }

    private void markCurrentUserActive() {
        if (isGuest || firestoreRepository == null) {
            return;
        }

        firestoreRepository.markCurrentUserActive(new FirebaseCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                // Keeps players active while they are in a match.
            }

            @Override
            public void onError(String error) {
                // Activity tracking should not interrupt gameplay.
            }
        });
    }

    private int getLocalPlayerScore() {
        if (remoteMatchMode && localPlayerNumber == 2) {
            return playerTwoScore;
        }

        return playerOneScore;
    }

    public boolean isRemoteMatchMode() {
        return remoteMatchMode;
    }

    public boolean isChallengeMode() {
        return challengeMode;
    }

    @Override
    public boolean shouldPersistStatistics() {
        return !isGuest && !isFriendlyMatch() && !challengeMode;
    }

    @Override
    public boolean isFriendlyMatch() {
        return friendlyMatch
                || (sharedMatchState != null
                && SharedMatchState.MATCH_TYPE_FRIENDLY.equals(sharedMatchState.matchType));
    }

    public boolean hasForfeitedPlayer() {
        return sharedMatchState != null && sharedMatchState.forfeitedPlayer != 0;
    }

    public int getForfeitedPlayerNumber() {
        return sharedMatchState == null ? 0 : sharedMatchState.forfeitedPlayer;
    }

    public boolean hasOpponentForfeited() {
        return hasForfeitedPlayer() && sharedMatchState.forfeitedPlayer != localPlayerNumber;
    }

    public int getRemainingRemotePlayerNumber() {
        if (!hasForfeitedPlayer()) {
            return 0;
        }

        return sharedMatchState.forfeitedPlayer == 1 ? 2 : 1;
    }

    public boolean isRemoteProgressCoordinator() {
        if (!remoteMatchMode) {
            return false;
        }

        return hasForfeitedPlayer()
                ? localPlayerNumber == getRemainingRemotePlayerNumber()
                : localPlayerNumber == 1;
    }

    public boolean isRemotePlayerAvailable(int playerNumber) {
        return !hasForfeitedPlayer() || sharedMatchState.forfeitedPlayer != playerNumber;
    }

    public int getLocalPlayerNumber() {
        return localPlayerNumber;
    }

    public SharedMatchState getSharedMatchState() {
        return sharedMatchState;
    }

    public SharedKorakPoKorakRound getSharedKorakRound(int turnIndex) {
        if (sharedMatchState == null
                || sharedMatchState.korakRounds == null
                || turnIndex < 0
                || turnIndex >= sharedMatchState.korakRounds.size()) {
            return null;
        }

        return sharedMatchState.korakRounds.get(turnIndex);
    }

    public KoZnaZnaQuestion getSharedQuizQuestion(int questionIndex) {
        if (sharedMatchState == null
                || sharedMatchState.quizQuestions == null
                || questionIndex < 0
                || questionIndex >= sharedMatchState.quizQuestions.size()) {
            return null;
        }

        return sharedMatchState.quizQuestions.get(questionIndex);
    }

    public SharedSpojniceRound getSharedSpojniceRound(int roundIndex) {
        if (sharedMatchState == null
                || sharedMatchState.spojniceRounds == null
                || roundIndex < 0
                || roundIndex >= sharedMatchState.spojniceRounds.size()) {
            return null;
        }

        return sharedMatchState.spojniceRounds.get(roundIndex);
    }

    public SharedSkockoRound getSharedSkockoRound(int roundIndex) {
        if (sharedMatchState == null
                || sharedMatchState.skockoRounds == null
                || roundIndex < 0
                || roundIndex >= sharedMatchState.skockoRounds.size()) {
            return null;
        }

        return sharedMatchState.skockoRounds.get(roundIndex);
    }

    public SharedAsocijacijeRound getSharedAsocijacijeRound(int roundIndex) {
        if (sharedMatchState == null
                || sharedMatchState.asocijacijeRounds == null
                || roundIndex < 0
                || roundIndex >= sharedMatchState.asocijacijeRounds.size()) {
            return null;
        }

        return sharedMatchState.asocijacijeRounds.get(roundIndex);
    }

    public SharedMojBrojRound getSharedMojBrojRound(int turnIndex) {
        if (sharedMatchState == null
                || sharedMatchState.myNumberRounds == null
                || turnIndex < 0
                || turnIndex >= sharedMatchState.myNumberRounds.size()) {
            return null;
        }

        return sharedMatchState.myNumberRounds.get(turnIndex);
    }

    public String getRemoteMatchId() {
        return remoteMatchId;
    }

    private boolean isWaitingForFriendlyInvite() {
        return remoteMatchMode
                && sharedMatchState != null
                && localPlayerNumber == 1
                && SharedMatchState.STATUS_WAITING.equals(sharedMatchState.status)
                && SharedMatchState.MATCH_TYPE_FRIENDLY.equals(sharedMatchState.matchType);
    }

    public void updateSharedMatch(Map<String, Object> updates) {
        if (!remoteMatchMode || remoteMatchId == null || updates == null || updates.isEmpty()) {
            return;
        }

        updates.put("updatedAt", System.currentTimeMillis());
        sharedMatchRepository.updateMatchState(remoteMatchId, updates);
    }

    private void startListeningToRemoteMatch() {
        if (remoteMatchId == null || remoteMatchId.trim().isEmpty()) {
            Toast.makeText(this, R.string.shared_match_missing_id, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        sharedMatchListener = sharedMatchRepository.listenToMatch(remoteMatchId, new FirebaseCallback<SharedMatchState>() {
            @Override
            public void onSuccess(SharedMatchState state) {
                sharedMatchState = state;
                applyRemoteMatchState(state);
            }

            @Override
            public void onError(String error) {
                Toast.makeText(GameHostActivity.this, error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void applyRemoteMatchState(SharedMatchState state) {
        if (state == null) {
            return;
        }

        if (SharedMatchState.STATUS_DECLINED.equals(state.status)) {
            Toast.makeText(this, state.phaseMessage, Toast.LENGTH_SHORT).show();
            finishMatch();
            return;
        }

        if (SharedMatchState.STATUS_CANCELED.equals(state.status)) {
            Toast.makeText(this, state.phaseMessage, Toast.LENGTH_SHORT).show();
            finishMatch();
            return;
        }

        syncLocalTournamentWaitingPlayerNumber(state);

        if (state.playerOneName != null && !state.playerOneName.trim().isEmpty()) {
            tvPlayerOneName.setText(state.playerOneName);
        }

        if (state.playerTwoName != null && !state.playerTwoName.trim().isEmpty()) {
            tvPlayerTwoName.setText(state.playerTwoName);
        }

        applyForfeitUi(state);
        resolveOpponentRegion();

        setScores(state.playerOneScore, state.playerTwoScore);
        setTimerValue(getRemainingSeconds(state));
        setPhaseText(buildRemotePhaseText(state));
        tvMatchMeta.setText(getString(R.string.shared_match_you_are_player_format, localPlayerNumber));
        maybeRefreshStatusAfterRemoteMatchStart(state);
        updatePendingInviteActionUi(state);

        if (isTournamentWaiting(state)) {
            showTournamentMatchingPanel(state);
            return;
        }

        if (shouldAnimateTournamentStart(state)) {
            showTournamentStartAnimation(state);
            return;
        }

        hideTournamentMatchingPanel();

        if (state.updatedAt == lastAppliedRemoteUpdatedAt && currentRound != null) {
            return;
        }

        lastAppliedRemoteUpdatedAt = state.updatedAt;

        GameRound nextRound = parseRemoteRound(state.currentRound);
        if (nextRound == null) {
            nextRound = GameRound.KO_ZNA_ZNA;
        }

        goToRound(nextRound, null);
        updatePendingInviteActionUi(state);
    }

    private void updatePendingInviteActionUi(SharedMatchState state) {
        if (state != null
                && SharedMatchState.STATUS_WAITING.equals(state.status)
                && SharedMatchState.MATCH_TYPE_FRIENDLY.equals(state.matchType)
                && localPlayerNumber == 1) {
            btnQuitMatch.setVisibility(View.VISIBLE);
            btnQuitMatch.setText(R.string.cancel_invite_action);
            return;
        }

        btnQuitMatch.setText(R.string.quit_game);
    }

    private void setupTournamentMatchingSlots() {
        View[] slots = new View[]{
                findViewById(R.id.tournamentSlotOne),
                findViewById(R.id.tournamentSlotTwo),
                findViewById(R.id.tournamentSlotThree),
                findViewById(R.id.tournamentSlotFour)
        };
        tournamentSlotAvatars = new ImageView[slots.length];
        tournamentSlotNames = new TextView[slots.length];
        tournamentSlotLeagues = new TextView[slots.length];
        for (int index = 0; index < slots.length; index++) {
            tournamentSlotAvatars[index] = slots[index].findViewById(R.id.tournamentSlotAvatar);
            tournamentSlotNames[index] = slots[index].findViewById(R.id.tournamentSlotName);
            tournamentSlotLeagues[index] = slots[index].findViewById(R.id.tournamentSlotLeague);
        }
    }

    private void syncLocalTournamentWaitingPlayerNumber(SharedMatchState state) {
        if (!isTournamentWaiting(state) || com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() == null) {
            return;
        }
        String currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (currentUserId.equals(state.playerOneId)) {
            localPlayerNumber = 1;
        } else if (currentUserId.equals(state.playerTwoId)) {
            localPlayerNumber = 2;
        }
    }
    private boolean isTournamentWaiting(SharedMatchState state) {
        return state != null
                && SharedMatchState.MATCH_TYPE_TOURNAMENT.equals(state.matchType)
                && SharedMatchState.STATUS_WAITING.equals(state.status);
    }

    private boolean shouldAnimateTournamentStart(SharedMatchState state) {
        return state != null
                && SharedMatchState.MATCH_TYPE_TOURNAMENT.equals(state.matchType)
                && SharedMatchState.STATUS_ACTIVE.equals(state.status)
                && tournamentMatchingPanel != null
                && tournamentMatchingPanel.getVisibility() == View.VISIBLE
                && !tournamentStartAnimationRunning;
    }

    private void showTournamentMatchingPanel(SharedMatchState state) {
        matchPlayersHeader.setVisibility(View.GONE);
        matchPhaseHeader.setVisibility(View.GONE);
        matchContentArea.setVisibility(View.GONE);
        tournamentMatchingPanel.setVisibility(View.VISIBLE);
        tournamentMatchingPanel.setAlpha(1f);
        tournamentMatchingTitle.setText("Tournament matching");
        tournamentMatchingSubtitle.setText("Players will be paired into two semifinals.");
        renderTournamentSlots(state, null);
        if (!TextUtils.isEmpty(state.siblingMatchId)) {
            sharedMatchRepository.getMatchState(state.siblingMatchId, new FirebaseCallback<SharedMatchState>() {
                @Override
                public void onSuccess(SharedMatchState siblingState) {
                    if (sharedMatchState != null && isTournamentWaiting(sharedMatchState)) {
                        renderTournamentSlots(sharedMatchState, siblingState);
                    }
                }

                @Override
                public void onError(String error) {
                    // The sibling semifinal may not exist yet while the tournament is filling.
                }
            });
        }
    }

    private void renderTournamentSlots(SharedMatchState current, SharedMatchState sibling) {
        List<TournamentPlayerSlot> players = new ArrayList<>();
        SharedMatchState firstSemi = current;
        SharedMatchState secondSemi = sibling;
        if (current != null && current.tournamentSemiNumber == 2) {
            firstSemi = sibling;
            secondSemi = current;
        }
        appendTournamentPlayers(players, firstSemi);
        appendTournamentPlayers(players, secondSemi);
        while (players.size() < 4) {
            players.add(new TournamentPlayerSlot("", "Waiting"));
        }
        int filled = 0;
        for (TournamentPlayerSlot player : players) {
            if (!TextUtils.isEmpty(player.userId)) {
                filled++;
            }
        }
        tournamentMatchingStatus.setText("Waiting for players: " + filled + "/4");
        for (int index = 0; index < 4; index++) {
            bindTournamentSlot(index, players.get(index));
        }
    }

    private void appendTournamentPlayers(List<TournamentPlayerSlot> players, SharedMatchState state) {
        if (state == null) {
            return;
        }
        players.add(new TournamentPlayerSlot(state.playerOneId, state.playerOneName));
        if (!TextUtils.isEmpty(state.playerTwoId)) {
            players.add(new TournamentPlayerSlot(state.playerTwoId, state.playerTwoName));
        }
    }

    private void bindTournamentSlot(int index, TournamentPlayerSlot player) {
        if (index < 0 || index >= tournamentSlotNames.length) {
            return;
        }

        if (player == null || TextUtils.isEmpty(player.userId)) {
            tournamentSlotNames[index].setText("Waiting");
            tournamentSlotLeagues[index].setText("Empty slot");
            tournamentSlotAvatars[index].setBackgroundResource(R.drawable.bg_match_avatar_frame);
            tournamentSlotAvatars[index].setPadding(dpToPx(7), dpToPx(7), dpToPx(7), dpToPx(7));
            tournamentSlotAvatars[index].setScaleType(ImageView.ScaleType.CENTER);
            tournamentSlotAvatars[index].setImageResource(R.drawable.baseline_person_24);
            tournamentSlotAvatars[index].setColorFilter(ContextCompat.getColor(this, R.color.slagalica_dark_blue));
            return;
        }

        tournamentSlotNames[index].setText(TextUtils.isEmpty(player.name) ? "Player" : player.name);
        tournamentSlotLeagues[index].setText("Loading league...");
        tournamentSlotAvatars[index].clearColorFilter();

        firestoreRepository.getUserById(player.userId, new FirebaseCallback<User>() {
            @Override
            public void onSuccess(User user) {
                int avatarResId = profileService.getAvatarResource(
                        TextUtils.isEmpty(user.avatar) ? "avatar_1" : user.avatar
                );

                tournamentSlotAvatars[index].clearColorFilter();
                tournamentSlotAvatars[index].setBackgroundResource(R.drawable.bg_avatar_circle);
                tournamentSlotAvatars[index].setPadding(dpToPx(2), dpToPx(2), dpToPx(2), dpToPx(2));
                tournamentSlotAvatars[index].setScaleType(ImageView.ScaleType.CENTER_CROP);
                tournamentSlotAvatars[index].setImageResource(avatarResId);

                tournamentSlotLeagues[index].setText(
                        LeagueUtils.getLeagueIcon(user.league) + " " + LeagueUtils.getLeagueName(user.league)
                );

                if (!TextUtils.isEmpty(user.username)) {
                    tournamentSlotNames[index].setText(user.username);
                }
            }

            @Override
            public void onError(String error) {
                tournamentSlotLeagues[index].setText("No League");
                tournamentSlotAvatars[index].setBackgroundResource(R.drawable.bg_match_avatar_frame);
                tournamentSlotAvatars[index].setPadding(dpToPx(7), dpToPx(7), dpToPx(7), dpToPx(7));
                tournamentSlotAvatars[index].setScaleType(ImageView.ScaleType.CENTER);
                tournamentSlotAvatars[index].setImageResource(R.drawable.baseline_person_24);
                tournamentSlotAvatars[index].setColorFilter(ContextCompat.getColor(
                        GameHostActivity.this,
                        R.color.slagalica_dark_blue
                ));
            }
        });
    }
    private void showTournamentStartAnimation(SharedMatchState state) {
        tournamentStartAnimationRunning = true;
        tournamentMatchingTitle.setText("Semifinals starting");
        tournamentMatchingStatus.setText("4/4 players ready");
        tournamentMatchingSubtitle.setText("Good luck in your semifinal match.");
        AnimatorSet set = new AnimatorSet();
        set.playTogether(
                ObjectAnimator.ofFloat(tournamentMatchingPanel, View.SCALE_X, 0.96f, 1.04f, 1f),
                ObjectAnimator.ofFloat(tournamentMatchingPanel, View.SCALE_Y, 0.96f, 1.04f, 1f),
                ObjectAnimator.ofFloat(tournamentMatchingTitle, View.ALPHA, 0.35f, 1f)
        );
        set.setDuration(900);
        set.start();
        tournamentMatchingPanel.postDelayed(() -> {
            tournamentStartAnimationRunning = false;
            hideTournamentMatchingPanel();
            applyRemoteMatchState(state);
        }, 1100);
    }

    private void hideTournamentMatchingPanel() {
        if (tournamentMatchingPanel != null) {
            tournamentMatchingPanel.setVisibility(View.GONE);
        }
        if (matchPlayersHeader != null) {
            matchPlayersHeader.setVisibility(View.VISIBLE);
        }
        if (matchPhaseHeader != null) {
            matchPhaseHeader.setVisibility(View.VISIBLE);
        }
        if (matchContentArea != null) {
            matchContentArea.setVisibility(View.VISIBLE);
        }
    }


    private int dpToPx(int dpValue) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dpValue * density);
    }
    private static final class TournamentPlayerSlot {
        final String userId;
        final String name;

        TournamentPlayerSlot(String userId, String name) {
            this.userId = userId == null ? "" : userId;
            this.name = name == null ? "" : name;
        }
    }
    private void applyForfeitUi(SharedMatchState state) {
        if (state == null || state.forfeitedPlayer == 0) {
            lastObservedForfeitedPlayer = 0;
            return;
        }

        if (state.forfeitedPlayer == 1) {
            tvPlayerOneName.setText(R.string.shared_match_player_left_label);
        } else if (state.forfeitedPlayer == 2) {
            tvPlayerTwoName.setText(R.string.shared_match_player_left_label);
        }

        if (state.forfeitedPlayer != localPlayerNumber
                && state.forfeitedPlayer != lastObservedForfeitedPlayer) {
            Toast.makeText(this, R.string.shared_match_opponent_left_toast, Toast.LENGTH_SHORT).show();
        }

        lastObservedForfeitedPlayer = state.forfeitedPlayer;
    }

    private String buildRemotePhaseText(SharedMatchState state) {
        if (SharedMatchState.STATUS_WAITING.equals(state.status)) {
            return getString(R.string.shared_match_waiting_phase);
        }

        if (state.phaseMessage != null && !state.phaseMessage.trim().isEmpty()) {
            return state.phaseMessage;
        }

        return getString(R.string.phase_waiting_for_round);
    }

    private int getRemainingSeconds(SharedMatchState state) {
        if (state.phaseDurationSeconds <= 0 || state.phaseStartedAt <= 0) {
            return 0;
        }

        long elapsedMs = System.currentTimeMillis() - state.phaseStartedAt;
        int remaining = state.phaseDurationSeconds - (int) Math.floor(elapsedMs / 1000d);
        return Math.max(0, remaining);
    }

    private GameRound parseRemoteRound(String roundName) {
        if (roundName == null || roundName.trim().isEmpty()) {
            return null;
        }

        try {
            return GameRound.valueOf(roundName);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private void goToNextRemoteRound() {
        if (sharedMatchState == null) {
            return;
        }

        Map<String, Object> updates = new HashMap<>();

        if (currentRound == GameRound.KO_ZNA_ZNA) {
            updates.put("currentRound", GameRound.SPOJNICE.name());
            updates.put("phase", SharedMatchState.PHASE_SPOJNICE_PLAY);
            updates.put("currentTurnIndex", 0);
            updates.put("activePlayer", 1);
            updates.put("phaseStartedAt", System.currentTimeMillis());
            updates.put("phaseDurationSeconds", 30);
            updates.put("phaseMessage", getString(R.string.shared_match_spojnice_round_one_phase));
            updateSharedMatch(updates);
            return;
        }

        if (currentRound == GameRound.SPOJNICE) {
            updates.put("currentRound", GameRound.SKOCKO.name());
            updates.put("phase", SharedMatchState.PHASE_SKOCKO_PLAY);
            updates.put("currentTurnIndex", 0);
            updates.put("activePlayer", 1);
            updates.put("phaseStartedAt", System.currentTimeMillis());
            updates.put("phaseDurationSeconds", 30);
            updates.put("phaseMessage", getString(R.string.shared_match_skocko_round_one_phase));
            updateSharedMatch(updates);
            return;
        }

        if (currentRound == GameRound.SKOCKO) {
            updates.put("currentRound", GameRound.ASOCIJACIJE.name());
            updates.put("phase", SharedMatchState.PHASE_ASOC_PLAY);
            updates.put("currentTurnIndex", 0);
            updates.put("activePlayer", 1);
            updates.put("phaseStartedAt", System.currentTimeMillis());
            updates.put("phaseDurationSeconds", 120);
            updates.put("phaseMessage", getString(R.string.shared_match_asoc_round_one_phase));
            updateSharedMatch(updates);
            return;
        }

        if (currentRound == GameRound.ASOCIJACIJE) {
            updates.put("currentRound", GameRound.KORAK_PO_KORAK.name());
            updates.put("phase", SharedMatchState.PHASE_KPP_STARTER);
            updates.put("currentTurnIndex", 0);
            updates.put("activePlayer", 1);
            updates.put("phaseStartedAt", System.currentTimeMillis());
            updates.put("phaseDurationSeconds", 70);
            updates.put("phaseMessage", getString(R.string.shared_match_kpp_round_one_phase));
            updates.put("revealedAnswer", "");
            updateSharedMatch(updates);
            return;
        }

        if (currentRound == GameRound.KORAK_PO_KORAK) {
            updates.put("currentRound", GameRound.MOJ_BROJ.name());
            updates.put("phase", SharedMatchState.PHASE_MB_TARGET);
            updates.put("currentTurnIndex", 0);
            updates.put("activePlayer", 1);
            updates.put("phaseStartedAt", System.currentTimeMillis());
            updates.put("phaseDurationSeconds", 5);
            updates.put("phaseMessage", getString(R.string.shared_match_mb_round_one_phase));
            updates.put("playerOneExpression", "");
            updates.put("playerTwoExpression", "");
            updates.put("revealedAnswer", "");
            updateSharedMatch(updates);
            return;
        }

        if (currentRound == GameRound.MOJ_BROJ) {
            updates.put("currentRound", GameRound.RESULT.name());
            updates.put("phase", SharedMatchState.PHASE_RESULT);
            updates.put("status", SharedMatchState.STATUS_FINISHED);
            updates.put("activePlayer", 0);
            updates.put("phaseStartedAt", System.currentTimeMillis());
            updates.put("phaseDurationSeconds", 0);
            updates.put("phaseMessage", getString(R.string.match_result_phase));
            updateSharedMatch(updates);
            return;
        }

        finishMatch();
    }

    private void restartRemoteMatch() {
        if (sharedMatchState == null) {
            return;
        }

        if (sharedMatchState.rematchRequestedBy == 0) {
            requestRemoteRematch();
            return;
        }

        if (sharedMatchState.rematchRequestedBy == localPlayerNumber) {
            Toast.makeText(this, R.string.rematch_waiting_for_response, Toast.LENGTH_SHORT).show();
            return;
        }

        matchResultRecorded = false;
        sharedMatchRepository.resetStudentOneMatch(remoteMatchId, sharedMatchState);
    }

    private void surrenderRemoteMatch() {
        if (sharedMatchState == null) {
            finishMatch();
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        int surrenderedPlayer = localPlayerNumber;
        int continuingPlayer = surrenderedPlayer == 1 ? 2 : 1;

        updates.put("forfeitedPlayer", surrenderedPlayer);
        updates.put(
                "phaseMessage",
                getString(
                        R.string.shared_match_opponent_left_continue_phase_format,
                        surrenderedPlayer,
                        continuingPlayer
                )
        );
        updateSharedMatch(updates);

        Toast.makeText(this, R.string.match_surrendered_message, Toast.LENGTH_SHORT).show();
        finishMatch();
    }

    private void cancelPendingRemoteMatch() {
        if (remoteMatchId == null || sharedMatchState == null) {
            return;
        }

        if (isFriendlyMatch()) {
            sharedMatchRepository.cancelPendingFriendlyInvite(remoteMatchId);
        } else if (sharedMatchState != null
                && SharedMatchState.MATCH_TYPE_TOURNAMENT.equals(sharedMatchState.matchType)) {
            sharedMatchRepository.leaveTournamentWaitingMatch(remoteMatchId);
        } else {
            sharedMatchRepository.cancelWaitingCompetitiveMatch(remoteMatchId);
        }
    }

    private void requestRemoteRematch() {
        if (remoteMatchId == null || sharedMatchState == null) {
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("rematchRequestedBy", localPlayerNumber);
        updates.put("rematchDeclinedBy", 0);
        updates.put("phaseMessage", getString(R.string.rematch_requested_phase_format, localPlayerNumber));
        updateSharedMatch(updates);
    }

    private void cancelPendingRemoteRematchRequest() {
        if (remoteMatchId == null || sharedMatchState == null || sharedMatchState.rematchRequestedBy == 0) {
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("rematchRequestedBy", 0);
        updates.put("rematchDeclinedBy", 0);
        updates.put("phaseMessage", getString(R.string.match_result_phase));
        updateSharedMatch(updates);
    }

    private void declinePendingRemoteRematchRequest() {
        if (remoteMatchId == null || sharedMatchState == null || sharedMatchState.rematchRequestedBy == 0) {
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("rematchRequestedBy", 0);
        updates.put("rematchDeclinedBy", localPlayerNumber);
        updates.put("phaseMessage", getString(R.string.rematch_declined_phase_format, localPlayerNumber));
        updateSharedMatch(updates);
    }

    private void maybeRefreshStatusAfterRemoteMatchStart(SharedMatchState state) {
        if (state == null || state.matchStartedAt <= 0 || state.matchStartedAt == lastObservedRemoteMatchStartedAt) {
            return;
        }

        lastObservedRemoteMatchStartedAt = state.matchStartedAt;

        if (!isGuest) {
            loadUserStatus();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!isGuest) {
            firestoreRepository.markCurrentUserLoggedIn();
            firestoreRepository.markCurrentUserInApp(true);
            if (remoteMatchMode && remoteMatchId != null && !remoteMatchId.trim().isEmpty()) {
                firestoreRepository.setCurrentUserMatch(remoteMatchId);
            }
        }
        markCurrentUserActive();
        if (remoteMatchMode && sharedMatchState != null) {
            applyRemoteMatchState(sharedMatchState);
        }
    }

    @Override
    protected void onPause() {
        if (!isGuest) {
            firestoreRepository.markCurrentUserInApp(false);
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (unreadChatListener != null) {
            unreadChatListener.remove();
            unreadChatListener = null;
        }
        if (sharedMatchListener != null) {
            sharedMatchListener.remove();
            sharedMatchListener = null;
        }
        super.onDestroy();
    }
}
