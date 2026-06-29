package com.tim14.slagalica.fragments;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.tim14.slagalica.GameHostActivity;
import com.tim14.slagalica.R;
import com.tim14.slagalica.game.BaseGameFragment;
import com.tim14.slagalica.model.SharedMatchState;

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
        animateTournamentOutcome(titleText, winnerText, playerOneScore, playerTwoScore);
        backButton.setOnClickListener(v -> ((GameHostActivity) requireActivity()).finishMatchAndReturnHome());
        rematchButton.setOnClickListener(v -> host().restartMatch());
        configureRemoteRematchUi(summaryText, backButton, rematchButton, playerOneScore, playerTwoScore);
    }

    private void configureRemoteRematchUi(
            TextView summaryText,
            Button backButton,
            Button rematchButton,
            int playerOneScore,
            int playerTwoScore
    ) {
        GameHostActivity activity = (GameHostActivity) requireActivity();
        if (!activity.isRemoteMatchMode()) {
            return;
        }

        SharedMatchState state = activity.getSharedMatchState();
        if (state == null) {
            return;
        }

        int localPlayerNumber = activity.getLocalPlayerNumber();
        if (SharedMatchState.MATCH_TYPE_TOURNAMENT.equals(state.matchType)) {
            boolean localWon = (localPlayerNumber == 1 && playerOneScore > playerTwoScore)
                    || (localPlayerNumber == 2 && playerTwoScore > playerOneScore)
                    || activity.hasOpponentForfeited();
            boolean semiFinal = "SEMI".equals(state.tournamentStage);
            rematchButton.setVisibility(View.GONE);
            if (semiFinal && localWon) {
                if (state.finalMatchId != null && !state.finalMatchId.trim().isEmpty()) {
                    summaryText.setText("Final is ready. Open your final match.");
                    backButton.setText("Open final");
                    backButton.setOnClickListener(v -> activity.openTournamentFinalMatch());
                } else {
                    summaryText.setText("You reached the final. Waiting for the other semifinal to finish.");
                    backButton.setText(R.string.back_to_main_room);
                }
            } else if (semiFinal) {
                summaryText.setText("You were eliminated from the tournament.");
                backButton.setText(R.string.back_to_main_room);
            } else {
                summaryText.setText(localWon ? "You won the tournament." : "Final match finished.");
                backButton.setText(R.string.back_to_main_room);
            }
            return;
        }
        backButton.setText(R.string.back_to_main_room);
        rematchButton.setText(R.string.play_rematch);
        rematchButton.setEnabled(true);

        if (state.rematchRequestedBy == localPlayerNumber) {
            summaryText.setText(R.string.rematch_waiting_for_opponent_message);
            backButton.setText(R.string.rematch_cancel_request);
            rematchButton.setText(R.string.rematch_requested_button);
            rematchButton.setEnabled(false);
            return;
        }

        if (state.rematchRequestedBy != 0) {
            summaryText.setText(getString(
                    R.string.rematch_requested_by_player_format,
                    state.rematchRequestedBy
            ));
            backButton.setText(R.string.notification_decline);
            rematchButton.setText(R.string.rematch_accept_button);
            return;
        }

        if (state.rematchDeclinedBy != 0) {
            summaryText.setText(getString(
                    R.string.rematch_declined_by_player_format,
                    state.rematchDeclinedBy
            ));
        }
    }

    private void animateTournamentOutcome(
            TextView titleText,
            TextView winnerText,
            int playerOneScore,
            int playerTwoScore
    ) {
        GameHostActivity activity = (GameHostActivity) requireActivity();
        SharedMatchState state = activity.getSharedMatchState();
        if (state == null || !SharedMatchState.MATCH_TYPE_TOURNAMENT.equals(state.matchType)) {
            return;
        }

        boolean localWon = (activity.getLocalPlayerNumber() == 1 && playerOneScore > playerTwoScore)
                || (activity.getLocalPlayerNumber() == 2 && playerTwoScore > playerOneScore)
                || activity.hasOpponentForfeited();
        boolean draw = playerOneScore == playerTwoScore && !activity.hasForfeitedPlayer();
        titleText.setText(draw ? "Tournament result" : (localWon ? "Tournament victory" : "Tournament defeat"));
        winnerText.setText((draw ? "Draw" : (localWon ? "You won this tournament match" : "You lost this tournament match"))
                + "\n" + getWinnerText(playerOneScore, playerTwoScore));

        winnerText.setScaleX(0.82f);
        winnerText.setScaleY(0.82f);
        winnerText.setAlpha(0.25f);
        AnimatorSet set = new AnimatorSet();
        set.playTogether(
                ObjectAnimator.ofFloat(winnerText, View.SCALE_X, 0.82f, 1.08f, 1f),
                ObjectAnimator.ofFloat(winnerText, View.SCALE_Y, 0.82f, 1.08f, 1f),
                ObjectAnimator.ofFloat(winnerText, View.ALPHA, 0.25f, 1f)
        );
        set.setDuration(850);
        set.start();
    }
    private String getWinnerText(int playerOneScore, int playerTwoScore) {
        GameHostActivity activity = (GameHostActivity) requireActivity();
        SharedMatchState state = activity.getSharedMatchState();

        if (state != null && state.forfeitedPlayer == 1) {
            return getString(R.string.match_result_winner_player_two);
        }

        if (state != null && state.forfeitedPlayer == 2) {
            return getString(R.string.match_result_winner_player_one);
        }

        if (playerOneScore > playerTwoScore) {
            return getString(R.string.match_result_winner_player_one);
        }

        if (playerTwoScore > playerOneScore) {
            return getString(R.string.match_result_winner_player_two);
        }

        return getString(R.string.match_result_draw);
    }
}
