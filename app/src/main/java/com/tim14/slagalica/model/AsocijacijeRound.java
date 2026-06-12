package com.tim14.slagalica.model;

import java.util.List;

public class AsocijacijeRound {
    public String columnA_solution;
    public List<String> columnA_clues;
    public String columnB_solution;
    public List<String> columnB_clues;
    public String columnC_solution;
    public List<String> columnC_clues;
    public String columnD_solution;
    public List<String> columnD_clues;
    public String finalSolution;

    public AsocijacijeRound() {}

    public AsocijacijeRound(String finalSolution, String solA, List<String> cluesA, String solB, List<String> cluesB, String solC, List<String> cluesC, String solD, List<String> cluesD) {
        this.finalSolution = finalSolution;
        this.columnA_solution = solA;
        this.columnA_clues = cluesA;
        this.columnB_solution = solB;
        this.columnB_clues = cluesB;
        this.columnC_solution = solC;
        this.columnC_clues = cluesC;
        this.columnD_solution = solD;
        this.columnD_clues = cluesD;
    }
}
