package com.tim14.slagalica.model;

public class Region {
    public String id;
    public String code;
    public String name;
    public String icon;
    public int monthlyStars;
    public int firstPlaces;
    public int secondPlaces;
    public int thirdPlaces;
    public int activePlayers;
    public int registeredPlayers;
    public int previousMonthlyRank;

    public Region() {
        // Empty constructor required for Firebase
    }

    public Region(String code, String name, String icon) {
        this.id = code;
        this.code = code;
        this.name = name;
        this.icon = icon;
        this.monthlyStars = 0;
        this.firstPlaces = 0;
        this.secondPlaces = 0;
        this.thirdPlaces = 0;
        this.activePlayers = 0;
        this.registeredPlayers = 0;
        this.previousMonthlyRank = 0;
    }
}
