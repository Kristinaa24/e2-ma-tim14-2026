package com.tim14.slagalica.model;

public class HomeFriendItem {

    private final int rank;
    private final String initial;
    private final String name;
    private final String userId;
    private final String avatar;
    private final int stars;
    private final int monthlyRank;
    private final int monthlyStars;
    private final int league;
    private final boolean online;
    private final boolean inMatch;
    private final boolean inviteTile;

    public HomeFriendItem(
            int rank,
            String initial,
            String name,
            String userId,
            String avatar,
            int stars,
            int monthlyRank,
            int monthlyStars,
            int league,
            boolean online,
            boolean inMatch
    ) {
        this(rank, initial, name, userId, avatar, stars, monthlyRank, monthlyStars, league, online, inMatch, false);
    }

    private HomeFriendItem(
            int rank,
            String initial,
            String name,
            String userId,
            String avatar,
            int stars,
            int monthlyRank,
            int monthlyStars,
            int league,
            boolean online,
            boolean inMatch,
            boolean inviteTile
    ) {
        this.rank = rank;
        this.initial = initial;
        this.name = name;
        this.userId = userId;
        this.avatar = avatar;
        this.stars = stars;
        this.monthlyRank = monthlyRank;
        this.monthlyStars = monthlyStars;
        this.league = league;
        this.online = online;
        this.inMatch = inMatch;
        this.inviteTile = inviteTile;
    }

    public static HomeFriendItem createInviteTile() {
        return new HomeFriendItem(0, "", "", "", "", 0, 0, 0, 0, false, false, true);
    }

    public int getRank() {
        return rank;
    }

    public String getInitial() {
        return initial;
    }

    public String getName() {
        return name;
    }

    public String getUserId() {
        return userId;
    }

    public String getAvatar() {
        return avatar;
    }

    public int getStars() {
        return stars;
    }

    public int getMonthlyRank() {
        return monthlyRank;
    }

    public int getMonthlyStars() {
        return monthlyStars;
    }

    public int getLeague() {
        return league;
    }

    public boolean isOnline() {
        return online;
    }

    public boolean isInMatch() {
        return inMatch;
    }

    public boolean canInvite() {
        return !inviteTile && online && !inMatch && userId != null && !userId.trim().isEmpty();
    }

    public boolean isInviteTile() {
        return inviteTile;
    }
}
