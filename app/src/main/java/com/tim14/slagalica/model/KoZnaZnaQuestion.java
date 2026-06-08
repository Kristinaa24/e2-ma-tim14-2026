package com.tim14.slagalica.model;

import java.util.List;

public class KoZnaZnaQuestion {
    public String id;
    public String question;
    public List<String> answers;
    public int correctIndex;

    public KoZnaZnaQuestion() {
        // Empty constructor required for Firebase
    }

    public KoZnaZnaQuestion(String id, String question, List<String> answers, int correctIndex) {
        this.id = id;
        this.question = question;
        this.answers = answers;
        this.correctIndex = correctIndex;
    }
}