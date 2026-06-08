package com.tim14.slagalica;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.tim14.slagalica.model.SpojniceRound;
import com.tim14.slagalica.repository.FirebaseCallback;
import com.tim14.slagalica.repository.FirestoreRepository;

import java.util.ArrayList;
import java.util.List;

public class SpojniceActivity extends AppCompatActivity {

    private static final String TAG = "SpojniceActivity";

    private TextView roundText, currentPlayerText, timerText;
    private TextView playerOneScoreText, playerTwoScoreText, selectedPairText, secondChanceInfoText;

    private Button left1Button, left2Button, left3Button, left4Button, left5Button;
    private Button right1Button, right2Button, right3Button, right4Button, right5Button;
    private Button confirmConnectionButton, quitButton;

    private LinearLayout statusBarLayout;
    private FirestoreRepository firestoreRepository;

    private boolean isGuest;

    private int round = 1;
    private int currentPlayer = 1;
    private int playerOneScore = 0;
    private int playerTwoScore = 0;

    private int solvedPairsInRound = 0;
    private int attemptsInTurn = 0;
    private int secondChancePairsCount = 0;

    private int correctPairsTotal = 0;
    private final int totalPairs = 10;

    private boolean secondChance = false;
    private boolean gameFinished = false;

    private String selectedLeft = "";
    private String selectedRight = "";

    private CountDownTimer roundTimer;

    private List<SpojniceRound> rounds = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        setContentView(R.layout.activity_spojnice);

        firestoreRepository = new FirestoreRepository();

        roundText = findViewById(R.id.roundText);
        currentPlayerText = findViewById(R.id.currentPlayerText);
        timerText = findViewById(R.id.timerText);
        playerOneScoreText = findViewById(R.id.playerOneScoreText);
        playerTwoScoreText = findViewById(R.id.playerTwoScoreText);
        selectedPairText = findViewById(R.id.selectedPairText);
        secondChanceInfoText = findViewById(R.id.secondChanceInfoText);

        left1Button = findViewById(R.id.left1Button);
        left2Button = findViewById(R.id.left2Button);
        left3Button = findViewById(R.id.left3Button);
        left4Button = findViewById(R.id.left4Button);
        left5Button = findViewById(R.id.left5Button);

        right1Button = findViewById(R.id.right1Button);
        right2Button = findViewById(R.id.right2Button);
        right3Button = findViewById(R.id.right3Button);
        right4Button = findViewById(R.id.right4Button);
        right5Button = findViewById(R.id.right5Button);

        confirmConnectionButton = findViewById(R.id.confirmConnectionButton);
        quitButton = findViewById(R.id.spojniceQuitButton);
        statusBarLayout = findViewById(R.id.statusBarLayout);

        isGuest = getIntent().getBooleanExtra("IS_GUEST", false);

        playerOneScore = getIntent().getIntExtra("PLAYER_ONE_SCORE", 0);
        playerTwoScore = getIntent().getIntExtra("PLAYER_TWO_SCORE", 0);

        if (isGuest) {
            statusBarLayout.setVisibility(View.GONE);
        }

        confirmConnectionButton.setOnClickListener(v -> confirmConnection());
        quitButton.setOnClickListener(v -> finish());

        disableAllPairButtons();
        confirmConnectionButton.setEnabled(false);
        secondChanceInfoText.setText("Loading Spojnice...");

