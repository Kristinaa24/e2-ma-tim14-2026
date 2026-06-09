package com.tim14.slagalica.fragments;

import android.os.Bundle;
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
import com.tim14.slagalica.model.KorakPoKorakRound;
import com.tim14.slagalica.repository.FirestoreRepository;
import com.tim14.slagalica.repository.LocalGameRepository;

public class KorakPoKorakFragment extends BaseGameFragment {

    private TextView[] clueViews;
    private EditText answerInput;
    private Button nextStepButton;
    private Button submitButton;

    private LocalGameRepository localGameRepository;
    private FirestoreRepository firestoreRepository;
    private KorakPoKorakRound currentRound;

    private int openedClues = 1;
    private boolean roundFinished;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_korak_po_korak, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        localGameRepository = new LocalGameRepository();
        firestoreRepository = new FirestoreRepository();

        clueViews = new TextView[]{
                view.findViewById(R.id.step1Text),
                view.findViewById(R.id.step2Text),
                view.findViewById(R.id.step3Text),
                view.findViewById(R.id.step4Text),
                view.findViewById(R.id.step5Text),
                view.findViewById(R.id.step6Text),
                view.findViewById(R.id.step7Text)
        };

        answerInput = view.findViewById(R.id.answerInput);
        nextStepButton = view.findViewById(R.id.nextStepButton);
        submitButton = view.findViewById(R.id.submitButton);

        currentRound = localGameRepository.getRandomKorakPoKorakRound();

        host().setPhaseText(getString(R.string.step_by_step_phase));
        host().setScores(host().getPlayerOneScore(), host().getPlayerTwoScore());

        renderClues();
        startRoundTimer(70, this::handleTimeExpired);

        nextStepButton.setOnClickListener(v -> openNextStep());
        submitButton.setOnClickListener(v -> checkAnswer());
    }

    private void renderClues() {
        String[] clues = currentRound.getClues();

        for (int index = 0; index < clueViews.length; index++) {
            clueViews[index].setText(index < openedClues ? clues[index] : "");
        }
    }

    private void openNextStep() {
        if (roundFinished) {
            return;
        }

        if (openedClues >= currentRound.getClues().length) {
            Toast.makeText(
                    requireContext(),
                    getString(R.string.step_by_step_all_clues_opened),
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        openedClues++;
        renderClues();
    }

    private void checkAnswer() {
        if (roundFinished) {
            return;
        }

        String answer = answerInput.getText().toString().trim();

        if (TextUtils.isEmpty(answer)) {
            Toast.makeText(
                    requireContext(),
                    getString(R.string.step_by_step_enter_answer),
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        if (!answer.equalsIgnoreCase(currentRound.getAnswer())) {
            Toast.makeText(
                    requireContext(),
                    getString(R.string.step_by_step_wrong),
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        int earnedPoints = calculateScore();
        finishRound(true, earnedPoints);

        Toast.makeText(
                requireContext(),
                getString(R.string.step_by_step_correct_format, earnedPoints),
                Toast.LENGTH_SHORT
        ).show();
    }

    private void handleTimeExpired() {
        if (roundFinished) {
            return;
        }

        finishRound(false, 0);

        Toast.makeText(
                requireContext(),
                getString(R.string.step_by_step_timeout, currentRound.getAnswer()),
                Toast.LENGTH_LONG
        ).show();
    }

    private void finishRound(boolean solved, int earnedPoints) {
        roundFinished = true;
        stopRoundTimer();
        host().setTimerValue(0);
        host().setPhaseText(
                getString(solved
                        ? R.string.step_by_step_correct_phase
                        : R.string.step_by_step_finished_phase)
        );

        revealAllClues();
        answerInput.setEnabled(false);
        nextStepButton.setEnabled(false);
        submitButton.setEnabled(false);

        if (solved) {
            host().setScores(host().getPlayerOneScore() + earnedPoints, host().getPlayerTwoScore());
            firestoreRepository.updateKorakPoKorakStatistics(openedClues, earnedPoints, true);
        } else {
            answerInput.setText(currentRound.getAnswer());
            firestoreRepository.updateKorakPoKorakStatistics(openedClues, 0, false);
        }

        submitButton.postDelayed(() -> host().goToNextRound(), 1500);
    }

    private void revealAllClues() {
        openedClues = currentRound.getClues().length;
        renderClues();
    }

    private int calculateScore() {
        return Math.max(8, 24 - openedClues * 2);
    }
}
