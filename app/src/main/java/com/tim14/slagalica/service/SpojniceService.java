package com.tim14.slagalica.service;

import com.tim14.slagalica.model.SpojniceRound;

public class SpojniceService {

    private static final int PAIRS_PER_ROUND = 5;
    private static final int CORRECT_PAIR_POINTS = 2;

    public enum ConnectionType {
        NO_OP,
        MISSING_SELECTION,
        CORRECT,
        WRONG,
        ROUND_SOLVED,
        TURN_FINISHED
    }

    public static final class ConnectionResult {
        private final ConnectionType type;
        private final String selectedLeft;
        private final String selectedRight;
        private final int awardedPlayer;
        private final int awardedPoints;
        private final boolean roundFinished;

        public ConnectionResult(
                ConnectionType type,
                String selectedLeft,
                String selectedRight,
                int awardedPlayer,
                int awardedPoints,
                boolean roundFinished
        ) {
            this.type = type;
            this.selectedLeft = selectedLeft;
            this.selectedRight = selectedRight;
            this.awardedPlayer = awardedPlayer;
            this.awardedPoints = awardedPoints;
            this.roundFinished = roundFinished;
        }

        public ConnectionType getType() {
            return type;
        }

        public String getSelectedLeft() {
            return selectedLeft;
        }

        public String getSelectedRight() {
            return selectedRight;
        }

        public int getAwardedPlayer() {
            return awardedPlayer;
        }

        public int getAwardedPoints() {
            return awardedPoints;
        }

        public boolean isRoundFinished() {
            return roundFinished;
        }
    }

    private SpojniceRound currentRound;
    private int roundNumber = 1;
    private int currentPlayer = 1;
    private int playerOneScore;
    private int playerTwoScore;
    private int solvedPairsInRound;
    private int attemptsInTurn;
    private int secondChancePairsCount;
    private int correctPairs;
    private int totalPairs;
    private int totalScore;
    private boolean secondChance;
    private boolean roundFinished;
    private boolean gameFinished;
    private String selectedLeft = "";
    private String selectedRight = "";

    public void startGame(int playerOneScore, int playerTwoScore) {
        this.playerOneScore = playerOneScore;
        this.playerTwoScore = playerTwoScore;
        roundNumber = 1;
        currentPlayer = 1;
        correctPairs = 0;
        totalPairs = 0;
        totalScore = 0;
        gameFinished = false;
    }

    public boolean startRound(SpojniceRound round, int roundNumber) {
        if (!isRoundValid(round)) {
            return false;
        }

        currentRound = round;
        this.roundNumber = roundNumber;
        currentPlayer = roundNumber == 1 ? 1 : 2;
        solvedPairsInRound = 0;
        attemptsInTurn = 0;
        secondChancePairsCount = 0;
        secondChance = false;
        roundFinished = false;
        clearSelection();
        totalPairs += PAIRS_PER_ROUND;
        return true;
    }

    public void selectLeft(String value) {
        selectedLeft = value == null ? "" : value;
    }

    public void selectRight(String value) {
        selectedRight = value == null ? "" : value;
    }

    public ConnectionResult confirmConnection() {
        if (gameFinished || roundFinished) {
            return result(ConnectionType.NO_OP, 0, 0, false);
        }

        if (selectedLeft.isEmpty() || selectedRight.isEmpty()) {
            return result(ConnectionType.MISSING_SELECTION, 0, 0, false);
        }

        String confirmedLeft = selectedLeft;
        String confirmedRight = selectedRight;
        attemptsInTurn++;

        if (isCorrectPair(confirmedLeft, confirmedRight)) {
            if (currentPlayer == 1) {
                playerOneScore += CORRECT_PAIR_POINTS;
                totalScore += CORRECT_PAIR_POINTS;
                correctPairs++;
            } else {
                playerTwoScore += CORRECT_PAIR_POINTS;
            }

            solvedPairsInRound++;
            clearSelection();

            boolean solvedRound = solvedPairsInRound == PAIRS_PER_ROUND;
            if (solvedRound) {
                roundFinished = true;
            }

            return new ConnectionResult(
                    solvedRound ? ConnectionType.ROUND_SOLVED : ConnectionType.CORRECT,
                    confirmedLeft,
                    confirmedRight,
                    currentPlayer,
                    CORRECT_PAIR_POINTS,
                    solvedRound
            );
        }

        clearSelection();
        boolean turnFinished = shouldFinishCurrentTurn();
        return new ConnectionResult(
                turnFinished ? ConnectionType.TURN_FINISHED : ConnectionType.WRONG,
                confirmedLeft,
                confirmedRight,
                0,
                0,
                false
        );
    }

