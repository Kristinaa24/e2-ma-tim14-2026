package com.tim14.slagalica.fragments;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.tim14.slagalica.GameHostActivity;
import com.tim14.slagalica.R;
import com.tim14.slagalica.game.BaseGameFragment;
import com.tim14.slagalica.model.SharedMatchState;
import com.tim14.slagalica.model.SharedSpojniceRound;
import com.tim14.slagalica.model.SpojniceRound;
import com.tim14.slagalica.repository.FirebaseCallback;
import com.tim14.slagalica.repository.FirestoreRepository;
import com.tim14.slagalica.service.SpojniceService;

import java.util.ArrayList;
import java.util.List;

public class SpojniceFragment extends BaseGameFragment {

    private static final String TAG = "SpojniceFragment";

    private TextView roundText;
    private TextView currentPlayerText;
    private TextView selectedPairText;
    private TextView secondChanceInfoText;

    private Button left1Button;
    private Button left2Button;
    private Button left3Button;
    private Button left4Button;
    private Button left5Button;
    private Button right1Button;
    private Button right2Button;
    private Button right3Button;
    private Button right4Button;
    private Button right5Button;
    private Button confirmConnectionButton;

    private FirestoreRepository firestoreRepository;
    private SpojniceService spojniceService;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private int round = 1;
    private boolean gameFinished;
    private boolean remoteMode;
    private boolean remoteStatisticsPersisted;
    private int lastScheduledRemoteRound = -1;
    private String remoteSelectedLeft = "";
    private String remoteSelectedRight = "";

    private CountDownTimer roundTimer;

    private List<SpojniceRound> rounds = new ArrayList<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_spojnice, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        firestoreRepository = new FirestoreRepository();
        spojniceService = new SpojniceService();
        remoteMode = ((GameHostActivity) requireActivity()).isRemoteMatchMode();

        roundText = view.findViewById(R.id.roundText);
        currentPlayerText = view.findViewById(R.id.currentPlayerText);
        selectedPairText = view.findViewById(R.id.selectedPairText);
        secondChanceInfoText = view.findViewById(R.id.secondChanceInfoText);

        left1Button = view.findViewById(R.id.left1Button);
        left2Button = view.findViewById(R.id.left2Button);
        left3Button = view.findViewById(R.id.left3Button);
        left4Button = view.findViewById(R.id.left4Button);
        left5Button = view.findViewById(R.id.left5Button);

        right1Button = view.findViewById(R.id.right1Button);
        right2Button = view.findViewById(R.id.right2Button);
        right3Button = view.findViewById(R.id.right3Button);
        right4Button = view.findViewById(R.id.right4Button);
        right5Button = view.findViewById(R.id.right5Button);

        confirmConnectionButton = view.findViewById(R.id.confirmConnectionButton);
        confirmConnectionButton.setOnClickListener(v -> {
            if (remoteMode) {
                confirmRemoteConnection();
            } else {
                confirmConnection();
            }
        });

        host().setPhaseText(getString(R.string.phase_spojnice));

        if (remoteMode) {
            renderRemoteRound();
            return;
        }

        spojniceService.startGame(host().getPlayerOneScore(), host().getPlayerTwoScore());
        updateScores();
        host().setTimerValue(30);

