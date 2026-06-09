package com.tim14.slagalica.fragments;

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

public class MojBrojFragment extends BaseGameFragment {

    private final Handler handler = new Handler(Looper.getMainLooper());

    private TextView targetNumberText;
    private TextView resultText;
    private EditText expressionInput;

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

    private Runnable targetRoller;
    private Runnable numbersRoller;

    private int targetNumber;
    private int[] offeredNumbers = new int[6];
    private boolean targetLocked;
    private boolean numbersLocked;
    private boolean roundFinished;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_moj_broj, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        localGameRepository = new LocalGameRepository();
        firestoreRepository = new FirestoreRepository();
        expressionHelper = new MojBrojExpressionHelper();

        targetNumberText = view.findViewById(R.id.targetNumberText);
        resultText = view.findViewById(R.id.resultText);
        expressionInput = view.findViewById(R.id.expressionInput);

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

        host().setScores(host().getPlayerOneScore(), host().getPlayerTwoScore());
        host().setPhaseText(getString(R.string.my_number_lock_values_phase));

        stopTargetButton.setOnClickListener(v -> stopTargetRolling());
        stopNumbersButton.setOnClickListener(v -> stopNumbersRolling());

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

        clearButton.setOnClickListener(v -> {
            expressionInput.setText("");
            resultText.setText(getString(R.string.moj_broj_rules));
        });

        submitButton.setOnClickListener(v -> submitExpression());

        updateInputAvailability();
        startTargetRolling();
        startNumbersRolling();
        startRoundTimer(60, this::handleTimeExpired);
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
                handler.postDelayed(this, 120);
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
                handler.postDelayed(this, 180);
            }
        };

        handler.post(numbersRoller);
    }

    private void stopTargetRolling() {
        if (roundFinished || targetLocked) {
            return;
        }

        targetLocked = true;
        handler.removeCallbacks(targetRoller);
        updateInputAvailability();
        updatePhaseText();
    }

    private void stopNumbersRolling() {
        if (roundFinished || numbersLocked) {
            return;
        }

        numbersLocked = true;
        handler.removeCallbacks(numbersRoller);
        updateInputAvailability();
        updatePhaseText();
    }

    private void bindOfferedNumbers() {
        numberButton1.setText(String.valueOf(offeredNumbers[0]));
        numberButton2.setText(String.valueOf(offeredNumbers[1]));
        numberButton3.setText(String.valueOf(offeredNumbers[2]));
        numberButton4.setText(String.valueOf(offeredNumbers[3]));
        numberButton5.setText(String.valueOf(offeredNumbers[4]));
        numberButton6.setText(String.valueOf(offeredNumbers[5]));
    }

    private void setAppendClick(Button button) {
        button.setOnClickListener(v -> {
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

            String value = button.getText().toString();

            if (!TextUtils.isEmpty(value)
                    && !value.equals(getString(R.string.number_placeholder))) {
                expressionInput.append(value);
            }
        });
    }

    private void submitExpression() {
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

        MojBrojExpressionHelper.ValidationResult validationResult =
                expressionHelper.validateAndEvaluate(
                        expressionInput.getText().toString(),
                        offeredNumbers
                );

        if (!validationResult.isValid()) {
            showValidationError(validationResult.getErrorType());
            return;
        }

        int resultValue = validationResult.getValue();
        int difference = Math.abs(targetNumber - resultValue);
        int earnedPoints = calculatePoints(difference);

        roundFinished = true;
        stopRoundTimer();
        stopRollingCallbacks();

        host().setTimerValue(0);
        host().setPhaseText(getString(R.string.my_number_finished_phase));
        host().setScores(host().getPlayerOneScore() + earnedPoints, host().getPlayerTwoScore());

        resultText.setText(getString(
                R.string.my_number_result_format,
                resultValue,
                targetNumber,
                difference,
                earnedPoints
        ));

        updateInputAvailability();
        firestoreRepository.updateMojBrojStatistics(difference, earnedPoints);

        Toast.makeText(requireContext(), getString(R.string.my_number_round_finished), Toast.LENGTH_SHORT)
                .show();

        submitButton.postDelayed(() -> host().goToNextRound(), 1700);
    }

    private void handleTimeExpired() {
        if (roundFinished) {
            return;
        }

        roundFinished = true;
        stopRollingCallbacks();
        updateInputAvailability();

        host().setTimerValue(0);
        host().setPhaseText(getString(R.string.my_number_finished_phase));
        resultText.setText(getString(R.string.my_number_time_up, targetNumber));

        submitButton.postDelayed(() -> host().goToNextRound(), 1700);
    }

    private void showValidationError(MojBrojExpressionHelper.ErrorType errorType) {
        int messageResId;

        switch (errorType) {
            case EMPTY:
                messageResId = R.string.my_number_enter_expression;
                break;
            case INVALID_CHARACTERS:
                messageResId = R.string.my_number_invalid_characters;
                break;
            case INVALID_NUMBERS:
                messageResId = R.string.my_number_use_offered_numbers;
                break;
            case INVALID_EXPRESSION:
            default:
                messageResId = R.string.my_number_invalid_expression;
                break;
        }

        Toast.makeText(requireContext(), getString(messageResId), Toast.LENGTH_SHORT).show();
    }

    private void updatePhaseText() {
        if (roundFinished) {
            host().setPhaseText(getString(R.string.my_number_finished_phase));
            return;
        }

        if (targetLocked && numbersLocked) {
            host().setPhaseText(getString(R.string.my_number_phase));
            return;
        }

        host().setPhaseText(getString(R.string.my_number_lock_values_phase));
    }

    private void updateInputAvailability() {
        boolean inputEnabled = !roundFinished && targetLocked && numbersLocked;

        stopTargetButton.setEnabled(!roundFinished && !targetLocked);
        stopNumbersButton.setEnabled(!roundFinished && !numbersLocked);

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

        expressionInput.setEnabled(inputEnabled);
        clearButton.setEnabled(inputEnabled);
        submitButton.setEnabled(inputEnabled);
    }

    private int calculatePoints(int difference) {
        if (difference == 0) {
            return 10;
        }

        if (difference <= 5) {
            return 5;
        }

        if (difference <= 10) {
            return 3;
        }

        return 0;
    }

    private void stopRollingCallbacks() {
        if (targetRoller != null) {
            handler.removeCallbacks(targetRoller);
        }

        if (numbersRoller != null) {
            handler.removeCallbacks(numbersRoller);
        }
    }

    @Override
    public void onDestroyView() {
        stopRollingCallbacks();
        super.onDestroyView();
    }
}
