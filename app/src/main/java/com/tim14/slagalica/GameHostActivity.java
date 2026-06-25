package com.tim14.slagalica;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
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
import com.tim14.slagalica.repository.FirebaseCallback;
import com.tim14.slagalica.repository.FirestoreRepository;
import com.tim14.slagalica.repository.SharedMatchRepository;

import java.util.HashMap;
import java.util.Map;

public class GameHostActivity extends AppCompatActivity implements GameNavigator {

    public static final String EXTRA_START_ROUND = "start_round";
    public static final String EXTRA_REMOTE_MATCH = "remote_match";
    public static final String EXTRA_REMOTE_MATCH_ID = "remote_match_id";
    public static final String EXTRA_LOCAL_PLAYER_NUMBER = "local_player_number";
    public static final String EXTRA_FRIENDLY_MATCH = "friendly_match";

    private TextView tvRoundTimer;
    private TextView tvPhaseText;
    private TextView tvMatchMeta;
    private TextView tvPlayerOneScore;
    private TextView tvPlayerTwoScore;
    private TextView tvPlayerOneName;
    private TextView tvPlayerTwoName;
    private TextView tvChatBubble;
    private TextView tvStatusTokens;
    private TextView tvStatusStars;
    private TextView tvStatusLeague;
    private Button btnQuitMatch;

    private int playerOneScore;
    private int playerTwoScore;
    private GameRound currentRound;
    private boolean isGuest;
    private boolean friendlyMatch;
    private boolean matchResultRecorded;
    private FirestoreRepository firestoreRepository;
    private SharedMatchRepository sharedMatchRepository;
    private ListenerRegistration sharedMatchListener;
    private SharedMatchState sharedMatchState;
    private boolean remoteMatchMode;
    private String remoteMatchId;
    private int localPlayerNumber;
    private long lastAppliedRemoteUpdatedAt;
    private long lastObservedRemoteMatchStartedAt;
    private int lastObservedForfeitedPlayer;

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
        tvChatBubble = findViewById(R.id.tvChatBubble);
        tvStatusTokens = findViewById(R.id.tvStatusTokens);
        tvStatusStars = findViewById(R.id.tvStatusStars);
        tvStatusLeague = findViewById(R.id.tvStatusLeague);
        btnQuitMatch = findViewById(R.id.btnQuitMatch);
        firestoreRepository = new FirestoreRepository();
        sharedMatchRepository = new SharedMatchRepository();
        isGuest = getIntent().getBooleanExtra("IS_GUEST", false);
        friendlyMatch = getIntent().getBooleanExtra(EXTRA_FRIENDLY_MATCH, false);
        remoteMatchMode = getIntent().getBooleanExtra(EXTRA_REMOTE_MATCH, false);
        remoteMatchId = getIntent().getStringExtra(EXTRA_REMOTE_MATCH_ID);
        localPlayerNumber = getIntent().getIntExtra(EXTRA_LOCAL_PLAYER_NUMBER, 1);

        btnQuitMatch.setOnClickListener(v -> surrenderMatch());
        tvChatBubble.setOnClickListener(v ->
                Toast.makeText(this, R.string.chat_placeholder_message, Toast.LENGTH_SHORT).show()
        );

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                surrenderMatch();
            }
        });

        setScores(0, 0);
        setTimerValue(0);
        setPhaseText(getString(R.string.phase_waiting_for_round));
        tvMatchMeta.setText("");

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
                startRound = GameRound.MOJ_BROJ;
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
                .commit();
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

        if (remoteMatchMode) {
            matchResultRecorded = true;

            if (isFriendlyMatch()) {
                firestoreRepository.clearCurrentUserMatch();
                loadUserStatus();
                return;
            }

            sharedMatchRepository.finalizeMatchIfNeeded(remoteMatchId, new FirebaseCallback<SharedMatchRepository.MatchFinalizationResult>() {
                @Override
                public void onSuccess(SharedMatchRepository.MatchFinalizationResult result) {
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
                Toast.makeText(GameHostActivity.this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void surrenderMatch() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.quit_confirmation_title)
                .setMessage(R.string.quit_confirmation_message)
                .setPositiveButton(R.string.yes, (dialog, which) -> {
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

    private void recordMatchStatistics(boolean won, boolean lost, boolean awardStars) {
        if (matchResultRecorded || isGuest) {
            return;
        }

        matchResultRecorded = true;
        firestoreRepository.updateMatchStatistics(won, lost);

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

    @Override
    public boolean shouldPersistStatistics() {
        return !isGuest && !isFriendlyMatch();
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

        if (state.playerOneName != null && !state.playerOneName.trim().isEmpty()) {
            tvPlayerOneName.setText(state.playerOneName);
        }

        if (state.playerTwoName != null && !state.playerTwoName.trim().isEmpty()) {
            tvPlayerTwoName.setText(state.playerTwoName);
        }

        applyForfeitUi(state);

        setScores(state.playerOneScore, state.playerTwoScore);
        setTimerValue(getRemainingSeconds(state));
        setPhaseText(buildRemotePhaseText(state));
        tvMatchMeta.setText(getString(R.string.shared_match_you_are_player_format, localPlayerNumber));
        maybeRefreshStatusAfterRemoteMatchStart(state);

        if (state.updatedAt == lastAppliedRemoteUpdatedAt && currentRound != null) {
            return;
        }

        lastAppliedRemoteUpdatedAt = state.updatedAt;

        GameRound nextRound = parseRemoteRound(state.currentRound);
        if (nextRound == null) {
            nextRound = GameRound.KO_ZNA_ZNA;
        }

        goToRound(nextRound, null);
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
        if (sharedMatchListener != null) {
            sharedMatchListener.remove();
            sharedMatchListener = null;
        }
        super.onDestroy();
    }
}
