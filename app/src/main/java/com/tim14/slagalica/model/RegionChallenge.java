package com.tim14.slagalica.model;

import java.util.ArrayList;
import java.util.List;

public class RegionChallenge {
    public static final String STATUS_OPEN = "OPEN";
    public static final String STATUS_READY = "READY";
    public static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    public static final String STATUS_FINISHED = "FINISHED";
    public static final String STATUS_CANCELED = "CANCELED";

    public String id;
    public String creatorId;
    public String creatorName;
    public String creatorRegion;
    public int stakeTokens;
    public int stakeStars;
    public int requiredPlayers;
    public String status;
    public long createdAt;
    public long updatedAt;
    public List<ChallengeParticipant> participants;
    public int winnerRewardTokens;
    public int winnerRewardStars;
    public int runnerUpRewardTokens;
    public int runnerUpRewardStars;

    public RegionChallenge() {
        participants = new ArrayList<>();
    }

    public int getParticipantCount() {
        return participants == null ? 0 : participants.size();
    }
}
