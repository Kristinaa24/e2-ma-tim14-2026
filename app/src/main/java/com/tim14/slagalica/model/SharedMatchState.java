package com.tim14.slagalica.model;

import java.util.ArrayList;
import java.util.List;

public class SharedMatchState {

    public static final String STATUS_WAITING = "WAITING";
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_FINISHED = "FINISHED";
    public static final String STATUS_DECLINED = "DECLINED";
    public static final String STATUS_CANCELED = "CANCELED";

    public static final String PHASE_WAITING = "WAITING";
    public static final String PHASE_KZZ_QUESTION = "KZZ_QUESTION";
    public static final String PHASE_KZZ_REVEAL = "KZZ_REVEAL";
    public static final String PHASE_SPOJNICE_PLAY = "SPOJNICE_PLAY";
    public static final String PHASE_SPOJNICE_DONE = "SPOJNICE_DONE";
    public static final String PHASE_SKOCKO_PLAY = "SKOCKO_PLAY";
    public static final String PHASE_SKOCKO_BONUS = "SKOCKO_BONUS";
    public static final String PHASE_SKOCKO_DONE = "SKOCKO_DONE";
    public static final String PHASE_ASOC_PLAY = "ASOC_PLAY";
    public static final String PHASE_ASOC_DONE = "ASOC_DONE";
    public static final String PHASE_KPP_STARTER = "KPP_STARTER";
    public static final String PHASE_KPP_BONUS = "KPP_BONUS";
    public static final String PHASE_KPP_DONE = "KPP_DONE";
    public static final String PHASE_MB_TARGET = "MB_TARGET";
    public static final String PHASE_MB_NUMBERS = "MB_NUMBERS";
    public static final String PHASE_MB_ENTRY = "MB_ENTRY";
    public static final String PHASE_MB_DONE = "MB_DONE";
    public static final String PHASE_RESULT = "RESULT";
    public static final String MATCH_TYPE_COMPETITIVE = "COMPETITIVE";
    public static final String MATCH_TYPE_FRIENDLY = "FRIENDLY";
    public static final String MATCH_TYPE_TOURNAMENT = "TOURNAMENT";

    public String roomCode;
    public String status;
    public String matchType;
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
    public int answeredByPlayer;
    public int selectedAnswerIndex;
    public long phaseStartedAt;
    public int phaseDurationSeconds;
    public String phaseMessage;
    public String revealedAnswer;
    public String playerOneExpression;
    public String playerTwoExpression;
    public boolean resultApplied;
    public int forfeitedPlayer;
    public int rematchRequestedBy;
    public int rematchDeclinedBy;
    public long matchStartedAt;
    public long friendlyInviteExpiresAt;
    public long updatedAt;
    public String tournamentId;
    public String tournamentStage;
    public int tournamentSemiNumber;
    public String siblingMatchId;
    public String finalMatchId;
    public String winnerId;
    public List<KoZnaZnaQuestion> quizQuestions;
    public List<SharedSpojniceRound> spojniceRounds;
    public List<SharedSkockoRound> skockoRounds;
    public List<SharedAsocijacijeRound> asocijacijeRounds;
    public List<SharedKorakPoKorakRound> korakRounds;
    public List<SharedMojBrojRound> myNumberRounds;

    public SharedMatchState() {
        matchType = MATCH_TYPE_COMPETITIVE;
        quizQuestions = new ArrayList<>();
        spojniceRounds = new ArrayList<>();
        skockoRounds = new ArrayList<>();
        asocijacijeRounds = new ArrayList<>();
        korakRounds = new ArrayList<>();
        myNumberRounds = new ArrayList<>();
        resultApplied = false;
        forfeitedPlayer = 0;
        rematchRequestedBy = 0;
        rematchDeclinedBy = 0;
        matchStartedAt = 0L;
        friendlyInviteExpiresAt = 0L;
        tournamentId = "";
        tournamentStage = "";
        tournamentSemiNumber = 0;
        siblingMatchId = "";
        finalMatchId = "";
        winnerId = "";
    }
}
