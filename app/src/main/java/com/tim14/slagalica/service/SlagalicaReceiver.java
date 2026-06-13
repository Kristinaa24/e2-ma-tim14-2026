package com.tim14.slagalica.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class SlagalicaReceiver extends BroadcastReceiver {
    private static final String TAG = "SlagalicaReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "Received broadcast action: " + action);

        if (action != null) {
            String title = intent.getStringExtra("title");
            String message = intent.getStringExtra("message");
            String type = intent.getStringExtra("type"); // CHAT, REWARD, etc.

            if (title == null) title = "Slagalica Alert";
            if (message == null) message = "You have a new update!";

            // Trigger the actual system notification
            NotificationHelper.showSystemNotification(context, title, message, type);
        }
    }
}
