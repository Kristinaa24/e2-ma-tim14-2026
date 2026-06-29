package com.tim14.slagalica.model;

public class User {
    public String id;
    public String username;
    public String email;
    public String region;
    public int tokens;
    public int stars;
    public int monthlyStars;
    public int weeklyStars;
    public int monthlyGames;
    public int weeklyGames;
    public int league;
    public String avatar;
    public String avatarFrame;
    public String qrCode;
    public String lastTokenGrantDate;
    public int earnedStarsSinceLastToken;
    public String lastDailyTokenRewardDate;
    public String weeklyCycleKey;
    public String monthlyCycleKey;
    public String lastWeeklyRewardCycle;
    public String lastMonthlyRewardCycle;
    public String dailyMissionDate;
    public boolean dailyMissionWinMatch;
    public boolean dailyMissionSendChat;
    public boolean dailyMissionPlayFriendly;
    public boolean dailyMissionWinTournament;
    public boolean dailyMissionBonusClaimed;
    public boolean loggedIn;
    public boolean inApp;
    public String currentMatchId;
    public long lastSeenAt;
    public long lastActiveAt;

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
        this.weeklyStars = 0;
        this.monthlyGames = 0;
        this.weeklyGames = 0;
        this.league = league;
        this.avatar = "avatar_1";
        this.avatarFrame = "None";
        this.qrCode = "Available for friend invite";
        this.lastTokenGrantDate = "";
        this.earnedStarsSinceLastToken = 0;
        this.lastDailyTokenRewardDate = "";
        this.weeklyCycleKey = "";
        this.monthlyCycleKey = "";
        this.lastWeeklyRewardCycle = "";
        this.lastMonthlyRewardCycle = "";
        this.dailyMissionDate = "";
        this.loggedIn = false;
        this.inApp = false;
        this.currentMatchId = "";
        this.lastSeenAt = 0L;
        this.lastActiveAt = 0L;
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
        this.weeklyStars = 0;
        this.monthlyGames = 0;
        this.weeklyGames = 0;
        this.league = league;
        this.avatar = "avatar_1";
        this.avatarFrame = avatarFrame;
        this.qrCode = qrCode;
        this.lastTokenGrantDate = "";
        this.earnedStarsSinceLastToken = 0;
        this.lastDailyTokenRewardDate = "";
        this.weeklyCycleKey = "";
        this.monthlyCycleKey = "";
        this.lastWeeklyRewardCycle = "";
        this.lastMonthlyRewardCycle = "";
        this.dailyMissionDate = "";
        this.loggedIn = false;
        this.inApp = false;
        this.currentMatchId = "";
        this.lastSeenAt = 0L;
        this.lastActiveAt = 0L;
    }
}