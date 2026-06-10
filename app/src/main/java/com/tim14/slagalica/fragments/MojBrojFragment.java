package com.tim14.slagalica.fragments;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.tim14.slagalica.R;
import com.tim14.slagalica.game.BaseGameFragment;
import com.tim14.slagalica.game.MojBrojExpressionHelper;
import com.tim14.slagalica.repository.FirestoreRepository;
import com.tim14.slagalica.repository.LocalGameRepository;

public class MojBrojFragment extends BaseGameFragment implements SensorEventListener {

    private static final int ROUND_DURATION_SECONDS = 60;
    private static final long AUTO_STOP_DELAY_MS = 5000L;
    private static final float SHAKE_THRESHOLD = 12.0f;
    private static final long SHAKE_COOLDOWN_MS = 900L;

    private final Handler handler = new Handler(Looper.getMainLooper());

    private TextView roundOwnerText;
    private TextView targetNumberText;
    private TextView activeInputText;
    private TextView resultText;

    private EditText playerOneExpressionInput;
    private EditText playerTwoExpressionInput;
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

    private LocalGameRepository localGameRepository;
    private FirestoreRepository firestoreRepository;
    private MojBrojExpressionHelper expressionHelper;

    private SensorManager sensorManager;
    private Sensor accelerometer;

    private Runnable targetRoller;
    private Runnable numbersRoller;
    private Runnable autoStopTargetRunnable;
    private Runnable autoStopNumbersRunnable;

    private int currentRoundIndex;
    private int currentStarterPlayer;
    private int targetNumber;
    private int[] offeredNumbers = new int[6];
    private boolean targetLocked;
    private boolean numbersLocked;
    private boolean roundFinished;
    private long lastShakeTimestamp;

    private static final class EvaluatedExpression {
        private final boolean entered;
        private final boolean valid;
        private final Integer value;
        private final int difference;
        private final MojBrojExpressionHelper.ErrorType errorType;

        private EvaluatedExpression(
                boolean entered,
                boolean valid,
                Integer value,
                int difference,
                MojBrojExpressionHelper.ErrorType errorType
        ) {
            this.entered = entered;
            this.valid = valid;
            this.value = value;
            this.difference = difference;
            this.errorType = errorType;
        }

        private boolean isExact() {
            return valid && difference == 0;
        }

        private boolean hasResult() {
            return valid && value != null;
        }
    }

    private static final class RoundPoints {
        private int playerOnePoints;
        private int playerTwoPoints;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_moj_broj, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        localGameRepository = new LocalGameRepository();
        firestoreRepository = new FirestoreRepository();
        expressionHelper = new MojBrojExpressionHelper();

        sensorManager = (SensorManager) requireContext().getSystemService(Context.SENSOR_SERVICE);

        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }

        roundOwnerText = view.findViewById(R.id.roundOwnerText);
        targetNumberText = view.findViewById(R.id.targetNumberText);
        activeInputText = view.findViewById(R.id.activeInputText);
        resultText = view.findViewById(R.id.resultText);

        playerOneExpressionInput = view.findViewById(R.id.playerOneExpressionInput);
        playerTwoExpressionInput = view.findViewById(R.id.playerTwoExpressionInput);

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
        clearButton.setOnClickListener(v -> clearActiveInput());
        submitButton.setOnClickListener(v -> previewCurrentResults());

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

