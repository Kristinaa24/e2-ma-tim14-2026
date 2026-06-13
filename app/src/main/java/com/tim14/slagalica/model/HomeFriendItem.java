package com.tim14.slagalica.model;

public class HomeFriendItem {

    private final int rank;
    private final String initial;
    private final String name;
    private final int stars;
    private final boolean online;
    private final boolean inviteTile;

    public HomeFriendItem(int rank, String initial, String name, int stars, boolean online) {
        this(rank, initial, name, stars, online, false);
    }

    private HomeFriendItem(
            int rank,
            String initial,
            String name,
            int stars,
            boolean online,
            boolean inviteTile
    ) {
        this.rank = rank;
        this.initial = initial;
        this.name = name;
        this.stars = stars;
        this.online = online;
        this.inviteTile = inviteTile;
    }

    public static HomeFriendItem createInviteTile() {
        return new HomeFriendItem(0, "", "", 0, false, true);
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

    public int getStars() {
        return stars;
    }

    public boolean isOnline() {
        return online;
    }

    public boolean isInviteTile() {
        return inviteTile;
    }
}
