package com.tim14.slagalica.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.tim14.slagalica.R;
import com.tim14.slagalica.game.BaseGameFragment;

public class MatchResultFragment extends BaseGameFragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_match_result, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        TextView titleText = view.findViewById(R.id.resultTitleText);
        TextView summaryText = view.findViewById(R.id.resultSummaryText);
        TextView playerOneScoreText = view.findViewById(R.id.resultPlayerOneScoreText);
        TextView playerTwoScoreText = view.findViewById(R.id.resultPlayerTwoScoreText);
        TextView winnerText = view.findViewById(R.id.resultWinnerText);
        Button backButton = view.findViewById(R.id.resultBackButton);
        Button rematchButton = view.findViewById(R.id.resultRematchButton);

        int playerOneScore = host().getPlayerOneScore();
        int playerTwoScore = host().getPlayerTwoScore();

        host().setTimerValue(0);
        host().setPhaseText(getString(R.string.match_result_phase));

        titleText.setText(R.string.match_finished_title);
        summaryText.setText(R.string.match_result_summary);
        playerOneScoreText.setText(getString(R.string.match_result_player_one_format, playerOneScore));
        playerTwoScoreText.setText(getString(R.string.match_result_player_two_format, playerTwoScore));
        winnerText.setText(getWinnerText(playerOneScore, playerTwoScore));

        backButton.setOnClickListener(v -> host().finishMatch());
        rematchButton.setOnClickListener(v -> host().restartMatch());
    }

    private String getWinnerText(int playerOneScore, int playerTwoScore) {
        if (playerOneScore > playerTwoScore) {
            return getString(R.string.match_result_winner_player_one);
        }

        if (playerTwoScore > playerOneScore) {
            return getString(R.string.match_result_winner_player_two);
        }

        return getString(R.string.match_result_draw);
    }
}