        startRound(0);
    }

    private void startRound(int roundIndex) {
        currentRoundIndex = roundIndex;
        currentStarterPlayer = roundIndex == 0 ? 1 : 2;
        targetNumber = 0;
        offeredNumbers = new int[6];
        targetLocked = false;
        numbersLocked = false;
        roundFinished = false;

        playerOneExpressionInput.setText("");
        playerTwoExpressionInput.setText("");
        targetNumberText.setText(getString(R.string.target_number_placeholder));
        resultText.setText(getString(R.string.my_number_rules_extended));
        bindNumberPlaceholders();

        activeInput = currentStarterPlayer == 1
                ? playerOneExpressionInput
                : playerTwoExpressionInput;

        updateRoundHeader();
        updateActiveInputLabel();
        updateInputAvailability();
        updatePhaseText();

        startTargetRolling();
        scheduleAutoStopTarget();
        startRoundTimer(ROUND_DURATION_SECONDS, this::handleRoundTimeout);
    }

    private void startTargetRolling() {
        targetRoller = new Runnable() {
            @Override
            public void run() {
                if (!isAdded() || targetLocked || roundFinished) {
                    return;
                }

                targetNumber = localGameRepository.generateTargetNumber();
                targetNumberText.setText(String.valueOf(targetNumber));
                handler.postDelayed(this, 120L);
            }
        };

        handler.post(targetRoller);
    }

    private void startNumbersRolling() {
        numbersRoller = new Runnable() {
            @Override
            public void run() {
                if (!isAdded() || numbersLocked || roundFinished) {
                    return;
                }

                offeredNumbers = localGameRepository.generateOfferedNumbers();
                bindOfferedNumbers();
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
        if (roundFinished || targetLocked) {
            return;
        }

        targetLocked = true;

        if (targetNumber == 0) {
            targetNumber = localGameRepository.generateTargetNumber();
            targetNumberText.setText(String.valueOf(targetNumber));
        }

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
        if (roundFinished || numbersLocked) {
            return;
        }

        numbersLocked = true;

        if (offeredNumbers[0] == 0) {
            offeredNumbers = localGameRepository.generateOfferedNumbers();
            bindOfferedNumbers();
        }

        if (numbersRoller != null) {
            handler.removeCallbacks(numbersRoller);
        }

        if (autoStopNumbersRunnable != null) {
            handler.removeCallbacks(autoStopNumbersRunnable);
        }

        updateInputAvailability();
        updatePhaseText();

        if (activeInput != null) {
            activeInput.requestFocus();
            activeInput.setSelection(activeInput.getText().length());
        }
    }

    private void handleRoundTimeout() {
        if (roundFinished) {
            return;
        }

        finishRound();
    }

    private void finishRound() {
        roundFinished = true;
        stopRoundTimer();
        stopRollingCallbacks();
        host().setTimerValue(0);

        EvaluatedExpression playerOneResult = evaluateExpression(playerOneExpressionInput);
        EvaluatedExpression playerTwoResult = evaluateExpression(playerTwoExpressionInput);
        RoundPoints roundPoints = calculateRoundPoints(playerOneResult, playerTwoResult);

        host().setScores(
                host().getPlayerOneScore() + roundPoints.playerOnePoints,
                host().getPlayerTwoScore() + roundPoints.playerTwoPoints
        );

        if (playerOneResult.hasResult() || roundPoints.playerOnePoints > 0) {
            firestoreRepository.updateMojBrojStatistics(
                    playerOneResult.hasResult() ? playerOneResult.difference : targetNumber,
                    roundPoints.playerOnePoints
            );
        }

        updateInputAvailability();
        host().setPhaseText(
                getString(R.string.my_number_round_finished_phase_format, currentRoundIndex + 1)
        );
        resultText.setText(buildRoundSummary(playerOneResult, playerTwoResult, roundPoints));

        Toast.makeText(requireContext(), getString(R.string.my_number_round_finished), Toast.LENGTH_SHORT)
                .show();

        resultText.postDelayed(() -> {
            if (!isAdded()) {
                return;
            }

            if (currentRoundIndex == 0) {
                startRound(1);
            } else {
                host().goToNextRound();
            }
        }, 2200L);
    }

    private RoundPoints calculateRoundPoints(
            EvaluatedExpression playerOneResult,
            EvaluatedExpression playerTwoResult
    ) {
        RoundPoints roundPoints = new RoundPoints();

        if (playerOneResult.isExact()) {
            roundPoints.playerOnePoints += 10;
        }

        if (playerTwoResult.isExact()) {
            roundPoints.playerTwoPoints += 10;
        }

        if (roundPoints.playerOnePoints > 0 || roundPoints.playerTwoPoints > 0) {
            return roundPoints;
        }

        if (playerOneResult.hasResult() && !playerTwoResult.hasResult()) {
            roundPoints.playerOnePoints = 5;
            return roundPoints;
        }

        if (!playerOneResult.hasResult() && playerTwoResult.hasResult()) {
            roundPoints.playerTwoPoints = 5;
            return roundPoints;
        }

        if (!playerOneResult.hasResult()) {
            return roundPoints;
        }

        if (playerOneResult.difference < playerTwoResult.difference) {
            roundPoints.playerOnePoints = 5;
            return roundPoints;
        }

        if (playerTwoResult.difference < playerOneResult.difference) {
            roundPoints.playerTwoPoints = 5;
            return roundPoints;
        }

        if (playerOneResult.value != null
                && playerOneResult.value.equals(playerTwoResult.value)
                && playerOneResult.value != 0) {
            if (currentStarterPlayer == 1) {
                roundPoints.playerOnePoints = 5;
            } else {
                roundPoints.playerTwoPoints = 5;
            }
        }

        return roundPoints;
    }

    private EvaluatedExpression evaluateExpression(EditText input) {
        String expression = input.getText().toString().trim();

        if (TextUtils.isEmpty(expression)) {
            return new EvaluatedExpression(
                    false,
                    false,
                    null,
                    Integer.MAX_VALUE,
                    MojBrojExpressionHelper.ErrorType.EMPTY
            );
        }

        MojBrojExpressionHelper.ValidationResult validationResult =
                expressionHelper.validateAndEvaluate(expression, offeredNumbers);

        if (!validationResult.isValid()) {
            return new EvaluatedExpression(
                    true,
                    false,
                    null,
                    Integer.MAX_VALUE,
                    validationResult.getErrorType()
            );
        }

        int value = validationResult.getValue();
        return new EvaluatedExpression(
                true,
                true,
                value,
                Math.abs(targetNumber - value),
                MojBrojExpressionHelper.ErrorType.NONE
        );
    }

    private void previewCurrentResults() {
        if (roundFinished) {
            return;
        }

        if (!targetLocked || !numbersLocked) {
            Toast.makeText(
                    requireContext(),
                    getString(R.string.my_number_stop_both_first),
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        EvaluatedExpression playerOneResult = evaluateExpression(playerOneExpressionInput);
        EvaluatedExpression playerTwoResult = evaluateExpression(playerTwoExpressionInput);

        String previewText =
                getString(R.string.my_number_current_preview_title)
                        + "\n"
                        + buildPlayerPreview(1, playerOneResult)
                        + "\n"
                        + buildPlayerPreview(2, playerTwoResult);

        resultText.setText(previewText);
    }

    private String buildRoundSummary(
            EvaluatedExpression playerOneResult,
            EvaluatedExpression playerTwoResult,
            RoundPoints roundPoints
    ) {
        StringBuilder summary = new StringBuilder();
        summary.append(getString(R.string.my_number_round_summary_title, currentRoundIndex + 1));
        summary.append('\n');
        summary.append(buildPlayerPreview(1, playerOneResult));
        summary.append('\n');
        summary.append(buildPlayerPreview(2, playerTwoResult));
        summary.append('\n');
        summary.append(
                getString(
                        R.string.my_number_round_points_format,
                        roundPoints.playerOnePoints,
                        roundPoints.playerTwoPoints
                )
        );

        return summary.toString();
    }

    private String buildPlayerPreview(int playerNumber, EvaluatedExpression result) {
        if (!result.entered) {
            return getString(R.string.my_number_preview_empty_format, playerNumber);
        }

        if (!result.valid) {
            return getString(
                    R.string.my_number_preview_invalid_format,
                    playerNumber,
                    getValidationLabel(result.errorType)
            );
        }

        return getString(
                R.string.my_number_preview_value_format,
                playerNumber,
                result.value,
                result.difference
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
            }
        });

        input.setOnClickListener(v -> {
            activeInput = input;
            updateActiveInputLabel();
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
                activeInput = currentStarterPlayer == 1
                        ? playerOneExpressionInput
                        : playerTwoExpressionInput;
            }

            String value = button.getText().toString();

            if (!TextUtils.isEmpty(value)
                    && !value.equals(getString(R.string.number_placeholder))) {
                activeInput.append(value);
                activeInput.setSelection(activeInput.getText().length());
            }
        });
    }

    private void clearActiveInput() {
        if (!isInputReady() || activeInput == null) {
            return;
        }

        activeInput.setText("");
    }

    private boolean isInputReady() {
        return !roundFinished && targetLocked && numbersLocked;
    }

    private void updateRoundHeader() {
        roundOwnerText.setText(
                getString(
                        R.string.my_number_round_owner_format,
                        currentRoundIndex + 1,
                        currentStarterPlayer
                )
        );
    }

    private void updateActiveInputLabel() {
        int activePlayerNumber = activeInput == playerTwoExpressionInput ? 2 : 1;

        activeInputText.setText(
                getString(R.string.my_number_active_input_format, activePlayerNumber)
        );
    }

    private void updatePhaseText() {
        if (roundFinished) {
            host().setPhaseText(getString(R.string.my_number_finished_phase));
            return;
        }

        if (!targetLocked) {
            host().setPhaseText(
                    getString(
                            R.string.my_number_lock_target_phase,
                            currentRoundIndex + 1,
                            currentStarterPlayer
                    )
            );
            return;
        }

        if (!numbersLocked) {
            host().setPhaseText(
                    getString(
                            R.string.my_number_lock_numbers_phase,
                            currentRoundIndex + 1,
                            currentStarterPlayer
                    )
            );
            return;
        }

        host().setPhaseText(
                getString(
                        R.string.my_number_round_phase_format,
                        currentRoundIndex + 1,
                        currentStarterPlayer
                )
        );
    }

    private void updateInputAvailability() {
        boolean inputEnabled = isInputReady();

        stopTargetButton.setEnabled(!roundFinished && !targetLocked);
        stopNumbersButton.setEnabled(!roundFinished && targetLocked && !numbersLocked);

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
        clearButton.setEnabled(inputEnabled);
        submitButton.setEnabled(inputEnabled);
    }

    private void bindNumberPlaceholders() {
        numberButton1.setText(R.string.number_placeholder);
        numberButton2.setText(R.string.number_placeholder);
        numberButton3.setText(R.string.number_placeholder);
        numberButton4.setText(R.string.number_placeholder);
        numberButton5.setText(R.string.number_placeholder);
        numberButton6.setText(R.string.number_placeholder);
    }

    private void bindOfferedNumbers() {
        numberButton1.setText(String.valueOf(offeredNumbers[0]));
        numberButton2.setText(String.valueOf(offeredNumbers[1]));
        numberButton3.setText(String.valueOf(offeredNumbers[2]));
        numberButton4.setText(String.valueOf(offeredNumbers[3]));
        numberButton5.setText(String.valueOf(offeredNumbers[4]));
        numberButton6.setText(String.valueOf(offeredNumbers[5]));
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
        if (roundFinished || event.sensor.getType() != Sensor.TYPE_ACCELEROMETER) {
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

        if (!targetLocked) {
            stopTargetRolling();
            return;
        }

        if (!numbersLocked) {
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
}
