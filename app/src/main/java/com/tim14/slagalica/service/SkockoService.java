package com.tim14.slagalica.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class SkockoService {

    public enum Symbol {
        SKOCKO, CLUB, SPADE, HEART, DIAMOND, STAR
    }

    private List<Symbol> secretCombination;
    private int currentAttempt = 0;
    private boolean isFinished = false;
    private int score = 0;

    public SkockoService() {
        generateSecretCombination();
    }

    private void generateSecretCombination() {
        secretCombination = new ArrayList<>();
        Symbol[] symbols = Symbol.values();
        Random random = new Random();
        for (int i = 0; i < 4; i++) {
            secretCombination.add(symbols[random.nextInt(symbols.length)]);
        }
    }

    public static class Result {
        public int red;
        public int yellow;
        public boolean isWin;

        public Result(int red, int yellow, boolean isWin) {
            this.red = red;
            this.yellow = yellow;
            this.isWin = isWin;
        }
    }

    public Result checkAttempt(List<Symbol> attempt) {
        if (attempt.size() != 4) return null;

        int red = 0;
        int yellow = 0;

        List<Symbol> secretCopy = new ArrayList<>(secretCombination);
        List<Symbol> attemptCopy = new ArrayList<>(attempt);

        // Check for red (correct symbol and position)
        for (int i = 3; i >= 0; i--) {
            if (attemptCopy.get(i) == secretCopy.get(i)) {
                red++;
                attemptCopy.remove(i);
                secretCopy.remove(i);
            }
        }

        // Check for yellow (correct symbol, wrong position)
        for (Symbol s : attemptCopy) {
            if (secretCopy.contains(s)) {
                yellow++;
                secretCopy.remove(s);
            }
        }

        boolean win = red == 4;
        if (win) {
            isFinished = true;
            calculateScore();
        } else {
            currentAttempt++;
            if (currentAttempt >= 6) {
                isFinished = true;
            }
        }

        return new Result(red, yellow, win);
    }

    private void calculateScore() {
        if (currentAttempt < 2) score = 20; // 1st and 2nd attempt (index 0 and 1)
        else if (currentAttempt < 4) score = 15; // 3rd and 4th
        else score = 10; // 5th and 6th
    }

    public List<Symbol> getSecretCombination() {
        return secretCombination;
    }

    public int getCurrentAttempt() {
        return currentAttempt;
    }

    public int getScore() {
        return score;
    }

    public boolean isFinished() {
        return isFinished;
    }
}
