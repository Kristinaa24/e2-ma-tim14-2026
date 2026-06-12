package com.tim14.slagalica.service;

import com.tim14.slagalica.model.PlayerStatistics;

public class StatisticsService {

    public static final class StatisticsUiData {
        private final int gamesPlayed;
        private final int wins;
        private final int losses;
        private final int koZnaZnaSuccessPercent;
        private final int koZnaZnaCorrect;
        private final int koZnaZnaWrong;
        private final int koZnaZnaTotalScore;
        private final int spojniceSuccessPercent;
        private final int spojniceCorrectPairs;
        private final int spojniceWrongPairs;
        private final int spojniceTotalScore;
        private final int mojBrojExactHits;
        private final int mojBrojCloseHits;
        private final int mojBrojSuccessPercent;
        private final int mojBrojTotalScore;
        private final int korakPoKorakSolved;
        private final int korakPoKorakSuccessPercent;
        private final String korakPoKorakBestStep;
        private final int korakPoKorakTotalScore;
        private final int[] korakPoKorakStepPercents;

        public StatisticsUiData(
                int gamesPlayed,
                int wins,
                int losses,
                int koZnaZnaSuccessPercent,
                int koZnaZnaCorrect,
                int koZnaZnaWrong,
                int koZnaZnaTotalScore,
                int spojniceSuccessPercent,
                int spojniceCorrectPairs,
                int spojniceWrongPairs,
                int spojniceTotalScore,
                int mojBrojExactHits,
                int mojBrojCloseHits,
                int mojBrojSuccessPercent,
                int mojBrojTotalScore,
                int korakPoKorakSolved,
                int korakPoKorakSuccessPercent,
                String korakPoKorakBestStep,
                int korakPoKorakTotalScore,
                int[] korakPoKorakStepPercents
        ) {
            this.gamesPlayed = gamesPlayed;
            this.wins = wins;
            this.losses = losses;
            this.koZnaZnaSuccessPercent = koZnaZnaSuccessPercent;
            this.koZnaZnaCorrect = koZnaZnaCorrect;
            this.koZnaZnaWrong = koZnaZnaWrong;
            this.koZnaZnaTotalScore = koZnaZnaTotalScore;
            this.spojniceSuccessPercent = spojniceSuccessPercent;
            this.spojniceCorrectPairs = spojniceCorrectPairs;
            this.spojniceWrongPairs = spojniceWrongPairs;
            this.spojniceTotalScore = spojniceTotalScore;
            this.mojBrojExactHits = mojBrojExactHits;
            this.mojBrojCloseHits = mojBrojCloseHits;
            this.mojBrojSuccessPercent = mojBrojSuccessPercent;
            this.mojBrojTotalScore = mojBrojTotalScore;
            this.korakPoKorakSolved = korakPoKorakSolved;
            this.korakPoKorakSuccessPercent = korakPoKorakSuccessPercent;
            this.korakPoKorakBestStep = korakPoKorakBestStep;
            this.korakPoKorakTotalScore = korakPoKorakTotalScore;
            this.korakPoKorakStepPercents = korakPoKorakStepPercents.clone();
        }

        public int getGamesPlayed() {
            return gamesPlayed;
        }

        public int getWins() {
            return wins;
        }

        public int getLosses() {
            return losses;
        }

        public int getKoZnaZnaSuccessPercent() {
            return koZnaZnaSuccessPercent;
        }

        public int getKoZnaZnaCorrect() {
            return koZnaZnaCorrect;
        }

        public int getKoZnaZnaWrong() {
            return koZnaZnaWrong;
        }

        public int getKoZnaZnaTotalScore() {
            return koZnaZnaTotalScore;
        }

        public int getSpojniceSuccessPercent() {
            return spojniceSuccessPercent;
        }

        public int getSpojniceCorrectPairs() {
            return spojniceCorrectPairs;
        }

        public int getSpojniceWrongPairs() {
            return spojniceWrongPairs;
        }

        public int getSpojniceTotalScore() {
            return spojniceTotalScore;
        }

        public int getMojBrojExactHits() {
            return mojBrojExactHits;
        }

