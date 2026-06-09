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

public class KorakPoKorakFragment extends BaseGameFragment {

    private TextView step1Text;
    private TextView step2Text;
    private TextView step3Text;
    private TextView step4Text;
    private TextView step5Text;
    private TextView step6Text;
    private TextView step7Text;
    private EditText answerInput;
    private Button nextStepButton;
    private Button submitButton;

    private int currentStep = 1;

    private final String correctAnswer = "Sunce";

    private final String[] steps = {
            "1. Nebesko telo",
            "2. Daje svetlost",
            "3. Daje toplotu",
            "4. Izlazi ujutru",
            "5. Zalazi uvece",
            "6. Zute je boje",
            "7. Centar Suncevog sistema"
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_korak_po_korak, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        step1Text = view.findViewById(R.id.step1Text);
        step2Text = view.findViewById(R.id.step2Text);
        step3Text = view.findViewById(R.id.step3Text);
        step4Text = view.findViewById(R.id.step4Text);
        step5Text = view.findViewById(R.id.step5Text);
        step6Text = view.findViewById(R.id.step6Text);
        step7Text = view.findViewById(R.id.step7Text);
        answerInput = view.findViewById(R.id.answerInput);
        nextStepButton = view.findViewById(R.id.nextStepButton);
        submitButton = view.findViewById(R.id.submitButton);

        step1Text.setText(steps[0]);

        host().setPhaseText("Otvaraj tragove i unesi konacno resenje");
        host().setTimerValue(70);
        host().setScores(host().getPlayerOneScore(), host().getPlayerTwoScore());

        nextStepButton.setOnClickListener(v -> openNextStep());
        submitButton.setOnClickListener(v -> checkAnswer());
    }

    private void openNextStep() {
        if (currentStep >= 7) {
            Toast.makeText(requireContext(), "Svi koraci su vec otvoreni.", Toast.LENGTH_SHORT).show();
            return;
        }

        currentStep++;

        if (currentStep == 2) {
            step2Text.setText(steps[1]);
        } else if (currentStep == 3) {
            step3Text.setText(steps[2]);
        } else if (currentStep == 4) {
            step4Text.setText(steps[3]);
        } else if (currentStep == 5) {
            step5Text.setText(steps[4]);
        } else if (currentStep == 6) {
            step6Text.setText(steps[5]);
        } else if (currentStep == 7) {
            step7Text.setText(steps[6]);
        }

        int fakeTimerValue = Math.max(10, 80 - currentStep * 10);
        host().setTimerValue(fakeTimerValue);
    }

    private void checkAnswer() {
        String answer = answerInput.getText().toString().trim();

        if (answer.isEmpty()) {
            Toast.makeText(requireContext(), "Unesi odgovor.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (answer.equalsIgnoreCase(correctAnswer)) {
            int earnedPoints = Math.max(0, 22 - currentStep * 2);
            int newPlayerOneScore = host().getPlayerOneScore() + earnedPoints;

            host().setScores(newPlayerOneScore, host().getPlayerTwoScore());
            host().setPhaseText("Tacan odgovor");
            host().setTimerValue(0);

            revealAllSteps();

            Toast.makeText(
                    requireContext(),
                    "Tacan odgovor! Osvojeno: " + earnedPoints,
                    Toast.LENGTH_SHORT
            ).show();

            submitButton.postDelayed(() -> host().goToNextRound(), 900);
        } else {
            Toast.makeText(requireContext(), "Pogresan odgovor. Pokusaj ponovo.", Toast.LENGTH_SHORT).show();
        }
    }

    private void revealAllSteps() {
        step1Text.setText(steps[0]);
        step2Text.setText(steps[1]);
        step3Text.setText(steps[2]);
        step4Text.setText(steps[3]);
        step5Text.setText(steps[4]);
        step6Text.setText(steps[5]);
        step7Text.setText(steps[6]);
    }
}
