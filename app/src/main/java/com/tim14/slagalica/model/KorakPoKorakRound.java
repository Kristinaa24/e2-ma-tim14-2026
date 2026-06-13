package com.tim14.slagalica.model;

public class KorakPoKorakRound {

    private final String answer;
    private final String[] clues;

    public KorakPoKorakRound(String answer, String... clues) {
        this.answer = answer;
        this.clues = clues;
    }

    public String getAnswer() {
        return answer;
    }

    public String[] getClues() {
        return clues;
    }
}
