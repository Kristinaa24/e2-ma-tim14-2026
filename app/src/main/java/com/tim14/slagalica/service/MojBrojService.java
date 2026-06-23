package com.tim14.slagalica.service;

import com.tim14.slagalica.game.MojBrojExpressionHelper;
import com.tim14.slagalica.repository.LocalGameRepository;

import java.util.Arrays;

public class MojBrojService {

    public enum Phase {
        LOCK_TARGET,
        LOCK_NUMBERS,
        ENTER_EXPRESSIONS,
        FINISHED
    }

    public static final class EvaluatedExpression {
        private final boolean entered;
        private final boolean valid;
        private final Integer value;
        private final int difference;
        private final MojBrojExpressionHelper.ErrorType errorType;

        public EvaluatedExpression(
                boolean entered,
                boolean valid,
                Integer value,
                int difference,
                MojBrojExpressionHelper.ErrorType errorType
        ) {
            this.entered = entered;
            this.valid = valid;
            this.value = value;
            this.difference = difference;
            this.errorType = errorType;
        }

        public boolean isEntered() {
            return entered;
        }

        public boolean isValid() {
            return valid;
        }

        public Integer getValue() {
            return value;
        }

        public int getDifference() {
            return difference;
        }

        public MojBrojExpressionHelper.ErrorType getErrorType() {
            return errorType;
        }

        public boolean isExact() {
            return valid && difference == 0;
        }

        public boolean hasResult() {
            return valid && value != null;
        }
    }

    public static final class RoundOutcome {
        private final EvaluatedExpression playerOneResult;
        private final EvaluatedExpression playerTwoResult;
        private final int playerOnePoints;
        private final int playerTwoPoints;
        private final int playerOneStatisticsDifference;
        private final boolean playerOneStatisticsRelevant;

        public RoundOutcome(
                EvaluatedExpression playerOneResult,
                EvaluatedExpression playerTwoResult,
                int playerOnePoints,
                int playerTwoPoints,
                int playerOneStatisticsDifference,
                boolean playerOneStatisticsRelevant
        ) {
            this.playerOneResult = playerOneResult;
            this.playerTwoResult = playerTwoResult;
            this.playerOnePoints = playerOnePoints;
            this.playerTwoPoints = playerTwoPoints;
            this.playerOneStatisticsDifference = playerOneStatisticsDifference;
            this.playerOneStatisticsRelevant = playerOneStatisticsRelevant;
        }

        public EvaluatedExpression getPlayerOneResult() {
            return playerOneResult;
        }

        public EvaluatedExpression getPlayerTwoResult() {
            return playerTwoResult;
        }

        public int getPlayerOnePoints() {
            return playerOnePoints;
        }

        public int getPlayerTwoPoints() {
            return playerTwoPoints;
        }

        public int getPlayerOneStatisticsDifference() {
            return playerOneStatisticsDifference;
        }

        public boolean isPlayerOneStatisticsRelevant() {
            return playerOneStatisticsRelevant;
        }
    }

    private final LocalGameRepository localGameRepository;
    private final MojBrojExpressionHelper expressionHelper;

    private int currentRoundIndex;
    private int currentStarterPlayer;
    private int targetNumber;
    private int[] offeredNumbers = new int[6];
    private boolean targetLocked;
    private boolean numbersLocked;
    private boolean roundFinished;

    public MojBrojService(
            LocalGameRepository localGameRepository,
            MojBrojExpressionHelper expressionHelper
    ) {
        this.localGameRepository = localGameRepository;
        this.expressionHelper = expressionHelper;
    }

    public void startRound(int roundIndex) {
        currentRoundIndex = roundIndex;
        currentStarterPlayer = roundIndex == 0 ? 1 : 2;
        targetNumber = 0;
        offeredNumbers = new int[6];
        targetLocked = false;
        numbersLocked = false;
        roundFinished = false;
    }

    public int rollTargetNumber() {
        if (targetLocked || roundFinished) {
            return targetNumber;
        }

        targetNumber = localGameRepository.generateTargetNumber();
        return targetNumber;
    }

    public int[] rollOfferedNumbers() {
        if (numbersLocked || roundFinished) {
            return getOfferedNumbers();
        }

        offeredNumbers = localGameRepository.generateOfferedNumbers();
        return getOfferedNumbers();
    }

    public int lockTarget() {
        if (!targetLocked) {
            if (targetNumber == 0) {
                targetNumber = localGameRepository.generateTargetNumber();
            }

            targetLocked = true;
        }

        return targetNumber;
    }

