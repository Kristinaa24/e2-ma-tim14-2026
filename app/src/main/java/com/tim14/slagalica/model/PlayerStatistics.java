package com.tim14.slagalica.model;

import java.util.ArrayList;
import java.util.List;

public class PlayerStatistics {
    public String userId;

    public int gamesPlayed;
    public int wins;
    public int losses;

    public int koZnaZnaCorrect;
    public int koZnaZnaWrong;
    public int koZnaZnaTotalScore;

    public int spojnicaCorrectPairs;
    public int spojnicaTotalPairs;
    public int spojnicaTotalScore;

    public int korakPoKorakSolved;
    public int korakPoKorakTotalRounds;
    public int korakPoKorakBestStep;
    public int korakPoKorakTotalScore;
    public int korakPoKorakStep1Hits;
    public int korakPoKorakStep2Hits;
    public int korakPoKorakStep3Hits;
    public int korakPoKorakStep4Hits;
    public int korakPoKorakStep5Hits;
    public int korakPoKorakStep6Hits;
    public int korakPoKorakStep7Hits;

    public int mojBrojExactHits;
    public int mojBrojCloseHits;
    public int mojBrojTotalRounds;
    public int mojBrojTotalScore;

    public int skockoTotalScore;
    public int skockoSolvedCount;
    public List<Integer> skockoAttemptsCount = new ArrayList<>(); // 0: 1st attempt, 1: 2nd, ...

    public int asocijacijeSolved;
    public int asocijacijeUnsolved;
    public int asocijacijeTotalScore;

    public PlayerStatistics() {
        // Empty constructor required for Firebase
    }

    public PlayerStatistics(String userId) {
        this.userId = userId;
        this.gamesPlayed = 0;
        this.wins = 0;
        this.losses = 0;
        this.koZnaZnaCorrect = 0;
        this.koZnaZnaWrong = 0;
        this.koZnaZnaTotalScore = 0;
        this.spojnicaCorrectPairs = 0;
        this.spojnicaTotalPairs = 0;
        this.spojnicaTotalScore = 0;
        this.korakPoKorakSolved = 0;
        this.korakPoKorakTotalRounds = 0;
        this.korakPoKorakBestStep = 0;
        this.korakPoKorakTotalScore = 0;
        this.korakPoKorakStep1Hits = 0;
        this.korakPoKorakStep2Hits = 0;
        this.korakPoKorakStep3Hits = 0;
        this.korakPoKorakStep4Hits = 0;
        this.korakPoKorakStep5Hits = 0;
        this.korakPoKorakStep6Hits = 0;
        this.korakPoKorakStep7Hits = 0;
        this.mojBrojExactHits = 0;
        this.mojBrojCloseHits = 0;
        this.mojBrojTotalRounds = 0;
        this.mojBrojTotalScore = 0;
        this.skockoTotalScore = 0;
        this.skockoSolvedCount = 0;
        this.asocijacijeSolved = 0;
        this.asocijacijeUnsolved = 0;
        this.asocijacijeTotalScore = 0;
        for (int i = 0; i < 6; i++) {
            this.skockoAttemptsCount.add(0);
        }
    }
}
