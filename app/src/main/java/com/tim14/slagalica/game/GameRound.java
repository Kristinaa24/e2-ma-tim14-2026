package com.tim14.slagalica.game;

public enum GameRound {
    KO_ZNA_ZNA,
    SPOJNICE,
    SKOCKO,
    ASOCIJACIJE,
    KORAK_PO_KORAK,
    MOJ_BROJ,
    RESULT;

    private static final GameRound[] MATCH_ORDER = {
            KO_ZNA_ZNA,
            SPOJNICE,
            SKOCKO,
            ASOCIJACIJE,
            KORAK_PO_KORAK,
            MOJ_BROJ,
            RESULT
    };

    public GameRound nextInMatch() {
        for (int i = 0; i < MATCH_ORDER.length; i++) {
            if (MATCH_ORDER[i] == this) {
                return i + 1 < MATCH_ORDER.length ? MATCH_ORDER[i + 1] : null;
            }
        }

        return null;
    }

    public String getDisplayName() {
        switch (this) {
            case KO_ZNA_ZNA:
                return "Quiz";
            case SPOJNICE:
                return "Pairs";
            case SKOCKO:
                return "Mastermind";
            case ASOCIJACIJE:
                return "Associations";
            case KORAK_PO_KORAK:
                return "Step by Step";
            case MOJ_BROJ:
                return "My Number";
            default:
                return "Results";
        }
    }
}
