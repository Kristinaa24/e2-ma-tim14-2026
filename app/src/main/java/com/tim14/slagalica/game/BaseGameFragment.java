package com.tim14.slagalica.game;

import android.os.CountDownTimer;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;

public abstract class BaseGameFragment extends Fragment {

    private CountDownTimer roundTimer;

    protected GameNavigator host() {
        return (GameNavigator) requireActivity();
    }

    protected void startRoundTimer(int totalSeconds, Runnable onFinished) {
        stopRoundTimer();
        host().setTimerValue(totalSeconds);

        roundTimer = new CountDownTimer(totalSeconds * 1000L, 1000L) {
            @Override
            public void onTick(long millisUntilFinished) {
                if (!isAdded()) {
                    return;
                }

                int remainingSeconds = (int) Math.ceil(millisUntilFinished / 1000d);
                host().setTimerValue(remainingSeconds);
            }

            @Override
            public void onFinish() {
                if (!isAdded()) {
                    return;
                }

                host().setTimerValue(0);

                if (onFinished != null) {
                    onFinished.run();
                }
            }
        };

        roundTimer.start();
    }

    protected void stopRoundTimer() {
        if (roundTimer != null) {
            roundTimer.cancel();
            roundTimer = null;
        }
    }

    protected void setEnabledRecursively(View view, boolean enabled) {
        if (view == null) {
            return;
        }

        view.setEnabled(enabled);

        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;

            for (int index = 0; index < group.getChildCount(); index++) {
                setEnabledRecursively(group.getChildAt(index), enabled);
            }
        }
    }

    @Override
    public void onDestroyView() {
        stopRoundTimer();
        super.onDestroyView();
    }
}
