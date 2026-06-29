package com.tim14.slagalica.game;

import android.os.CountDownTimer;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;

import com.tim14.slagalica.GameHostActivity;

public abstract class BaseGameFragment extends Fragment {

    protected interface RoundTimerTickListener {
        void onTick(int remainingSeconds);
    }

    private CountDownTimer roundTimer;

    protected GameNavigator host() {
        return (GameNavigator) requireActivity();
    }

    protected boolean isChallengeMode() {
        return requireActivity() instanceof GameHostActivity
                && ((GameHostActivity) requireActivity()).isChallengeMode();
    }

    protected void startRoundTimer(int totalSeconds, Runnable onFinished) {
        startRoundTimer(totalSeconds, null, onFinished);
    }

    protected void startRoundTimer(
            int totalSeconds,
            RoundTimerTickListener onTick,
            Runnable onFinished
    ) {
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

                if (onTick != null) {
                    onTick.onTick(remainingSeconds);
                }
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
