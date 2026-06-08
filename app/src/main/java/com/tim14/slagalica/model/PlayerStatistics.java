package com.tim14.slagalica.model;

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
    }
}