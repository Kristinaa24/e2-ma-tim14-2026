package com.tim14.slagalica.game;

import android.os.Bundle;

public interface GameNavigator {
    void setPhaseText(String text);
    void setTimerValue(int seconds);
    void setScores(int playerOne, int playerTwo);
    int getPlayerOneScore();
    int getPlayerTwoScore();
    void goToRound(GameRound round, Bundle args);
    void goToNextRound();
    void restartMatch();
    void finishMatch();
}
