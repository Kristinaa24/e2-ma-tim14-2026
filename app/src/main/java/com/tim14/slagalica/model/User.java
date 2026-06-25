package com.tim14.slagalica.model;

public class User {
    public String id;
    public String username;
    public String email;
    public String region;
    public int tokens;
    public int stars;
    public int monthlyStars;
    public int league;
    public String avatar;
    public String avatarFrame;
    public String qrCode;
    public String lastDailyTokenRewardDate;
    public long lastActiveAt;
    public boolean loggedIn;

    public User() {
        // Empty constructor required for Firebase
    }

    public User(String username, String email, String region, int tokens, int stars, int league) {
        this.username = username;
        this.email = email;
        this.region = region;
        this.tokens = tokens;
        this.stars = stars;
        this.monthlyStars = 0;
        this.league = league;
        this.avatar = "avatar_1";
        this.avatarFrame = "None";
        this.qrCode = "Available for friend invite";
        this.lastDailyTokenRewardDate = "";
        this.lastActiveAt = 0;
        this.loggedIn = false;
    }

    public User(String id, String username, String email, String region,
                int tokens, int stars, int league, String avatarFrame, String qrCode) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.region = region;
        this.tokens = tokens;
        this.stars = stars;
        this.monthlyStars = 0;
        this.league = league;
        this.avatar = "avatar_1";
        this.avatarFrame = avatarFrame;
        this.qrCode = qrCode;
        this.lastDailyTokenRewardDate = "";
        this.lastActiveAt = 0;
        this.loggedIn = false;
    }
}
