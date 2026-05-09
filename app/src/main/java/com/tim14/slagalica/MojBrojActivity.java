package com.tim14.slagalica;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Random;

public class MojBrojActivity extends AppCompatActivity {

    private TextView targetNumberText, resultText;
    private EditText expressionInput;

    private Button stopTargetButton, stopNumbersButton, checkButton, clearButton;

    private Button numberButton1, numberButton2, numberButton3;
    private Button numberButton4, numberButton5, numberButton6;

    private Button plusButton, minusButton, multiplyButton, divideButton;
    private Button openBracketButton, closeBracketButton;
    private Button quitGameButton;

    private LinearLayout statusBarLayout;

    private boolean isGuest;

    private final Random random = new Random();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_moj_broj);

        targetNumberText = findViewById(R.id.targetNumberText);
        resultText = findViewById(R.id.resultText);
        expressionInput = findViewById(R.id.expressionInput);

        stopTargetButton = findViewById(R.id.stopTargetButton);
        stopNumbersButton = findViewById(R.id.stopNumbersButton);
        checkButton = findViewById(R.id.checkButton);
        clearButton = findViewById(R.id.clearButton);

        numberButton1 = findViewById(R.id.numberButton1);
        numberButton2 = findViewById(R.id.numberButton2);
        numberButton3 = findViewById(R.id.numberButton3);
        numberButton4 = findViewById(R.id.numberButton4);
        numberButton5 = findViewById(R.id.numberButton5);
        numberButton6 = findViewById(R.id.numberButton6);

        plusButton = findViewById(R.id.plusButton);
        minusButton = findViewById(R.id.minusButton);
        multiplyButton = findViewById(R.id.multiplyButton);
        divideButton = findViewById(R.id.divideButton);
        openBracketButton = findViewById(R.id.openBracketButton);
        closeBracketButton = findViewById(R.id.closeBracketButton);
        quitGameButton = findViewById(R.id.quitGameButton);

        statusBarLayout = findViewById(R.id.statusBarLayout);

        isGuest = getIntent().getBooleanExtra("IS_GUEST", false);

        if (isGuest) {
            statusBarLayout.setVisibility(View.GONE);
        }

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
            resultText.setText("");
        });

        checkButton.setOnClickListener(v -> checkExpression());

        quitGameButton.setOnClickListener(v -> {
            Intent intent = new Intent(
                    MojBrojActivity.this,
                    HomeActivity.class
            );

            intent.putExtra("IS_GUEST", isGuest);

            startActivity(intent);
            finish();
        });
    }

    private void generateTargetNumber() {
        int targetNumber = random.nextInt(900) + 100;
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

    private void checkExpression() {
        String expression = expressionInput.getText().toString().trim();

        if (expression.isEmpty()) {
            Toast.makeText(this, getString(R.string.enter_expression), Toast.LENGTH_SHORT).show();
            return;
        }

        resultText.setText(getString(R.string.entered_expression, expression));

        Toast.makeText(this, getString(R.string.match_finished), Toast.LENGTH_SHORT).show();

        resultText.postDelayed(() -> {
            Intent intent = new Intent(MojBrojActivity.this, HomeActivity.class);

            intent.putExtra("IS_GUEST", isGuest);

            startActivity(intent);
            finish();
        }, 2000);
    }
}