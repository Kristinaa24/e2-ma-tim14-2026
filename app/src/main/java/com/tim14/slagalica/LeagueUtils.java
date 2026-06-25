package com.tim14.slagalica;

public final class LeagueUtils {

    public static final int BASE_DAILY_TOKENS = 5;
    public static final int INITIAL_REGISTRATION_TOKENS = 5;
    public static final int MAX_LEAGUE = 5;

    private LeagueUtils() {
    }

    public static int calculateLeague(int stars) {
        int safeStars = Math.max(0, stars);
        int league = 0;

        for (int level = 1; level <= MAX_LEAGUE; level++) {
            if (safeStars >= getRequiredStars(level)) {
                league = level;
            }
        }

        return league;
    }

    public static int getRequiredStars(int league) {
        if (league <= 0) {
            return 0;
        }

        int requiredStars = 100;
        for (int level = 1; level < league && level < MAX_LEAGUE; level++) {
            requiredStars *= 2;
        }
        return requiredStars;
    }

    public static int getDailyTokenGrant(int league) {
        return BASE_DAILY_TOKENS + Math.max(0, Math.min(league, MAX_LEAGUE));
    }

    public static String getLeagueName(int league) {
        switch (league) {
            case 0:
                return "No League";
            case 1:
                return "Bronze";
            case 2:
                return "Silver";
            case 3:
                return "Gold";
            case 4:
                return "Diamond";
            case 5:
                return "Master";
            default:
                return "Unranked";
        }
    }

    public static String getLeagueIcon(int league) {
        switch (league) {
            case 1:
                return "\uD83E\uDD49";
            case 2:
                return "\uD83E\uDD48";
            case 3:
                return "\uD83E\uDD47";
            case 4:
                return "\uD83D\uDD37";
            case 5:
                return "\uD83D\uDC51";
            case 0:
            default:
                return "\uD83D\uDEAB";
        }
    }
}
