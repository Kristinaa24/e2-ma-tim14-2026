package com.tim14.slagalica.model;

public class HomeRankingItem {

    private final int position;
    private final String playerName;
    private final int stars;

    public HomeRankingItem(int position, String playerName, int stars) {
        this.position = position;
        this.playerName = playerName;
        this.stars = stars;
    }

    public int getPosition() {
        return position;
    }

    public String getPlayerName() {
        return playerName;
    }

    public int getStars() {
        return stars;
    }
}