    public int[] lockNumbers() {
        if (!numbersLocked) {
            if (offeredNumbers[0] == 0) {
                offeredNumbers = localGameRepository.generateOfferedNumbers();
            }

            numbersLocked = true;
        }

        return getOfferedNumbers();
    }

    public void setPreparedRoundData(int targetNumber, int[] offeredNumbers) {
        this.targetNumber = targetNumber;
        this.offeredNumbers = Arrays.copyOf(offeredNumbers, offeredNumbers.length);
        this.targetLocked = true;
        this.numbersLocked = true;
    }

    public boolean isInputReady() {
        return !roundFinished && targetLocked && numbersLocked;
    }

    public Phase getPhase() {
        if (roundFinished) {
            return Phase.FINISHED;
        }

        if (!targetLocked) {
            return Phase.LOCK_TARGET;
        }

        if (!numbersLocked) {
            return Phase.LOCK_NUMBERS;
        }

        return Phase.ENTER_EXPRESSIONS;
    }

    public RoundOutcome preview(String playerOneExpression, String playerTwoExpression) {
        return assessRound(playerOneExpression, playerTwoExpression);
    }

    public RoundOutcome finishRound(String playerOneExpression, String playerTwoExpression) {
        roundFinished = true;
        return assessRound(playerOneExpression, playerTwoExpression);
    }

    public int getCurrentRoundIndex() {
        return currentRoundIndex;
    }

    public int getCurrentStarterPlayer() {
        return currentStarterPlayer;
    }

    public int getTargetNumber() {
        return targetNumber;
    }

    public int[] getOfferedNumbers() {
        return Arrays.copyOf(offeredNumbers, offeredNumbers.length);
    }

    public boolean isTargetLocked() {
        return targetLocked;
    }

    public boolean isNumbersLocked() {
        return numbersLocked;
    }

    public boolean isRoundFinished() {
        return roundFinished;
    }

    private RoundOutcome assessRound(String playerOneExpression, String playerTwoExpression) {
        EvaluatedExpression playerOneResult = evaluateExpression(playerOneExpression);
        EvaluatedExpression playerTwoResult = evaluateExpression(playerTwoExpression);

        int playerOnePoints = 0;
        int playerTwoPoints = 0;

        if (playerOneResult.isExact()) {
            playerOnePoints += 10;
        }

        if (playerTwoResult.isExact()) {
            playerTwoPoints += 10;
        }

        if (playerOnePoints == 0 && playerTwoPoints == 0) {
            if (playerOneResult.hasResult() && !playerTwoResult.hasResult()) {
                playerOnePoints = 5;
            } else if (!playerOneResult.hasResult() && playerTwoResult.hasResult()) {
                playerTwoPoints = 5;
            } else if (playerOneResult.hasResult() && playerTwoResult.hasResult()) {
                if (playerOneResult.getDifference() < playerTwoResult.getDifference()) {
                    playerOnePoints = 5;
                } else if (playerTwoResult.getDifference() < playerOneResult.getDifference()) {
                    playerTwoPoints = 5;
                } else if (playerOneResult.getValue() != null
                        && playerOneResult.getValue().equals(playerTwoResult.getValue())
                        && playerOneResult.getValue() != 0) {
                    if (currentStarterPlayer == 1) {
                        playerOnePoints = 5;
                    } else {
                        playerTwoPoints = 5;
                    }
                }
            }
        }

        int playerOneStatisticsDifference = playerOneResult.hasResult()
                ? playerOneResult.getDifference()
                : targetNumber;
        boolean playerOneStatisticsRelevant = playerOneResult.hasResult() || playerOnePoints > 0;

        return new RoundOutcome(
                playerOneResult,
                playerTwoResult,
                playerOnePoints,
                playerTwoPoints,
                playerOneStatisticsDifference,
                playerOneStatisticsRelevant
        );
    }

    private EvaluatedExpression evaluateExpression(String expression) {
        String trimmedExpression = expression == null ? "" : expression.trim();

        if (trimmedExpression.isEmpty()) {
            return new EvaluatedExpression(
                    false,
                    false,
                    null,
                    Integer.MAX_VALUE,
                    MojBrojExpressionHelper.ErrorType.EMPTY
            );
        }

        MojBrojExpressionHelper.ValidationResult validationResult =
                expressionHelper.validateAndEvaluate(trimmedExpression, offeredNumbers);

        if (!validationResult.isValid()) {
            return new EvaluatedExpression(
                    true,
                    false,
                    null,
                    Integer.MAX_VALUE,
                    validationResult.getErrorType()
            );
        }

        int value = validationResult.getValue();
        return new EvaluatedExpression(
                true,
                true,
                value,
                Math.abs(targetNumber - value),
                MojBrojExpressionHelper.ErrorType.NONE
        );
    }
}