        disableAllPairButtons();
        confirmConnectionButton.setEnabled(false);
        secondChanceInfoText.setText(getString(R.string.loading_spojnice));
        loadRoundsFromFirestore();
    }

    private void loadRoundsFromFirestore() {
        firestoreRepository.getSpojniceRounds(new FirebaseCallback<List<SpojniceRound>>() {
            @Override
            public void onSuccess(List<SpojniceRound> result) {
                if (!isAdded() || gameFinished) {
                    return;
                }

                if (result == null || result.size() < 2) {
                    secondChanceInfoText.setText(getString(R.string.not_enough_spojnice_rounds));
                    Toast.makeText(requireContext(),
                            R.string.need_at_least_2_spojnice_rounds,
                            Toast.LENGTH_LONG).show();
                    return;
                }

                rounds = result;
                Log.d(TAG, "Spojnice rounds loaded from Firestore: " + rounds.size());
                startRound();
            }

            @Override
            public void onError(String error) {
                if (!isAdded() || gameFinished) {
                    return;
                }

                secondChanceInfoText.setText(getString(R.string.spojnice_load_failed));
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Error loading Spojnice rounds: " + error);
            }
        });
    }

    private void startRound() {
        if (gameFinished || !startRoundService()) {
            return;
        }

        setRoundItems();
        enableAllPairButtons();
        confirmConnectionButton.setEnabled(true);
        secondChanceInfoText.setText(getString(R.string.spojnice_start_info));

        updateHeader();
        updateScores();
        updateSelectedPairText();
        startRoundTimer();
    }

    private void setRoundItems() {
        int index = round - 1;
        SpojniceRound currentRound = rounds.get(index);

        left1Button.setText(currentRound.getLeftItems().get(0));
        left2Button.setText(currentRound.getLeftItems().get(1));
        left3Button.setText(currentRound.getLeftItems().get(2));
        left4Button.setText(currentRound.getLeftItems().get(3));
        left5Button.setText(currentRound.getLeftItems().get(4));

        right1Button.setText(currentRound.getDisplayedRightItems().get(0));
        right2Button.setText(currentRound.getDisplayedRightItems().get(1));
        right3Button.setText(currentRound.getDisplayedRightItems().get(2));
        right4Button.setText(currentRound.getDisplayedRightItems().get(3));
        right5Button.setText(currentRound.getDisplayedRightItems().get(4));

        left1Button.setOnClickListener(v -> selectLeft(left1Button.getText().toString()));
        left2Button.setOnClickListener(v -> selectLeft(left2Button.getText().toString()));
        left3Button.setOnClickListener(v -> selectLeft(left3Button.getText().toString()));
        left4Button.setOnClickListener(v -> selectLeft(left4Button.getText().toString()));
        left5Button.setOnClickListener(v -> selectLeft(left5Button.getText().toString()));

        right1Button.setOnClickListener(v -> selectRight(right1Button.getText().toString()));
        right2Button.setOnClickListener(v -> selectRight(right2Button.getText().toString()));
        right3Button.setOnClickListener(v -> selectRight(right3Button.getText().toString()));
        right4Button.setOnClickListener(v -> selectRight(right4Button.getText().toString()));
        right5Button.setOnClickListener(v -> selectRight(right5Button.getText().toString()));
    }

    private void startRoundTimer() {
        cancelRoundTimer();

        roundTimer = new CountDownTimer(31000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                host().setTimerValue((int) (millisUntilFinished / 1000));
            }

            @Override
            public void onFinish() {
                roundTimer = null;
                host().setTimerValue(0);

                if (!spojniceService.isSecondChance()
                        && spojniceService.getRemainingPairsCount() > 0) {
                    switchToSecondPlayer();
                } else {
                    prepareNextRoundOrEnd();
                }
            }
        };

        roundTimer.start();
    }

    private void selectLeft(String value) {
        spojniceService.selectLeft(value);
        updateSelectedPairText();
    }

    private void selectRight(String value) {
        spojniceService.selectRight(value);
        updateSelectedPairText();
    }

    private void updateSelectedPairText() {
        String selectedLeft = spojniceService.getSelectedLeft();
        String selectedRight = spojniceService.getSelectedRight();

        if (selectedLeft.isEmpty() && selectedRight.isEmpty()) {
            selectedPairText.setText(getString(R.string.selected_pair_none));
        } else {
            selectedPairText.setText(getString(R.string.selected_pair_format, selectedLeft, selectedRight));
        }
    }

    private void confirmConnection() {
        if (gameFinished) {
            return;
        }

        SpojniceService.ConnectionResult result = spojniceService.confirmConnection();

        if (result.getType() == SpojniceService.ConnectionType.MISSING_SELECTION) {
            Toast.makeText(requireContext(), R.string.select_both_columns, Toast.LENGTH_SHORT).show();
            return;
        }

        if (result.getType() == SpojniceService.ConnectionType.NO_OP) {
            return;
        }

        if (result.getType() == SpojniceService.ConnectionType.CORRECT
                || result.getType() == SpojniceService.ConnectionType.ROUND_SOLVED) {
            disableSolvedPair(result.getSelectedLeft(), result.getSelectedRight());
            Toast.makeText(requireContext(), R.string.correct_connection_points, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(requireContext(), R.string.wrong_connection, Toast.LENGTH_SHORT).show();
        }

        updateSelectedPairText();
        updateScores();

        if (result.getType() == SpojniceService.ConnectionType.ROUND_SOLVED) {
            Toast.makeText(requireContext(), R.string.all_pairs_solved, Toast.LENGTH_SHORT).show();
            handler.postDelayed(() -> {
                if (isAdded()) {
                    prepareNextRoundOrEnd();
                }
            }, 900);
            return;
        }

        if (spojniceService.shouldSwitchToSecondChance()) {
            handler.postDelayed(() -> {
                if (isAdded()) {
                    switchToSecondPlayer();
                }
            }, 900);
            return;
        }

        if (spojniceService.shouldFinishSecondChance()) {
            handler.postDelayed(() -> {
                if (isAdded()) {
                    prepareNextRoundOrEnd();
                }
            }, 900);
        }
    }

    private void switchToSecondPlayer() {
        cancelRoundTimer();

        if (!spojniceService.startSecondChance()) {
            prepareNextRoundOrEnd();
            return;
        }

        updateHeader();
        updateSelectedPairText();

        secondChanceInfoText.setText(
                getString(
                        R.string.second_chance_player_format,
                        spojniceService.getCurrentPlayer(),
                        spojniceService.getSecondChancePairsCount()
                )
        );

        Toast.makeText(requireContext(),
                R.string.second_player_remaining_time,
                Toast.LENGTH_LONG).show();

        handler.postDelayed(() -> {
            if (isAdded()) {
                startRoundTimer();
            }
        }, 2000);
    }

    private void prepareNextRoundOrEnd() {
        cancelRoundTimer();
        spojniceService.finishRound();
        confirmConnectionButton.setEnabled(false);

        if (round == 1) {
            Toast.makeText(requireContext(), R.string.round_one_finished, Toast.LENGTH_LONG).show();
            handler.postDelayed(() -> {
                if (isAdded()) {
                    round = 2;
                    startRound();
                }
            }, 2500);
        } else {
            endGame();
        }
    }

    private void disableSolvedPair(String left, String right) {
        disableLeftButton(left);
        disableRightButton(right);
    }

    private void disableLeftButton(String left) {
        if (left.equals(left1Button.getText().toString())) fadeSolvedButton(left1Button);
        if (left.equals(left2Button.getText().toString())) fadeSolvedButton(left2Button);
        if (left.equals(left3Button.getText().toString())) fadeSolvedButton(left3Button);
        if (left.equals(left4Button.getText().toString())) fadeSolvedButton(left4Button);
        if (left.equals(left5Button.getText().toString())) fadeSolvedButton(left5Button);
    }

    private void disableRightButton(String right) {
        if (right.equals(right1Button.getText().toString())) fadeSolvedButton(right1Button);
        if (right.equals(right2Button.getText().toString())) fadeSolvedButton(right2Button);
        if (right.equals(right3Button.getText().toString())) fadeSolvedButton(right3Button);
        if (right.equals(right4Button.getText().toString())) fadeSolvedButton(right4Button);
        if (right.equals(right5Button.getText().toString())) fadeSolvedButton(right5Button);
    }

    private void fadeSolvedButton(Button button) {
        button.setEnabled(false);
        button.setAlpha(0.45f);
    }

    private void enableAllPairButtons() {
        for (Button button : getLeftButtons()) {
            button.setEnabled(true);
            button.setAlpha(1f);
        }

        for (Button button : getRightButtons()) {
            button.setEnabled(true);
            button.setAlpha(1f);
        }
    }

    private void disableAllPairButtons() {
        for (Button button : getLeftButtons()) {
            button.setEnabled(false);
        }

        for (Button button : getRightButtons()) {
            button.setEnabled(false);
        }
    }

    private void endGame() {
        if (gameFinished) {
            return;
        }

        gameFinished = true;
        spojniceService.finishGame();
        cancelRoundTimer();
        disableAllPairButtons();
        confirmConnectionButton.setEnabled(false);

        secondChanceInfoText.setText(getString(R.string.end_of_spojnice));

        firestoreRepository.updateSpojniceStatistics(
                spojniceService.getCorrectPairs(),
                spojniceService.getTotalPairs(),
                spojniceService.getTotalScore()
        );

        host().setScores(
                spojniceService.getPlayerOneScore(),
                spojniceService.getPlayerTwoScore()
        );
        host().setTimerValue(0);
        host().setPhaseText(getString(R.string.phase_spojnice_finished));

        Toast.makeText(requireContext(),
                getString(
                        R.string.spojnice_end_format,
                        spojniceService.getPlayerOneScore(),
                        spojniceService.getPlayerTwoScore()
                ),
                Toast.LENGTH_LONG).show();

        handler.postDelayed(() -> {
            if (isAdded()) {
                host().goToNextRound();
            }
        }, 3000);
    }

    private void updateHeader() {
        roundText.setText(getString(R.string.round_counter_format, round));
        currentPlayerText.setText(
                getString(R.string.current_player_format, spojniceService.getCurrentPlayer())
        );
    }

    private void updateScores() {
        host().setScores(
                spojniceService.getPlayerOneScore(),
                spojniceService.getPlayerTwoScore()
        );
    }

    private boolean startRoundService() {
        if (spojniceService.startRound(rounds.get(round - 1), round)) {
            return true;
        }

        Toast.makeText(requireContext(), R.string.invalid_spojnice_data, Toast.LENGTH_LONG).show();
        endGame();
        return false;
    }

    private void cancelRoundTimer() {
        if (roundTimer != null) {
            roundTimer.cancel();
            roundTimer = null;
        }
    }

    private Button[] getLeftButtons() {
        return new Button[]{left1Button, left2Button, left3Button, left4Button, left5Button};
    }

    private Button[] getRightButtons() {
        return new Button[]{right1Button, right2Button, right3Button, right4Button, right5Button};
    }

    private void renderRemoteRound() {
        stopRoundTimer();
        cancelRoundTimer();

        GameHostActivity activity = (GameHostActivity) requireActivity();
        SharedMatchState state = activity.getSharedMatchState();
        SharedSpojniceRound roundState =
                state == null ? null : activity.getSharedSpojniceRound(state.currentTurnIndex);

        if (state == null || roundState == null) {
            return;
        }

        bindRemoteRoundItems(roundState);
        applyRemoteSolvedPairs(roundState);

        roundText.setText(getString(R.string.round_counter_format, state.currentTurnIndex + 1));
        currentPlayerText.setText(getString(R.string.current_player_format, state.activePlayer));
        secondChanceInfoText.setText(state.phaseMessage);
        selectedPairText.setText(
                remoteSelectedLeft.isEmpty() && remoteSelectedRight.isEmpty()
                        ? getString(R.string.selected_pair_none)
                        : getString(R.string.selected_pair_format, remoteSelectedLeft, remoteSelectedRight)
        );
        host().setTimerValue(getRemoteRemainingSeconds(state));

        if (SharedMatchState.PHASE_SPOJNICE_DONE.equals(state.phase)) {
            configureRemoteButtons(roundState, false);
            confirmConnectionButton.setEnabled(false);
            maybePersistRemoteStatistics(state);
            scheduleRemoteAdvanceIfCoordinator(state);
            return;
        }

        boolean localTurn = activity.getLocalPlayerNumber() == state.activePlayer;
        configureRemoteButtons(roundState, localTurn);
        confirmConnectionButton.setEnabled(localTurn);

        if (getRemoteRemainingSeconds(state) > 0) {
            startRoundTimer(getRemoteRemainingSeconds(state), this::handleRemoteTimeout);
        }
    }

    private void bindRemoteRoundItems(SharedSpojniceRound roundState) {
        bindButtonTexts(getLeftButtons(), roundState.leftItems);
        bindButtonTexts(getRightButtons(), roundState.displayedRightItems);
    }

    private void bindButtonTexts(Button[] buttons, List<String> values) {
        for (int index = 0; index < buttons.length; index++) {
            buttons[index].setText(values.get(index));
        }
    }

    private void configureRemoteButtons(SharedSpojniceRound roundState, boolean localTurn) {
        for (Button button : getLeftButtons()) {
            String value = button.getText().toString();
            boolean solved = roundState.solvedLeftItems.contains(value);
            button.setEnabled(localTurn && !solved);
            button.setOnClickListener(localTurn && !solved ? v -> {
                remoteSelectedLeft = value;
                updateRemoteSelectedText();
            } : null);
            button.setAlpha(solved ? 0.45f : 1f);
        }

        for (Button button : getRightButtons()) {
            String value = button.getText().toString();
            boolean solved = roundState.solvedRightItems.contains(value);
            button.setEnabled(localTurn && !solved);
            button.setOnClickListener(localTurn && !solved ? v -> {
                remoteSelectedRight = value;
                updateRemoteSelectedText();
            } : null);
            button.setAlpha(solved ? 0.45f : 1f);
        }
    }

    private void applyRemoteSolvedPairs(SharedSpojniceRound roundState) {
        for (Button button : getLeftButtons()) {
            boolean solved = roundState.solvedLeftItems.contains(button.getText().toString());
            button.setAlpha(solved ? 0.45f : 1f);
        }

        for (Button button : getRightButtons()) {
            boolean solved = roundState.solvedRightItems.contains(button.getText().toString());
            button.setAlpha(solved ? 0.45f : 1f);
        }
    }

    private void updateRemoteSelectedText() {
        selectedPairText.setText(
                remoteSelectedLeft.isEmpty() && remoteSelectedRight.isEmpty()
                        ? getString(R.string.selected_pair_none)
                        : getString(R.string.selected_pair_format, remoteSelectedLeft, remoteSelectedRight)
        );
    }

    private void confirmRemoteConnection() {
        GameHostActivity activity = (GameHostActivity) requireActivity();
        SharedMatchState state = activity.getSharedMatchState();
        SharedSpojniceRound currentRound =
                state == null ? null : activity.getSharedSpojniceRound(state.currentTurnIndex);

        if (state == null
                || currentRound == null
                || !SharedMatchState.PHASE_SPOJNICE_PLAY.equals(state.phase)
                || activity.getLocalPlayerNumber() != state.activePlayer) {
            return;
        }

        if (remoteSelectedLeft.isEmpty() || remoteSelectedRight.isEmpty()) {
            Toast.makeText(requireContext(), R.string.select_both_columns, Toast.LENGTH_SHORT).show();
            return;
        }

        SharedSpojniceRound updatedRound = copyRound(currentRound);
        updatedRound.attemptsInTurn += 1;
        boolean correct = isCorrectPair(updatedRound, remoteSelectedLeft, remoteSelectedRight);

        int playerOneScore = state.playerOneScore;
        int playerTwoScore = state.playerTwoScore;

        if (correct && !updatedRound.solvedLeftItems.contains(remoteSelectedLeft)) {
            updatedRound.solvedLeftItems.add(remoteSelectedLeft);
            updatedRound.solvedRightItems.add(remoteSelectedRight);
            updatedRound.solvedByPlayers.add(state.activePlayer);

            if (state.activePlayer == 1) {
                playerOneScore += 2;
            } else {
                playerTwoScore += 2;
            }
        }

        remoteSelectedLeft = "";
        remoteSelectedRight = "";
        updateRemoteSelectedText();

        List<SharedSpojniceRound> allRounds = new ArrayList<>(state.spojniceRounds);
        allRounds.set(state.currentTurnIndex, updatedRound);

        if (updatedRound.solvedLeftItems.size() == 5) {
            updatedRound.finished = true;
            java.util.Map<String, Object> updates = new java.util.HashMap<>();
            updates.put("spojniceRounds", allRounds);
            updates.put("playerOneScore", playerOneScore);
            updates.put("playerTwoScore", playerTwoScore);
            updates.put("phase", SharedMatchState.PHASE_SPOJNICE_DONE);
            updates.put("activePlayer", 0);
            updates.put("phaseStartedAt", System.currentTimeMillis());
            updates.put("phaseDurationSeconds", 1);
            updates.put(
                    "phaseMessage",
                    getString(R.string.shared_match_spojnice_done_phase_format, state.currentTurnIndex + 1)
            );
            activity.updateSharedMatch(updates);
            return;
        }

        if (!updatedRound.secondChance && updatedRound.attemptsInTurn >= 5) {
            switchToRemoteSecondChance(activity, state, updatedRound, allRounds, playerOneScore, playerTwoScore);
            return;
        }

        if (updatedRound.secondChance && updatedRound.attemptsInTurn >= updatedRound.secondChancePairsCount) {
            updatedRound.finished = true;
            java.util.Map<String, Object> updates = new java.util.HashMap<>();
            updates.put("spojniceRounds", allRounds);
            updates.put("playerOneScore", playerOneScore);
            updates.put("playerTwoScore", playerTwoScore);
            updates.put("phase", SharedMatchState.PHASE_SPOJNICE_DONE);
            updates.put("activePlayer", 0);
            updates.put("phaseStartedAt", System.currentTimeMillis());
            updates.put("phaseDurationSeconds", 1);
            updates.put(
                    "phaseMessage",
                    getString(R.string.shared_match_spojnice_done_phase_format, state.currentTurnIndex + 1)
            );
            activity.updateSharedMatch(updates);
            return;
        }

        java.util.Map<String, Object> updates = new java.util.HashMap<>();
        updates.put("spojniceRounds", allRounds);
        updates.put("playerOneScore", playerOneScore);
        updates.put("playerTwoScore", playerTwoScore);
        updates.put(
                "phaseMessage",
                correct
                        ? getString(R.string.correct_connection_points)
                        : getString(R.string.wrong_connection)
        );
        activity.updateSharedMatch(updates);
    }

    private void switchToRemoteSecondChance(
            GameHostActivity activity,
            SharedMatchState state,
            SharedSpojniceRound updatedRound,
            List<SharedSpojniceRound> allRounds,
            int playerOneScore,
            int playerTwoScore
    ) {
        updatedRound.secondChance = true;
        updatedRound.currentPlayer = state.activePlayer == 1 ? 2 : 1;
        updatedRound.secondChancePairsCount = 5 - updatedRound.solvedLeftItems.size();
        updatedRound.attemptsInTurn = 0;

        activity.updateSharedMatch(new java.util.HashMap<String, Object>() {{
            put("spojniceRounds", allRounds);
            put("playerOneScore", playerOneScore);
            put("playerTwoScore", playerTwoScore);
            put("activePlayer", updatedRound.currentPlayer);
            put("phase", SharedMatchState.PHASE_SPOJNICE_PLAY);
            put("phaseStartedAt", System.currentTimeMillis());
            put("phaseDurationSeconds", 30);
            put(
                    "phaseMessage",
                    getString(
                            R.string.shared_match_spojnice_second_chance_phase_format,
                            updatedRound.currentPlayer,
                            updatedRound.secondChancePairsCount
                    )
            );
        }});
    }

    private void handleRemoteTimeout() {
        GameHostActivity activity = (GameHostActivity) requireActivity();
        SharedMatchState state = activity.getSharedMatchState();
        SharedSpojniceRound currentRound =
                state == null ? null : activity.getSharedSpojniceRound(state.currentTurnIndex);

        if (state == null
                || currentRound == null
                || activity.getLocalPlayerNumber() != state.activePlayer
                || !SharedMatchState.PHASE_SPOJNICE_PLAY.equals(state.phase)) {
            return;
        }

        SharedSpojniceRound updatedRound = copyRound(currentRound);
        List<SharedSpojniceRound> allRounds = new ArrayList<>(state.spojniceRounds);
        allRounds.set(state.currentTurnIndex, updatedRound);

        if (!updatedRound.secondChance && updatedRound.solvedLeftItems.size() < 5) {
            switchToRemoteSecondChance(
                    activity,
                    state,
                    updatedRound,
                    allRounds,
                    state.playerOneScore,
                    state.playerTwoScore
            );
            return;
        }

        updatedRound.finished = true;
        activity.updateSharedMatch(new java.util.HashMap<String, Object>() {{
            put("spojniceRounds", allRounds);
            put("phase", SharedMatchState.PHASE_SPOJNICE_DONE);
            put("activePlayer", 0);
            put("phaseStartedAt", System.currentTimeMillis());
            put("phaseDurationSeconds", 1);
            put("phaseMessage", getString(R.string.shared_match_spojnice_done_phase_format, state.currentTurnIndex + 1));
        }});
    }

    private void scheduleRemoteAdvanceIfCoordinator(SharedMatchState state) {
        GameHostActivity activity = (GameHostActivity) requireActivity();

        if (activity.getLocalPlayerNumber() != 1 || lastScheduledRemoteRound == state.currentTurnIndex) {
            return;
        }

        lastScheduledRemoteRound = state.currentTurnIndex;

        handler.postDelayed(() -> {
            if (!isAdded()) {
                return;
            }

            SharedMatchState currentState = activity.getSharedMatchState();
            if (currentState == null
                    || currentState.currentTurnIndex != state.currentTurnIndex
                    || !SharedMatchState.PHASE_SPOJNICE_DONE.equals(currentState.phase)) {
                return;
            }

            if (currentState.currentTurnIndex == 0) {
                activity.updateSharedMatch(new java.util.HashMap<String, Object>() {{
                    put("currentTurnIndex", 1);
                    put("activePlayer", 2);
                    put("phase", SharedMatchState.PHASE_SPOJNICE_PLAY);
                    put("phaseStartedAt", System.currentTimeMillis());
                    put("phaseDurationSeconds", 30);
                    put("phaseMessage", getString(R.string.shared_match_spojnice_round_two_phase));
                }});
            } else {
                host().goToNextRound();
            }
        }, 1200L);
    }

    private void maybePersistRemoteStatistics(SharedMatchState state) {
        if (remoteStatisticsPersisted || state.currentTurnIndex != 1) {
            return;
        }

        int localPlayer = ((GameHostActivity) requireActivity()).getLocalPlayerNumber();
        int correctPairs = 0;

        for (SharedSpojniceRound roundState : state.spojniceRounds) {
            for (Integer solver : roundState.solvedByPlayers) {
                if (solver != null && solver == localPlayer) {
                    correctPairs++;
                }
            }
        }

        firestoreRepository.updateSpojniceStatistics(correctPairs, 10, correctPairs * 2);
        remoteStatisticsPersisted = true;
    }

    private boolean isCorrectPair(
            SharedSpojniceRound roundState,
            String selectedLeft,
            String selectedRight
    ) {
        for (int index = 0; index < roundState.leftItems.size(); index++) {
            if (selectedLeft.equals(roundState.leftItems.get(index))
                    && selectedRight.equals(roundState.correctRightItems.get(index))) {
                return true;
            }
        }

        return false;
    }

    private SharedSpojniceRound copyRound(SharedSpojniceRound source) {
        SharedSpojniceRound copy = new SharedSpojniceRound();
        copy.title = source.title;
        copy.leftItems = new ArrayList<>(source.leftItems);
        copy.correctRightItems = new ArrayList<>(source.correctRightItems);
        copy.displayedRightItems = new ArrayList<>(source.displayedRightItems);
        copy.solvedLeftItems = new ArrayList<>(source.solvedLeftItems);
        copy.solvedRightItems = new ArrayList<>(source.solvedRightItems);
        copy.solvedByPlayers = new ArrayList<>(source.solvedByPlayers);
        copy.starterPlayer = source.starterPlayer;
        copy.currentPlayer = source.currentPlayer;
        copy.secondChance = source.secondChance;
        copy.attemptsInTurn = source.attemptsInTurn;
        copy.secondChancePairsCount = source.secondChancePairsCount;
        copy.finished = source.finished;
        return copy;
    }

    private int getRemoteRemainingSeconds(SharedMatchState state) {
        if (state == null || state.phaseDurationSeconds <= 0) {
            return 0;
        }

        long elapsedMs = System.currentTimeMillis() - state.phaseStartedAt;
        int remaining = state.phaseDurationSeconds - (int) Math.floor(elapsedMs / 1000d);
        return Math.max(0, remaining);
    }

    @Override
    public void onDestroyView() {
        cancelRoundTimer();
        handler.removeCallbacksAndMessages(null);
        super.onDestroyView();
    }
}
