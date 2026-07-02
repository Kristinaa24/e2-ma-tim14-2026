package com.tim14.slagalica.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class KorakPoKorakRound {

    private String answer;
    private List<String> clues;

    public KorakPoKorakRound() {
        clues = new ArrayList<>();
    }

    public KorakPoKorakRound(String answer, String... clues) {
        this.answer = answer;
        this.clues = new ArrayList<>(Arrays.asList(clues));
    }

    public KorakPoKorakRound(String answer, List<String> clues) {
        this.answer = answer;
        this.clues = clues == null ? new ArrayList<>() : new ArrayList<>(clues);
    }

    public String getAnswer() {
        return answer;
    }

    public String[] getClues() {
        return clues == null ? new String[0] : clues.toArray(new String[0]);
    }

    public List<String> getCluesList() {
        return clues == null ? new ArrayList<>() : new ArrayList<>(clues);
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public void setClues(List<String> clues) {
        this.clues = clues == null ? new ArrayList<>() : new ArrayList<>(clues);
    }
}
