package com.tim14.slagalica.model;

import java.util.ArrayList;
import java.util.List;

public class SharedKorakPoKorakRound {

    public String answer;
    public List<String> clues;

    public SharedKorakPoKorakRound() {
        clues = new ArrayList<>();
    }

    public SharedKorakPoKorakRound(String answer, List<String> clues) {
        this.answer = answer;
        this.clues = clues;
    }
}
