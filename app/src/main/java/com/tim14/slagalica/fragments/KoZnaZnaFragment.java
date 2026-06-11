package com.tim14.slagalica.fragments;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.tim14.slagalica.R;
import com.tim14.slagalica.game.BaseGameFragment;
import com.tim14.slagalica.model.KoZnaZnaQuestion;
import com.tim14.slagalica.repository.FirebaseCallback;
import com.tim14.slagalica.repository.FirestoreRepository;

import java.util.ArrayList;
import java.util.List;

public class KoZnaZnaFragment extends BaseGameFragment {

    private static final String TAG = "KoZnaZnaFragment";

    private TextView questionTimerText;
    private TextView questionCounterText;
    private TextView questionText;
    private TextView ruleInfoText;

    private Button answerAButton;
    private Button answerBButton;
    private Button answerCButton;
    private Button answerDButton;
    private Button playerTwoAnswerButton;

    private FirestoreRepository firestoreRepository;
    private final Handler handler = new Handler(Looper.getMainLooper());

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
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_ko_zna_zna, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        firestoreRepository = new FirestoreRepository();

        questionTimerText = view.findViewById(R.id.questionTimerText);
        questionCounterText = view.findViewById(R.id.questionCounterText);
        questionText = view.findViewById(R.id.questionText);
        ruleInfoText = view.findViewById(R.id.ruleInfoText);

        answerAButton = view.findViewById(R.id.answerAButton);
        answerBButton = view.findViewById(R.id.answerBButton);
        answerCButton = view.findViewById(R.id.answerCButton);
        answerDButton = view.findViewById(R.id.answerDButton);
        playerTwoAnswerButton = view.findViewById(R.id.playerTwoAnswerButton);

        playerOneScore = host().getPlayerOneScore();
        playerTwoScore = host().getPlayerTwoScore();

        host().setPhaseText(getString(R.string.phase_ko_zna_zna));
        host().setTimerValue(25);
        host().setScores(playerOneScore, playerTwoScore);

        ruleInfoText.setText(getString(R.string.ko_zna_zna_rules_short));

        answerAButton.setOnClickListener(v -> checkPlayerOneAnswer(0));
        answerBButton.setOnClickListener(v -> checkPlayerOneAnswer(1));
        answerCButton.setOnClickListener(v -> checkPlayerOneAnswer(2));
        answerDButton.setOnClickListener(v -> checkPlayerOneAnswer(3));
        playerTwoAnswerButton.setOnClickListener(v -> playerTwoAnswered());

        disableAnswerButtons();
        questionText.setText(getString(R.string.loading_questions));
        updateScores();

        loadQuestionsFromFirestore();
    }

    private void loadQuestionsFromFirestore() {
        firestoreRepository.getKoZnaZnaQuestions(new FirebaseCallback<List<KoZnaZnaQuestion>>() {
            @Override
            public void onSuccess(List<KoZnaZnaQuestion> result) {
                if (!isAdded() || gameFinished) {
                    return;
                }

                if (result == null || result.size() < 5) {
                    questionText.setText(getString(R.string.not_enough_questions));
                    Toast.makeText(requireContext(),
                            R.string.need_at_least_5_questions,
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
                if (!isAdded() || gameFinished) {
                    return;
                }

                questionText.setText(getString(R.string.questions_load_failed));
                Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show();
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
                host().setTimerValue((int) seconds);
            }

            @Override
            public void onFinish() {
                gameTimer = null;
                host().setTimerValue(0);
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
                questionTimerText.setText(getString(R.string.question_time_format, seconds));
            }

            @Override
            public void onFinish() {
                questionTimer = null;
                questionTimerText.setText(getString(R.string.question_time_format, 0));

                if (!questionAnswered && !gameFinished) {
                    questionAnswered = true;
                    disableAnswerButtons();

                    Toast.makeText(requireContext(),
                            R.string.no_answer_next_question,
                            Toast.LENGTH_SHORT).show();

                    handler.postDelayed(() -> {
                        if (isAdded()) {
                            goToNextQuestion();
                        }
                    }, 900);
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
            Toast.makeText(requireContext(), R.string.invalid_question_answers, Toast.LENGTH_LONG).show();
            endGame();
            return;
        }

        questionAnswered = false;
        enableAnswerButtons();

        questionCounterText.setText(getString(R.string.question_counter_format, questionIndex + 1));
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
            Toast.makeText(requireContext(), R.string.correct_points_message, Toast.LENGTH_SHORT).show();
        } else {
            playerOneScore -= 5;
            wrongAnswers++;
            Toast.makeText(requireContext(), R.string.wrong_points_message, Toast.LENGTH_SHORT).show();
        }

        updateScores();
        handler.postDelayed(() -> {
            if (isAdded()) {
                goToNextQuestion();
            }
        }, 900);
    }

    private void playerTwoAnswered() {
        if (questionAnswered || gameFinished) {
            return;
        }

        questionAnswered = true;
        disableAnswerButtons();
        cancelQuestionTimer();

        playerTwoScore += 10;

        Toast.makeText(requireContext(), R.string.player_two_answered_first, Toast.LENGTH_SHORT).show();

        updateScores();
        handler.postDelayed(() -> {
            if (isAdded()) {
                goToNextQuestion();
            }
        }, 900);
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

        host().setScores(playerOneScore, playerTwoScore);
        host().setTimerValue(0);
        host().setPhaseText(getString(R.string.phase_ko_zna_zna_finished));

        Toast.makeText(requireContext(),
                getString(R.string.ko_zna_zna_end_format, playerOneScore, playerTwoScore),
                Toast.LENGTH_LONG).show();

        handler.postDelayed(() -> {
            if (isAdded()) {
                host().goToNextRound();
            }
        }, 3000);
    }

    private void updateScores() {
        host().setScores(playerOneScore, playerTwoScore);
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
    public void onDestroyView() {
        cancelGameTimer();
        cancelQuestionTimer();
        handler.removeCallbacksAndMessages(null);
        super.onDestroyView();
    }
}