        loadRoundsFromFirestore();
    }

    private void loadRoundsFromFirestore() {
        firestoreRepository.getSpojniceRounds(new FirebaseCallback<List<SpojniceRound>>() {
            @Override
            public void onSuccess(List<SpojniceRound> result) {
                if (result == null || result.size() < 2) {
                    secondChanceInfoText.setText("Not enough Spojnice rounds in database.");
                    Toast.makeText(SpojniceActivity.this,
                            "You need at least 2 Spojnice rounds in Firestore.",
                            Toast.LENGTH_LONG).show();
                    return;
                }

                rounds = result;

                Log.d(TAG, "Spojnice rounds loaded from Firestore: " + rounds.size());

                startRound();
            }

            @Override
            public void onError(String error) {
                secondChanceInfoText.setText("Spojnice could not be loaded.");
                Toast.makeText(SpojniceActivity.this, error, Toast.LENGTH_LONG).show();
                Log.e(TAG, "Error loading Spojnice rounds: " + error);
            }
        });
    }

    private void startRound() {
        solvedPairsInRound = 0;
        attemptsInTurn = 0;
        secondChancePairsCount = 0;
        secondChance = false;
        selectedLeft = "";
        selectedRight = "";

        currentPlayer = round == 1 ? 1 : 2;

        setRoundItems();
        enableAllPairButtons();

        confirmConnectionButton.setEnabled(true);
        secondChanceInfoText.setText(
                "First player connects all 5 pairs. If some pairs remain, the other player gets a second chance."
        );

        updateHeader();
        updateScores();
        updateSelectedPairText();
        startRoundTimer();
    }

    private void setRoundItems() {
        int index = round - 1;
        SpojniceRound currentRound = rounds.get(index);

        if (!isRoundValid(currentRound)) {
            Toast.makeText(this, "Invalid Spojnice data in Firestore.", Toast.LENGTH_LONG).show();
            endGame();
            return;
        }

        left1Button.setText(currentRound.getLeftItems().get(0));
        left2Button.setText(currentRound.getLeftItems().get(1));
        left3Button.setText(currentRound.getLeftItems().get(2));
        left4Button.setText(currentRound.getLeftItems().get(3));
        left5Button.setText(currentRound.getLeftItems().get(4));

        right1Button.setText(currentRound.getDisplayedRightItems().get(0));
        right2Button.setText(currentRound.getDisplayedRightItems().get(1));
        right3Button.setText(currentRound.getDisplayedRightItems().get(2));
        right4Button.setText(currentRound.getDisplayedRightItems().get(3));
        right5Button.setText(currentRound.getDisplayedRightItems().get(4));

        left1Button.setOnClickListener(v -> selectLeft(left1Button.getText().toString()));
        left2Button.setOnClickListener(v -> selectLeft(left2Button.getText().toString()));
        left3Button.setOnClickListener(v -> selectLeft(left3Button.getText().toString()));
        left4Button.setOnClickListener(v -> selectLeft(left4Button.getText().toString()));
        left5Button.setOnClickListener(v -> selectLeft(left5Button.getText().toString()));

        right1Button.setOnClickListener(v -> selectRight(right1Button.getText().toString()));
        right2Button.setOnClickListener(v -> selectRight(right2Button.getText().toString()));
        right3Button.setOnClickListener(v -> selectRight(right3Button.getText().toString()));
        right4Button.setOnClickListener(v -> selectRight(right4Button.getText().toString()));
        right5Button.setOnClickListener(v -> selectRight(right5Button.getText().toString()));
    }

    private boolean isRoundValid(SpojniceRound currentRound) {
        return currentRound != null
                && currentRound.getLeftItems() != null
                && currentRound.getCorrectRightItems() != null
                && currentRound.getDisplayedRightItems() != null
                && currentRound.getLeftItems().size() >= 5
                && currentRound.getCorrectRightItems().size() >= 5
                && currentRound.getDisplayedRightItems().size() >= 5;
    }

    private void startRoundTimer() {
        cancelRoundTimer();

        roundTimer = new CountDownTimer(31000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long seconds = millisUntilFinished / 1000;
                timerText.setText("Time: " + seconds + "s");
            }

            @Override
            public void onFinish() {
                roundTimer = null;
                timerText.setText("Time: 0s");

                if (!secondChance && solvedPairsInRound < 5) {
                    switchToSecondPlayer();
                } else {
                    prepareNextRoundOrEnd();
                }
            }
        };

        roundTimer.start();
    }

    private void selectLeft(String value) {
        selectedLeft = value;
        updateSelectedPairText();
    }

    private void selectRight(String value) {
        selectedRight = value;
        updateSelectedPairText();
    }

    private void updateSelectedPairText() {
        if (selectedLeft.isEmpty() && selectedRight.isEmpty()) {
            selectedPairText.setText("Selected pair: none");
        } else {
            selectedPairText.setText("Selected pair: " + selectedLeft + " - " + selectedRight);
        }
    }

    private void confirmConnection() {
        if (gameFinished) {
            return;
        }

        if (selectedLeft.isEmpty() || selectedRight.isEmpty()) {
            Toast.makeText(this, "Select one item from both columns.", Toast.LENGTH_SHORT).show();
            return;
        }

        attemptsInTurn++;

        if (isCorrectPair(selectedLeft, selectedRight)) {
            if (currentPlayer == 1) {
                playerOneScore += 2;
            } else {
                playerTwoScore += 2;
            }

            correctPairsTotal++;
            solvedPairsInRound++;

            disableSolvedPair(selectedLeft, selectedRight);

            Toast.makeText(this, "Correct connection! +2", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Wrong connection.", Toast.LENGTH_SHORT).show();
        }

        selectedLeft = "";
        selectedRight = "";

        updateSelectedPairText();
        updateScores();

        if (solvedPairsInRound == 5) {
            Toast.makeText(this, "All pairs solved in this round.", Toast.LENGTH_SHORT).show();
            selectedPairText.postDelayed(this::prepareNextRoundOrEnd, 900);
            return;
        }

        if (!secondChance && attemptsInTurn == 5) {
            selectedPairText.postDelayed(this::switchToSecondPlayer, 900);
            return;
        }

        if (secondChance && attemptsInTurn >= secondChancePairsCount) {
            selectedPairText.postDelayed(this::prepareNextRoundOrEnd, 900);
        }
    }

    private void switchToSecondPlayer() {
        cancelRoundTimer();

        secondChance = true;
        attemptsInTurn = 0;
        secondChancePairsCount = getRemainingPairsCount();

        if (secondChancePairsCount == 0) {
            prepareNextRoundOrEnd();
            return;
        }

        selectedLeft = "";
        selectedRight = "";

        currentPlayer = currentPlayer == 1 ? 2 : 1;

        updateHeader();
        updateSelectedPairText();

        secondChanceInfoText.setText(
                "Second chance: Player " + currentPlayer +
                        " connects the remaining " + secondChancePairsCount + " pairs."
        );

        Toast.makeText(this,
                "Second player gets 30 seconds for remaining pairs.",
                Toast.LENGTH_LONG).show();

        selectedPairText.postDelayed(this::startRoundTimer, 2000);
    }

    private int getRemainingPairsCount() {
        return 5 - solvedPairsInRound;
    }

    private void prepareNextRoundOrEnd() {
        cancelRoundTimer();

        confirmConnectionButton.setEnabled(false);

        if (round == 1) {
            Toast.makeText(this,
                    "Round 1 finished. Round 2 starts soon.",
                    Toast.LENGTH_LONG).show();

            selectedPairText.postDelayed(() -> {
                round = 2;
                startRound();
            }, 2500);
        } else {
            endGame();
        }
    }

    private boolean isCorrectPair(String left, String right) {
        int roundIndex = round - 1;
        SpojniceRound currentRound = rounds.get(roundIndex);

        for (int i = 0; i < 5; i++) {
            if (left.equals(currentRound.getLeftItems().get(i))
                    && right.equals(currentRound.getCorrectRightItems().get(i))) {
                return true;
            }
        }

        return false;
    }

    private void disableSolvedPair(String left, String right) {
        disableLeftButton(left);
        disableRightButton(right);
    }

    private void disableLeftButton(String left) {
        if (left.equals(left1Button.getText().toString())) fadeSolvedButton(left1Button);
        if (left.equals(left2Button.getText().toString())) fadeSolvedButton(left2Button);
        if (left.equals(left3Button.getText().toString())) fadeSolvedButton(left3Button);
        if (left.equals(left4Button.getText().toString())) fadeSolvedButton(left4Button);
        if (left.equals(left5Button.getText().toString())) fadeSolvedButton(left5Button);
    }

    private void disableRightButton(String right) {
        if (right.equals(right1Button.getText().toString())) fadeSolvedButton(right1Button);
        if (right.equals(right2Button.getText().toString())) fadeSolvedButton(right2Button);
        if (right.equals(right3Button.getText().toString())) fadeSolvedButton(right3Button);
        if (right.equals(right4Button.getText().toString())) fadeSolvedButton(right4Button);
        if (right.equals(right5Button.getText().toString())) fadeSolvedButton(right5Button);
    }

    private void fadeSolvedButton(Button button) {
        button.setEnabled(false);
        button.setAlpha(0.45f);
    }

    private void enableAllPairButtons() {
        left1Button.setEnabled(true);
        left2Button.setEnabled(true);
        left3Button.setEnabled(true);
        left4Button.setEnabled(true);
        left5Button.setEnabled(true);

        right1Button.setEnabled(true);
        right2Button.setEnabled(true);
        right3Button.setEnabled(true);
        right4Button.setEnabled(true);
        right5Button.setEnabled(true);

        left1Button.setAlpha(1f);
        left2Button.setAlpha(1f);
        left3Button.setAlpha(1f);
        left4Button.setAlpha(1f);
        left5Button.setAlpha(1f);

        right1Button.setAlpha(1f);
        right2Button.setAlpha(1f);
        right3Button.setAlpha(1f);
        right4Button.setAlpha(1f);
        right5Button.setAlpha(1f);
    }

    private void disableAllPairButtons() {
        left1Button.setEnabled(false);
        left2Button.setEnabled(false);
        left3Button.setEnabled(false);
        left4Button.setEnabled(false);
        left5Button.setEnabled(false);

        right1Button.setEnabled(false);
        right2Button.setEnabled(false);
        right3Button.setEnabled(false);
        right4Button.setEnabled(false);
        right5Button.setEnabled(false);
    }

    private void endGame() {
        if (gameFinished) {
            return;
        }

        gameFinished = true;

        cancelRoundTimer();

        disableAllPairButtons();
        confirmConnectionButton.setEnabled(false);

        secondChanceInfoText.setText("End of Spojnice.");

        firestoreRepository.updateSpojniceStatistics(
                correctPairsTotal,
                totalPairs,
                playerOneScore
        );

        Toast.makeText(this,
                "End of Spojnice. Player 1: " + playerOneScore + " | Player 2: " + playerTwoScore,
                Toast.LENGTH_LONG).show();

        selectedPairText.postDelayed(() -> {
            Intent intent = new Intent(
                    SpojniceActivity.this,
                    KorakPoKorakActivity.class
            );

            intent.putExtra("IS_GUEST", isGuest);
            intent.putExtra("PLAYER_ONE_SCORE", playerOneScore);
            intent.putExtra("PLAYER_TWO_SCORE", playerTwoScore);

            startActivity(intent);
            finish();
        }, 3000);
    }

    private void updateHeader() {
        roundText.setText("Round " + round + " / 2");
        currentPlayerText.setText("Current player: Player " + currentPlayer);
    }

    private void updateScores() {
        playerOneScoreText.setText("Player 1: " + playerOneScore);
        playerTwoScoreText.setText("Player 2: " + playerTwoScore);
    }

    private void cancelRoundTimer() {
        if (roundTimer != null) {
            roundTimer.cancel();
            roundTimer = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        cancelRoundTimer();

        selectedPairText.removeCallbacks(null);

        Log.d(TAG, "onDestroy");
    }

    @Override protected void onStart() { super.onStart(); Log.d(TAG, "onStart"); }
    @Override protected void onRestart() { super.onRestart(); Log.d(TAG, "onRestart"); }
    @Override protected void onResume() { super.onResume(); Log.d(TAG, "onResume"); }
    @Override protected void onPause() { super.onPause(); Log.d(TAG, "onPause"); }
    @Override protected void onStop() { super.onStop(); Log.d(TAG, "onStop"); }
}