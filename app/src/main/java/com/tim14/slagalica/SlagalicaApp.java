package com.tim14.slagalica;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import com.tim14.slagalica.service.ChatNotificationWatcher;
import com.tim14.slagalica.service.InviteNotificationWatcher;
import com.tim14.slagalica.service.NotificationHelper;

public class SlagalicaApp extends Application {

    private int startedActivityCount;

    @Override
    public void onCreate() {
        super.onCreate();
        NotificationHelper.createNotificationChannels(this);
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
                // No-op.
            }

            @Override
            public void onActivityStarted(Activity activity) {
                startedActivityCount++;
                ChatNotificationWatcher chatWatcher = ChatNotificationWatcher.getInstance();
                chatWatcher.setAppInForeground(true);
                chatWatcher.ensureStarted(SlagalicaApp.this);

                InviteNotificationWatcher watcher = InviteNotificationWatcher.getInstance();
                watcher.setAppInForeground(true);
                watcher.ensureStarted(SlagalicaApp.this);
            }

            @Override
            public void onActivityResumed(Activity activity) {
                // No-op.
            }

            @Override
            public void onActivityPaused(Activity activity) {
                // No-op.
            }

            @Override
            public void onActivityStopped(Activity activity) {
                startedActivityCount = Math.max(0, startedActivityCount - 1);
                if (startedActivityCount == 0) {
                    ChatNotificationWatcher.getInstance().setAppInForeground(false);
                    InviteNotificationWatcher.getInstance().setAppInForeground(false);
                }
            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
                // No-op.
            }

            @Override
            public void onActivityDestroyed(Activity activity) {
                // No-op.
            }
        });
    }
}