    public boolean shouldSwitchToSecondChance() {
        return !secondChance && !roundFinished && attemptsInTurn >= PAIRS_PER_ROUND;
    }

    public boolean shouldFinishSecondChance() {
        return secondChance && attemptsInTurn >= secondChancePairsCount;
    }

    public boolean startSecondChance() {
        if (roundFinished || secondChance) {
            return false;
        }

        secondChance = true;
        attemptsInTurn = 0;
        secondChancePairsCount = getRemainingPairsCount();
        clearSelection();

        if (secondChancePairsCount == 0) {
            roundFinished = true;
            return false;
        }

        currentPlayer = currentPlayer == 1 ? 2 : 1;
        return true;
    }

    public void finishRound() {
        roundFinished = true;
        clearSelection();
    }

    public void finishGame() {
        gameFinished = true;
        roundFinished = true;
        clearSelection();
    }

    public boolean isCorrectPair(String left, String right) {
        if (currentRound == null) {
            return false;
        }

        for (int index = 0; index < PAIRS_PER_ROUND; index++) {
            if (left.equals(currentRound.getLeftItems().get(index))
                    && right.equals(currentRound.getCorrectRightItems().get(index))) {
                return true;
            }
        }

        return false;
    }

    public int getRemainingPairsCount() {
        return PAIRS_PER_ROUND - solvedPairsInRound;
    }

    public int getRoundNumber() {
        return roundNumber;
    }

    public int getCurrentPlayer() {
        return currentPlayer;
    }

    public int getPlayerOneScore() {
        return playerOneScore;
    }

    public int getPlayerTwoScore() {
        return playerTwoScore;
    }

    public int getCorrectPairs() {
        return correctPairs;
    }

    public int getTotalPairs() {
        return totalPairs;
    }

    public int getTotalScore() {
        return totalScore;
    }

    public int getSecondChancePairsCount() {
        return secondChancePairsCount;
    }

    public boolean isSecondChance() {
        return secondChance;
    }

    public boolean isRoundFinished() {
        return roundFinished;
    }

    public boolean isGameFinished() {
        return gameFinished;
    }

    public String getSelectedLeft() {
        return selectedLeft;
    }

    public String getSelectedRight() {
        return selectedRight;
    }

    private boolean shouldFinishCurrentTurn() {
        return shouldSwitchToSecondChance() || shouldFinishSecondChance();
    }

    private ConnectionResult result(
            ConnectionType type,
            int awardedPlayer,
            int awardedPoints,
            boolean roundFinished
    ) {
        return new ConnectionResult(
                type,
                selectedLeft,
                selectedRight,
                awardedPlayer,
                awardedPoints,
                roundFinished
        );
    }

    private void clearSelection() {
        selectedLeft = "";
        selectedRight = "";
    }

    private boolean isRoundValid(SpojniceRound round) {
        return round != null
                && round.getLeftItems() != null
                && round.getCorrectRightItems() != null
                && round.getDisplayedRightItems() != null
                && round.getLeftItems().size() >= PAIRS_PER_ROUND
                && round.getCorrectRightItems().size() >= PAIRS_PER_ROUND
                && round.getDisplayedRightItems().size() >= PAIRS_PER_ROUND;
    }
}
