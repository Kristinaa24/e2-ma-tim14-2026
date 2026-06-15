package com.tim14.slagalica;

import android.os.Bundle;
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
import com.tim14.slagalica.model.SharedKorakPoKorakRound;
import com.tim14.slagalica.model.SharedMatchState;
import com.tim14.slagalica.model.SharedMojBrojRound;
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
    private boolean matchResultRecorded;
    private FirestoreRepository firestoreRepository;
    private SharedMatchRepository sharedMatchRepository;
    private ListenerRegistration sharedMatchListener;
    private SharedMatchState sharedMatchState;
    private boolean remoteMatchMode;
    private String remoteMatchId;
    private int localPlayerNumber;
    private long lastAppliedRemoteUpdatedAt;

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
                startRound = GameRound.KO_ZNA_ZNA;
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
        currentRound = round;

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
            boolean won;
            boolean lost;

            if (localPlayerNumber == 1) {
                won = playerOneScore > playerTwoScore;
                lost = playerOneScore < playerTwoScore;
            } else {
                won = playerTwoScore > playerOneScore;
                lost = playerTwoScore < playerOneScore;
            }

            recordMatchStatistics(won, lost);
            return;
        }

        recordMatchStatistics(
                playerOneScore > playerTwoScore,
                playerOneScore < playerTwoScore
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
                        surrenderRemoteMatch();
                        return;
                    }
                    recordMatchStatistics(false, true);
                    Toast.makeText(this, R.string.match_surrendered_message, Toast.LENGTH_SHORT).show();
                    finishMatch();
                })
                .setNegativeButton(R.string.no, null)
                .show();
    }

    private void recordMatchStatistics(boolean won, boolean lost) {
        if (matchResultRecorded || isGuest) {
            return;
        }

        matchResultRecorded = true;
        firestoreRepository.updateMatchStatistics(won, lost);
    }

    public boolean isRemoteMatchMode() {
        return remoteMatchMode;
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

    public SharedMojBrojRound getSharedMojBrojRound(int turnIndex) {
        if (sharedMatchState == null
                || sharedMatchState.myNumberRounds == null
                || turnIndex < 0
                || turnIndex >= sharedMatchState.myNumberRounds.size()) {
            return null;
        }

        return sharedMatchState.myNumberRounds.get(turnIndex);
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

        if (state.playerOneName != null && !state.playerOneName.trim().isEmpty()) {
            tvPlayerOneName.setText(state.playerOneName);
        }

        if (state.playerTwoName != null && !state.playerTwoName.trim().isEmpty()) {
            tvPlayerTwoName.setText(state.playerTwoName);
        }

        setScores(state.playerOneScore, state.playerTwoScore);
        setTimerValue(getRemainingSeconds(state));
        setPhaseText(buildRemotePhaseText(state));
        tvMatchMeta.setText(getString(R.string.shared_match_meta_format, state.roomCode, localPlayerNumber));

        if (state.updatedAt == lastAppliedRemoteUpdatedAt && currentRound != null) {
            return;
        }

        lastAppliedRemoteUpdatedAt = state.updatedAt;

        GameRound nextRound = parseRemoteRound(state.currentRound);
        if (nextRound == null) {
            nextRound = GameRound.KORAK_PO_KORAK;
        }

        goToRound(nextRound, null);
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
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", SharedMatchState.STATUS_ACTIVE);
        updates.put("currentRound", GameRound.KORAK_PO_KORAK.name());
        updates.put("phase", SharedMatchState.PHASE_KPP_STARTER);
        updates.put("currentTurnIndex", 0);
        updates.put("activePlayer", 1);
        updates.put("phaseStartedAt", System.currentTimeMillis());
        updates.put("phaseDurationSeconds", 70);
        updates.put("phaseMessage", getString(R.string.shared_match_kpp_round_one_phase));
        updates.put("revealedAnswer", "");
        updates.put("playerOneExpression", "");
        updates.put("playerTwoExpression", "");
        updates.put("playerOneScore", 0);
        updates.put("playerTwoScore", 0);
        matchResultRecorded = false;
        updateSharedMatch(updates);
    }

    private void surrenderRemoteMatch() {
        if (sharedMatchState == null) {
            finishMatch();
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        int surrenderedPlayer = localPlayerNumber;
        int winnerPlayer = surrenderedPlayer == 1 ? 2 : 1;

        updates.put("currentRound", GameRound.RESULT.name());
        updates.put("phase", SharedMatchState.PHASE_RESULT);
        updates.put("status", SharedMatchState.STATUS_FINISHED);
        updates.put("activePlayer", 0);
        updates.put("phaseStartedAt", System.currentTimeMillis());
        updates.put("phaseDurationSeconds", 0);
        updates.put("phaseMessage", getString(R.string.shared_match_surrender_phase_format, winnerPlayer));
        updateSharedMatch(updates);

        recordMatchStatistics(localPlayerNumber != surrenderedPlayer, true);
        Toast.makeText(this, R.string.match_surrendered_message, Toast.LENGTH_SHORT).show();
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
