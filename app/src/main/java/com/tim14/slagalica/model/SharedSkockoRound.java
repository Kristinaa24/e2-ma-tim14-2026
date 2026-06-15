package com.tim14.slagalica.model;

import java.util.ArrayList;
import java.util.List;

public class SharedSkockoRound {

    public List<String> secretCombination;
    public List<String> starterAttempts;
    public List<Integer> starterRedCounts;
    public List<Integer> starterYellowCounts;
    public String bonusAttempt;
    public int bonusRedCount;
    public int bonusYellowCount;
    public int starterPlayer;
    public boolean finished;
    public boolean solved;
    public int solvedByPlayer;
    public int solvedAttemptIndex;
    public int awardedPoints;
    public int playerOneRoundScore;
    public int playerTwoRoundScore;

    public SharedSkockoRound() {
        secretCombination = new ArrayList<>();
        starterAttempts = new ArrayList<>();
        starterRedCounts = new ArrayList<>();
        starterYellowCounts = new ArrayList<>();
        bonusAttempt = "";
        solvedAttemptIndex = -1;
    }
}
