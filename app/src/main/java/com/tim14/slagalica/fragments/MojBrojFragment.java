package com.tim14.slagalica.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.tim14.slagalica.R;
import com.tim14.slagalica.game.BaseGameFragment;

import java.util.ArrayDeque;
import java.util.Random;

public class MojBrojFragment extends BaseGameFragment {

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

    private final Random random = new Random();
    private int targetNumber = 0;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_moj_broj, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
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

        host().setPhaseText("Formiraj izraz i pogodi ciljni broj");
        host().setTimerValue(60);
        host().setScores(host().getPlayerOneScore(), host().getPlayerTwoScore());

        stopTargetButton.setOnClickListener(v -> generateTargetNumber());
        stopNumbersButton.setOnClickListener(v -> generateOfferedNumbers());

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
    }

    private void generateTargetNumber() {
        targetNumber = random.nextInt(900) + 100;
        targetNumberText.setText(String.valueOf(targetNumber));
    }

    private void generateOfferedNumbers() {
        int firstSmall = random.nextInt(9) + 1;
        int secondSmall = random.nextInt(9) + 1;
        int thirdSmall = random.nextInt(9) + 1;
        int fourthSmall = random.nextInt(9) + 1;

        int[] middleNumbers = {10, 15, 20};
        int[] bigNumbers = {25, 50, 75, 100};

        int middleNumber = middleNumbers[random.nextInt(middleNumbers.length)];
        int bigNumber = bigNumbers[random.nextInt(bigNumbers.length)];

        numberButton1.setText(String.valueOf(firstSmall));
        numberButton2.setText(String.valueOf(secondSmall));
        numberButton3.setText(String.valueOf(thirdSmall));
        numberButton4.setText(String.valueOf(fourthSmall));
        numberButton5.setText(String.valueOf(middleNumber));
        numberButton6.setText(String.valueOf(bigNumber));
    }

    private void setAppendClick(Button button) {
        button.setOnClickListener(v -> {
            String value = button.getText().toString();

            if (!value.equals(getString(R.string.number_placeholder))) {
                expressionInput.append(value);
            }
        });
    }

    private void submitExpression() {
        if (targetNumber == 0) {
            Toast.makeText(requireContext(), "Prvo zaustavi ciljni broj.", Toast.LENGTH_SHORT).show();
            return;
        }

        String expression = expressionInput.getText().toString().trim();

        if (expression.isEmpty()) {
            Toast.makeText(requireContext(), "Unesi izraz.", Toast.LENGTH_SHORT).show();
            return;
        }

        Integer resultValue = evaluateExpression(expression);

        if (resultValue == null) {
            Toast.makeText(requireContext(), "Neispravan izraz.", Toast.LENGTH_SHORT).show();
            return;
        }

        int difference = Math.abs(targetNumber - resultValue);
        int earnedPoints = calculatePoints(difference);
        int newPlayerOneScore = host().getPlayerOneScore() + earnedPoints;

        host().setScores(newPlayerOneScore, host().getPlayerTwoScore());
        host().setTimerValue(0);
        host().setPhaseText("Moj broj zavrsen");

        resultText.setText(
                "Dobijeni broj: " + resultValue +
                        "\nCilj: " + targetNumber +
                        "\nRazlika: " + difference +
                        "\nPoeni: " + earnedPoints
        );

        Toast.makeText(requireContext(), "Runda je zavrsena.", Toast.LENGTH_SHORT).show();
        submitButton.postDelayed(() -> host().goToNextRound(), 1200);
    }

    private int calculatePoints(int difference) {
        if (difference == 0) {
            return 10;
        }

        if (difference <= 5) {
            return 5;
        }

        return 0;
    }

    private Integer evaluateExpression(String expression) {
        String cleanExpression = expression.replace(" ", "");

        if (cleanExpression.isEmpty()) {
            return null;
        }

        ArrayDeque<Integer> values = new ArrayDeque<>();
        ArrayDeque<Character> operators = new ArrayDeque<>();

        int i = 0;

        while (i < cleanExpression.length()) {
            char current = cleanExpression.charAt(i);

            if (Character.isDigit(current)) {
                int number = 0;

                while (i < cleanExpression.length() && Character.isDigit(cleanExpression.charAt(i))) {
                    number = number * 10 + (cleanExpression.charAt(i) - '0');
                    i++;
                }

                values.push(number);
                continue;
            }

            if (current == '(') {
                operators.push(current);
                i++;
                continue;
            }

            if (current == ')') {
                while (!operators.isEmpty() && operators.peek() != '(') {
                    if (!applyTopOperation(values, operators)) {
                        return null;
                    }
                }

                if (operators.isEmpty() || operators.pop() != '(') {
                    return null;
                }

                i++;
                continue;
            }

            if (!isOperator(current)) {
                return null;
            }

            while (!operators.isEmpty() && precedence(operators.peek()) >= precedence(current)) {
                if (!applyTopOperation(values, operators)) {
                    return null;
                }
            }

            operators.push(current);
            i++;
        }

        while (!operators.isEmpty()) {
            if (operators.peek() == '(') {
                return null;
            }

            if (!applyTopOperation(values, operators)) {
                return null;
            }
        }

        if (values.size() != 1) {
            return null;
        }

        return values.pop();
    }

    private boolean applyTopOperation(ArrayDeque<Integer> values, ArrayDeque<Character> operators) {
        if (values.size() < 2 || operators.isEmpty()) {
            return false;
        }

        int right = values.pop();
        int left = values.pop();
        char operator = operators.pop();

        Integer result = applyOperation(left, right, operator);

        if (result == null) {
            return false;
        }

        values.push(result);
        return true;
    }

    private Integer applyOperation(int left, int right, char operator) {
        switch (operator) {
            case '+':
                return left + right;
            case '-':
                return left - right;
            case '*':
                return left * right;
            case '/':
                if (right == 0 || left % right != 0) {
                    return null;
                }
                return left / right;
            default:
                return null;
        }
    }

    private boolean isOperator(char value) {
        return value == '+' || value == '-' || value == '*' || value == '/';
    }

    private int precedence(char operator) {
        if (operator == '+' || operator == '-') {
            return 1;
        }

        if (operator == '*' || operator == '/') {
            return 2;
        }

        return 0;
    }
}
