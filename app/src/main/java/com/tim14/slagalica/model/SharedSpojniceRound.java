package com.tim14.slagalica.model;

import java.util.ArrayList;
import java.util.List;

public class SharedSpojniceRound {

    public String title;
    public List<String> leftItems;
    public List<String> correctRightItems;
    public List<String> displayedRightItems;
    public List<String> solvedLeftItems;
    public List<String> solvedRightItems;
    public List<Integer> solvedByPlayers;
    public int starterPlayer;
    public int currentPlayer;
    public boolean secondChance;
    public int attemptsInTurn;
    public int secondChancePairsCount;
    public boolean finished;

    public SharedSpojniceRound() {
        leftItems = new ArrayList<>();
        correctRightItems = new ArrayList<>();
        displayedRightItems = new ArrayList<>();
        solvedLeftItems = new ArrayList<>();
        solvedRightItems = new ArrayList<>();
        solvedByPlayers = new ArrayList<>();
    }
}
