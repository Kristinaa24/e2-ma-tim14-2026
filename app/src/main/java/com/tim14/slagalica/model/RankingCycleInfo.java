package com.tim14.slagalica.model;

public class RankingCycleInfo {
    public final String type;
    public final String cycleKey;
    public final String dateRange;

    public RankingCycleInfo(String type, String cycleKey, String dateRange) {
        this.type = type;
        this.cycleKey = cycleKey;
        this.dateRange = dateRange;
    }
}