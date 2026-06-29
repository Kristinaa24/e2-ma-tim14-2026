package com.tim14.slagalica.model;

public class HomeRankingItem {

    private final int position;
    private final String playerName;
    private final String leagueName;
    private final int league;
    private final int stars;

    public HomeRankingItem(int position, String playerName, int stars) {
        this(position, playerName, "", 0, stars);
    }

    public HomeRankingItem(int position, String playerName, String leagueName, int league, int stars) {
        this.position = position;
        this.playerName = playerName;
        this.leagueName = leagueName;
        this.league = league;
        this.stars = stars;
    }

    public int getPosition() {
        return position;
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getLeagueName() {
        return leagueName;
    }

    public int getLeague() {
        return league;
    }

    public int getStars() {
        return stars;
    }
}