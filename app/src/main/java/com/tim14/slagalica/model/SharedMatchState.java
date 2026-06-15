package com.tim14.slagalica.model;

import java.util.ArrayList;
import java.util.List;

public class SharedMatchState {

    public static final String STATUS_WAITING = "WAITING";
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_FINISHED = "FINISHED";

    public static final String PHASE_WAITING = "WAITING";
    public static final String PHASE_KPP_STARTER = "KPP_STARTER";
    public static final String PHASE_KPP_BONUS = "KPP_BONUS";
    public static final String PHASE_KPP_DONE = "KPP_DONE";
    public static final String PHASE_MB_TARGET = "MB_TARGET";
    public static final String PHASE_MB_NUMBERS = "MB_NUMBERS";
    public static final String PHASE_MB_ENTRY = "MB_ENTRY";
    public static final String PHASE_MB_DONE = "MB_DONE";
    public static final String PHASE_RESULT = "RESULT";

    public String roomCode;
    public String status;
    public String currentRound;
    public String phase;
    public String playerOneId;
    public String playerTwoId;
    public String playerOneName;
    public String playerTwoName;
    public int activePlayer;
    public int currentTurnIndex;
    public int playerOneScore;
    public int playerTwoScore;
    public long phaseStartedAt;
    public int phaseDurationSeconds;
    public String phaseMessage;
    public String revealedAnswer;
    public String playerOneExpression;
    public String playerTwoExpression;
    public long updatedAt;
    public List<SharedKorakPoKorakRound> korakRounds;
    public List<SharedMojBrojRound> myNumberRounds;

    public SharedMatchState() {
        korakRounds = new ArrayList<>();
        myNumberRounds = new ArrayList<>();
    }
}
