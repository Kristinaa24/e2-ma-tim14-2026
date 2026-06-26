package com.tim14.slagalica.model;

public class ChallengeParticipant {
    public String userId;
    public String username;
    public long joinedAt;
    public boolean played;
    public int score;
    public long completedAt;
    public int rewardTokens;
    public int rewardStars;
    public int placement;

    public ChallengeParticipant() {
        // Required for Firebase.
    }

    public ChallengeParticipant(String userId, String username, long joinedAt) {
        this.userId = userId;
        this.username = username;
        this.joinedAt = joinedAt;
        this.played = false;
        this.score = 0;
        this.completedAt = 0L;
        this.rewardTokens = 0;
        this.rewardStars = 0;
        this.placement = 0;
    }
}
