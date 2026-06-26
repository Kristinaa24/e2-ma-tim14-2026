package com.tim14.slagalica.fragments;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.text.Editable;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.tim14.slagalica.GameHostActivity;
import com.tim14.slagalica.R;
import com.tim14.slagalica.game.BaseGameFragment;
import com.tim14.slagalica.game.MojBrojExpressionHelper;
import com.tim14.slagalica.model.SharedMatchState;
import com.tim14.slagalica.model.SharedMojBrojRound;
import com.tim14.slagalica.repository.FirestoreRepository;
import com.tim14.slagalica.repository.LocalGameRepository;
import com.tim14.slagalica.service.MojBrojService;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MojBrojFragment extends BaseGameFragment implements SensorEventListener {

    private static final int ROUND_DURATION_SECONDS = 60;
    private static final long AUTO_STOP_DELAY_MS = 5000L;
    private static final float SHAKE_THRESHOLD = 12.0f;
    private static final long SHAKE_COOLDOWN_MS = 900L;
    private static final float USED_NUMBER_ALPHA = 0.35f;
    private static final float DISABLED_NUMBER_ALPHA = 0.50f;
    private static final Pattern NUMBER_PATTERN = Pattern.compile("\\d+");

    private final Handler handler = new Handler(Looper.getMainLooper());

    private TextView roundOwnerText;
    private TextView targetNumberText;
    private TextView activeInputText;
    private TextView resultText;
    private TextView playerOneExpressionTitle;

    private EditText playerOneExpressionInput;
    private EditText playerTwoExpressionInput;
    private View playerOneExpressionContainer;
    private View playerTwoExpressionContainer;
    private EditText activeInput;

    private Button stopTargetButton;
    private Button stopNumbersButton;
    private Button clearButton;
    private Button submitButton;

    private Button numberButton1;
    private Button numberButton2;
    private Button numberButton3;
    private Button numberButton4;
    private Button numberButton5;
    private Button numberButton6;

    private Button plusButton;
    private Button minusButton;
    private Button multiplyButton;
    private Button divideButton;
    private Button openBracketButton;
    private Button closeBracketButton;

    private FirestoreRepository firestoreRepository;
    private MojBrojService mojBrojService;
    private boolean remoteMode;
    private int lastRemoteStatsRound = -1;

    private SensorManager sensorManager;
    private Sensor accelerometer;

    private Runnable targetRoller;
    private Runnable numbersRoller;
    private Runnable autoStopTargetRunnable;
    private Runnable autoStopNumbersRunnable;

    private long lastShakeTimestamp;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_moj_broj, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        mojBrojService = new MojBrojService(
                new LocalGameRepository(),
                new MojBrojExpressionHelper()
        );
        firestoreRepository = new FirestoreRepository(requireContext());
        remoteMode = ((GameHostActivity) requireActivity()).isRemoteMatchMode();

        sensorManager = (SensorManager) requireContext().getSystemService(Context.SENSOR_SERVICE);

        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }

        roundOwnerText = view.findViewById(R.id.roundOwnerText);
        targetNumberText = view.findViewById(R.id.targetNumberText);
        activeInputText = view.findViewById(R.id.activeInputText);
        resultText = view.findViewById(R.id.resultText);
        playerOneExpressionTitle = view.findViewById(R.id.playerOneExpressionTitle);

        playerOneExpressionInput = view.findViewById(R.id.playerOneExpressionInput);
        playerTwoExpressionInput = view.findViewById(R.id.playerTwoExpressionInput);
        playerOneExpressionContainer = view.findViewById(R.id.playerOneExpressionContainer);
        playerTwoExpressionContainer = view.findViewById(R.id.playerTwoExpressionContainer);

        stopTargetButton = view.findViewById(R.id.stopTargetButton);
        stopNumbersButton = view.findViewById(R.id.stopNumbersButton);
        clearButton = view.findViewById(R.id.clearButton);
        submitButton = view.findViewById(R.id.submitButton);

        numberButton1 = view.findViewById(R.id.numberButton1);
        numberButton2 = view.findViewById(R.id.numberButton2);
        numberButton3 = view.findViewById(R.id.numberButton3);
        numberButton4 = view.findViewById(R.id.numberButton4);
        numberButton5 = view.findViewById(R.id.numberButton5);
        numberButton6 = view.findViewById(R.id.numberButton6);

        plusButton = view.findViewById(R.id.plusButton);
        minusButton = view.findViewById(R.id.minusButton);
        multiplyButton = view.findViewById(R.id.multiplyButton);
        divideButton = view.findViewById(R.id.divideButton);
        openBracketButton = view.findViewById(R.id.openBracketButton);
        closeBracketButton = view.findViewById(R.id.closeBracketButton);

        stopTargetButton.setOnClickListener(v -> stopTargetRolling());
        stopNumbersButton.setOnClickListener(v -> stopNumbersRolling());
        clearButton.setOnClickListener(v -> deleteLastToken());
        submitButton.setOnClickListener(v -> submitRoundEarly());

        setInputTracking(playerOneExpressionInput);
        setInputTracking(playerTwoExpressionInput);

        setAppendClick(numberButton1);
        setAppendClick(numberButton2);
        setAppendClick(numberButton3);
        setAppendClick(numberButton4);
        setAppendClick(numberButton5);
        setAppendClick(numberButton6);
        setAppendClick(plusButton);
        setAppendClick(minusButton);
        setAppendClick(multiplyButton);
        setAppendClick(divideButton);
        setAppendClick(openBracketButton);
        setAppendClick(closeBracketButton);

        if (remoteMode) {
            renderRemoteRound();
            return;
        }

        startRound(0);
    }

    private void startRound(int roundIndex) {
        mojBrojService.startRound(roundIndex);

        playerOneExpressionInput.setText("");
        playerTwoExpressionInput.setText("");
        targetNumberText.setText(getString(R.string.target_number_placeholder));
        resultText.setText(getString(R.string.my_number_rules_extended));
        playerOneExpressionTitle.setText(isChallengeMode()
                ? R.string.challenge_my_number_expression_title
                : R.string.my_number_player_one_expression_title);
        bindNumberPlaceholders();

        activeInput = isChallengeMode()
                ? playerOneExpressionInput
                : mojBrojService.getCurrentStarterPlayer() == 1
                ? playerOneExpressionInput
                : playerTwoExpressionInput;

        updateRoundHeader();
        updateActiveInputLabel();
        updateInputAvailability();
        updatePhaseText();
        updateNumberButtonStates();

        startTargetRolling();
        scheduleAutoStopTarget();
        startRoundTimer(ROUND_DURATION_SECONDS, this::handleRoundTimeout);
    }

    private void startTargetRolling() {
        targetRoller = new Runnable() {
            @Override
            public void run() {
                if (!isAdded() || mojBrojService.isTargetLocked() || mojBrojService.isRoundFinished()) {
                    return;
                }

                targetNumberText.setText(String.valueOf(mojBrojService.rollTargetNumber()));
                handler.postDelayed(this, 120L);
            }
        };

        handler.post(targetRoller);
    }

    private void startNumbersRolling() {
        numbersRoller = new Runnable() {
            @Override
            public void run() {
                if (!isAdded() || mojBrojService.isNumbersLocked() || mojBrojService.isRoundFinished()) {
                    return;
                }

                bindOfferedNumbers(mojBrojService.rollOfferedNumbers());
                handler.postDelayed(this, 180L);
            }
        };

        handler.post(numbersRoller);
    }

    private void scheduleAutoStopTarget() {
        autoStopTargetRunnable = this::stopTargetRolling;
        handler.postDelayed(autoStopTargetRunnable, AUTO_STOP_DELAY_MS);
    }

    private void scheduleAutoStopNumbers() {
        autoStopNumbersRunnable = this::stopNumbersRolling;
        handler.postDelayed(autoStopNumbersRunnable, AUTO_STOP_DELAY_MS);
    }

    private void stopTargetRolling() {
        if (remoteMode) {
            stopRemoteTargetRolling();
            return;
        }

        if (mojBrojService.isRoundFinished() || mojBrojService.isTargetLocked()) {
            return;
        }

        targetNumberText.setText(String.valueOf(mojBrojService.lockTarget()));

        if (targetRoller != null) {
            handler.removeCallbacks(targetRoller);
        }

        if (autoStopTargetRunnable != null) {
            handler.removeCallbacks(autoStopTargetRunnable);
        }

        startNumbersRolling();
        scheduleAutoStopNumbers();
        updateInputAvailability();
        updatePhaseText();
    }

    private void stopNumbersRolling() {
        if (remoteMode) {
            stopRemoteNumbersRolling();
            return;
        }

        if (mojBrojService.isRoundFinished() || mojBrojService.isNumbersLocked()) {
            return;
        }

        bindOfferedNumbers(mojBrojService.lockNumbers());

        if (numbersRoller != null) {
            handler.removeCallbacks(numbersRoller);
        }

        if (autoStopNumbersRunnable != null) {
            handler.removeCallbacks(autoStopNumbersRunnable);
        }

        updateInputAvailability();
        updatePhaseText();

        resultText.setText(getString(
                isChallengeMode()
                        ? R.string.challenge_my_number_ready_for_input_message
                        : R.string.my_number_ready_for_input_message
        ));
        updateNumberButtonStates();

        if (activeInput != null) {
            activeInput.requestFocus();
            activeInput.setSelection(activeInput.getText().length());
        }
    }

    private void handleRoundTimeout() {
        if (remoteMode) {
            handleRemoteRoundTimeout();
            return;
        }

        if (mojBrojService.isRoundFinished()) {
            return;
        }

        finishRound();
    }

    private void finishRound() {
        stopRoundTimer();
        stopRollingCallbacks();
        host().setTimerValue(0);

        MojBrojService.RoundOutcome roundOutcome = mojBrojService.finishRound(
                playerOneExpressionInput.getText().toString(),
                isChallengeMode() ? "" : playerTwoExpressionInput.getText().toString()
        );

        host().setScores(
                host().getPlayerOneScore() + roundOutcome.getPlayerOnePoints(),
                host().getPlayerTwoScore() + (isChallengeMode() ? 0 : roundOutcome.getPlayerTwoPoints())
        );

        if (host().shouldPersistStatistics()) {
            firestoreRepository.updateMojBrojStatistics(
                    roundOutcome.getPlayerOneStatisticsDifference(),
                    roundOutcome.getPlayerOnePoints()
            );
        }

        updateInputAvailability();
        host().setPhaseText(
                getString(
                        R.string.my_number_round_finished_phase_format,
                        mojBrojService.getCurrentRoundIndex() + 1
                )
        );
        resultText.setText(isChallengeMode()
                ? buildChallengeRoundSummary(
                roundOutcome.getPlayerOneResult(),
                roundOutcome.getPlayerOnePoints()
        )
                : buildRoundSummary(
                roundOutcome.getPlayerOneResult(),
                roundOutcome.getPlayerTwoResult(),
                roundOutcome.getPlayerOnePoints(),
                roundOutcome.getPlayerTwoPoints()
        ));

        Toast.makeText(requireContext(), getString(R.string.my_number_round_finished), Toast.LENGTH_SHORT)
                .show();

        resultText.postDelayed(() -> {
            if (!isAdded()) {
                return;
            }

            if (mojBrojService.getCurrentRoundIndex() == 0 && !isChallengeMode()) {
                startRound(1);
            } else {
                host().goToNextRound();
            }
        }, 2200L);
    }

    private void submitRoundEarly() {
        if (remoteMode) {
            submitRemoteRound();
            return;
        }

        if (mojBrojService.isRoundFinished()) {
            return;
        }

        if (!mojBrojService.isInputReady()) {
            Toast.makeText(
                    requireContext(),
                    getString(R.string.my_number_stop_both_first),
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        boolean playerOneEmpty = TextUtils.isEmpty(
                playerOneExpressionInput.getText().toString().trim()
        );
        boolean playerTwoEmpty = TextUtils.isEmpty(
                playerTwoExpressionInput.getText().toString().trim()
        );

        if (playerOneEmpty && playerTwoEmpty) {
            Toast.makeText(
                    requireContext(),
                    getString(
                            isChallengeMode()
                                    ? R.string.challenge_my_number_submit_without_input
                                    : R.string.my_number_submit_without_input
                    ),
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        finishRound();
    }

    private String buildRoundSummary(
            MojBrojService.EvaluatedExpression playerOneResult,
            MojBrojService.EvaluatedExpression playerTwoResult,
            int playerOnePoints,
            int playerTwoPoints
    ) {
        StringBuilder summary = new StringBuilder();
        summary.append(getString(
                R.string.my_number_round_summary_title,
                mojBrojService.getCurrentRoundIndex() + 1
        ));
        summary.append('\n');
        summary.append(
                getString(
                        R.string.my_number_round_target_format,
                        mojBrojService.getTargetNumber()
                )
        );
        summary.append('\n');
        summary.append(buildPlayerPreview(1, playerOneResult));
        summary.append('\n');
        summary.append(buildPlayerPreview(2, playerTwoResult));
        summary.append('\n');
        summary.append(
                getString(
                        R.string.my_number_round_points_format,
                        playerOnePoints,
                        playerTwoPoints
                )
        );

        return summary.toString();
    }

    private String buildChallengeRoundSummary(
            MojBrojService.EvaluatedExpression playerOneResult,
            int playerOnePoints
    ) {
        StringBuilder summary = new StringBuilder();
        summary.append(getString(
                R.string.my_number_round_summary_title,
                mojBrojService.getCurrentRoundIndex() + 1
        ));
        summary.append('\n');
        summary.append(getString(
                R.string.my_number_round_target_format,
                mojBrojService.getTargetNumber()
        ));
        summary.append('\n');
        summary.append(buildPlayerPreview(1, playerOneResult));
        summary.append('\n');
        summary.append(getString(R.string.challenge_points_won_format, playerOnePoints));
        return summary.toString();
    }

    private String buildPlayerPreview(int playerNumber, MojBrojService.EvaluatedExpression result) {
        if (isChallengeMode()) {
            if (!result.isEntered()) {
                return getString(R.string.challenge_my_number_preview_empty);
            }

            if (!result.isValid()) {
                return getString(
                        R.string.challenge_my_number_preview_invalid_format,
                        getValidationLabel(result.getErrorType())
                );
            }

            return getString(
                    R.string.challenge_my_number_preview_value_format,
                    result.getValue(),
                    result.getDifference()
            );
        }

        if (!result.isEntered()) {
            return getString(R.string.my_number_preview_empty_format, playerNumber);
        }

        if (!result.isValid()) {
            return getString(
                    R.string.my_number_preview_invalid_format,
                    playerNumber,
                    getValidationLabel(result.getErrorType())
            );
        }

        return getString(
                R.string.my_number_preview_value_format,
                playerNumber,
                result.getValue(),
                result.getDifference()
        );
    }

    private String getValidationLabel(MojBrojExpressionHelper.ErrorType errorType) {
        switch (errorType) {
            case INVALID_CHARACTERS:
                return getString(R.string.my_number_invalid_characters);
            case INVALID_NUMBERS:
                return getString(R.string.my_number_use_offered_numbers);
            case INVALID_EXPRESSION:
                return getString(R.string.my_number_invalid_expression);
            case EMPTY:
            default:
                return getString(R.string.my_number_enter_expression);
        }
    }

    private void setInputTracking(EditText input) {
        input.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                activeInput = input;
                updateActiveInputLabel();
                updateInputAvailability();
            }
        });

        input.setOnClickListener(v -> {
            activeInput = input;
            updateActiveInputLabel();
            updateInputAvailability();
        });

        input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // No-op.
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // No-op.
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (activeInput == input) {
                    updateNumberButtonStates();
                }
            }
        });
    }

    private void setAppendClick(Button button) {
        button.setOnClickListener(v -> {
            if (!isInputReady()) {
                Toast.makeText(
                        requireContext(),
                    getString(R.string.my_number_stop_both_first),
                    Toast.LENGTH_SHORT
                ).show();
                return;
            }

            if (activeInput == null) {
                activeInput = mojBrojService.getCurrentStarterPlayer() == 1
                        ? playerOneExpressionInput
                        : playerTwoExpressionInput;
            }

            String value = button.getText().toString();

            if (!TextUtils.isEmpty(value)
                    && !value.equals(getString(R.string.number_placeholder))) {
                activeInput.append(value);
                activeInput.setSelection(activeInput.getText().length());
                updateNumberButtonStates();
            }
        });
    }

    private void deleteLastToken() {
        if (!isInputReady() || activeInput == null) {
            return;
        }

        String expression = activeInput.getText().toString();

        if (expression.isEmpty()) {
            return;
        }

        int endIndex = expression.length();

        while (endIndex > 0 && Character.isWhitespace(expression.charAt(endIndex - 1))) {
            endIndex--;
        }

        if (endIndex == 0) {
            activeInput.setText("");
            updateNumberButtonStates();
            return;
        }

        int startIndex = endIndex - 1;

        if (Character.isDigit(expression.charAt(startIndex))) {
            while (startIndex >= 0 && Character.isDigit(expression.charAt(startIndex))) {
                startIndex--;
            }

            startIndex++;
        }

        String updatedExpression = expression.substring(0, startIndex);
        activeInput.setText(updatedExpression);
        activeInput.setSelection(updatedExpression.length());
        updateNumberButtonStates();
    }

    private boolean isInputReady() {
        if (remoteMode) {
            GameHostActivity activity = (GameHostActivity) requireActivity();
            SharedMatchState state = activity.getSharedMatchState();
            return state != null
                    && SharedMatchState.PHASE_MB_ENTRY.equals(state.phase)
                    && (state.activePlayer == 0 || activity.getLocalPlayerNumber() == state.activePlayer)
                    && !isRemoteSubmissionLocked(state);
        }

        return mojBrojService.isInputReady();
    }

    private void updateRoundHeader() {
        if (isChallengeMode()) {
            roundOwnerText.setText(getString(
                    R.string.challenge_my_number_round_owner_format,
                    mojBrojService.getCurrentRoundIndex() + 1
            ));
            return;
        }

        roundOwnerText.setText(getString(
                R.string.my_number_round_owner_format,
                mojBrojService.getCurrentRoundIndex() + 1,
                mojBrojService.getCurrentStarterPlayer()
        ));
    }

    private void updateActiveInputLabel() {
        if (isChallengeMode()) {
            activeInputText.setText(R.string.challenge_active_input_label);
            return;
        }

        int activePlayerNumber = activeInput == playerTwoExpressionInput ? 2 : 1;

        activeInputText.setText(
                getString(R.string.my_number_active_input_format, activePlayerNumber)
        );
    }

    private void updatePhaseText() {
        if (isChallengeMode()) {
            switch (mojBrojService.getPhase()) {
                case FINISHED:
                    host().setPhaseText(getString(R.string.my_number_finished_phase));
                    return;
                case LOCK_TARGET:
                    host().setPhaseText(getString(
                            R.string.challenge_my_number_lock_target_phase,
                            mojBrojService.getCurrentRoundIndex() + 1
                    ));
                    return;
                case LOCK_NUMBERS:
                    host().setPhaseText(getString(
                            R.string.challenge_my_number_lock_numbers_phase,
                            mojBrojService.getCurrentRoundIndex() + 1
                    ));
                    return;
                case ENTER_EXPRESSIONS:
                default:
                    host().setPhaseText(getString(
                            R.string.challenge_my_number_round_phase,
                            mojBrojService.getCurrentRoundIndex() + 1
                    ));
                    return;
            }
        }

        switch (mojBrojService.getPhase()) {
            case FINISHED:
                host().setPhaseText(getString(R.string.my_number_finished_phase));
                return;
            case LOCK_TARGET:
                host().setPhaseText(
                        getString(
                                R.string.my_number_lock_target_phase,
                                mojBrojService.getCurrentRoundIndex() + 1,
                                mojBrojService.getCurrentStarterPlayer()
                        )
                );
                return;
            case LOCK_NUMBERS:
                host().setPhaseText(
                        getString(
                                R.string.my_number_lock_numbers_phase,
                                mojBrojService.getCurrentRoundIndex() + 1,
                                mojBrojService.getCurrentStarterPlayer()
                        )
                );
                return;
            case ENTER_EXPRESSIONS:
            default:
                host().setPhaseText(
                        getString(
                                R.string.my_number_round_phase_format,
                                mojBrojService.getCurrentRoundIndex() + 1,
                                mojBrojService.getCurrentStarterPlayer()
                        )
                );
        }
    }

    private void updateInputAvailability() {
        boolean inputEnabled = isInputReady();

        if (remoteMode) {
            GameHostActivity activity = (GameHostActivity) requireActivity();
            SharedMatchState state = activity.getSharedMatchState();
            boolean localTurn = state != null && activity.getLocalPlayerNumber() == state.activePlayer;
            boolean targetPhase = state != null && SharedMatchState.PHASE_MB_TARGET.equals(state.phase);
            boolean numbersPhase = state != null && SharedMatchState.PHASE_MB_NUMBERS.equals(state.phase);
            boolean entryPhase = state != null && SharedMatchState.PHASE_MB_ENTRY.equals(state.phase);
            boolean donePhase = state != null && SharedMatchState.PHASE_MB_DONE.equals(state.phase);
            boolean submissionLocked = state != null && isRemoteSubmissionLocked(state);
            int localPlayer = activity.getLocalPlayerNumber();
            boolean expressionEnabled = entryPhase && !submissionLocked;

            stopTargetButton.setEnabled(localTurn && targetPhase);
            stopNumbersButton.setEnabled(localTurn && numbersPhase);

            numberButton1.setEnabled(expressionEnabled);
            numberButton2.setEnabled(expressionEnabled);
            numberButton3.setEnabled(expressionEnabled);
            numberButton4.setEnabled(expressionEnabled);
            numberButton5.setEnabled(expressionEnabled);
            numberButton6.setEnabled(expressionEnabled);

            plusButton.setEnabled(expressionEnabled);
            minusButton.setEnabled(expressionEnabled);
            multiplyButton.setEnabled(expressionEnabled);
            divideButton.setEnabled(expressionEnabled);
            openBracketButton.setEnabled(expressionEnabled);
            closeBracketButton.setEnabled(expressionEnabled);

            playerOneExpressionInput.setEnabled(expressionEnabled && localPlayer == 1);
            playerTwoExpressionInput.setEnabled(expressionEnabled && localPlayer == 2);

            if (targetPhase || numbersPhase) {
                playerOneExpressionContainer.setVisibility(View.GONE);
                playerTwoExpressionContainer.setVisibility(View.GONE);
            } else {
                playerOneExpressionContainer.setVisibility((localPlayer == 1 || donePhase) ? View.VISIBLE : View.GONE);
                playerTwoExpressionContainer.setVisibility((localPlayer == 2 || donePhase) ? View.VISIBLE : View.GONE);
            }

            clearButton.setVisibility(submissionLocked ? View.GONE : View.VISIBLE);
            clearButton.setEnabled(expressionEnabled
                    && !submissionLocked
                    && activeInput != null
                    && activeInput.length() > 0);
            submitButton.setEnabled(expressionEnabled && !submissionLocked);
            updateNumberButtonStates();
            return;
        }

        stopTargetButton.setEnabled(
                !mojBrojService.isRoundFinished() && !mojBrojService.isTargetLocked()
        );
        stopNumbersButton.setEnabled(
                !mojBrojService.isRoundFinished()
                        && mojBrojService.isTargetLocked()
                        && !mojBrojService.isNumbersLocked()
        );

        numberButton1.setEnabled(inputEnabled);
        numberButton2.setEnabled(inputEnabled);
        numberButton3.setEnabled(inputEnabled);
        numberButton4.setEnabled(inputEnabled);
        numberButton5.setEnabled(inputEnabled);
        numberButton6.setEnabled(inputEnabled);

        plusButton.setEnabled(inputEnabled);
        minusButton.setEnabled(inputEnabled);
        multiplyButton.setEnabled(inputEnabled);
        divideButton.setEnabled(inputEnabled);
        openBracketButton.setEnabled(inputEnabled);
        closeBracketButton.setEnabled(inputEnabled);

        playerOneExpressionInput.setEnabled(inputEnabled);
        playerTwoExpressionInput.setEnabled(inputEnabled);

        // Visibility logic for local/remote mode
        if (remoteMode) {
            GameHostActivity activity = (GameHostActivity) requireActivity();
            SharedMatchState state = activity.getSharedMatchState();
            int localPlayer = activity.getLocalPlayerNumber();
            boolean isDone = state != null && SharedMatchState.PHASE_MB_DONE.equals(state.phase);

            // Hide everything during rolling phases
            if (state != null && (SharedMatchState.PHASE_MB_TARGET.equals(state.phase) || SharedMatchState.PHASE_MB_NUMBERS.equals(state.phase))) {
                playerOneExpressionContainer.setVisibility(View.GONE);
                playerTwoExpressionContainer.setVisibility(View.GONE);
            } else {
                // Entry or Done: Show local player's field, or both if done
                playerOneExpressionContainer.setVisibility((localPlayer == 1 || isDone) ? View.VISIBLE : View.GONE);
                playerTwoExpressionContainer.setVisibility((localPlayer == 2 || isDone) ? View.VISIBLE : View.GONE);
            }
        } else {
            // Local mode
            MojBrojService.Phase phase = mojBrojService.getPhase();
            if (isChallengeMode()) {
                if (phase == MojBrojService.Phase.ENTER_EXPRESSIONS || phase == MojBrojService.Phase.FINISHED) {
                    playerOneExpressionContainer.setVisibility(View.VISIBLE);
                } else {
                    playerOneExpressionContainer.setVisibility(View.GONE);
                }
                playerTwoExpressionContainer.setVisibility(View.GONE);
            } else if (phase == MojBrojService.Phase.ENTER_EXPRESSIONS) {
                playerOneExpressionContainer.setVisibility(View.VISIBLE);
                playerTwoExpressionContainer.setVisibility(View.VISIBLE);
            } else if (phase == MojBrojService.Phase.FINISHED) {
                playerOneExpressionContainer.setVisibility(View.VISIBLE);
                playerTwoExpressionContainer.setVisibility(View.VISIBLE);
            } else {
                // Rolling: Hide both
                playerOneExpressionContainer.setVisibility(View.GONE);
                playerTwoExpressionContainer.setVisibility(View.GONE);
            }
        }

        clearButton.setEnabled(inputEnabled && activeInput != null && activeInput.length() > 0);
        submitButton.setEnabled(inputEnabled);
        updateNumberButtonStates();
    }

    private void bindNumberPlaceholders() {
        numberButton1.setText(R.string.number_placeholder);
        numberButton2.setText(R.string.number_placeholder);
        numberButton3.setText(R.string.number_placeholder);
        numberButton4.setText(R.string.number_placeholder);
        numberButton5.setText(R.string.number_placeholder);
        numberButton6.setText(R.string.number_placeholder);
    }

    private void bindOfferedNumbers(int[] offeredNumbers) {
        numberButton1.setText(String.valueOf(offeredNumbers[0]));
        numberButton2.setText(String.valueOf(offeredNumbers[1]));
        numberButton3.setText(String.valueOf(offeredNumbers[2]));
        numberButton4.setText(String.valueOf(offeredNumbers[3]));
        numberButton5.setText(String.valueOf(offeredNumbers[4]));
        numberButton6.setText(String.valueOf(offeredNumbers[5]));
        updateNumberButtonStates();
    }

    private void updateNumberButtonStates() {
        boolean inputEnabled = isInputReady();
        Map<Integer, Integer> usedCounts = getUsedCountsForActiveExpression();

        for (Button button : getNumberButtons()) {
            String text = button.getText().toString();
            boolean placeholder = text.equals(getString(R.string.number_placeholder));

            if (!inputEnabled || placeholder) {
                button.setEnabled(false);
                button.setAlpha(DISABLED_NUMBER_ALPHA);
                continue;
            }

            int numberValue = Integer.parseInt(text);
            Integer usedCount = usedCounts.get(numberValue);
            int remainingUsedCount = usedCount != null ? usedCount : 0;

            if (remainingUsedCount > 0) {
                usedCounts.put(numberValue, remainingUsedCount - 1);
                button.setEnabled(false);
                button.setAlpha(USED_NUMBER_ALPHA);
            } else {
                button.setEnabled(true);
                button.setAlpha(1f);
            }
        }

        clearButton.setEnabled(inputEnabled && activeInput != null && activeInput.length() > 0);
    }

    private Map<Integer, Integer> getUsedCountsForActiveExpression() {
        Map<Integer, Integer> usedCounts = new HashMap<>();

        if (activeInput == null) {
            return usedCounts;
        }

        Matcher matcher = NUMBER_PATTERN.matcher(activeInput.getText().toString());

        while (matcher.find()) {
            int number = Integer.parseInt(matcher.group());
            Integer current = usedCounts.get(number);
            usedCounts.put(number, (current != null ? current : 0) + 1);
        }

        return usedCounts;
    }

    private Button[] getNumberButtons() {
        return new Button[]{
                numberButton1,
                numberButton2,
                numberButton3,
                numberButton4,
                numberButton5,
                numberButton6
        };
    }

    private void stopRollingCallbacks() {
        if (targetRoller != null) {
            handler.removeCallbacks(targetRoller);
        }

        if (numbersRoller != null) {
            handler.removeCallbacks(numbersRoller);
        }

        if (autoStopTargetRunnable != null) {
            handler.removeCallbacks(autoStopTargetRunnable);
        }

        if (autoStopNumbersRunnable != null) {
            handler.removeCallbacks(autoStopNumbersRunnable);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (sensorManager != null && accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    public void onPause() {
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }

        super.onPause();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (mojBrojService.isRoundFinished() || event.sensor.getType() != Sensor.TYPE_ACCELEROMETER) {
            return;
        }

        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];
        float acceleration = (float) Math.sqrt(x * x + y * y + z * z) - SensorManager.GRAVITY_EARTH;

        if (Math.abs(acceleration) < SHAKE_THRESHOLD) {
            return;
        }

        long now = System.currentTimeMillis();

        if (now - lastShakeTimestamp < SHAKE_COOLDOWN_MS) {
            return;
        }

        lastShakeTimestamp = now;

        if (!mojBrojService.isTargetLocked()) {
            stopTargetRolling();
            return;
        }

        if (!mojBrojService.isNumbersLocked()) {
            stopNumbersRolling();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // No-op for this game.
    }

    @Override
    public void onDestroyView() {
        stopRollingCallbacks();
        super.onDestroyView();
    }

    private void renderRemoteRound() {
        stopRoundTimer();
        stopRollingCallbacks();

        GameHostActivity activity = (GameHostActivity) requireActivity();
        SharedMatchState state = activity.getSharedMatchState();
        SharedMojBrojRound round = state == null ? null : activity.getSharedMojBrojRound(state.currentTurnIndex);
        int localPlayer = activity.getLocalPlayerNumber();

        if (state == null || round == null) {
            return;
        }

        if (maybeContinueAfterForfeit(activity, state, round)) {
            return;
        }

        roundOwnerText.setText(
                getString(
                        R.string.my_number_round_owner_format,
                        state.currentTurnIndex + 1,
                        state.currentTurnIndex == 0 ? 1 : 2
                )
        );

        bindRemoteExpressions(state);
        activeInput = localPlayer == 2 ? playerTwoExpressionInput : playerOneExpressionInput;
        activeInputText.setText(getString(R.string.my_number_active_input_format, localPlayer));
        updateRemoteSubmitButtonLabel(state);

        host().setPhaseText(state.phaseMessage);
        host().setTimerValue(getRemoteRemainingSeconds(state));

        if (SharedMatchState.PHASE_MB_TARGET.equals(state.phase)) {
            targetNumberText.setText(getString(R.string.target_number_placeholder));
            bindNumberPlaceholders();
            startRemoteTargetAnimation();
            updateRemoteInputAvailability(false, false, false);
            updateInputAvailability();
            startRemoteTimer(state);
            resultText.setText(getString(R.string.my_number_lock_target_phase,
                    state.currentTurnIndex + 1,
                    state.currentTurnIndex == 0 ? 1 : 2));
            return;
        }

        if (SharedMatchState.PHASE_MB_NUMBERS.equals(state.phase)) {
            targetNumberText.setText(String.valueOf(round.targetNumber));
            startRemoteNumbersAnimation();
            updateRemoteInputAvailability(false, true, false);
            updateInputAvailability();
            startRemoteTimer(state);
            resultText.setText(getString(R.string.my_number_lock_numbers_phase,
                    state.currentTurnIndex + 1,
                    state.currentTurnIndex == 0 ? 1 : 2));
            return;
        }

        targetNumberText.setText(String.valueOf(round.targetNumber));
        bindRemoteOfferedNumbers(round);

        if (SharedMatchState.PHASE_MB_ENTRY.equals(state.phase)) {
            if (areBothRemoteExpressionsSubmitted(state) && activity.isRemoteProgressCoordinator()) {
                finalizeRemoteExpressionRound(state, round, null,
                        state.playerOneExpression == null ? "" : state.playerOneExpression,
                        state.playerTwoExpression == null ? "" : state.playerTwoExpression);
                return;
            }

            updateRemoteInputAvailability(true, false, true);
            updateInputAvailability();
            startRemoteTimer(state);
            resultText.setText(getString(R.string.my_number_ready_for_input_message));
            updateNumberButtonStates();
            return;
        }

        if (SharedMatchState.PHASE_MB_DONE.equals(state.phase)) {
            updateRemoteInputAvailability(true, false, false);
            updateInputAvailability();
            resultText.setText(buildRemoteMyNumberSummary(state, round));
            maybePersistRemoteMojBrojStatistics(state, round);
            scheduleRemoteMyNumberAdvanceIfCoordinator(state);
            return;
        }
    }

    private boolean maybeContinueAfterForfeit(
            GameHostActivity activity,
            SharedMatchState state,
            SharedMojBrojRound round
    ) {
        if (!activity.hasOpponentForfeited() || state.activePlayer != state.forfeitedPlayer) {
            return false;
        }

        int continuingPlayer = activity.getRemainingRemotePlayerNumber();
        int starterPlayer = state.currentTurnIndex == 0 ? 1 : 2;

        if (SharedMatchState.PHASE_MB_TARGET.equals(state.phase)) {
            Map<String, Object> updates = new HashMap<>();
            updates.put("phase", SharedMatchState.PHASE_MB_NUMBERS);
            updates.put("activePlayer", continuingPlayer);
            updates.put("phaseStartedAt", System.currentTimeMillis());
            updates.put("phaseDurationSeconds", 5);
            updates.put("phaseMessage", getString(R.string.shared_match_mb_numbers_phase_format, continuingPlayer));
            activity.updateSharedMatch(updates);
            return true;
        }

        if (SharedMatchState.PHASE_MB_NUMBERS.equals(state.phase)) {
            Map<String, Object> updates = new HashMap<>();
            updates.put("phase", SharedMatchState.PHASE_MB_ENTRY);
            updates.put("activePlayer", 0);
            updates.put("phaseStartedAt", System.currentTimeMillis());
            updates.put("phaseDurationSeconds", ROUND_DURATION_SECONDS);
            updates.put("phaseMessage", getString(
                    R.string.shared_match_mb_both_entry_phase_format,
                    state.currentTurnIndex + 1
            ));
            activity.updateSharedMatch(updates);
            return true;
        }

        if (SharedMatchState.PHASE_MB_ENTRY.equals(state.phase)) {
            finalizeRemoteExpressionRound(
                    state,
                    round,
                    null,
                    state.playerOneExpression == null ? "" : state.playerOneExpression,
                    state.playerTwoExpression == null ? "" : state.playerTwoExpression
            );
            return true;
        }

        return false;
    }

    private void bindRemoteExpressions(SharedMatchState state) {
        GameHostActivity activity = (GameHostActivity) requireActivity();
        boolean revealBothExpressions = SharedMatchState.PHASE_MB_DONE.equals(state.phase);
        int localPlayer = activity.getLocalPlayerNumber();

        String playerOneExpression = state.playerOneExpression == null ? "" : state.playerOneExpression;
        String playerTwoExpression = state.playerTwoExpression == null ? "" : state.playerTwoExpression;

        if (!revealBothExpressions) {
            if (localPlayer == 1) {
                playerTwoExpression = "";
            } else {
                playerOneExpression = "";
            }
        }

        if (!TextUtils.equals(playerOneExpressionInput.getText(), playerOneExpression)) {
            playerOneExpressionInput.setText(playerOneExpression);
        }

        if (!TextUtils.equals(playerTwoExpressionInput.getText(), playerTwoExpression)) {
            playerTwoExpressionInput.setText(playerTwoExpression);
        }
    }

    private void updateRemoteSubmitButtonLabel(SharedMatchState state) {
        if (isRemoteSubmissionLocked(state)) {
            submitButton.setText(R.string.my_number_submitted);
            return;
        }

        submitButton.setText(R.string.my_number_preview_round);
    }

    private boolean isRemoteSubmissionLocked(SharedMatchState state) {
        GameHostActivity activity = (GameHostActivity) requireActivity();
        int localPlayer = activity.getLocalPlayerNumber();
        boolean hasSubmitted = localPlayer == 1
                ? !TextUtils.isEmpty(state.playerOneExpression)
                : !TextUtils.isEmpty(state.playerTwoExpression);

        return hasSubmitted
                && (SharedMatchState.PHASE_MB_DONE.equals(state.phase)
                || (SharedMatchState.PHASE_MB_ENTRY.equals(state.phase)
                && state.activePlayer != localPlayer));
    }

    private void startRemoteTargetAnimation() {
        Random random = new Random();
        targetRoller = new Runnable() {
            @Override
            public void run() {
                if (!isAdded()) {
                    return;
                }

                SharedMatchState state = ((GameHostActivity) requireActivity()).getSharedMatchState();
                if (state == null || !SharedMatchState.PHASE_MB_TARGET.equals(state.phase)) {
                    return;
                }

                targetNumberText.setText(String.valueOf(100 + random.nextInt(900)));
                handler.postDelayed(this, 120L);
            }
        };
        handler.post(targetRoller);
    }

    private void startRemoteNumbersAnimation() {
        Random random = new Random();
        numbersRoller = new Runnable() {
            @Override
            public void run() {
                if (!isAdded()) {
                    return;
                }

                SharedMatchState state = ((GameHostActivity) requireActivity()).getSharedMatchState();
                if (state == null || !SharedMatchState.PHASE_MB_NUMBERS.equals(state.phase)) {
                    return;
                }

                bindOfferedNumbers(new int[]{
                        random.nextInt(9) + 1,
                        random.nextInt(9) + 1,
                        random.nextInt(9) + 1,
                        random.nextInt(9) + 1,
                        new int[]{10, 15, 20}[random.nextInt(3)],
                        new int[]{25, 50, 75, 100}[random.nextInt(4)]
                });
                handler.postDelayed(this, 180L);
            }
        };
        handler.post(numbersRoller);
    }

    private void stopRemoteTargetRolling() {
        GameHostActivity activity = (GameHostActivity) requireActivity();
        SharedMatchState state = activity.getSharedMatchState();

        if (state == null
                || !SharedMatchState.PHASE_MB_TARGET.equals(state.phase)
                || activity.getLocalPlayerNumber() != state.activePlayer) {
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("phase", SharedMatchState.PHASE_MB_NUMBERS);
        updates.put("phaseStartedAt", System.currentTimeMillis());
        updates.put("phaseDurationSeconds", 5);
        updates.put("phaseMessage", getString(R.string.shared_match_mb_numbers_phase_format, state.activePlayer));
        activity.updateSharedMatch(updates);
    }

    private void stopRemoteNumbersRolling() {
        GameHostActivity activity = (GameHostActivity) requireActivity();
        SharedMatchState state = activity.getSharedMatchState();

        if (state == null
                || !SharedMatchState.PHASE_MB_NUMBERS.equals(state.phase)
                || activity.getLocalPlayerNumber() != state.activePlayer) {
            return;
        }

        int starterPlayer = state.currentTurnIndex == 0 ? 1 : 2;
        Map<String, Object> updates = new HashMap<>();
        updates.put("phase", SharedMatchState.PHASE_MB_ENTRY);
        updates.put("activePlayer", 0);
        updates.put("phaseStartedAt", System.currentTimeMillis());
        updates.put("phaseDurationSeconds", ROUND_DURATION_SECONDS);
        updates.put("phaseMessage", getString(
                R.string.shared_match_mb_both_entry_phase_format,
                state.currentTurnIndex + 1
        ));
        activity.updateSharedMatch(updates);
    }

    private void handleRemoteRoundTimeout() {
        GameHostActivity activity = (GameHostActivity) requireActivity();
        SharedMatchState state = activity.getSharedMatchState();

        if (state == null) {
            return;
        }

        if (SharedMatchState.PHASE_MB_TARGET.equals(state.phase)) {
            if (activity.getLocalPlayerNumber() != state.activePlayer) {
                return;
            }
            stopRemoteTargetRolling();
            return;
        }

        if (SharedMatchState.PHASE_MB_NUMBERS.equals(state.phase)) {
            if (activity.getLocalPlayerNumber() != state.activePlayer) {
                return;
            }
            stopRemoteNumbersRolling();
            return;
        }

        if (SharedMatchState.PHASE_MB_ENTRY.equals(state.phase)) {
            if (activity.isRemoteProgressCoordinator()) {
                commitRemoteExpressionAndAdvance(true);
            }
        }
    }

    private void submitRemoteRound() {
        GameHostActivity activity = (GameHostActivity) requireActivity();
        SharedMatchState state = activity.getSharedMatchState();

        if (state == null
                || !SharedMatchState.PHASE_MB_ENTRY.equals(state.phase)
                || isRemoteSubmissionLocked(state)) {
            return;
        }

        commitRemoteExpressionAndAdvance(false);
    }

    private void commitRemoteExpressionAndAdvance(boolean forceFinalize) {
        GameHostActivity activity = (GameHostActivity) requireActivity();
        SharedMatchState state = activity.getSharedMatchState();
        SharedMojBrojRound round = state == null ? null : activity.getSharedMojBrojRound(state.currentTurnIndex);

        if (state == null || round == null || !SharedMatchState.PHASE_MB_ENTRY.equals(state.phase)) {
            return;
        }

        int localPlayer = activity.getLocalPlayerNumber();
        String playerOneExpression = state.playerOneExpression == null ? "" : state.playerOneExpression;
        String playerTwoExpression = state.playerTwoExpression == null ? "" : state.playerTwoExpression;

        String localExpression = localPlayer == 1
                ? playerOneExpressionInput.getText().toString().trim()
                : playerTwoExpressionInput.getText().toString().trim();

        if (!forceFinalize && TextUtils.isEmpty(localExpression)) {
            Toast.makeText(requireContext(), R.string.my_number_enter_expression, Toast.LENGTH_SHORT).show();
            return;
        }

        if (localPlayer == 1) {
            if (!TextUtils.isEmpty(localExpression)) {
                playerOneExpression = localExpression;
            }
        } else {
            if (!TextUtils.isEmpty(localExpression)) {
                playerTwoExpression = localExpression;
            }
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("playerOneExpression", playerOneExpression);
        updates.put("playerTwoExpression", playerTwoExpression);

        boolean bothSubmitted = !TextUtils.isEmpty(playerOneExpression)
                && !TextUtils.isEmpty(playerTwoExpression);

        if (!bothSubmitted && !forceFinalize) {
            activity.updateSharedMatch(updates);
            return;
        }

        finalizeRemoteExpressionRound(state, round, updates, playerOneExpression, playerTwoExpression);
    }

    private void scheduleRemoteMyNumberAdvanceIfCoordinator(SharedMatchState state) {
        GameHostActivity activity = (GameHostActivity) requireActivity();

        if (!activity.isRemoteProgressCoordinator()) {
            return;
        }

        resultText.postDelayed(() -> {
            if (!isAdded()) {
                return;
            }

            SharedMatchState currentState = activity.getSharedMatchState();
            if (currentState == null
                    || currentState.currentTurnIndex != state.currentTurnIndex
                    || !SharedMatchState.PHASE_MB_DONE.equals(currentState.phase)) {
                return;
            }

            if (currentState.currentTurnIndex == 0) {
                Map<String, Object> updates = new HashMap<>();
                updates.put("currentTurnIndex", 1);
                updates.put("phase", SharedMatchState.PHASE_MB_TARGET);
                updates.put("activePlayer", 2);
                updates.put("phaseStartedAt", System.currentTimeMillis());
                updates.put("phaseDurationSeconds", 5);
                updates.put("phaseMessage", getString(R.string.shared_match_mb_round_two_phase));
                updates.put("playerOneExpression", "");
                updates.put("playerTwoExpression", "");
                activity.updateSharedMatch(updates);
            } else {
                activity.goToNextRound();
            }
        }, 2200L);
    }

    private void updateRemoteInputAvailability(
            boolean inputPhase,
            boolean numbersPhase,
            boolean entryPhase
    ) {
        GameHostActivity activity = (GameHostActivity) requireActivity();
        SharedMatchState state = activity.getSharedMatchState();
        boolean localTurn = state != null && activity.getLocalPlayerNumber() == state.activePlayer;
        boolean submissionLocked = state != null && isRemoteSubmissionLocked(state);
        int localPlayer = activity.getLocalPlayerNumber();
        boolean expressionEnabled = entryPhase && !submissionLocked;

        stopTargetButton.setEnabled(localTurn && inputPhase && SharedMatchState.PHASE_MB_TARGET.equals(state.phase));
        stopNumbersButton.setEnabled(localTurn && numbersPhase && SharedMatchState.PHASE_MB_NUMBERS.equals(state.phase));
        numberButton1.setEnabled(expressionEnabled);
        numberButton2.setEnabled(expressionEnabled);
        numberButton3.setEnabled(expressionEnabled);
        numberButton4.setEnabled(expressionEnabled);
        numberButton5.setEnabled(expressionEnabled);
        numberButton6.setEnabled(expressionEnabled);
        plusButton.setEnabled(expressionEnabled);
        minusButton.setEnabled(expressionEnabled);
        multiplyButton.setEnabled(expressionEnabled);
        divideButton.setEnabled(expressionEnabled);
        openBracketButton.setEnabled(expressionEnabled);
        closeBracketButton.setEnabled(expressionEnabled);
        clearButton.setEnabled(expressionEnabled);
        submitButton.setEnabled(expressionEnabled);

        clearButton.setVisibility(submissionLocked ? View.GONE : View.VISIBLE);
        if (submissionLocked) {
            clearButton.setEnabled(false);
        }

        playerOneExpressionInput.setEnabled(expressionEnabled && localPlayer == 1);
        playerTwoExpressionInput.setEnabled(expressionEnabled && localPlayer == 2);
    }

    private void startRemoteTimer(SharedMatchState state) {
        int remainingSeconds = getRemoteRemainingSeconds(state);
        if (remainingSeconds <= 0) {
            return;
        }

        startRoundTimer(remainingSeconds, null, this::handleRemoteRoundTimeout);
    }

    private int getRemoteRemainingSeconds(SharedMatchState state) {
        if (state == null || state.phaseDurationSeconds <= 0) {
            return 0;
        }

        long elapsedMs = System.currentTimeMillis() - state.phaseStartedAt;
        int remaining = state.phaseDurationSeconds - (int) Math.floor(elapsedMs / 1000d);
        return Math.max(0, remaining);
    }

    private void bindRemoteOfferedNumbers(SharedMojBrojRound round) {
        bindOfferedNumbers(toIntArray(round.offeredNumbers));
    }

    private int[] toIntArray(java.util.List<Integer> values) {
        int[] result = new int[values == null ? 0 : values.size()];
        if (values == null) {
            return result;
        }

        for (int index = 0; index < values.size(); index++) {
            result[index] = values.get(index);
        }

        return result;
    }

    private String buildRemoteMyNumberSummary(SharedMatchState state, SharedMojBrojRound round) {
        MojBrojService summaryService = new MojBrojService(
                new LocalGameRepository(),
                new MojBrojExpressionHelper()
        );
        summaryService.startRound(state.currentTurnIndex);
        summaryService.setPreparedRoundData(round.targetNumber, toIntArray(round.offeredNumbers));
        MojBrojService.RoundOutcome outcome = summaryService.preview(
                state.playerOneExpression,
                state.playerTwoExpression
        );

        return buildRoundSummary(
                outcome.getPlayerOneResult(),
                outcome.getPlayerTwoResult(),
                outcome.getPlayerOnePoints(),
                outcome.getPlayerTwoPoints()
        );
    }

    private boolean areBothRemoteExpressionsSubmitted(SharedMatchState state) {
        return state != null
                && !TextUtils.isEmpty(state.playerOneExpression)
                && !TextUtils.isEmpty(state.playerTwoExpression);
    }

    private void finalizeRemoteExpressionRound(
            SharedMatchState state,
            SharedMojBrojRound round,
            Map<String, Object> updates,
            String playerOneExpression,
            String playerTwoExpression
    ) {
        GameHostActivity activity = (GameHostActivity) requireActivity();
        Map<String, Object> finalUpdates = updates == null ? new HashMap<>() : updates;

        MojBrojService outcomeService = new MojBrojService(
                new LocalGameRepository(),
                new MojBrojExpressionHelper()
        );
        outcomeService.startRound(state.currentTurnIndex);
        outcomeService.setPreparedRoundData(round.targetNumber, toIntArray(round.offeredNumbers));
        MojBrojService.RoundOutcome outcome = outcomeService.finishRound(playerOneExpression, playerTwoExpression);

        finalUpdates.put("phase", SharedMatchState.PHASE_MB_DONE);
        finalUpdates.put("activePlayer", 0);
        finalUpdates.put("phaseStartedAt", System.currentTimeMillis());
        finalUpdates.put("phaseDurationSeconds", 2);
        finalUpdates.put("playerOneScore", state.playerOneScore + outcome.getPlayerOnePoints());
        finalUpdates.put("playerTwoScore", state.playerTwoScore + outcome.getPlayerTwoPoints());
        finalUpdates.put("phaseMessage", getString(
                R.string.shared_match_mb_done_phase_format,
                outcome.getPlayerOnePoints(),
                outcome.getPlayerTwoPoints()
        ));
        activity.updateSharedMatch(finalUpdates);
    }

    private void maybePersistRemoteMojBrojStatistics(SharedMatchState state, SharedMojBrojRound round) {
        if (!host().shouldPersistStatistics()
                || state == null
                || round == null
                || state.currentTurnIndex == lastRemoteStatsRound) {
            return;
        }

        lastRemoteStatsRound = state.currentTurnIndex;

        MojBrojService summaryService = new MojBrojService(
                new LocalGameRepository(),
                new MojBrojExpressionHelper()
        );
        summaryService.startRound(state.currentTurnIndex);
        summaryService.setPreparedRoundData(round.targetNumber, toIntArray(round.offeredNumbers));
        MojBrojService.RoundOutcome outcome = summaryService.finishRound(
                state.playerOneExpression,
                state.playerTwoExpression
        );

        GameHostActivity activity = (GameHostActivity) requireActivity();
        if (activity.getLocalPlayerNumber() == 1) {
            firestoreRepository.updateMojBrojStatistics(
                    outcome.getPlayerOneResult().hasResult()
                            ? outcome.getPlayerOneResult().getDifference()
                            : round.targetNumber,
                    outcome.getPlayerOnePoints()
            );
            return;
        }

        firestoreRepository.updateMojBrojStatistics(
                outcome.getPlayerTwoResult().hasResult()
                        ? outcome.getPlayerTwoResult().getDifference()
                        : round.targetNumber,
                outcome.getPlayerTwoPoints()
        );
    }
}
