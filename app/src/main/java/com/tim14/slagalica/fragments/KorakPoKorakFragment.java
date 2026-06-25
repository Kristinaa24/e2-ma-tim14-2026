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

import com.tim14.slagalica.GameHostActivity;
import com.tim14.slagalica.R;
import com.tim14.slagalica.game.BaseGameFragment;
import com.tim14.slagalica.model.SharedKorakPoKorakRound;
import com.tim14.slagalica.model.SharedMatchState;
import com.tim14.slagalica.repository.FirestoreRepository;
import com.tim14.slagalica.repository.LocalGameRepository;
import com.tim14.slagalica.service.KorakPoKorakService;

import java.util.HashMap;
import java.util.Map;

public class KorakPoKorakFragment extends BaseGameFragment {

    private TextView[] clueViews;
    private EditText answerInput;
    private Button nextStepButton;
    private Button submitButton;

    private FirestoreRepository firestoreRepository;
    private KorakPoKorakService korakPoKorakService;
    private boolean remoteMode;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_korak_po_korak, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        firestoreRepository = new FirestoreRepository(requireContext());
        remoteMode = ((GameHostActivity) requireActivity()).isRemoteMatchMode();

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

        if (remoteMode) {
            renderRemoteRound();
            return;
        }

        korakPoKorakService = new KorakPoKorakService(new LocalGameRepository());
        korakPoKorakService.startMatch();
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
        if (remoteMode) {
            submitRemoteAnswer();
            return;
        }

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

