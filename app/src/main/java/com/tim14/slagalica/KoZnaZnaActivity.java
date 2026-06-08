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

import com.tim14.slagalica.model.KoZnaZnaQuestion;
import com.tim14.slagalica.repository.FirebaseCallback;
import com.tim14.slagalica.repository.FirestoreRepository;

import java.util.ArrayList;
import java.util.List;

public class KoZnaZnaActivity extends AppCompatActivity {

    private static final String TAG = "KoZnaZnaActivity";

    private TextView gameTimerText, questionTimerText, questionCounterText;
    private TextView questionText, playerOneScoreText, playerTwoScoreText, ruleInfoText;
    private Button answerAButton, answerBButton, answerCButton, answerDButton;
    private Button playerTwoAnswerButton, quitButton;
    private LinearLayout statusBarLayout;

    private FirestoreRepository firestoreRepository;

    private boolean isGuest;

    private int questionIndex = 0;
    private int playerOneScore = 0;
    private int playerTwoScore = 0;

    private int correctAnswers = 0;
    private int wrongAnswers = 0;

    private CountDownTimer gameTimer;
    private CountDownTimer questionTimer;

    private boolean questionAnswered = false;
    private boolean gameFinished = false;

    private List<KoZnaZnaQuestion> questions = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        setContentView(R.layout.activity_ko_zna_zna);

        firestoreRepository = new FirestoreRepository();

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
        statusBarLayout = findViewById(R.id.statusBarLayout);

        isGuest = getIntent().getBooleanExtra("IS_GUEST", false);

        if (isGuest) {
            statusBarLayout.setVisibility(View.GONE);
        }

        ruleInfoText.setText(getString(R.string.ko_zna_zna_rules));

        answerAButton.setOnClickListener(v -> checkPlayerOneAnswer(0));
        answerBButton.setOnClickListener(v -> checkPlayerOneAnswer(1));
        answerCButton.setOnClickListener(v -> checkPlayerOneAnswer(2));
        answerDButton.setOnClickListener(v -> checkPlayerOneAnswer(3));

        playerTwoAnswerButton.setOnClickListener(v -> playerTwoAnswered());
        quitButton.setOnClickListener(v -> finish());

        disableAnswerButtons();
        questionText.setText("Loading questions...");

        loadQuestionsFromFirestore();
    }

    private void loadQuestionsFromFirestore() {
        firestoreRepository.getKoZnaZnaQuestions(new FirebaseCallback<List<KoZnaZnaQuestion>>() {
            @Override
            public void onSuccess(List<KoZnaZnaQuestion> result) {
                if (result == null || result.size() < 5) {
                    questionText.setText("Not enough questions in database.");
                    Toast.makeText(KoZnaZnaActivity.this,
                            "You need at least 5 questions in Firestore.",
                            Toast.LENGTH_LONG).show();
                    return;
                }

                questions = result;

                Log.d(TAG, "Questions loaded from Firestore: " + questions.size());

                startGameTimer();
                loadQuestion();
            }

            @Override
            public void onError(String error) {
                questionText.setText("Questions could not be loaded.");
                Toast.makeText(KoZnaZnaActivity.this, error, Toast.LENGTH_LONG).show();
                Log.e(TAG, "Error loading questions: " + error);
            }
        });
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
        if (gameFinished || questions.isEmpty()) {
            return;
        }

        KoZnaZnaQuestion currentQuestion = questions.get(questionIndex);

        if (currentQuestion.answers == null || currentQuestion.answers.size() < 4) {
            Toast.makeText(this, "Question has invalid answers.", Toast.LENGTH_LONG).show();
            endGame();
            return;
        }

        questionAnswered = false;
        enableAnswerButtons();

        questionCounterText.setText("Question " + (questionIndex + 1) + " / 5");
        questionText.setText(currentQuestion.question);

        answerAButton.setText(currentQuestion.answers.get(0));
        answerBButton.setText(currentQuestion.answers.get(1));
        answerCButton.setText(currentQuestion.answers.get(2));
        answerDButton.setText(currentQuestion.answers.get(3));

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

        KoZnaZnaQuestion currentQuestion = questions.get(questionIndex);

        if (selectedIndex == currentQuestion.correctIndex) {
            playerOneScore += 10;
            correctAnswers++;
            Toast.makeText(this, "Correct! +10", Toast.LENGTH_SHORT).show();
        } else {
            playerOneScore -= 5;
            wrongAnswers++;
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

        if (questionIndex < 4) {
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

        firestoreRepository.updateKoZnaZnaStatistics(
                correctAnswers,
                wrongAnswers,
                playerOneScore
        );

        Toast.makeText(this,
                "End of game. Player 1: " + playerOneScore + " | Player 2: " + playerTwoScore,
                Toast.LENGTH_LONG).show();

        questionText.postDelayed(() -> {
            Intent intent = new Intent(KoZnaZnaActivity.this, SpojniceActivity.class);

            intent.putExtra("IS_GUEST", isGuest);
            intent.putExtra("PLAYER_ONE_SCORE", playerOneScore);
            intent.putExtra("PLAYER_TWO_SCORE", playerTwoScore);

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