        public int getMojBrojCloseHits() {
            return mojBrojCloseHits;
        }

        public int getMojBrojSuccessPercent() {
            return mojBrojSuccessPercent;
        }

        public int getMojBrojTotalScore() {
            return mojBrojTotalScore;
        }

        public int getKorakPoKorakSolved() {
            return korakPoKorakSolved;
        }

        public int getKorakPoKorakSuccessPercent() {
            return korakPoKorakSuccessPercent;
        }

        public String getKorakPoKorakBestStep() {
            return korakPoKorakBestStep;
        }

        public int getKorakPoKorakTotalScore() {
            return korakPoKorakTotalScore;
        }

        public int[] getKorakPoKorakStepPercents() {
            return korakPoKorakStepPercents.clone();
        }
    }

    public StatisticsUiData prepareStatistics(PlayerStatistics statistics) {
        if (statistics == null) {
            statistics = new PlayerStatistics();
        }

        int totalKoZnaZnaAnswers =
                statistics.koZnaZnaCorrect + statistics.koZnaZnaWrong;
        int koZnaZnaSuccessPercent = calculatePercent(
                statistics.koZnaZnaCorrect,
                totalKoZnaZnaAnswers
        );
        int spojniceSuccessPercent = calculatePercent(
                statistics.spojnicaCorrectPairs,
                statistics.spojnicaTotalPairs
        );
        int spojniceWrongPairs = Math.max(
                0,
                statistics.spojnicaTotalPairs - statistics.spojnicaCorrectPairs
        );
        int mojBrojTotalRounds = Math.max(
                statistics.mojBrojTotalRounds,
                statistics.mojBrojExactHits
        );
        int korakPoKorakTotalRounds = Math.max(
                statistics.korakPoKorakTotalRounds,
                statistics.korakPoKorakSolved
        );
        int mojBrojSuccessPercent = calculatePercent(
                statistics.mojBrojExactHits,
                mojBrojTotalRounds
        );
        int korakPoKorakSuccessPercent = calculatePercent(
                statistics.korakPoKorakSolved,
                korakPoKorakTotalRounds
        );
        int[] korakPoKorakStepPercents = calculateKorakPoKorakStepPercents(
                statistics,
                korakPoKorakTotalRounds
        );
        String korakPoKorakBestStep = statistics.korakPoKorakBestStep > 0
                ? String.valueOf(statistics.korakPoKorakBestStep)
                : "-";

        return new StatisticsUiData(
                statistics.gamesPlayed,
                statistics.wins,
                statistics.losses,
                koZnaZnaSuccessPercent,
                statistics.koZnaZnaCorrect,
                statistics.koZnaZnaWrong,
                statistics.koZnaZnaTotalScore,
                spojniceSuccessPercent,
                statistics.spojnicaCorrectPairs,
                spojniceWrongPairs,
                statistics.spojnicaTotalScore,
                statistics.mojBrojExactHits,
                statistics.mojBrojCloseHits,
                mojBrojSuccessPercent,
                statistics.mojBrojTotalScore,
                statistics.korakPoKorakSolved,
                korakPoKorakSuccessPercent,
                korakPoKorakBestStep,
                statistics.korakPoKorakTotalScore,
                korakPoKorakStepPercents
        );
    }

    private int[] calculateKorakPoKorakStepPercents(
            PlayerStatistics statistics,
            int totalRounds
    ) {
        return new int[]{
                calculatePercent(statistics.korakPoKorakStep1Hits, totalRounds),
                calculatePercent(statistics.korakPoKorakStep2Hits, totalRounds),
                calculatePercent(statistics.korakPoKorakStep3Hits, totalRounds),
                calculatePercent(statistics.korakPoKorakStep4Hits, totalRounds),
                calculatePercent(statistics.korakPoKorakStep5Hits, totalRounds),
                calculatePercent(statistics.korakPoKorakStep6Hits, totalRounds),
                calculatePercent(statistics.korakPoKorakStep7Hits, totalRounds)
        };
    }

    private int calculatePercent(int value, int total) {
        if (total <= 0) {
            return 0;
        }

        int percent = value * 100 / total;
        return Math.max(0, Math.min(100, percent));
    }

}
