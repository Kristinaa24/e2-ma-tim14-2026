package com.tim14.slagalica.game;

import androidx.fragment.app.Fragment;

public abstract class BaseGameFragment extends Fragment {
    protected GameNavigator host() {
        return (GameNavigator) requireActivity();
    }
}