        if (host().shouldPersistStatistics()) {
            firestoreRepository.updateKorakPoKorakStatistics(
                    statisticsUpdate.getOpenedClues(),
                    statisticsUpdate.getScore(),
                    statisticsUpdate.isSolved()
            );
        }
    }

    private void renderRemoteRound() {
        stopRoundTimer();

        GameHostActivity activity = (GameHostActivity) requireActivity();
        SharedMatchState state = activity.getSharedMatchState();

        if (state == null) {
            disableRemoteInteraction();
            return;
        }

        if (SharedMatchState.STATUS_WAITING.equals(state.status)
                || SharedMatchState.PHASE_WAITING.equals(state.phase)) {
            for (TextView clueView : clueViews) {
                clueView.setText("");
            }
            answerInput.setText("");
            answerInput.setEnabled(false);
            submitButton.setEnabled(false);
            host().setTimerValue(0);
            host().setPhaseText(state.phaseMessage);
            return;
        }

        SharedKorakPoKorakRound round = activity.getSharedKorakRound(state.currentTurnIndex);
        if (round == null) {
            disableRemoteInteraction();
            return;
        }

        if (maybeContinueAfterForfeit(activity, state, round)) {
            return;
        }

        int remainingSeconds = getRemoteRemainingSeconds(state);
        int visibleClues = resolveVisibleCluesCount(state, round, remainingSeconds);
        boolean revealAll = SharedMatchState.PHASE_KPP_BONUS.equals(state.phase)
                || SharedMatchState.PHASE_KPP_DONE.equals(state.phase);

        renderRemoteClues(round, revealAll ? round.clues.size() : visibleClues);
        host().setTimerValue(remainingSeconds);
        host().setPhaseText(state.phaseMessage);

        if (SharedMatchState.PHASE_KPP_DONE.equals(state.phase)) {
            answerInput.setText(
                    TextUtils.isEmpty(state.revealedAnswer) ? round.answer : state.revealedAnswer
            );
            answerInput.setEnabled(false);
            submitButton.setEnabled(false);
            scheduleRemoteAdvanceIfCoordinator(state);
            return;
        }

        boolean canAnswer = activity.getLocalPlayerNumber() == state.activePlayer;
        answerInput.setEnabled(canAnswer);
        submitButton.setEnabled(canAnswer);

        if (!canAnswer) {
            answerInput.setHint(getString(R.string.shared_match_wait_turn_hint));
        } else {
            answerInput.setHint(getString(R.string.enter_final_answer));
        }

        if (remainingSeconds > 0) {
            startRoundTimer(remainingSeconds, secondsLeft -> {
                renderRemoteClues(
                        round,
                        revealAll ? round.clues.size() : resolveVisibleCluesCount(state, round, secondsLeft)
                );
            }, () -> handleRemoteTimeout(state, round));
        }
    }

    private boolean maybeContinueAfterForfeit(
            GameHostActivity activity,
            SharedMatchState state,
            SharedKorakPoKorakRound round
    ) {
        if (!activity.hasOpponentForfeited() || state.activePlayer != state.forfeitedPlayer) {
            return false;
        }

        if (SharedMatchState.PHASE_KPP_STARTER.equals(state.phase)) {
            Map<String, Object> updates = new HashMap<>();
            updates.put("phase", SharedMatchState.PHASE_KPP_BONUS);
            updates.put("activePlayer", activity.getRemainingRemotePlayerNumber());
            updates.put("phaseStartedAt", System.currentTimeMillis());
            updates.put("phaseDurationSeconds", 10);
            updates.put("phaseMessage", getString(
                    R.string.shared_match_kpp_bonus_phase_format,
                    state.forfeitedPlayer,
                    activity.getRemainingRemotePlayerNumber()
            ));
            activity.updateSharedMatch(updates);
            return true;
        }

        if (SharedMatchState.PHASE_KPP_BONUS.equals(state.phase)) {
            Map<String, Object> updates = new HashMap<>();
            updates.put("phase", SharedMatchState.PHASE_KPP_DONE);
            updates.put("activePlayer", 0);
            updates.put("phaseStartedAt", System.currentTimeMillis());
            updates.put("phaseDurationSeconds", 2);
            updates.put("revealedAnswer", round.answer);
            updates.put("phaseMessage", getString(R.string.step_by_step_no_bonus_points));
            activity.updateSharedMatch(updates);
            return true;
        }

        return false;
    }

    private void submitRemoteAnswer() {
        GameHostActivity activity = (GameHostActivity) requireActivity();
        SharedMatchState state = activity.getSharedMatchState();
        if (state == null) {
            return;
        }

        SharedKorakPoKorakRound round = activity.getSharedKorakRound(state.currentTurnIndex);

        if (state == null || round == null || activity.getLocalPlayerNumber() != state.activePlayer) {
            return;
        }

        String trimmedAnswer = answerInput.getText().toString().trim();
        if (trimmedAnswer.isEmpty()) {
            Toast.makeText(requireContext(), R.string.step_by_step_enter_answer, Toast.LENGTH_SHORT).show();
            return;
        }

        if (!trimmedAnswer.equalsIgnoreCase(round.answer)) {
            Toast.makeText(requireContext(), R.string.step_by_step_wrong, Toast.LENGTH_SHORT).show();
            return;
        }

        int visibleClues = resolveVisibleCluesCount(state, round, getRemoteRemainingSeconds(state));
        int awardedPoints = SharedMatchState.PHASE_KPP_BONUS.equals(state.phase)
                ? 5
                : 20 - (visibleClues - 1) * 2;

        Map<String, Object> updates = new HashMap<>();
        if (state.activePlayer == 1) {
            updates.put("playerOneScore", state.playerOneScore + awardedPoints);
        } else {
            updates.put("playerTwoScore", state.playerTwoScore + awardedPoints);
        }
        updates.put("phase", SharedMatchState.PHASE_KPP_DONE);
        updates.put("activePlayer", 0);
        updates.put("phaseStartedAt", System.currentTimeMillis());
        updates.put("phaseDurationSeconds", 2);
        updates.put("revealedAnswer", round.answer);
        updates.put("phaseMessage", SharedMatchState.PHASE_KPP_BONUS.equals(state.phase)
                ? getString(R.string.shared_match_kpp_bonus_success_phase_format, state.activePlayer)
                : getString(R.string.shared_match_kpp_success_phase_format, state.activePlayer, awardedPoints));
        activity.updateSharedMatch(updates);

        if (SharedMatchState.PHASE_KPP_BONUS.equals(state.phase)) {
            if (host().shouldPersistStatistics()) {
                firestoreRepository.updateKorakPoKorakStatistics(0, 5, false);
            }
        } else {
            if (host().shouldPersistStatistics()) {
                firestoreRepository.updateKorakPoKorakStatistics(visibleClues, awardedPoints, true);
            }
        }
    }

    private void handleRemoteTimeout(SharedMatchState state, SharedKorakPoKorakRound round) {
        GameHostActivity activity = (GameHostActivity) requireActivity();

        if (activity.getLocalPlayerNumber() != state.activePlayer) {
            return;
        }

        if (SharedMatchState.PHASE_KPP_STARTER.equals(state.phase)) {
            if (activity.getLocalPlayerNumber() == 1) {
                if (host().shouldPersistStatistics()) {
                    firestoreRepository.updateKorakPoKorakStatistics(round.clues.size(), 0, false);
                }
            }

            Map<String, Object> updates = new HashMap<>();
            updates.put("phase", SharedMatchState.PHASE_KPP_BONUS);
            updates.put("activePlayer", state.activePlayer == 1 ? 2 : 1);
            updates.put("phaseStartedAt", System.currentTimeMillis());
            updates.put("phaseDurationSeconds", 10);
            updates.put("phaseMessage", getString(
                    R.string.shared_match_kpp_bonus_phase_format,
                    state.activePlayer,
                    state.activePlayer == 1 ? 2 : 1
            ));
            activity.updateSharedMatch(updates);
            return;
        }

        if (SharedMatchState.PHASE_KPP_BONUS.equals(state.phase)) {
            Map<String, Object> updates = new HashMap<>();
            updates.put("phase", SharedMatchState.PHASE_KPP_DONE);
            updates.put("activePlayer", 0);
            updates.put("phaseStartedAt", System.currentTimeMillis());
            updates.put("phaseDurationSeconds", 2);
            updates.put("revealedAnswer", round.answer);
            updates.put("phaseMessage", getString(R.string.step_by_step_no_bonus_points));
            activity.updateSharedMatch(updates);
        }
    }

    private void scheduleRemoteAdvanceIfCoordinator(SharedMatchState state) {
        GameHostActivity activity = (GameHostActivity) requireActivity();

        if (!activity.isRemoteProgressCoordinator()) {
            return;
        }

        submitButton.postDelayed(() -> {
            if (!isAdded()) {
                return;
            }

            SharedMatchState currentState = activity.getSharedMatchState();
            if (currentState == null
                    || currentState.currentTurnIndex != state.currentTurnIndex
                    || !SharedMatchState.PHASE_KPP_DONE.equals(currentState.phase)) {
                return;
            }

            if (currentState.currentTurnIndex == 0) {
                Map<String, Object> updates = new HashMap<>();
                updates.put("phase", SharedMatchState.PHASE_KPP_STARTER);
                updates.put("currentTurnIndex", 1);
                updates.put("activePlayer", 2);
                updates.put("phaseStartedAt", System.currentTimeMillis());
                updates.put("phaseDurationSeconds", 70);
                updates.put("phaseMessage", getString(R.string.shared_match_kpp_round_two_phase));
                updates.put("revealedAnswer", "");
                activity.updateSharedMatch(updates);
            } else {
                activity.goToNextRound();
            }
        }, 1800L);
    }

    private int getRemoteRemainingSeconds(SharedMatchState state) {
        if (state == null || state.phaseDurationSeconds <= 0) {
            return 0;
        }

        long elapsedMs = System.currentTimeMillis() - state.phaseStartedAt;
        int remaining = state.phaseDurationSeconds - (int) Math.floor(elapsedMs / 1000d);
        return Math.max(0, remaining);
    }

    private int resolveVisibleCluesCount(
            SharedMatchState state,
            SharedKorakPoKorakRound round,
            int remainingSeconds
    ) {
        if (round == null || round.clues == null || round.clues.isEmpty()) {
            return 0;
        }

        if (SharedMatchState.PHASE_KPP_BONUS.equals(state.phase)
                || SharedMatchState.PHASE_KPP_DONE.equals(state.phase)) {
            return round.clues.size();
        }

        int elapsed = Math.max(0, state.phaseDurationSeconds - remainingSeconds);
        int opened = 1 + (elapsed / 10);
        return Math.min(round.clues.size(), Math.max(1, opened));
    }

    private void renderRemoteClues(SharedKorakPoKorakRound round, int visibleCount) {
        for (int index = 0; index < clueViews.length; index++) {
            if (round.clues != null && index < round.clues.size() && index < visibleCount) {
                clueViews[index].setText(round.clues.get(index));
            } else {
                clueViews[index].setText("");
            }
        }
    }

    private void disableRemoteInteraction() {
        answerInput.setEnabled(false);
        submitButton.setEnabled(false);
    }
}
