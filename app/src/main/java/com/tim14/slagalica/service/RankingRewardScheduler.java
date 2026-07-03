package com.tim14.slagalica.service;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.tim14.slagalica.repository.FirebaseCallback;
import com.tim14.slagalica.repository.FirestoreRepository;

public final class RankingRewardScheduler {

    private static final String TAG = "RankingRewardScheduler";
    private static final long REFRESH_INTERVAL_MS = 2L * 60L * 1000L;
    private static final RankingRewardScheduler INSTANCE = new RankingRewardScheduler();

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Context appContext;
    private boolean started;

    private final Runnable refreshRunnable = new Runnable() {
        @Override
        public void run() {
            refreshRankingRewards();
            handler.postDelayed(this, REFRESH_INTERVAL_MS);
        }
    };

    private RankingRewardScheduler() {
    }

    public static RankingRewardScheduler getInstance() {
        return INSTANCE;
    }

    public void ensureStarted(Context context) {
        if (context != null) {
            appContext = context.getApplicationContext();
        }
        if (started) {
            return;
        }
        started = true;
        handler.post(refreshRunnable);
    }

    private void refreshRankingRewards() {
        new FirestoreRepository(appContext).refreshRankingRewards(new FirebaseCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                Log.d(TAG, "Ranking reward refresh completed.");
            }

            @Override
            public void onError(String error) {
                Log.w(TAG, "Ranking reward refresh failed: " + error);
            }
        });
    }
}