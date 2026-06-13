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
import com.tim14.slagalica.repository.FirestoreRepository;
import com.tim14.slagalica.repository.LocalGameRepository;
import com.tim14.slagalica.service.KorakPoKorakService;

public class KorakPoKorakFragment extends BaseGameFragment {

    private TextView[] clueViews;
    private EditText answerInput;
    private Button nextStepButton;
    private Button submitButton;

    private FirestoreRepository firestoreRepository;
    private KorakPoKorakService korakPoKorakService;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_korak_po_korak, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        korakPoKorakService = new KorakPoKorakService(new LocalGameRepository());
        korakPoKorakService.startMatch();
        firestoreRepository = new FirestoreRepository(requireContext());

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
        korakPoKorakService.startTurn(turnIndex);

        answerInput.setText("");
        answerInput.setEnabled(true);
        submitButton.setEnabled(true);

        renderClues();
        updateStarterPhase();
        startStarterTimer();
    }

    private void startStarterTimer() {
        startRoundTimer(70, remainingSeconds -> {
            if (korakPoKorakService.onTick(remainingSeconds)) {
                renderClues();
            }
        }, this::handleStarterTimeout);
    }

    private void submitAnswer() {
        KorakPoKorakService.Resolution resolution =
                korakPoKorakService.submitAnswer(answerInput.getText().toString());

        switch (resolution.getType()) {
            case INVALID_EMPTY:
                Toast.makeText(
                        requireContext(),
                        getString(R.string.step_by_step_enter_answer),
                        Toast.LENGTH_SHORT
                ).show();
                return;
            case INVALID_WRONG:
                Toast.makeText(
                        requireContext(),
                        getString(R.string.step_by_step_wrong),
                        Toast.LENGTH_SHORT
                ).show();
                return;
            case STARTER_SUCCESS:
                persistStatistics(resolution.getStatisticsUpdate());
                awardPoints(resolution.getAwardedPlayer(), resolution.getAwardedPoints());
                finalizeCurrentTurn(
                        getString(
                                R.string.step_by_step_starter_success_format,
                                resolution.getAwardedPlayer(),
                                resolution.getAwardedPoints()
                        ),
                        true
                );
                return;
            case BONUS_SUCCESS:
                persistStatistics(resolution.getStatisticsUpdate());
                awardPoints(resolution.getAwardedPlayer(), resolution.getAwardedPoints());
                finalizeCurrentTurn(
                        getString(
                                R.string.step_by_step_bonus_success_format,
                                resolution.getAwardedPlayer()
                        ),
                        true
                );
                return;
            default:
                return;
        }
    }

    private void finalizeCurrentTurn(String message, boolean revealAnswerInInput) {
        stopRoundTimer();
        host().setTimerValue(0);
        korakPoKorakService.revealAllClues();
        renderClues();

        answerInput.setEnabled(false);
        submitButton.setEnabled(false);

        if (revealAnswerInInput || korakPoKorakService.isBonusMode()) {
            answerInput.setText(korakPoKorakService.getCurrentAnswer());
        }

        host().setPhaseText(message);
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();

        submitButton.postDelayed(() -> {
            if (korakPoKorakService.getCurrentTurnIndex() == 0) {
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

    private void updateStarterPhase() {
        host().setPhaseText(
                getString(
                        R.string.step_by_step_round_phase_format,
                        korakPoKorakService.getCurrentTurnIndex() + 1,
                        korakPoKorakService.getCurrentStarterPlayer()
                )
        );
    }

    private void renderClues() {
        String[] clues = korakPoKorakService.getVisibleClues();

        for (int index = 0; index < clueViews.length; index++) {
            clueViews[index].setText(index < clues.length ? clues[index] : "");
        }
    }

    private void handleStarterTimeout() {
        KorakPoKorakService.Resolution resolution = korakPoKorakService.handleStarterTimeout();
        persistStatistics(resolution.getStatisticsUpdate());
        renderClues();
        answerInput.setText("");
        answerInput.setEnabled(true);
        submitButton.setEnabled(true);
        host().setPhaseText(
                getString(
                        R.string.step_by_step_bonus_phase_format,
                        korakPoKorakService.getCurrentStarterPlayer(),
                        korakPoKorakService.getBonusPlayer()
                )
        );
        startRoundTimer(10, this::handleBonusTimeout);
    }

    private void handleBonusTimeout() {
        KorakPoKorakService.Resolution resolution = korakPoKorakService.handleBonusTimeout();
        persistStatistics(resolution.getStatisticsUpdate());
        finalizeCurrentTurn(getString(R.string.step_by_step_no_bonus_points), false);
    }

    private void persistStatistics(KorakPoKorakService.StatisticsUpdate statisticsUpdate) {
        if (statisticsUpdate == null) {
            return;
        }

        firestoreRepository.updateKorakPoKorakStatistics(
                statisticsUpdate.getOpenedClues(),
                statisticsUpdate.getScore(),
                statisticsUpdate.isSolved()
        );
    }
}
