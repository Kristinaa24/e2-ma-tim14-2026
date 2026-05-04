package com.tim14.slagalica;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class KorakPoKorakActivity extends AppCompatActivity {

    private TextView scoreText;
    private TextView step1Text, step2Text, step3Text, step4Text, step5Text, step6Text, step7Text;
    private EditText answerInput;
    private Button nextStepButton, submitButton;

    private int currentStep = 1;
    private int score = 0;

    private final String correctAnswer = "Sunce";

    private final String[] steps = {
            "1. Nebesko telo",
            "2. Daje svetlost",
            "3. Daje toplotu",
            "4. Izlazi ujutru",
            "5. Zalazi uveče",
            "6. Žute je boje",
            "7. Centar Sunčevog sistema"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_korak_po_korak);

        scoreText = findViewById(R.id.scoreText);

        step1Text = findViewById(R.id.step1Text);
        step2Text = findViewById(R.id.step2Text);
        step3Text = findViewById(R.id.step3Text);
        step4Text = findViewById(R.id.step4Text);
        step5Text = findViewById(R.id.step5Text);
        step6Text = findViewById(R.id.step6Text);
        step7Text = findViewById(R.id.step7Text);

        answerInput = findViewById(R.id.answerInput);
        nextStepButton = findViewById(R.id.nextStepButton);
        submitButton = findViewById(R.id.submitButton);

        nextStepButton.setOnClickListener(v -> openNextStep());

        submitButton.setOnClickListener(v -> checkAnswer());
    }

    private void openNextStep() {
        if (currentStep >= 7) {
            Toast.makeText(this, "All steps are opened", Toast.LENGTH_SHORT).show();
            return;
        }

        currentStep++;

        if (currentStep == 2) step2Text.setText(steps[1]);
        else if (currentStep == 3) step3Text.setText(steps[2]);
        else if (currentStep == 4) step4Text.setText(steps[3]);
        else if (currentStep == 5) step5Text.setText(steps[4]);
        else if (currentStep == 6) step6Text.setText(steps[5]);
        else if (currentStep == 7) step7Text.setText(steps[6]);
    }

    private void checkAnswer() {
        String answer = answerInput.getText().toString();

        if (answer.isEmpty()) {
            Toast.makeText(this, "Enter your answer", Toast.LENGTH_SHORT).show();
            return;
        }

        if (answer.equalsIgnoreCase(correctAnswer)) {
            score = 22 - currentStep * 2;
            scoreText.setText("Score: " + score);

            step1Text.setText(steps[0]);
            step2Text.setText(steps[1]);
            step3Text.setText(steps[2]);
            step4Text.setText(steps[3]);
            step5Text.setText(steps[4]);
            step6Text.setText(steps[5]);
            step7Text.setText(steps[6]);

            Toast.makeText(this, "Correct answer!", Toast.LENGTH_SHORT).show();
        }
    }
}