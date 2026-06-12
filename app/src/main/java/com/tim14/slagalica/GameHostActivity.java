package com.tim14.slagalica;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

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
import com.tim14.slagalica.model.User;
import com.tim14.slagalica.repository.FirebaseCallback;
import com.tim14.slagalica.repository.FirestoreRepository;

public class GameHostActivity extends AppCompatActivity implements GameNavigator {

    public static final String EXTRA_START_ROUND = "start_round";

    private TextView tvRoundTimer;
    private TextView tvPhaseText;
    private TextView tvPlayerOneScore;
    private TextView tvPlayerTwoScore;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_host);

        tvRoundTimer = findViewById(R.id.tvRoundTimer);
        tvPhaseText = findViewById(R.id.tvPhaseText);
        tvPlayerOneScore = findViewById(R.id.tvPlayerOneScore);
        tvPlayerTwoScore = findViewById(R.id.tvPlayerTwoScore);
        tvChatBubble = findViewById(R.id.tvChatBubble);
        tvStatusTokens = findViewById(R.id.tvStatusTokens);
        tvStatusStars = findViewById(R.id.tvStatusStars);
        tvStatusLeague = findViewById(R.id.tvStatusLeague);
        btnQuitMatch = findViewById(R.id.btnQuitMatch);
        firestoreRepository = new FirestoreRepository();
        isGuest = getIntent().getBooleanExtra("IS_GUEST", false);

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

        if (!isGuest) {
            loadUserStatus();
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

        recordMatchStatistics(
                playerOneScore > playerTwoScore,
                playerOneScore < playerTwoScore
        );
    }

    @Override
    public void restartMatch() {
        matchResultRecorded = false;
        setScores(0, 0);
        goToRound(GameRound.KO_ZNA_ZNA, null);
    }

    @Override
    public void finishMatch() {
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
}
