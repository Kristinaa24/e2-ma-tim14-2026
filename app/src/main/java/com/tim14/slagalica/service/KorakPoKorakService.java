package com.tim14.slagalica.service;

import com.tim14.slagalica.model.KorakPoKorakRound;
import com.tim14.slagalica.repository.LocalGameRepository;

import java.util.List;

public class KorakPoKorakService {

    public enum ResolutionType {
        NO_OP,
        INVALID_EMPTY,
        INVALID_WRONG,
        BONUS_STARTED,
        STARTER_SUCCESS,
        BONUS_SUCCESS,
        BONUS_EXPIRED
    }

    public static final class StatisticsUpdate {
        private final int openedClues;
        private final int score;
        private final boolean solved;

        public StatisticsUpdate(int openedClues, int score, boolean solved) {
            this.openedClues = openedClues;
            this.score = score;
            this.solved = solved;
        }

        public int getOpenedClues() {
            return openedClues;
        }

        public int getScore() {
            return score;
        }

        public boolean isSolved() {
            return solved;
        }
    }

    public static final class Resolution {
        private final ResolutionType type;
        private final int awardedPlayer;
        private final int awardedPoints;
        private final StatisticsUpdate statisticsUpdate;

        public Resolution(
                ResolutionType type,
                int awardedPlayer,
                int awardedPoints,
                StatisticsUpdate statisticsUpdate
        ) {
            this.type = type;
            this.awardedPlayer = awardedPlayer;
            this.awardedPoints = awardedPoints;
            this.statisticsUpdate = statisticsUpdate;
        }

        public ResolutionType getType() {
            return type;
        }

        public int getAwardedPlayer() {
            return awardedPlayer;
        }

        public int getAwardedPoints() {
            return awardedPoints;
        }

        public StatisticsUpdate getStatisticsUpdate() {
            return statisticsUpdate;
        }
    }

    private final LocalGameRepository localGameRepository;

    private List<KorakPoKorakRound> matchRounds;
    private KorakPoKorakRound currentRound;
    private int currentTurnIndex;
    private int currentStarterPlayer;
    private int bonusPlayer;
    private int openedClues;
    private int nextRevealAt;
    private boolean bonusMode;
    private boolean turnFinished;

    public KorakPoKorakService(LocalGameRepository localGameRepository) {
        this.localGameRepository = localGameRepository;
    }

    public void startMatch() {
        matchRounds = localGameRepository.getKorakPoKorakMatchRounds();
    }

    public void startTurn(int turnIndex) {
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
    }

    public boolean onTick(int remainingSeconds) {
        if (bonusMode || turnFinished || currentRound == null) {
            return false;
        }

        if (remainingSeconds == nextRevealAt && openedClues < currentRound.getClues().length) {
            openedClues++;
            nextRevealAt -= 10;
            return true;
        }

        return false;
    }

    public Resolution submitAnswer(String answer) {
        if (turnFinished || currentRound == null) {
            return new Resolution(ResolutionType.NO_OP, 0, 0, null);
        }

        String trimmedAnswer = answer == null ? "" : answer.trim();

        if (trimmedAnswer.isEmpty()) {
            return new Resolution(ResolutionType.INVALID_EMPTY, 0, 0, null);
        }

        if (!trimmedAnswer.equalsIgnoreCase(currentRound.getAnswer())) {
            return new Resolution(ResolutionType.INVALID_WRONG, 0, 0, null);
        }

        if (bonusMode) {
            turnFinished = true;

            StatisticsUpdate statisticsUpdate = bonusPlayer == 1
                    ? new StatisticsUpdate(0, 5, false)
                    : null;

            return new Resolution(
                    ResolutionType.BONUS_SUCCESS,
                    bonusPlayer,
                    5,
                    statisticsUpdate
            );
        }

        turnFinished = true;

        int earnedPoints = 20 - (openedClues - 1) * 2;
        StatisticsUpdate statisticsUpdate = currentStarterPlayer == 1
                ? new StatisticsUpdate(openedClues, earnedPoints, true)
                : null;

        return new Resolution(
                ResolutionType.STARTER_SUCCESS,
                currentStarterPlayer,
                earnedPoints,
                statisticsUpdate
        );
    }

    public Resolution handleStarterTimeout() {
        if (turnFinished || currentRound == null) {
            return new Resolution(ResolutionType.NO_OP, 0, 0, null);
        }

        bonusMode = true;
        openedClues = currentRound.getClues().length;

        StatisticsUpdate statisticsUpdate = currentStarterPlayer == 1
                ? new StatisticsUpdate(currentRound.getClues().length, 0, false)
                : null;

        return new Resolution(
                ResolutionType.BONUS_STARTED,
                0,
                0,
                statisticsUpdate
        );
    }

    public Resolution handleBonusTimeout() {
        if (turnFinished) {
            return new Resolution(ResolutionType.NO_OP, 0, 0, null);
        }

        turnFinished = true;
        StatisticsUpdate statisticsUpdate = bonusPlayer == 1
                ? new StatisticsUpdate(currentRound.getClues().length, 0, false)
                : null;

        return new Resolution(ResolutionType.BONUS_EXPIRED, 0, 0, statisticsUpdate);
    }

    public void revealAllClues() {
        if (currentRound == null) {
            return;
        }

        openedClues = currentRound.getClues().length;
    }

    public String[] getVisibleClues() {
        if (currentRound == null) {
            return new String[0];
        }

        String[] clues = currentRound.getClues();
        String[] visibleClues = new String[clues.length];

        for (int index = 0; index < clues.length; index++) {
            visibleClues[index] = index < openedClues ? clues[index] : "";
        }

        return visibleClues;
    }

    public String getCurrentAnswer() {
        return currentRound == null ? "" : currentRound.getAnswer();
    }

    public int getCurrentTurnIndex() {
        return currentTurnIndex;
    }

    public int getCurrentStarterPlayer() {
        return currentStarterPlayer;
    }

    public int getBonusPlayer() {
        return bonusPlayer;
    }

    public boolean isBonusMode() {
        return bonusMode;
    }
}
