package com.tim14.slagalica.fragments;

import android.content.res.ColorStateList;
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

import androidx.core.content.ContextCompat;

import com.tim14.slagalica.GameHostActivity;
import com.tim14.slagalica.R;
import com.tim14.slagalica.game.BaseGameFragment;
import com.tim14.slagalica.model.KoZnaZnaQuestion;
import com.tim14.slagalica.model.SharedMatchState;
import com.tim14.slagalica.repository.FirebaseCallback;
import com.tim14.slagalica.repository.FirestoreRepository;
import com.tim14.slagalica.repository.SharedMatchRepository;
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
    private SharedMatchRepository sharedMatchRepository;
    private KoZnaZnaService koZnaZnaService;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private CountDownTimer gameTimer;
    private CountDownTimer questionTimer;
    private boolean gameEnded;
    private boolean remoteMode;
    private int lastScheduledRevealIndex = -1;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_ko_zna_zna, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        firestoreRepository = new FirestoreRepository();
        sharedMatchRepository = new SharedMatchRepository();
        koZnaZnaService = new KoZnaZnaService();
        remoteMode = ((GameHostActivity) requireActivity()).isRemoteMatchMode();

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
        host().setScores(host().getPlayerOneScore(), host().getPlayerTwoScore());
        ruleInfoText.setText(getString(R.string.ko_zna_zna_rules_short));

        answerAButton.setOnClickListener(v -> onAnswerSelected(0));
        answerBButton.setOnClickListener(v -> onAnswerSelected(1));
        answerCButton.setOnClickListener(v -> onAnswerSelected(2));
        answerDButton.setOnClickListener(v -> onAnswerSelected(3));
        playerTwoAnswerButton.setOnClickListener(v -> playerTwoAnswered());

        disableAnswerButtons();
        questionText.setText(getString(R.string.loading_questions));

        if (remoteMode) {
            playerTwoAnswerButton.setVisibility(View.GONE);
            renderRemoteQuestion();
            return;
        }

        host().setTimerValue(25);
        loadQuestionsFromFirestore();
    }

    private void onAnswerSelected(int selectedIndex) {
        if (remoteMode) {
            submitRemoteAnswer(selectedIndex);
            return;
        }

        checkPlayerOneAnswer(selectedIndex);
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

        gameTimer = new CountDownTimer(25000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                host().setTimerValue((int) Math.ceil(millisUntilFinished / 1000.0));
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

        questionTimer = new CountDownTimer(5000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                questionTimerText.setText(
                        getString(R.string.question_time_format, (int) Math.ceil(millisUntilFinished / 1000.0))
                );
            }

            @Override
            public void onFinish() {
                questionTimer = null;
                questionTimerText.setText(getString(R.string.question_time_format, 0));

                if (!koZnaZnaService.isQuestionAnswered() && !koZnaZnaService.isGameFinished()) {
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
        bindQuestion(currentQuestion, koZnaZnaService.getQuestionNumber(), 5);
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
        showAnswerFeedback(selectedIndex, koZnaZnaService.getCurrentQuestion().correctIndex);

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
        }, 1500);
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
        }, 1500);
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

        if (host().shouldPersistStatistics()) {
            firestoreRepository.updateKoZnaZnaStatistics(
                    koZnaZnaService.getCorrectAnswers(),
                    koZnaZnaService.getWrongAnswers(),
                    koZnaZnaService.getTotalScore()
            );
        }

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

    private void bindQuestion(KoZnaZnaQuestion question, int questionNumber, int totalQuestions) {
        if (question == null || question.answers == null || question.answers.size() < 4) {
            questionText.setText(getString(R.string.invalid_question_answers));
            disableAnswerButtons();
            return;
        }

        questionCounterText.setText(
                getString(R.string.question_counter_format, questionNumber)
        );
        questionText.setText(question.question);
        answerAButton.setText(question.answers.get(0));
        answerBButton.setText(question.answers.get(1));
        answerCButton.setText(question.answers.get(2));
        answerDButton.setText(question.answers.get(3));
        clearAnswerFeedback();
        ruleInfoText.setText(getString(R.string.shared_match_kzz_phase_format, questionNumber, totalQuestions));
    }

    private void renderRemoteQuestion() {
        stopRoundTimer();
        cancelGameTimer();
        cancelQuestionTimer();

        GameHostActivity activity = (GameHostActivity) requireActivity();
        SharedMatchState state = activity.getSharedMatchState();
        if (state == null) {
            disableAnswerButtons();
            questionText.setText(getString(R.string.loading_questions));
            return;
        }

        if (SharedMatchState.STATUS_WAITING.equals(state.status)
                || SharedMatchState.PHASE_WAITING.equals(state.phase)) {
            renderWaitingForOpponentState();
            return;
        }

        KoZnaZnaQuestion question = activity.getSharedQuizQuestion(state.currentTurnIndex);

        if (question == null) {
            disableAnswerButtons();
            questionText.setText(getString(R.string.loading_questions));
            return;
        }

        bindQuestion(question, state.currentTurnIndex + 1, state.quizQuestions.size());
        host().setTimerValue(getRemoteRemainingSeconds(state));

        if (SharedMatchState.PHASE_KZZ_REVEAL.equals(state.phase)) {
            questionTimerText.setText(getString(R.string.question_time_format, 0));
            disableAnswerButtons();
            showRemoteRevealFeedback(question, state);
            scheduleRemoteAdvanceIfCoordinator(state);
            return;
        }

        enableAnswerButtons();
        questionTimerText.setText(
                getString(R.string.question_time_format, getRemoteRemainingSeconds(state))
        );
        startRoundTimer(
                getRemoteRemainingSeconds(state),
                secondsLeft -> questionTimerText.setText(
                        getString(R.string.question_time_format, secondsLeft)
                ),
                this::handleRemoteQuestionTimeout
        );
    }

    private void renderWaitingForOpponentState() {
        questionCounterText.setText("");
        questionTimerText.setText(getString(R.string.question_time_format, 0));
        questionText.setText(getString(R.string.shared_match_waiting_phase));
        ruleInfoText.setText(getString(R.string.shared_match_wait_turn_hint));
        answerAButton.setText("");
        answerBButton.setText("");
        answerCButton.setText("");
        answerDButton.setText("");
        disableAnswerButtons();
    }

    private void submitRemoteAnswer(int selectedIndex) {
        GameHostActivity activity = (GameHostActivity) requireActivity();
        SharedMatchState state = activity.getSharedMatchState();

        if (state == null
                || !SharedMatchState.PHASE_KZZ_QUESTION.equals(state.phase)
                || activity.getRemoteMatchId() == null) {
            return;
        }

        sharedMatchRepository.submitKoZnaZnaAnswer(
                activity.getRemoteMatchId(),
                state.currentTurnIndex,
                activity.getLocalPlayerNumber(),
                selectedIndex,
                new FirebaseCallback<SharedMatchRepository.KoZnaZnaAnswerOutcome>() {
                    @Override
                    public void onSuccess(SharedMatchRepository.KoZnaZnaAnswerOutcome outcome) {
                        if (!outcome.accepted || !isAdded()) {
                            return;
                        }

                        SharedMatchState latestState = activity.getSharedMatchState();
                        KoZnaZnaQuestion currentQuestion = latestState == null
                                ? null
                                : activity.getSharedQuizQuestion(latestState.currentTurnIndex);
                        if (currentQuestion != null) {
                            showAnswerFeedback(selectedIndex, currentQuestion.correctIndex);
                        }

                        if (host().shouldPersistStatistics()) {
                            firestoreRepository.updateKoZnaZnaStatistics(
                                    outcome.correct ? 1 : 0,
                                    outcome.correct ? 0 : 1,
                                    outcome.points
                            );
                        }
                    }

                    @Override
                    public void onError(String error) {
                        if (isAdded()) {
                            Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );
    }

    private void handleRemoteQuestionTimeout() {
        GameHostActivity activity = (GameHostActivity) requireActivity();
        SharedMatchState state = activity.getSharedMatchState();

        if (state == null
                || !activity.isRemoteProgressCoordinator()
                || !SharedMatchState.PHASE_KZZ_QUESTION.equals(state.phase)
                || activity.getRemoteMatchId() == null) {
            return;
        }

        sharedMatchRepository.resolveKoZnaZnaTimeout(
                activity.getRemoteMatchId(),
                state.currentTurnIndex,
                new FirebaseCallback<Boolean>() {
                    @Override
                    public void onSuccess(Boolean unused) {
                        // Listener will refresh the UI.
                    }

                    @Override
                    public void onError(String error) {
                        if (isAdded()) {
                            Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );
    }

    private void scheduleRemoteAdvanceIfCoordinator(SharedMatchState state) {
        GameHostActivity activity = (GameHostActivity) requireActivity();

        if (!activity.isRemoteProgressCoordinator() || lastScheduledRevealIndex == state.currentTurnIndex) {
            return;
        }

        lastScheduledRevealIndex = state.currentTurnIndex;

        handler.postDelayed(() -> {
            if (!isAdded()) {
                return;
            }

            SharedMatchState currentState = activity.getSharedMatchState();
            if (currentState == null
                    || currentState.currentTurnIndex != state.currentTurnIndex
                    || !SharedMatchState.PHASE_KZZ_REVEAL.equals(currentState.phase)) {
                return;
            }

            if (currentState.currentTurnIndex + 1 < currentState.quizQuestions.size()) {
                activity.updateSharedMatch(new java.util.HashMap<String, Object>() {{
                    put("currentTurnIndex", currentState.currentTurnIndex + 1);
                    put("phase", SharedMatchState.PHASE_KZZ_QUESTION);
                    put("activePlayer", 0);
                    put("phaseStartedAt", System.currentTimeMillis());
                    put("phaseDurationSeconds", 5);
                    put("answeredByPlayer", 0);
                    put("selectedAnswerIndex", -1);
                    put(
                            "phaseMessage",
                            getString(
                                    R.string.shared_match_kzz_phase_format,
                                    currentState.currentTurnIndex + 2,
                                    currentState.quizQuestions.size()
                            )
                    );
                }});
            } else {
                host().goToNextRound();
            }
        }, 900L);
    }

    private int getRemoteRemainingSeconds(SharedMatchState state) {
        if (state == null || state.phaseDurationSeconds <= 0) {
            return 0;
        }

        long elapsedMs = System.currentTimeMillis() - state.phaseStartedAt;
        int remaining = state.phaseDurationSeconds - (int) Math.floor(elapsedMs / 1000d);
        return Math.max(0, remaining);
    }

    private void enableAnswerButtons() {
        clearAnswerFeedback();
        answerAButton.setEnabled(true);
        answerBButton.setEnabled(true);
        answerCButton.setEnabled(true);
        answerDButton.setEnabled(true);
        if (!remoteMode) {
            playerTwoAnswerButton.setEnabled(true);
        }
    }

    private void disableAnswerButtons() {
        answerAButton.setEnabled(false);
        answerBButton.setEnabled(false);
        answerCButton.setEnabled(false);
        answerDButton.setEnabled(false);
        playerTwoAnswerButton.setEnabled(false);
    }

    private void showRemoteRevealFeedback(KoZnaZnaQuestion question, SharedMatchState state) {
        if (question == null || state == null) {
            return;
        }

        showAnswerFeedback(state.selectedAnswerIndex, question.correctIndex);
    }

    private void showAnswerFeedback(int selectedIndex, int correctIndex) {
        clearAnswerFeedback();

        Button selectedButton = getAnswerButton(selectedIndex);
        Button correctButton = getAnswerButton(correctIndex);

        if (selectedButton != null && selectedIndex != correctIndex) {
            tintAnswerButton(
                    selectedButton,
                    ContextCompat.getColor(requireContext(), R.color.slagalica_red),
                    ContextCompat.getColor(requireContext(), R.color.white)
            );
        }

        if (correctButton != null) {
            tintAnswerButton(
                    correctButton,
                    ContextCompat.getColor(requireContext(), R.color.slagalica_yellow),
                    ContextCompat.getColor(requireContext(), R.color.slagalica_dark_blue)
            );
        }
    }

    private void clearAnswerFeedback() {
        for (Button button : getAnswerButtons()) {
            button.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(requireContext(), R.color.slagalica_card)
            ));
            button.setTextColor(ContextCompat.getColor(requireContext(), R.color.slagalica_dark_blue));
        }
    }

    private void tintAnswerButton(Button button, int color, int textColor) {
        button.setBackgroundTintList(ColorStateList.valueOf(color));
        button.setTextColor(textColor);
    }

    private Button getAnswerButton(int index) {
        Button[] buttons = getAnswerButtons();
        if (index < 0 || index >= buttons.length) {
            return null;
        }

        return buttons[index];
    }

    private Button[] getAnswerButtons() {
        return new Button[]{answerAButton, answerBButton, answerCButton, answerDButton};
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
