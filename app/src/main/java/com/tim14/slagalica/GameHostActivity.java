package com.tim14.slagalica;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.tim14.slagalica.fragments.KorakPoKorakFragment;
import com.tim14.slagalica.fragments.MojBrojFragment;
import com.tim14.slagalica.fragments.PlaceholderRoundFragment;
import com.tim14.slagalica.game.GameNavigator;
import com.tim14.slagalica.game.GameRound;

public class GameHostActivity extends AppCompatActivity implements GameNavigator {

    public static final String EXTRA_START_ROUND = "start_round";

    private TextView tvRoundTimer;
    private TextView tvPhaseText;
    private TextView tvPlayerOneScore;
    private TextView tvPlayerTwoScore;
    private TextView tvChatBubble;
    private Button btnQuitMatch;

    private int playerOneScore;
    private int playerTwoScore;
    private GameRound currentRound;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_host);

        tvRoundTimer = findViewById(R.id.tvRoundTimer);
        tvPhaseText = findViewById(R.id.tvPhaseText);
        tvPlayerOneScore = findViewById(R.id.tvPlayerOneScore);
        tvPlayerTwoScore = findViewById(R.id.tvPlayerTwoScore);
        tvChatBubble = findViewById(R.id.tvChatBubble);
        btnQuitMatch = findViewById(R.id.btnQuitMatch);

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
            case KORAK_PO_KORAK:
                fragment = new KorakPoKorakFragment();
                break;
            case MOJ_BROJ:
                fragment = new MojBrojFragment();
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
    public void finishMatch() {
        finish();
    }

    private void surrenderMatch() {
        Toast.makeText(this, R.string.match_surrendered_message, Toast.LENGTH_SHORT).show();
        finishMatch();
    }
}
