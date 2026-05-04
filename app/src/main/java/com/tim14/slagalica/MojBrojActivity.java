package com.tim14.slagalica;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MojBrojActivity extends AppCompatActivity {

    private TextView targetNumberText, numbersText, resultText;
    private EditText expressionInput;
    private Button checkButton;

    private final int targetNumber = 972;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_moj_broj);

        targetNumberText = findViewById(R.id.targetNumberText);
        numbersText = findViewById(R.id.numbersText);
        expressionInput = findViewById(R.id.expressionInput);
        checkButton = findViewById(R.id.checkButton);
        resultText = findViewById(R.id.resultText);

        targetNumberText.setText(getString(R.string.target_number_972));
        numbersText.setText(getString(R.string.offered_numbers));

        checkButton.setOnClickListener(v -> {
            String answer = expressionInput.getText().toString().trim();

            if (answer.equals(getString(R.string.target_number_972))) {
                resultText.setText(getString(R.string.correct_moj_broj));
                Toast.makeText(this, getString(R.string.correct), Toast.LENGTH_SHORT).show();
            } else {
                resultText.setText(getString(R.string.wrong_moj_broj));
                Toast.makeText(this, getString(R.string.wrong), Toast.LENGTH_SHORT).show();
            }
        });
    }
}