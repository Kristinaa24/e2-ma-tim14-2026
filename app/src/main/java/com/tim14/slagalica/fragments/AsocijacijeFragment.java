package com.tim14.slagalica.fragments;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.tim14.slagalica.R;
import com.tim14.slagalica.game.GameNavigator;

public class AsocijacijeFragment extends Fragment {

    private GameNavigator navigator;
    private Button btnConfirm;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof GameNavigator) {
            navigator = (GameNavigator) context;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_asocijacije, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        btnConfirm = view.findViewById(R.id.confirmSolutionButton);

        if (navigator != null) {
            navigator.setPhaseText(getString(R.string.asocijacije));
            navigator.setTimerValue(120);
        }

        btnConfirm.setOnClickListener(v -> {
            if (navigator != null) {
                navigator.goToNextRound();
            }
        });
    }
}
