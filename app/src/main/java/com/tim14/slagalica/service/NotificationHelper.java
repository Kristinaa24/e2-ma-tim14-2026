package com.tim14.slagalica.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.tim14.slagalica.HomeActivity;
import com.tim14.slagalica.NotificationsActivity;
import com.tim14.slagalica.R;
import com.tim14.slagalica.repository.FirestoreRepository;

public class NotificationHelper {

    public static final String CHANNEL_CHAT = "chat_notifications";
    public static final String CHANNEL_INVITES = "invite_notifications";
    public static final String CHANNEL_RANKING = "ranking_notifications";
    public static final String CHANNEL_REWARDS = "rewards_notifications";
    public static final String CHANNEL_OTHER = "other_notifications";

    public static void createNotificationChannels(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager == null) return;

            NotificationChannel chatChannel = new NotificationChannel(CHANNEL_CHAT, "Chat Messages", NotificationManager.IMPORTANCE_DEFAULT);
            NotificationChannel inviteChannel = new NotificationChannel(CHANNEL_INVITES, "Match Invites", NotificationManager.IMPORTANCE_HIGH);
            NotificationChannel rankingChannel = new NotificationChannel(CHANNEL_RANKING, "Ranking Updates", NotificationManager.IMPORTANCE_LOW);
            NotificationChannel rewardsChannel = new NotificationChannel(CHANNEL_REWARDS, "Rewards & Missions", NotificationManager.IMPORTANCE_HIGH);
            NotificationChannel otherChannel = new NotificationChannel(CHANNEL_OTHER, "Other Notifications", NotificationManager.IMPORTANCE_DEFAULT);

            manager.createNotificationChannel(chatChannel);
            manager.createNotificationChannel(inviteChannel);
            manager.createNotificationChannel(rankingChannel);
            manager.createNotificationChannel(rewardsChannel);
            manager.createNotificationChannel(otherChannel);
        }
    }

    public static void showSystemNotification(Context context, String title, String message, String type) {
        showSystemNotification(context, title, message, type, true);
    }

    public static void showSystemNotification(
            Context context,
            String title,
            String message,
            String type,
            boolean saveToHistory
    ) {
        String channelId = getChannelIdByType(type);

        Intent intent;
        if ("INVITE".equalsIgnoreCase(type)) {
            intent = new Intent(context, NotificationsActivity.class);
        } else {
            intent = new Intent(context, HomeActivity.class);
            intent.putExtra("TARGET_SECTION", getTargetSectionByType(type));
            if ("RANKING".equalsIgnoreCase(type) || "REWARD".equalsIgnoreCase(type)) {
                intent.putExtra(HomeActivity.EXTRA_SHOW_REWARD_DIALOG, true);
            }
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 
                (int) System.currentTimeMillis(), 
                intent, 
                PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.baseline_person_24) // Replace with app icon later
                .setContentTitle(title)
                .setContentText(message)
                .setPriority("INVITE".equalsIgnoreCase(type)
                        ? NotificationCompat.PRIORITY_HIGH
                        : NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManagerCompat manager = NotificationManagerCompat.from(context);
        try {
            manager.notify((int) System.currentTimeMillis(), builder.build());
        } catch (SecurityException e) {
            // Handle notification permission missing for Android 13+
        }

        if (saveToHistory) {
            // Save to Firebase so the user can see it in the in-app notification history.
            new FirestoreRepository().saveNotification(title, message, type);
        }
    }

    private static String getChannelIdByType(String type) {
        if (type == null) return CHANNEL_OTHER;
        switch (type.toUpperCase()) {
            case "CHAT": return CHANNEL_CHAT;
            case "INVITE": return CHANNEL_INVITES;
            case "RANKING": return CHANNEL_RANKING;
            case "REWARD": return CHANNEL_REWARDS;
            default: return CHANNEL_OTHER;
        }
    }

    private static String getTargetSectionByType(String type) {
        if (type == null) return null;
        switch (type.toUpperCase()) {
            case "CHAT": return "chat";
            case "RANKING": return "ranking";
            case "REWARD": return "profile";
            case "INVITE": return "friends";
            default: return null;
        }
    }
}
