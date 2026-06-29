package com.tim14.slagalica.model;

public class DailyMissionStatus {
    public boolean winMatch;
    public boolean sendChat;
    public boolean playFriendly;
    public boolean winTournament;
    public boolean bonusClaimed;

    public int completedCount() {
        int count = 0;
        if (winMatch) count++;
        if (sendChat) count++;
        if (playFriendly) count++;
        if (winTournament) count++;
        return count;
    }
}