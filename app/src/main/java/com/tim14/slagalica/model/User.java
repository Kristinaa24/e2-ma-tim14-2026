package com.tim14.slagalica.model;

public class User {
    public String id;
    public String username;
    public String email;
    public String region;
    public int tokens;
    public int stars;
    public int league;
    public String avatarFrame;
    public String qrCode;

    public User() {
        // Empty constructor required for Firebase
    }

    public User(String username, String email, String region, int tokens, int stars, int league) {
        this.username = username;
        this.email = email;
        this.region = region;
        this.tokens = tokens;
        this.stars = stars;
        this.league = league;
        this.avatarFrame = "Silver";
        this.qrCode = "Available for friend invite";
    }

    public User(String id, String username, String email, String region,
                int tokens, int stars, int league, String avatarFrame, String qrCode) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.region = region;
        this.tokens = tokens;
        this.stars = stars;
        this.league = league;
        this.avatarFrame = avatarFrame;
        this.qrCode = qrCode;
    }
}