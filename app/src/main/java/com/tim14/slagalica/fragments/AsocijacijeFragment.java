package com.tim14.slagalica.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.tim14.slagalica.GameHostActivity;
import com.tim14.slagalica.R;
import com.tim14.slagalica.game.BaseGameFragment;
import com.tim14.slagalica.model.AsocijacijeRound;
import com.tim14.slagalica.model.SharedAsocijacijeRound;
import com.tim14.slagalica.model.SharedMatchState;
import com.tim14.slagalica.repository.FirebaseCallback;
import com.tim14.slagalica.repository.FirestoreRepository;
import com.tim14.slagalica.repository.LocalGameRepository;
import com.tim14.slagalica.service.AsocijacijeService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AsocijacijeFragment extends BaseGameFragment {

    private FirestoreRepository firestoreRepository;
    private AsocijacijeService asocijacijeService;
    private int currentRound = 1;
    private int playerOneScore = 0;
    private int playerTwoScore = 0;
    private boolean remoteMode;
    private int lastRemoteStatsRound = -1;
    private int lastScheduledRemoteRound = -1;
    private List<AsocijacijeRound> roundDataList;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private final Button[][] fieldButtons = new Button[4][4];
    private final EditText[] columnInputs = new EditText[4];
    private EditText finalInput;
    private Button btnConfirm;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.fragment_asocijacije, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        firestoreRepository = new FirestoreRepository(requireContext());
        remoteMode = ((GameHostActivity) requireActivity()).isRemoteMatchMode();

        if (!remoteMode) {
            playerOneScore = host().getPlayerOneScore();
            playerTwoScore = host().getPlayerTwoScore();
        }

        setupUI(view);

        if (remoteMode) {
            renderRemoteRound();
            return;
        }

        loadRoundsAndStart();
    }

    private void setupUI(View view) {
        fieldButtons[0][0] = view.findViewById(R.id.btnA1);
        fieldButtons[0][1] = view.findViewById(R.id.btnA2);
        fieldButtons[0][2] = view.findViewById(R.id.btnA3);
        fieldButtons[0][3] = view.findViewById(R.id.btnA4);
        fieldButtons[1][0] = view.findViewById(R.id.btnB1);
        fieldButtons[1][1] = view.findViewById(R.id.btnB2);
        fieldButtons[1][2] = view.findViewById(R.id.btnB3);
        fieldButtons[1][3] = view.findViewById(R.id.btnB4);
        fieldButtons[2][0] = view.findViewById(R.id.btnC1);
        fieldButtons[2][1] = view.findViewById(R.id.btnC2);
        fieldButtons[2][2] = view.findViewById(R.id.btnC3);
        fieldButtons[2][3] = view.findViewById(R.id.btnC4);
        fieldButtons[3][0] = view.findViewById(R.id.btnD1);
        fieldButtons[3][1] = view.findViewById(R.id.btnD2);
        fieldButtons[3][2] = view.findViewById(R.id.btnD3);
        fieldButtons[3][3] = view.findViewById(R.id.btnD4);

        for (int column = 0; column < 4; column++) {
            int finalColumn = column;
            for (int row = 0; row < 4; row++) {
                int finalRow = row;
                fieldButtons[column][row].setOnClickListener(v -> {
                    if (remoteMode) {
                        openRemoteField(finalColumn, finalRow);
                    } else {
                        openField(finalColumn, finalRow);
                    }
                });
            }
        }

        columnInputs[0] = view.findViewById(R.id.etSolutionA);
        columnInputs[1] = view.findViewById(R.id.etSolutionB);
        columnInputs[2] = view.findViewById(R.id.etSolutionC);
        columnInputs[3] = view.findViewById(R.id.etSolutionD);
        finalInput = view.findViewById(R.id.etFinalSolution);
        btnConfirm = view.findViewById(R.id.confirmSolutionButton);
        btnConfirm.setOnClickListener(v -> {
            if (remoteMode) {
                checkRemoteSolutions();
            } else {
                checkSolutions();
            }
        });
    }

    private void loadRoundsAndStart() {
        firestoreRepository.getAsocijacijeRounds(new FirebaseCallback<List<AsocijacijeRound>>() {
            @Override
            public void onSuccess(List<AsocijacijeRound> result) {
                if (!isAdded()) {
                    return;
                }

                if (result == null || result.isEmpty()) {
                    roundDataList = new LocalGameRepository().getAsocijacijeRounds();
                } else {
                    List<AsocijacijeRound> shuffled = new ArrayList<>(result);
                    Collections.shuffle(shuffled);
                    roundDataList = shuffled.subList(0, Math.min(2, shuffled.size()));
                }
                startRound();
            }

            @Override
            public void onError(String error) {
                if (!isAdded()) {
                    return;
                }

                roundDataList = new LocalGameRepository().getAsocijacijeRounds();
                startRound();
            }
        });
    }

    private void startRound() {
        if (currentRound > roundDataList.size()) {
            host().goToNextRound();
            return;
        }

        asocijacijeService = new AsocijacijeService(roundDataList.get(currentRound - 1), currentRound);
        resetUI();
        updateTurnDisplay();
        startRoundTimer(120, this::handleRoundTimeout);
    }

    private void resetUI() {
        for (int column = 0; column < 4; column++) {
            for (int row = 0; row < 4; row++) {
                String label = getColumnLabel(column) + (row + 1);
                fieldButtons[column][row].setText(label);
                fieldButtons[column][row].setBackgroundTintList(
                        ContextCompat.getColorStateList(requireContext(), R.color.slagalica_light_blue)
                );
                fieldButtons[column][row].setTextColor(
                        ContextCompat.getColor(requireContext(), R.color.slagalica_dark_blue)
                );
                fieldButtons[column][row].setEnabled(true);
            }

            columnInputs[column].setText("");
            columnInputs[column].setEnabled(false);
            columnInputs[column].setBackgroundResource(R.drawable.bg_button_primary);
            columnInputs[column].setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.slagalica_dark_blue)
            );
        }

        finalInput.setText("");
        finalInput.setEnabled(false);
        finalInput.setBackgroundResource(R.color.slagalica_red);
        finalInput.setTextColor(Color.WHITE);
    }

    private void updateInputStates() {
        if (!asocijacijeService.hasOpenedFieldThisTurn()) {
            for (int column = 0; column < 4; column++) {
                columnInputs[column].setEnabled(false);
            }
            finalInput.setEnabled(false);
            return;
        }

        for (int column = 0; column < 4; column++) {
            columnInputs[column].setEnabled(asocijacijeService.canGuessColumn(column));
        }
        finalInput.setEnabled(asocijacijeService.canGuessFinal());
    }

    private void openField(int col, int row) {
        if (asocijacijeService.openField(col, row)) {
            fieldButtons[col][row].setText(asocijacijeService.getClue(col, row));
            fieldButtons[col][row].setBackgroundTintList(
                    ContextCompat.getColorStateList(requireContext(), R.color.slagalica_yellow)
            );
            fieldButtons[col][row].setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.slagalica_dark_blue)
            );
            updateInputStates();
        } else if (asocijacijeService.hasOpenedFieldThisTurn()) {
            Toast.makeText(requireContext(), "You already opened a field this turn.", Toast.LENGTH_SHORT).show();
        }
    }

    private void checkSolutions() {
        boolean solvedSomething = false;
        boolean triedToGuess = false;

        if (!asocijacijeService.hasOpenedFieldThisTurn()) {
            Toast.makeText(requireContext(), "You must open a field first!", Toast.LENGTH_SHORT).show();
            return;
        }

        for (int column = 0; column < 4; column++) {
            String input = columnInputs[column].getText().toString().trim();
            if (!input.isEmpty() && !asocijacijeService.isColumnSolved(column)) {
                triedToGuess = true;
                int points = asocijacijeService.checkColumnSolution(column, input);
                if (points > 0) {
                    revealColumn(column);
                    awardPoints(asocijacijeService.getCurrentPlayer(), points);
                    solvedSomething = true;
                    updateInputStates();
                } else {
                    columnInputs[column].setText("");
                }
            }
        }

        String finalAttempt = finalInput.getText().toString().trim();
        if (!finalAttempt.isEmpty() && !asocijacijeService.isFinalSolved()) {
            if (!asocijacijeService.canGuessFinal()) {
                Toast.makeText(requireContext(), "You must solve at least one column first!", Toast.LENGTH_SHORT).show();
                finalInput.setText("");
                return;
            }

            triedToGuess = true;
            int points = asocijacijeService.checkFinalSolution(finalAttempt);
            if (points > 0) {
                revealAll();
                awardPoints(asocijacijeService.getCurrentPlayer(), points);
                solvedSomething = true;
                handleRoundWin();
                return;
            } else {
                finalInput.setText("");
            }
        }

        if (triedToGuess) {
            if (!solvedSomething) {
                asocijacijeService.nextTurn();
                updateTurnDisplay();
                updateInputStates();
                Toast.makeText(requireContext(), getString(R.string.msg_next_player), Toast.LENGTH_SHORT).show();
            }
        } else {
            asocijacijeService.nextTurn();
            updateTurnDisplay();
            updateInputStates();
        }
    }

    private void revealColumn(int column) {
        int solver = asocijacijeService.getColumnSolver(column);
        int colorRes = R.color.slagalica_yellow;
        if (solver == 1) {
            colorRes = R.color.slagalica_blue;
        } else if (solver == 2) {
            colorRes = R.color.slagalica_red;
        }

        for (int row = 0; row < 4; row++) {
            fieldButtons[column][row].setText(asocijacijeService.getClue(column, row));
            fieldButtons[column][row].setBackgroundTintList(
                    ContextCompat.getColorStateList(requireContext(), colorRes)
            );
            fieldButtons[column][row].setEnabled(false);
            if (solver != 0) {
                fieldButtons[column][row].setTextColor(Color.WHITE);
            }
        }

        columnInputs[column].setText(asocijacijeService.getColumnSolution(column));
        columnInputs[column].setEnabled(false);
        if (solver == 1) {
            columnInputs[column].setBackgroundResource(R.color.slagalica_blue);
            columnInputs[column].setTextColor(Color.WHITE);
        } else if (solver == 2) {
            columnInputs[column].setBackgroundResource(R.color.slagalica_red);
            columnInputs[column].setTextColor(Color.WHITE);
        } else {
            columnInputs[column].setBackgroundResource(R.drawable.avatar_silver);
            columnInputs[column].setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.slagalica_dark_blue)
            );
        }
    }

    private void revealAll() {
        for (int column = 0; column < 4; column++) {
            revealColumn(column);
        }
        finalInput.setText(asocijacijeService.getFinalSolution());
        finalInput.setEnabled(false);
        int solver = asocijacijeService.getFinalSolver();
        if (solver == 1) {
            finalInput.setBackgroundResource(R.color.slagalica_blue);
        } else if (solver == 2) {
            finalInput.setBackgroundResource(R.color.slagalica_red);
        } else {
            finalInput.setBackgroundResource(R.drawable.avatar_silver);
        }
        finalInput.setTextColor(
                solver != 0
                        ? Color.WHITE
                        : ContextCompat.getColor(requireContext(), R.color.slagalica_dark_blue)
        );
    }

    private void handleRoundWin() {
        stopRoundTimer();
        firestoreRepository.updateAsocijacijeStatistics(
                true,
                asocijacijeService.getScoreP1() + asocijacijeService.getScoreP2()
        );
        finishRoundAfterDelay();
    }

    private void handleRoundTimeout() {
        stopRoundTimer();
        revealAll();
        firestoreRepository.updateAsocijacijeStatistics(false, 0);
        Toast.makeText(requireContext(), getString(R.string.msg_times_up), Toast.LENGTH_SHORT).show();
        finishRoundAfterDelay();
    }

    private void finishRoundAfterDelay() {
        handler.postDelayed(() -> {
            if (currentRound == 1) {
                currentRound = 2;
                startRound();
            } else {
                host().goToNextRound();
            }
        }, 3000);
    }

    private void awardPoints(int player, int points) {
        if (player == 1) {
            playerOneScore += points;
        } else {
            playerTwoScore += points;
        }
        host().setScores(playerOneScore, playerTwoScore);
    }

    private void updateTurnDisplay() {
        host().setPhaseText("Player " + asocijacijeService.getCurrentPlayer() + "'s turn - Round " + currentRound);
    }

    private String getColumnLabel(int column) {
        return column == 0 ? "A" : column == 1 ? "B" : column == 2 ? "C" : "D";
    }

    private void renderRemoteRound() {
        stopRoundTimer();

        GameHostActivity activity = (GameHostActivity) requireActivity();
        SharedMatchState state = activity.getSharedMatchState();
        SharedAsocijacijeRound roundState =
                state == null ? null : activity.getSharedAsocijacijeRound(state.currentTurnIndex);

        if (state == null || roundState == null) {
            return;
        }

        applyRemoteBoard(roundState);
        host().setTimerValue(getRemoteRemainingSeconds(state));
        host().setPhaseText(state.phaseMessage);

        if (SharedMatchState.PHASE_ASOC_DONE.equals(state.phase)) {
            configureRemoteBoardInteraction(roundState, false);
            maybePersistRemoteStatistics(state, roundState);
            scheduleRemoteAdvanceIfCoordinator(state);
            return;
        }

        boolean localTurn = activity.getLocalPlayerNumber() == state.activePlayer;
        configureRemoteBoardInteraction(roundState, localTurn);

        if (getRemoteRemainingSeconds(state) > 0) {
            startRoundTimer(getRemoteRemainingSeconds(state), this::handleRemoteTimeout);
        }
    }

    private void applyRemoteBoard(SharedAsocijacijeRound roundState) {
        for (int column = 0; column < 4; column++) {
            for (int row = 0; row < 4; row++) {
                boolean opened = isRemoteFieldOpened(roundState, column, row);
                boolean solved = roundState.columnSolved.get(column);
                fieldButtons[column][row].setText(
                        opened || solved
                                ? getRemoteClue(roundState, column, row)
                                : getColumnLabel(column) + (row + 1)
                );
                fieldButtons[column][row].setEnabled(false);

                if (solved) {
                    int solver = roundState.columnSolvers.get(column);
                    int colorRes = solver == 1
                            ? R.color.slagalica_blue
                            : solver == 2 ? R.color.slagalica_red : R.color.slagalica_yellow;
                    fieldButtons[column][row].setBackgroundTintList(
                            ContextCompat.getColorStateList(requireContext(), colorRes)
                    );
                    fieldButtons[column][row].setTextColor(solver == 0
                            ? ContextCompat.getColor(requireContext(), R.color.slagalica_dark_blue)
                            : Color.WHITE);
                } else if (opened) {
                    fieldButtons[column][row].setBackgroundTintList(
                            ContextCompat.getColorStateList(requireContext(), R.color.slagalica_yellow)
                    );
                    fieldButtons[column][row].setTextColor(
                            ContextCompat.getColor(requireContext(), R.color.slagalica_dark_blue)
                    );
                } else {
                    fieldButtons[column][row].setBackgroundTintList(
                            ContextCompat.getColorStateList(requireContext(), R.color.slagalica_light_blue)
                    );
                    fieldButtons[column][row].setTextColor(
                            ContextCompat.getColor(requireContext(), R.color.slagalica_dark_blue)
                    );
                }
            }

            if (roundState.columnSolved.get(column)) {
                int solver = roundState.columnSolvers.get(column);
                columnInputs[column].setText(getRemoteColumnSolution(roundState, column));
                columnInputs[column].setEnabled(false);
                if (solver == 1) {
                    columnInputs[column].setBackgroundResource(R.color.slagalica_blue);
                    columnInputs[column].setTextColor(Color.WHITE);
                } else if (solver == 2) {
                    columnInputs[column].setBackgroundResource(R.color.slagalica_red);
                    columnInputs[column].setTextColor(Color.WHITE);
                } else {
                    columnInputs[column].setBackgroundResource(R.drawable.avatar_silver);
                    columnInputs[column].setTextColor(
                            ContextCompat.getColor(requireContext(), R.color.slagalica_dark_blue)
                    );
                }
            } else {
                columnInputs[column].setBackgroundResource(R.drawable.bg_button_primary);
                columnInputs[column].setTextColor(
                        ContextCompat.getColor(requireContext(), R.color.slagalica_dark_blue)
                );
            }
        }

        if (roundState.finalSolved) {
            finalInput.setText(roundState.finalSolution);
            finalInput.setEnabled(false);
            if (roundState.finalSolver == 1) {
                finalInput.setBackgroundResource(R.color.slagalica_blue);
                finalInput.setTextColor(Color.WHITE);
            } else if (roundState.finalSolver == 2) {
                finalInput.setBackgroundResource(R.color.slagalica_red);
                finalInput.setTextColor(Color.WHITE);
            } else {
                finalInput.setBackgroundResource(R.drawable.avatar_silver);
                finalInput.setTextColor(
                        ContextCompat.getColor(requireContext(), R.color.slagalica_dark_blue)
                );
            }
        }
    }

    private void configureRemoteBoardInteraction(SharedAsocijacijeRound roundState, boolean localTurn) {
        for (int column = 0; column < 4; column++) {
            for (int row = 0; row < 4; row++) {
                boolean canOpen = localTurn
                        && !roundState.hasOpenedFieldThisTurn
                        && !roundState.finalSolved
                        && !roundState.columnSolved.get(column)
                        && !isRemoteFieldOpened(roundState, column, row);

                fieldButtons[column][row].setEnabled(canOpen);
            }

            boolean canGuessColumn = localTurn
                    && canGuessRemoteColumn(roundState, column)
                    && !roundState.columnSolved.get(column);
            columnInputs[column].setEnabled(canGuessColumn);
        }

        finalInput.setEnabled(localTurn && canGuessRemoteFinal(roundState));
        btnConfirm.setEnabled(localTurn);
    }

    private void openRemoteField(int column, int row) {
        GameHostActivity activity = (GameHostActivity) requireActivity();
        SharedMatchState state = activity.getSharedMatchState();
        SharedAsocijacijeRound roundState =
                state == null ? null : activity.getSharedAsocijacijeRound(state.currentTurnIndex);

        if (state == null
                || roundState == null
                || activity.getLocalPlayerNumber() != state.activePlayer
                || roundState.hasOpenedFieldThisTurn
                || roundState.finalSolved
                || roundState.columnSolved.get(column)
                || isRemoteFieldOpened(roundState, column, row)) {
            return;
        }

        SharedAsocijacijeRound updatedRound = copyRound(roundState);
        setRemoteFieldOpened(updatedRound, column, row, true);
        updatedRound.hasOpenedFieldThisTurn = true;

        List<SharedAsocijacijeRound> allRounds = new ArrayList<>(state.asocijacijeRounds);
        allRounds.set(state.currentTurnIndex, updatedRound);

        activity.updateSharedMatch(new java.util.HashMap<String, Object>() {{
            put("asocijacijeRounds", allRounds);
            put(
                    "phaseMessage",
                    getString(
                            R.string.shared_match_asoc_field_opened_format,
                            state.activePlayer,
                            getColumnLabel(column),
                            row + 1
                    )
            );
        }});
    }

    private void checkRemoteSolutions() {
        GameHostActivity activity = (GameHostActivity) requireActivity();
        SharedMatchState state = activity.getSharedMatchState();
        SharedAsocijacijeRound roundState =
                state == null ? null : activity.getSharedAsocijacijeRound(state.currentTurnIndex);

        if (state == null
                || roundState == null
                || activity.getLocalPlayerNumber() != state.activePlayer) {
            return;
        }

        if (!roundState.hasOpenedFieldThisTurn) {
            Toast.makeText(requireContext(), "You must open a field first!", Toast.LENGTH_SHORT).show();
            return;
        }

        SharedAsocijacijeRound updatedRound = copyRound(roundState);
        boolean solvedSomething = false;
        boolean triedToGuess = false;
        int pointsForCurrentPlayer = 0;

        for (int column = 0; column < 4; column++) {
            String attempt = columnInputs[column].getText().toString().trim();
            if (!attempt.isEmpty() && canGuessRemoteColumn(updatedRound, column)) {
                triedToGuess = true;
                if (getRemoteColumnSolution(updatedRound, column).equalsIgnoreCase(attempt)) {
                    int points = 2 + countUnopenedInColumn(updatedRound, column);
                    solveRemoteColumn(updatedRound, column, state.activePlayer);
                    pointsForCurrentPlayer += points;
                    solvedSomething = true;
                } else {
                    columnInputs[column].setText("");
                }
            }
        }

        String finalAttempt = finalInput.getText().toString().trim();
        if (!finalAttempt.isEmpty() && !updatedRound.finalSolved) {
            if (!canGuessRemoteFinal(updatedRound)) {
                Toast.makeText(requireContext(), "You must solve at least one column first!", Toast.LENGTH_SHORT).show();
                finalInput.setText("");
                return;
            }

            triedToGuess = true;
            if (updatedRound.finalSolution.equalsIgnoreCase(finalAttempt)) {
                int points = 7;
                for (int column = 0; column < 4; column++) {
                    if (!updatedRound.columnSolved.get(column)) {
                        points += 2 + countUnopenedInColumn(updatedRound, column);
                        solveRemoteColumn(updatedRound, column, state.activePlayer);
                    }
                }
                updatedRound.finalSolved = true;
                updatedRound.finalSolver = state.activePlayer;
                pointsForCurrentPlayer += points;
                solvedSomething = true;
            } else {
                finalInput.setText("");
            }
        }

        if (pointsForCurrentPlayer > 0) {
            if (state.activePlayer == 1) {
                updatedRound.playerOneRoundScore += pointsForCurrentPlayer;
            } else {
                updatedRound.playerTwoRoundScore += pointsForCurrentPlayer;
            }
        }

        int playerOneScoreUpdate = state.playerOneScore + (state.activePlayer == 1 ? pointsForCurrentPlayer : 0);
        int playerTwoScoreUpdate = state.playerTwoScore + (state.activePlayer == 2 ? pointsForCurrentPlayer : 0);
        List<SharedAsocijacijeRound> allRounds = new ArrayList<>(state.asocijacijeRounds);
        allRounds.set(state.currentTurnIndex, updatedRound);

        if (updatedRound.finalSolved) {
            java.util.Map<String, Object> updates = new java.util.HashMap<>();
            updates.put("asocijacijeRounds", allRounds);
            updates.put("playerOneScore", playerOneScoreUpdate);
            updates.put("playerTwoScore", playerTwoScoreUpdate);
            updates.put("phase", SharedMatchState.PHASE_ASOC_DONE);
            updates.put("activePlayer", 0);
            updates.put("phaseStartedAt", System.currentTimeMillis());
            updates.put("phaseDurationSeconds", 2);
            updates.put(
                    "phaseMessage",
                    getString(
                            R.string.shared_match_asoc_done_phase_format,
                            updatedRound.finalSolver,
                            pointsForCurrentPlayer
                    )
            );
            activity.updateSharedMatch(updates);
            return;
        }

        if (!triedToGuess || !solvedSomething) {
            updatedRound.currentPlayer = state.activePlayer == 1 ? 2 : 1;
            updatedRound.hasOpenedFieldThisTurn = false;
            activity.updateSharedMatch(new java.util.HashMap<String, Object>() {{
                put("asocijacijeRounds", allRounds);
                put("playerOneScore", playerOneScoreUpdate);
                put("playerTwoScore", playerTwoScoreUpdate);
                put("activePlayer", updatedRound.currentPlayer);
                put(
                        "phaseMessage",
                        getString(R.string.shared_match_asoc_turn_phase_format, updatedRound.currentPlayer)
                );
            }});
            return;
        }

        activity.updateSharedMatch(new java.util.HashMap<String, Object>() {{
            put("asocijacijeRounds", allRounds);
            put("playerOneScore", playerOneScoreUpdate);
            put("playerTwoScore", playerTwoScoreUpdate);
            put(
                    "phaseMessage",
                    getString(R.string.shared_match_asoc_continue_phase_format, state.activePlayer)
            );
        }});
    }

    private void handleRemoteTimeout() {
        GameHostActivity activity = (GameHostActivity) requireActivity();
        SharedMatchState state = activity.getSharedMatchState();
        SharedAsocijacijeRound roundState =
                state == null ? null : activity.getSharedAsocijacijeRound(state.currentTurnIndex);

        if (state == null
                || roundState == null
                || activity.getLocalPlayerNumber() != 1
                || !SharedMatchState.PHASE_ASOC_PLAY.equals(state.phase)) {
            return;
        }

        activity.updateSharedMatch(new java.util.HashMap<String, Object>() {{
            put("phase", SharedMatchState.PHASE_ASOC_DONE);
            put("activePlayer", 0);
            put("phaseStartedAt", System.currentTimeMillis());
            put("phaseDurationSeconds", 2);
            put("phaseMessage", getString(R.string.msg_times_up));
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
                    || !SharedMatchState.PHASE_ASOC_DONE.equals(currentState.phase)) {
                return;
            }

            if (currentState.currentTurnIndex == 0) {
                activity.updateSharedMatch(new java.util.HashMap<String, Object>() {{
                    put("currentTurnIndex", 1);
                    put("phase", SharedMatchState.PHASE_ASOC_PLAY);
                    put("activePlayer", 2);
                    put("phaseStartedAt", System.currentTimeMillis());
                    put("phaseDurationSeconds", 120);
                    put("phaseMessage", getString(R.string.shared_match_asoc_round_two_phase));
                }});
            } else {
                host().goToNextRound();
            }
        }, 1500L);
    }

    private void maybePersistRemoteStatistics(SharedMatchState state, SharedAsocijacijeRound roundState) {
        if (lastRemoteStatsRound == state.currentTurnIndex) {
            return;
        }

        int localPlayer = ((GameHostActivity) requireActivity()).getLocalPlayerNumber();
        int localScore = localPlayer == 1 ? roundState.playerOneRoundScore : roundState.playerTwoRoundScore;
        boolean solved = roundState.finalSolver == localPlayer;
        firestoreRepository.updateAsocijacijeStatistics(solved, localScore);
        lastRemoteStatsRound = state.currentTurnIndex;
    }

    private SharedAsocijacijeRound copyRound(SharedAsocijacijeRound source) {
        SharedAsocijacijeRound copy = new SharedAsocijacijeRound();
        copy.columnASolution = source.columnASolution;
        copy.columnAClues = new ArrayList<>(source.columnAClues);
        copy.columnBSolution = source.columnBSolution;
        copy.columnBClues = new ArrayList<>(source.columnBClues);
        copy.columnCSolution = source.columnCSolution;
        copy.columnCClues = new ArrayList<>(source.columnCClues);
        copy.columnDSolution = source.columnDSolution;
        copy.columnDClues = new ArrayList<>(source.columnDClues);
        copy.finalSolution = source.finalSolution;
        copy.columnSolved = new ArrayList<>(source.columnSolved);
        copy.columnSolvers = new ArrayList<>(source.columnSolvers);
        copy.finalSolved = source.finalSolved;
        copy.finalSolver = source.finalSolver;
        copy.currentPlayer = source.currentPlayer;
        copy.hasOpenedFieldThisTurn = source.hasOpenedFieldThisTurn;
        copy.playerOneRoundScore = source.playerOneRoundScore;
        copy.playerTwoRoundScore = source.playerTwoRoundScore;

        copy.openedFields = new java.util.HashMap<>();
        for (java.util.Map.Entry<String, List<Boolean>> entry : source.openedFields.entrySet()) {
            copy.openedFields.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }

        return copy;
    }

    private void solveRemoteColumn(SharedAsocijacijeRound roundState, int column, int solver) {
        roundState.columnSolved.set(column, true);
        roundState.columnSolvers.set(column, solver);
        for (int row = 0; row < 4; row++) {
            setRemoteFieldOpened(roundState, column, row, true);
        }
    }

    private int countUnopenedInColumn(SharedAsocijacijeRound roundState, int column) {
        int count = 0;
        for (Boolean opened : getRemoteOpenedColumn(roundState, column)) {
            if (!Boolean.TRUE.equals(opened)) {
                count++;
            }
        }
        return count;
    }

    private boolean canGuessRemoteColumn(SharedAsocijacijeRound roundState, int column) {
        if (roundState.columnSolved.get(column) || roundState.finalSolved || !roundState.hasOpenedFieldThisTurn) {
            return false;
        }

        for (Boolean opened : getRemoteOpenedColumn(roundState, column)) {
            if (Boolean.TRUE.equals(opened)) {
                return true;
            }
        }

        return false;
    }

    private boolean canGuessRemoteFinal(SharedAsocijacijeRound roundState) {
        if (roundState.finalSolved || !roundState.hasOpenedFieldThisTurn) {
            return false;
        }

        for (Boolean solved : roundState.columnSolved) {
            if (Boolean.TRUE.equals(solved)) {
                return true;
            }
        }

        return false;
    }

    private boolean isRemoteFieldOpened(SharedAsocijacijeRound roundState, int column, int row) {
        List<Boolean> openedColumn = getRemoteOpenedColumn(roundState, column);
        return row >= 0 && row < openedColumn.size() && Boolean.TRUE.equals(openedColumn.get(row));
    }

    private void setRemoteFieldOpened(
            SharedAsocijacijeRound roundState,
            int column,
            int row,
            boolean opened
    ) {
        List<Boolean> openedColumn = getRemoteOpenedColumn(roundState, column);
        while (openedColumn.size() <= row) {
            openedColumn.add(false);
        }
        openedColumn.set(row, opened);
        roundState.openedFields.put(String.valueOf(column), openedColumn);
    }

    private List<Boolean> getRemoteOpenedColumn(SharedAsocijacijeRound roundState, int column) {
        if (roundState.openedFields == null) {
            roundState.openedFields = new java.util.HashMap<>();
        }

        String key = String.valueOf(column);
        List<Boolean> openedColumn = roundState.openedFields.get(key);
        if (openedColumn == null) {
            openedColumn = new ArrayList<>();
            for (int index = 0; index < 4; index++) {
                openedColumn.add(false);
            }
            roundState.openedFields.put(key, openedColumn);
        }

        return openedColumn;
    }

    private String getRemoteClue(SharedAsocijacijeRound roundState, int column, int row) {
        switch (column) {
            case 0:
                return roundState.columnAClues.get(row);
            case 1:
                return roundState.columnBClues.get(row);
            case 2:
                return roundState.columnCClues.get(row);
            case 3:
                return roundState.columnDClues.get(row);
            default:
                return "";
        }
    }

    private String getRemoteColumnSolution(SharedAsocijacijeRound roundState, int column) {
        switch (column) {
            case 0:
                return roundState.columnASolution;
            case 1:
                return roundState.columnBSolution;
            case 2:
                return roundState.columnCSolution;
            case 3:
                return roundState.columnDSolution;
            default:
                return "";
        }
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
        handler.removeCallbacksAndMessages(null);
        super.onDestroyView();
    }
}
