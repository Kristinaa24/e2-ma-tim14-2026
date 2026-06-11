package com.tim14.slagalica;

public final class LeagueUtils {

    private LeagueUtils() {
    }

    public static String getLeagueName(int league) {
        switch (league) {
            case 0:
                return "Bronze";
            case 1:
                return "Silver";
            case 2:
                return "Gold";
            case 3:
                return "Platinum";
            case 4:
                return "Diamond";
            case 5:
                return "Master";
            default:
                return "Unranked";
        }
    }
}
