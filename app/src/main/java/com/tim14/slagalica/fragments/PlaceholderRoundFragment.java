package com.tim14.slagalica.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.tim14.slagalica.R;
import com.tim14.slagalica.game.BaseGameFragment;
import com.tim14.slagalica.game.GameRound;

import java.util.Locale;

public class PlaceholderRoundFragment extends BaseGameFragment {

    private static final String ARG_ROUND = "arg_round";

    public static PlaceholderRoundFragment newInstance(String roundName) {
        PlaceholderRoundFragment fragment = new PlaceholderRoundFragment();
        Bundle args = new Bundle();
        args.putString(ARG_ROUND, roundName);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_placeholder_round, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        host().setTimerValue(0);
        TextView placeholderText = view.findViewById(R.id.placeholderText);
        TextView placeholderTitle = view.findViewById(R.id.placeholderTitle);
        Button continueButton = view.findViewById(R.id.btnContinueRound);
        String roundName = GameRound.RESULT.name();
        String displayName = getString(R.string.placeholder_round_default_title);

        if (getArguments() != null) {
            roundName = getArguments().getString(ARG_ROUND, "IGRA");
        }

        try {
            displayName = GameRound.valueOf(roundName).getDisplayName();
        } catch (IllegalArgumentException ignored) {
        }

        host().setPhaseText(getString(R.string.placeholder_phase_format, displayName));
        placeholderTitle.setText(displayName.toUpperCase(Locale.US));
        placeholderText.setText(getString(R.string.placeholder_round_message));

        continueButton.setOnClickListener(v -> host().goToNextRound());
    }
}
