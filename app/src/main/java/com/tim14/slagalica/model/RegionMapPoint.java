package com.tim14.slagalica.model;

public class RegionMapPoint {
    public final String playerId;
    public final String username;
    public final String region;
    public final float x;
    public final float y;

    public RegionMapPoint(String playerId, String username, String region, float x, float y) {
        this.playerId = playerId;
        this.username = username;
        this.region = region;
        this.x = x;
        this.y = y;
    }
}
