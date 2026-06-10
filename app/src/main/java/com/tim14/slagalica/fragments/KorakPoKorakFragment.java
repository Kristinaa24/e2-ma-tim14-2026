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

import java.util.List;

public class KorakPoKorakFragment extends BaseGameFragment {

    private TextView[] clueViews;
    private EditText answerInput;
    private Button nextStepButton;
    private Button submitButton;

    private LocalGameRepository localGameRepository;
    private FirestoreRepository firestoreRepository;
    private List<KorakPoKorakRound> matchRounds;
    private KorakPoKorakRound currentRound;

    private int currentTurnIndex;
    private int currentStarterPlayer;
    private int bonusPlayer;
    private int openedClues;
    private int nextRevealAt;
    private boolean bonusMode;
    private boolean turnFinished;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_korak_po_korak, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        localGameRepository = new LocalGameRepository();
        firestoreRepository = new FirestoreRepository();
        matchRounds = localGameRepository.getKorakPoKorakMatchRounds();

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

        nextStepButton.setVisibility(View.GONE);
        submitButton.setOnClickListener(v -> submitAnswer());

        startTurn(0);
    }

    private void startTurn(int turnIndex) {
        currentTurnIndex = turnIndex;

        if (matchRounds == null || matchRounds.isEmpty()) {
            currentRound = localGameRepository.getRandomKorakPoKorakRound();
        } else {
            currentRound = matchRounds.get(Math.min(turnIndex, matchRounds.size() - 1));
        }

        currentStarterPlayer = turnIndex == 0 ? 1 : 2;
        bonusPlayer = currentStarterPlayer == 1 ? 2 : 1;
        openedClues = 1;
        nextRevealAt = 60;
        bonusMode = false;
        turnFinished = false;

        answerInput.setText("");
        answerInput.setEnabled(true);
        submitButton.setEnabled(true);

        renderClues();
        updatePhaseForStarter();
        startStarterTimer();
    }

    private void startStarterTimer() {
        startRoundTimer(70, remainingSeconds -> {
            if (!bonusMode && remainingSeconds == nextRevealAt && openedClues < currentRound.getClues().length) {
                openedClues++;
                nextRevealAt -= 10;
                renderClues();
            }
        }, this::enterBonusMode);
    }

    private void submitAnswer() {
        if (turnFinished) {
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

        if (bonusMode) {
            completeTurnWithBonusSuccess();
            return;
        }

        completeTurnWithStarterSuccess();
    }

    private void completeTurnWithStarterSuccess() {
        int earnedPoints = 20 - (openedClues - 1) * 2;
        awardPoints(currentStarterPlayer, earnedPoints);

        if (currentStarterPlayer == 1) {
            firestoreRepository.updateKorakPoKorakStatistics(openedClues, earnedPoints, true);
        }

        finalizeCurrentTurn(
                getString(R.string.step_by_step_starter_success_format, currentStarterPlayer, earnedPoints),
                true
        );
    }

    private void enterBonusMode() {
        if (turnFinished) {
            return;
        }

        if (currentStarterPlayer == 1) {
            firestoreRepository.updateKorakPoKorakStatistics(currentRound.getClues().length, 0, false);
        }

        bonusMode = true;
        revealAllClues();
        answerInput.setText("");
        answerInput.setEnabled(true);
        submitButton.setEnabled(true);
        host().setPhaseText(
                getString(
                        R.string.step_by_step_bonus_phase_format,
                        currentStarterPlayer,
                        bonusPlayer
                )
        );

        startRoundTimer(10, this::completeTurnWithoutBonusPoints);
    }

    private void completeTurnWithBonusSuccess() {
        awardPoints(bonusPlayer, 5);

        if (bonusPlayer == 1) {
            firestoreRepository.updateKorakPoKorakStatistics(currentRound.getClues().length, 5, false);
        }

        finalizeCurrentTurn(
                getString(R.string.step_by_step_bonus_success_format, bonusPlayer),
                true
        );
    }

    private void completeTurnWithoutBonusPoints() {
        finalizeCurrentTurn(
                getString(R.string.step_by_step_no_bonus_points),
                false
        );
    }

    private void finalizeCurrentTurn(String message, boolean revealAnswerInInput) {
        turnFinished = true;
        stopRoundTimer();
        host().setTimerValue(0);
        revealAllClues();

        answerInput.setEnabled(false);
        submitButton.setEnabled(false);

        if (revealAnswerInInput || bonusMode) {
            answerInput.setText(currentRound.getAnswer());
        }

        host().setPhaseText(message);
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();

        submitButton.postDelayed(() -> {
            if (currentTurnIndex == 0) {
                startTurn(1);
            } else {
                host().goToNextRound();
            }
        }, 1800);
    }

    private void awardPoints(int playerNumber, int points) {
        if (playerNumber == 1) {
            host().setScores(host().getPlayerOneScore() + points, host().getPlayerTwoScore());
            return;
        }

        host().setScores(host().getPlayerOneScore(), host().getPlayerTwoScore() + points);
    }

    private void updatePhaseForStarter() {
        host().setPhaseText(
                getString(
                        R.string.step_by_step_round_phase_format,
                        currentTurnIndex + 1,
                        currentStarterPlayer
                )
        );
    }

    private void renderClues() {
        String[] clues = currentRound.getClues();

        for (int index = 0; index < clueViews.length; index++) {
            clueViews[index].setText(index < openedClues ? clues[index] : "");
        }
    }

    private void revealAllClues() {
        openedClues = currentRound.getClues().length;
        renderClues();
    }
}
