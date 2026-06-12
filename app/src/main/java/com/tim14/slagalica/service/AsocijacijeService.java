package com.tim14.slagalica.service;

import com.tim14.slagalica.model.AsocijacijeRound;
import java.util.List;

public class AsocijacijeService {

    private AsocijacijeRound roundData;
    private int currentPlayer = 1;
    private boolean[][] openedFields = new boolean[4][4]; // [column][row]
    private boolean[] columnSolved = new boolean[4];
    private int[] columnSolvers = new int[4]; // 0: none, 1: P1, 2: P2
    private boolean finalSolved = false;
    private int finalSolver = 0;
    private boolean hasOpenedFieldThisTurn = false;

    private int scoreP1 = 0;
    private int scoreP2 = 0;

    public AsocijacijeService(AsocijacijeRound data, int starter) {
        this.roundData = data;
        this.currentPlayer = starter;
    }

    public boolean openField(int col, int row) {
        if (hasOpenedFieldThisTurn || openedFields[col][row] || columnSolved[col] || finalSolved) return false;
        openedFields[col][row] = true;
        hasOpenedFieldThisTurn = true;
        return true;
    }

    public boolean canGuessColumn(int col) {
        if (columnSolved[col] || finalSolved || !hasOpenedFieldThisTurn) return false;
        for (int i = 0; i < 4; i++) {
            if (openedFields[col][i]) return true;
        }
        return false;
    }

    public boolean canGuessFinal() {
        if (finalSolved || !hasOpenedFieldThisTurn) return false;
        for (int i = 0; i < 4; i++) {
            if (columnSolved[i]) return true;
        }
        return false;
    }

    public int checkColumnSolution(int col, String attempt) {
        if (!canGuessColumn(col)) return 0;
        String correct = getColumnSolution(col);
        if (correct.equalsIgnoreCase(attempt.trim())) {
            // CRITICAL: Calculate points BEFORE revealing all fields
            int unopened = countUnopenedInColumn(col);
            int points = 2 + unopened;
            
            columnSolved[col] = true;
            columnSolvers[col] = currentPlayer;
            revealColumn(col);
            
            addPoints(points);
            return points;
        }
        return -1; // Wrong
    }

    public int checkFinalSolution(String attempt) {
        if (!canGuessFinal()) return 0;
        if (roundData.finalSolution.equalsIgnoreCase(attempt.trim())) {
            finalSolved = true;
            finalSolver = currentPlayer;
            
            int totalFinalPoints = 7;
            
            for (int i = 0; i < 4; i++) {
                if (columnSolvers[i] == 0) {
                    int unopenedInCol = countUnopenedInColumn(i);
                    totalFinalPoints += (2 + unopenedInCol);

                    columnSolved[i] = true;
                    columnSolvers[i] = currentPlayer;
                    revealColumn(i);
                }
            }
            addPoints(totalFinalPoints);
            return totalFinalPoints;
        }
        return -1;
    }

    private int countUnopenedInColumn(int col) {
        int count = 0;
        for (int i = 0; i < 4; i++) {
            if (!openedFields[col][i]) count++;
        }
        return count;
    }

    private void revealColumn(int col) {
        for (int i = 0; i < 4; i++) openedFields[col][i] = true;
    }

    private void addPoints(int p) {
        if (currentPlayer == 1) scoreP1 += p;
        else scoreP2 += p;
    }

    public void nextTurn() {
        currentPlayer = (currentPlayer == 1) ? 2 : 1;
        hasOpenedFieldThisTurn = false;
    }

    public String getClue(int col, int row) {
        switch (col) {
            case 0: return roundData.columnA_clues.get(row);
            case 1: return roundData.columnB_clues.get(row);
            case 2: return roundData.columnC_clues.get(row);
            case 3: return roundData.columnD_clues.get(row);
            default: return "";
        }
    }

    public String getColumnSolution(int col) {
        switch (col) {
            case 0: return roundData.columnA_solution;
            case 1: return roundData.columnB_solution;
            case 2: return roundData.columnC_solution;
            case 3: return roundData.columnD_solution;
            default: return "";
        }
    }

    public int getCurrentPlayer() { return currentPlayer; }
    public boolean isFinalSolved() { return finalSolved; }
    public int getScoreP1() { return scoreP1; }
    public int getScoreP2() { return scoreP2; }
    public boolean isFieldOpened(int col, int row) { return openedFields[col][row]; }
    public boolean isColumnSolved(int col) { return columnSolved[col]; }
    public String getFinalSolution() { return roundData.finalSolution; }
    public boolean hasOpenedFieldThisTurn() { return hasOpenedFieldThisTurn; }
    public int getColumnSolver(int col) { return columnSolvers[col]; }
    public int getFinalSolver() { return finalSolver; }
}
