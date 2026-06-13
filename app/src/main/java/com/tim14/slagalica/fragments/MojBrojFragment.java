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

import com.tim14.slagalica.R;
import com.tim14.slagalica.game.BaseGameFragment;
import com.tim14.slagalica.game.MojBrojExpressionHelper;
import com.tim14.slagalica.repository.FirestoreRepository;
import com.tim14.slagalica.repository.LocalGameRepository;
import com.tim14.slagalica.service.MojBrojService;

import java.util.HashMap;
import java.util.Map;
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

    private FirestoreRepository firestoreRepository;
    private MojBrojService mojBrojService;

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

        startRound(0);
    }

    private void startRound(int roundIndex) {
        mojBrojService.startRound(roundIndex);

        playerOneExpressionInput.setText("");
        playerTwoExpressionInput.setText("");
        targetNumberText.setText(getString(R.string.target_number_placeholder));
        resultText.setText(getString(R.string.my_number_rules_extended));
        bindNumberPlaceholders();

        activeInput = mojBrojService.getCurrentStarterPlayer() == 1
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

        resultText.setText(getString(R.string.my_number_ready_for_input_message));
        updateNumberButtonStates();

        if (activeInput != null) {
            activeInput.requestFocus();
            activeInput.setSelection(activeInput.getText().length());
        }
    }

    private void handleRoundTimeout() {
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
                playerTwoExpressionInput.getText().toString()
        );

        host().setScores(
                host().getPlayerOneScore() + roundOutcome.getPlayerOnePoints(),
                host().getPlayerTwoScore() + roundOutcome.getPlayerTwoPoints()
        );

        firestoreRepository.updateMojBrojStatistics(
                roundOutcome.getPlayerOneStatisticsDifference(),
                roundOutcome.getPlayerOnePoints()
        );

        updateInputAvailability();
        host().setPhaseText(
                getString(
                        R.string.my_number_round_finished_phase_format,
                        mojBrojService.getCurrentRoundIndex() + 1
                )
        );
        resultText.setText(buildRoundSummary(
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

            if (mojBrojService.getCurrentRoundIndex() == 0) {
                startRound(1);
            } else {
                host().goToNextRound();
            }
        }, 2200L);
    }

    private void submitRoundEarly() {
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
                    getString(R.string.my_number_submit_without_input),
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

    private String buildPlayerPreview(int playerNumber, MojBrojService.EvaluatedExpression result) {
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
                updateNumberButtonStates();
            }
        });

        input.setOnClickListener(v -> {
            activeInput = input;
            updateActiveInputLabel();
            updateNumberButtonStates();
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
        return mojBrojService.isInputReady();
    }

    private void updateRoundHeader() {
        roundOwnerText.setText(
                getString(
                        R.string.my_number_round_owner_format,
                        mojBrojService.getCurrentRoundIndex() + 1,
                        mojBrojService.getCurrentStarterPlayer()
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
        clearButton.setEnabled(inputEnabled);
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
            int remainingUsedCount = usedCounts.getOrDefault(numberValue, 0);

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
            usedCounts.put(number, usedCounts.getOrDefault(number, 0) + 1);
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
}
