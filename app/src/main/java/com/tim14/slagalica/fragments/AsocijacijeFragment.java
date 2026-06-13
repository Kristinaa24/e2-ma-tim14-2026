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
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.tim14.slagalica.R;
import com.tim14.slagalica.game.BaseGameFragment;
import com.tim14.slagalica.game.GameNavigator;
import com.tim14.slagalica.model.AsocijacijeRound;
import com.tim14.slagalica.repository.FirebaseCallback;
import com.tim14.slagalica.repository.FirestoreRepository;
import com.tim14.slagalica.repository.LocalGameRepository;
import com.tim14.slagalica.service.AsocijacijeService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AsocijacijeFragment extends BaseGameFragment {

    private GameNavigator navigator;
    private FirestoreRepository firestoreRepository;
    private AsocijacijeService asocijacijeService;
    private int currentRound = 1;
    private int playerOneScore = 0;
    private int playerTwoScore = 0;
    private List<AsocijacijeRound> roundDataList;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private Button[][] fieldButtons = new Button[4][4];
    private EditText[] columnInputs = new EditText[4];
    private EditText finalInput;
    private Button btnConfirm;

    @Override
    public void onAttach(@NonNull android.content.Context context) {
        super.onAttach(context);
        if (context instanceof GameNavigator) {
            navigator = (GameNavigator) context;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_asocijacije, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        firestoreRepository = new FirestoreRepository(requireContext());
        playerOneScore = navigator.getPlayerOneScore();
        playerTwoScore = navigator.getPlayerTwoScore();

        setupUI(view);
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

        for (int i = 0; i < 4; i++) {
            final int col = i;
            for (int j = 0; j < 4; j++) {
                final int row = j;
                fieldButtons[i][j].setOnClickListener(v -> openField(col, row));
            }
        }

        columnInputs[0] = view.findViewById(R.id.etSolutionA);
        columnInputs[1] = view.findViewById(R.id.etSolutionB);
        columnInputs[2] = view.findViewById(R.id.etSolutionC);
        columnInputs[3] = view.findViewById(R.id.etSolutionD);
        finalInput = view.findViewById(R.id.etFinalSolution);
        btnConfirm = view.findViewById(R.id.confirmSolutionButton);

        btnConfirm.setOnClickListener(v -> checkSolutions());
    }

    private void loadRoundsAndStart() {
        firestoreRepository.getAsocijacijeRounds(new FirebaseCallback<List<AsocijacijeRound>>() {
            @Override
            public void onSuccess(List<AsocijacijeRound> result) {
                if (!isAdded()) return;
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
                if (!isAdded()) return;
                roundDataList = new LocalGameRepository().getAsocijacijeRounds();
                startRound();
            }
        });
    }

    private void startRound() {
        if (currentRound > roundDataList.size()) {
            navigator.goToNextRound();
            return;
        }
        asocijacijeService = new AsocijacijeService(roundDataList.get(currentRound - 1), currentRound);
        resetUI();
        updateTurnDisplay();
        startRoundTimer(120, this::handleRoundTimeout);
    }

    private void resetUI() {
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                String label = getColumnLabel(i) + (j + 1);
                fieldButtons[i][j].setText(label);
                fieldButtons[i][j].setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.slagalica_light_blue));
                fieldButtons[i][j].setTextColor(ContextCompat.getColor(requireContext(), R.color.slagalica_dark_blue));
                fieldButtons[i][j].setEnabled(true);
            }
            columnInputs[i].setText("");
            columnInputs[i].setEnabled(false);
            columnInputs[i].setBackgroundResource(R.drawable.bg_button_primary);
            columnInputs[i].setTextColor(ContextCompat.getColor(requireContext(), R.color.slagalica_dark_blue));
        }
        finalInput.setText("");
        finalInput.setEnabled(false);
        finalInput.setBackgroundResource(R.color.slagalica_red);
        finalInput.setTextColor(Color.WHITE);
    }

    private void updateInputStates() {
        if (!asocijacijeService.hasOpenedFieldThisTurn()) {
            for (int i = 0; i < 4; i++) columnInputs[i].setEnabled(false);
            finalInput.setEnabled(false);
            return;
        }

        for (int i = 0; i < 4; i++) {
            columnInputs[i].setEnabled(asocijacijeService.canGuessColumn(i));
        }
        finalInput.setEnabled(asocijacijeService.canGuessFinal());
    }

    private void openField(int col, int row) {
        if (asocijacijeService.openField(col, row)) {
            fieldButtons[col][row].setText(asocijacijeService.getClue(col, row));
            fieldButtons[col][row].setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.slagalica_yellow));
            fieldButtons[col][row].setTextColor(ContextCompat.getColor(requireContext(), R.color.slagalica_dark_blue));
            updateInputStates();
        } else if (asocijacijeService.hasOpenedFieldThisTurn()) {
            Toast.makeText(requireContext(), "You already opened a field this turn.", Toast.LENGTH_SHORT).show();
        }
    }

    private void checkSolutions() {
        boolean correctlyGuessedSomething = false;
        boolean triedToGuess = false;

        if (!asocijacijeService.hasOpenedFieldThisTurn()) {
            Toast.makeText(requireContext(), "You must open a field first!", Toast.LENGTH_SHORT).show();
            return;
        }

        for (int i = 0; i < 4; i++) {
            String input = columnInputs[i].getText().toString().trim();
            if (!input.isEmpty() && !asocijacijeService.isColumnSolved(i)) {
                triedToGuess = true;
                int pts = asocijacijeService.checkColumnSolution(i, input);
                if (pts > 0) {
                    revealColumn(i);
                    awardPoints(asocijacijeService.getCurrentPlayer(), pts);
                    correctlyGuessedSomething = true;
                    Toast.makeText(requireContext(), getString(R.string.msg_column_solved, getColumnLabel(i), pts), Toast.LENGTH_SHORT).show();
                    updateInputStates();
                } else {
                    columnInputs[i].setText("");
                }
            }
        }

        String finInput = finalInput.getText().toString().trim();
        if (!finInput.isEmpty() && !asocijacijeService.isFinalSolved()) {
            if (!asocijacijeService.canGuessFinal()) {
                Toast.makeText(requireContext(), "You must solve at least one column first!", Toast.LENGTH_SHORT).show();
                finalInput.setText("");
                return;
            }
            triedToGuess = true;
            int pts = asocijacijeService.checkFinalSolution(finInput);
            if (pts > 0) {
                revealAll();
                awardPoints(asocijacijeService.getCurrentPlayer(), pts);
                correctlyGuessedSomething = true;
                Toast.makeText(requireContext(), getString(R.string.msg_final_solved, pts), Toast.LENGTH_SHORT).show();
                handleRoundWin();
                return;
            } else {
                finalInput.setText("");
            }
        }

        if (triedToGuess) {
            if (!correctlyGuessedSomething) {
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

    private void revealColumn(int col) {
        int solver = asocijacijeService.getColumnSolver(col);
        int colorRes = R.color.slagalica_yellow; 
        if (solver == 1) colorRes = R.color.slagalica_blue;
        else if (solver == 2) colorRes = R.color.slagalica_red;

        for (int i = 0; i < 4; i++) {
            fieldButtons[col][i].setText(asocijacijeService.getClue(col, i));
            fieldButtons[col][i].setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), colorRes));
            fieldButtons[col][i].setEnabled(false);
            if (solver != 0) fieldButtons[col][i].setTextColor(Color.WHITE);
        }
        columnInputs[col].setText(asocijacijeService.getColumnSolution(col));
        columnInputs[col].setEnabled(false);
        if (solver == 1) {
            columnInputs[col].setBackgroundResource(R.color.slagalica_blue);
            columnInputs[col].setTextColor(Color.WHITE);
        } else if (solver == 2) {
            columnInputs[col].setBackgroundResource(R.color.slagalica_red);
            columnInputs[col].setTextColor(Color.WHITE);
        } else {
            columnInputs[col].setBackgroundResource(R.drawable.avatar_silver);
            columnInputs[col].setTextColor(ContextCompat.getColor(requireContext(), R.color.slagalica_dark_blue));
        }
    }

    private void revealAll() {
        for (int i = 0; i < 4; i++) revealColumn(i);
        finalInput.setText(asocijacijeService.getFinalSolution());
        finalInput.setEnabled(false);
        int solver = asocijacijeService.getFinalSolver();
        if (solver == 1) finalInput.setBackgroundResource(R.color.slagalica_blue);
        else if (solver == 2) finalInput.setBackgroundResource(R.color.slagalica_red);
        else finalInput.setBackgroundResource(R.drawable.avatar_silver);
        finalInput.setTextColor(solver != 0 ? Color.WHITE : ContextCompat.getColor(requireContext(), R.color.slagalica_dark_blue));
    }

    private void handleRoundWin() {
        stopRoundTimer();
        firestoreRepository.updateAsocijacijeStatistics(true, asocijacijeService.getScoreP1() + asocijacijeService.getScoreP2());
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
                navigator.goToNextRound();
            }
        }, 3000);
    }

    private void awardPoints(int player, int pts) {
        if (player == 1) {
            playerOneScore += pts;
            navigator.setScores(playerOneScore, playerTwoScore);
        } else {
            playerTwoScore += pts;
            navigator.setScores(playerOneScore, playerTwoScore);
        }
    }

    private void updateTurnDisplay() {
        navigator.setPhaseText("Player " + asocijacijeService.getCurrentPlayer() + "'s turn - Round " + currentRound);
    }

    private String getColumnLabel(int i) {
        return i == 0 ? "A" : i == 1 ? "B" : i == 2 ? "C" : "D";
    }
}
