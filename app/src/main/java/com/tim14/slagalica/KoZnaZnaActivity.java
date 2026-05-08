package com.tim14.slagalica;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;

public class KoZnaZnaActivity extends AppCompatActivity {

    private static final String TAG = "KoZnaZnaActivity";

    private TextView gameTimerText, questionTimerText, questionCounterText;
    private TextView questionText, playerOneScoreText, playerTwoScoreText, ruleInfoText;
    private Button answerAButton, answerBButton, answerCButton, answerDButton;
    private Button playerTwoAnswerButton, quitButton;

    private int questionIndex = 0;
    private int playerOneScore = 0;
    private int playerTwoScore = 0;

    private CountDownTimer gameTimer;
    private CountDownTimer questionTimer;

    private boolean questionAnswered = false;
    private boolean gameFinished = false;

    private final String[] questions = {
            "Capital of France?",
            "Largest planet in the Solar System?",
            "Which language is used for Android development?",
            "How many continents are there?",
            "Which sea is near Montenegro?"
    };

    private final String[][] answers = {
            {"Paris", "London", "Berlin", "Rome"},
            {"Earth", "Mars", "Jupiter", "Venus"},
            {"Java", "HTML", "CSS", "SQL"},
            {"5", "6", "7", "8"},
            {"Adriatic Sea", "Baltic Sea", "Red Sea", "Black Sea"}
    };

    private final int[] correctIndexes = {0, 2, 0, 2, 0};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        setContentView(R.layout.activity_ko_zna_zna);

        gameTimerText = findViewById(R.id.gameTimerText);
        questionTimerText = findViewById(R.id.questionTimerText);
        questionCounterText = findViewById(R.id.questionCounterText);
        questionText = findViewById(R.id.questionText);

        playerOneScoreText = findViewById(R.id.playerOneScoreText);
        playerTwoScoreText = findViewById(R.id.playerTwoScoreText);
        ruleInfoText = findViewById(R.id.ruleInfoText);

        answerAButton = findViewById(R.id.answerAButton);
        answerBButton = findViewById(R.id.answerBButton);
        answerCButton = findViewById(R.id.answerCButton);
        answerDButton = findViewById(R.id.answerDButton);

        playerTwoAnswerButton = findViewById(R.id.playerTwoAnswerButton);
        quitButton = findViewById(R.id.koZnaZnaQuitButton);

        ruleInfoText.setText(getString(R.string.ko_zna_zna_rules));

        answerAButton.setOnClickListener(v -> checkPlayerOneAnswer(0));
        answerBButton.setOnClickListener(v -> checkPlayerOneAnswer(1));
        answerCButton.setOnClickListener(v -> checkPlayerOneAnswer(2));
        answerDButton.setOnClickListener(v -> checkPlayerOneAnswer(3));

        playerTwoAnswerButton.setOnClickListener(v -> playerTwoAnswered());
        quitButton.setOnClickListener(v -> finish());

