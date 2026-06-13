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
import com.tim14.slagalica.service.KoZnaZnaService;

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
    private KoZnaZnaService koZnaZnaService;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private CountDownTimer gameTimer;
    private CountDownTimer questionTimer;
    private boolean gameEnded;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_ko_zna_zna, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        firestoreRepository = new FirestoreRepository();
        koZnaZnaService = new KoZnaZnaService();

        questionTimerText = view.findViewById(R.id.questionTimerText);
        questionCounterText = view.findViewById(R.id.questionCounterText);
        questionText = view.findViewById(R.id.questionText);
        ruleInfoText = view.findViewById(R.id.ruleInfoText);

        answerAButton = view.findViewById(R.id.answerAButton);
        answerBButton = view.findViewById(R.id.answerBButton);
        answerCButton = view.findViewById(R.id.answerCButton);
        answerDButton = view.findViewById(R.id.answerDButton);
        playerTwoAnswerButton = view.findViewById(R.id.playerTwoAnswerButton);

        host().setPhaseText(getString(R.string.phase_ko_zna_zna));
        host().setTimerValue(25);
        host().setScores(host().getPlayerOneScore(), host().getPlayerTwoScore());

        ruleInfoText.setText(getString(R.string.ko_zna_zna_rules_short));

        answerAButton.setOnClickListener(v -> checkPlayerOneAnswer(0));
        answerBButton.setOnClickListener(v -> checkPlayerOneAnswer(1));
        answerCButton.setOnClickListener(v -> checkPlayerOneAnswer(2));
        answerDButton.setOnClickListener(v -> checkPlayerOneAnswer(3));
        playerTwoAnswerButton.setOnClickListener(v -> playerTwoAnswered());

        disableAnswerButtons();
        questionText.setText(getString(R.string.loading_questions));

        loadQuestionsFromFirestore();
    }

    private void loadQuestionsFromFirestore() {
        firestoreRepository.getKoZnaZnaQuestions(new FirebaseCallback<List<KoZnaZnaQuestion>>() {
            @Override
            public void onSuccess(List<KoZnaZnaQuestion> result) {
                if (!isAdded() || gameEnded) {
                    return;
                }

                if (!koZnaZnaService.startGame(
                        result,
                        host().getPlayerOneScore(),
                        host().getPlayerTwoScore()
                )) {
                    questionText.setText(getString(R.string.not_enough_questions));
                    Toast.makeText(requireContext(),
                            R.string.need_at_least_5_questions,
                            Toast.LENGTH_LONG).show();
                    return;
                }

                Log.d(TAG, "Questions loaded from Firestore: " + result.size());

                startGameTimer();
                loadQuestion();
            }

            @Override
            public void onError(String error) {
                if (!isAdded() || gameEnded) {
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

                if (!koZnaZnaService.isQuestionAnswered()
                        && !koZnaZnaService.isGameFinished()) {
                    koZnaZnaService.timeoutCurrentQuestion();
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
        if (koZnaZnaService.isGameFinished() || !koZnaZnaService.startCurrentQuestion()) {
            return;
        }

        KoZnaZnaQuestion currentQuestion = koZnaZnaService.getCurrentQuestion();

        if (!koZnaZnaService.isCurrentQuestionValid()) {
            Toast.makeText(requireContext(), R.string.invalid_question_answers, Toast.LENGTH_LONG).show();
            endGame();
            return;
        }

        enableAnswerButtons();

        questionCounterText.setText(
                getString(R.string.question_counter_format, koZnaZnaService.getQuestionNumber())
        );
        questionText.setText(currentQuestion.question);

        answerAButton.setText(currentQuestion.answers.get(0));
        answerBButton.setText(currentQuestion.answers.get(1));
        answerCButton.setText(currentQuestion.answers.get(2));
        answerDButton.setText(currentQuestion.answers.get(3));

        updateScores();
        startQuestionTimer();
    }

    private void checkPlayerOneAnswer(int selectedIndex) {
        KoZnaZnaService.AnswerResult result =
                koZnaZnaService.answerCurrentQuestion(selectedIndex);
        if (result.getType() == KoZnaZnaService.AnswerType.NO_OP) {
            return;
        }
        disableAnswerButtons();
        cancelQuestionTimer();

        if (result.getType() == KoZnaZnaService.AnswerType.CORRECT) {
            Toast.makeText(requireContext(), R.string.correct_points_message, Toast.LENGTH_SHORT).show();
        } else {
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
        KoZnaZnaService.AnswerResult result = koZnaZnaService.playerTwoAnswersFirst();
        if (result.getType() == KoZnaZnaService.AnswerType.NO_OP) {
            return;
        }

        disableAnswerButtons();
        cancelQuestionTimer();

        Toast.makeText(requireContext(), R.string.player_two_answered_first, Toast.LENGTH_SHORT).show();

        updateScores();
        handler.postDelayed(() -> {
            if (isAdded()) {
                goToNextQuestion();
            }
        }, 900);
    }

    private void goToNextQuestion() {
        if (koZnaZnaService.isGameFinished()) {
            return;
        }

        if (koZnaZnaService.goToNextQuestion()) {
            loadQuestion();
        } else {
            endGame();
        }
    }

    private void endGame() {
        if (gameEnded) {
            return;
        }

        gameEnded = true;
        koZnaZnaService.finishGame();

        cancelGameTimer();
        cancelQuestionTimer();

        disableAnswerButtons();
        playerTwoAnswerButton.setEnabled(false);

        firestoreRepository.updateKoZnaZnaStatistics(
                koZnaZnaService.getCorrectAnswers(),
                koZnaZnaService.getWrongAnswers(),
                koZnaZnaService.getTotalScore()
        );

        host().setScores(
                koZnaZnaService.getPlayerOneScore(),
                koZnaZnaService.getPlayerTwoScore()
        );
        host().setTimerValue(0);
        host().setPhaseText(getString(R.string.phase_ko_zna_zna_finished));

        Toast.makeText(requireContext(),
                getString(
                        R.string.ko_zna_zna_end_format,
                        koZnaZnaService.getPlayerOneScore(),
                        koZnaZnaService.getPlayerTwoScore()
                ),
                Toast.LENGTH_LONG).show();

        handler.postDelayed(() -> {
            if (isAdded()) {
                host().goToNextRound();
            }
        }, 3000);
    }

    private void updateScores() {
        host().setScores(
                koZnaZnaService.getPlayerOneScore(),
                koZnaZnaService.getPlayerTwoScore()
        );
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
