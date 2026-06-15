package com.tim14.slagalica.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SharedAsocijacijeRound {

    public String columnASolution;
    public List<String> columnAClues;
    public String columnBSolution;
    public List<String> columnBClues;
    public String columnCSolution;
    public List<String> columnCClues;
    public String columnDSolution;
    public List<String> columnDClues;
    public String finalSolution;
    public Map<String, List<Boolean>> openedFields;
    public List<Boolean> columnSolved;
    public List<Integer> columnSolvers;
    public boolean finalSolved;
    public int finalSolver;
    public int currentPlayer;
    public boolean hasOpenedFieldThisTurn;
    public int playerOneRoundScore;
    public int playerTwoRoundScore;

    public SharedAsocijacijeRound() {
        columnAClues = new ArrayList<>();
        columnBClues = new ArrayList<>();
        columnCClues = new ArrayList<>();
        columnDClues = new ArrayList<>();
        openedFields = new HashMap<>();
        columnSolved = new ArrayList<>();
        columnSolvers = new ArrayList<>();
    }
}