        startGameTimer();
        loadQuestion();
    }

    private void startGameTimer() {
        cancelGameTimer();

        gameTimer = new CountDownTimer(26000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long seconds = millisUntilFinished / 1000;
                gameTimerText.setText("Game time: " + seconds + "s");
            }

            @Override
            public void onFinish() {
                gameTimer = null;
                gameTimerText.setText("Game time: 0s");
                endGame();
            }
        };

        gameTimer.start();
    }

    private void startQuestionTimer() {
        cancelQuestionTimer();

        questionTimer = new CountDownTimer(6000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long seconds = millisUntilFinished / 1000;
                questionTimerText.setText("Question time: " + seconds + "s");
            }

            @Override
            public void onFinish() {
                questionTimer = null;
                questionTimerText.setText("Question time: 0s");

                if (!questionAnswered && !gameFinished) {
                    questionAnswered = true;
                    disableAnswerButtons();

                    Toast.makeText(KoZnaZnaActivity.this,
                            "No answer. Moving to next question.",
                            Toast.LENGTH_SHORT).show();

                    questionTimerText.postDelayed(() -> goToNextQuestion(), 900);
                }
            }
        };

        questionTimer.start();
    }

    private void loadQuestion() {
        if (gameFinished) {
            return;
        }

        questionAnswered = false;
        enableAnswerButtons();

        questionCounterText.setText("Question " + (questionIndex + 1) + " / 5");
        questionText.setText(questions[questionIndex]);

        answerAButton.setText(answers[questionIndex][0]);
        answerBButton.setText(answers[questionIndex][1]);
        answerCButton.setText(answers[questionIndex][2]);
        answerDButton.setText(answers[questionIndex][3]);

        updateScores();
        startQuestionTimer();
    }

    private void checkPlayerOneAnswer(int selectedIndex) {
        if (questionAnswered || gameFinished) {
            return;
        }

        questionAnswered = true;
        disableAnswerButtons();
        cancelQuestionTimer();

        if (selectedIndex == correctIndexes[questionIndex]) {
            playerOneScore += 10;
            Toast.makeText(this, "Correct! +10", Toast.LENGTH_SHORT).show();
        } else {
            playerOneScore -= 5;
            Toast.makeText(this, "Wrong! -5", Toast.LENGTH_SHORT).show();
        }

        updateScores();
        answerAButton.postDelayed(this::goToNextQuestion, 900);
    }

    private void playerTwoAnswered() {
        if (questionAnswered || gameFinished) {
            return;
        }

        questionAnswered = true;
        disableAnswerButtons();
        cancelQuestionTimer();

        playerTwoScore += 10;
        Toast.makeText(this, "Player 2 answered first! +10", Toast.LENGTH_SHORT).show();
        updateScores();

        answerAButton.postDelayed(this::goToNextQuestion, 900);
    }

    private void goToNextQuestion() {
        if (gameFinished) {
            return;
        }

        if (questionIndex < questions.length - 1) {
            questionIndex++;
            loadQuestion();
        } else {
            endGame();
        }
    }

    private void endGame() {
        if (gameFinished) {
            return;
        }

        gameFinished = true;

        cancelGameTimer();
        cancelQuestionTimer();

        disableAnswerButtons();
        playerTwoAnswerButton.setEnabled(false);

        Toast.makeText(this,
                "End of game. Player 1: " + playerOneScore + " | Player 2: " + playerTwoScore,
                Toast.LENGTH_LONG).show();

        questionText.postDelayed(() -> {

            Intent intent = new Intent(
                    KoZnaZnaActivity.this,
                    SpojniceActivity.class
            );

            startActivity(intent);
            finish();

        }, 3000);
    }

    private void updateScores() {
        playerOneScoreText.setText("Player 1 score: " + playerOneScore);
        playerTwoScoreText.setText("Player 2 score: " + playerTwoScore);
    }

    private void enableAnswerButtons() {
        answerAButton.setEnabled(true);
        answerBButton.setEnabled(true);
        answerCButton.setEnabled(true);
        answerDButton.setEnabled(true);
        playerTwoAnswerButton.setEnabled(true);
    }

    private void disableAnswerButtons() {
        answerAButton.setEnabled(false);
        answerBButton.setEnabled(false);
        answerCButton.setEnabled(false);
        answerDButton.setEnabled(false);
        playerTwoAnswerButton.setEnabled(false);
    }

    private void cancelGameTimer() {
        if (gameTimer != null) {
            gameTimer.cancel();
            gameTimer = null;
        }
    }

    private void cancelQuestionTimer() {
        if (questionTimer != null) {
            questionTimer.cancel();
            questionTimer = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        cancelGameTimer();
        cancelQuestionTimer();

        questionText.removeCallbacks(null);
        answerAButton.removeCallbacks(null);
        questionTimerText.removeCallbacks(null);

        Log.d(TAG, "onDestroy");
    }


    @Override protected void onStart() { super.onStart(); Log.d(TAG, "onStart"); }
    @Override protected void onRestart() { super.onRestart(); Log.d(TAG, "onRestart"); }
    @Override protected void onResume() { super.onResume(); Log.d(TAG, "onResume"); }
    @Override protected void onPause() { super.onPause(); Log.d(TAG, "onPause"); }
    @Override protected void onStop() { super.onStop(); Log.d(TAG, "onStop"); }
}