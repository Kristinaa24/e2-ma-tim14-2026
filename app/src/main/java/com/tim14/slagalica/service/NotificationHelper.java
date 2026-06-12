package com.tim14.slagalica.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

public class NotificationHelper {

    public static final String CHANNEL_CHAT = "chat_notifications";
    public static final String CHANNEL_RANKING = "ranking_notifications";
    public static final String CHANNEL_REWARDS = "rewards_notifications";
    public static final String CHANNEL_OTHER = "other_notifications";

    public static void createNotificationChannels(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager == null) return;

            // 1. Chat Channel
            NotificationChannel chatChannel = new NotificationChannel(
                    CHANNEL_CHAT,
                    "Chat Messages",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            chatChannel.setDescription("Notifications for real-time chat messages");

            // 2. Ranking Channel
            NotificationChannel rankingChannel = new NotificationChannel(
                    CHANNEL_RANKING,
                    "Ranking Updates",
                    NotificationManager.IMPORTANCE_LOW
            );
            rankingChannel.setDescription("Notifications about your position on weekly/monthly lists");

            // 3. Rewards Channel
            NotificationChannel rewardsChannel = new NotificationChannel(
                    CHANNEL_REWARDS,
                    "Rewards & Missions",
                    NotificationManager.IMPORTANCE_HIGH
            );
            rewardsChannel.setDescription("Notifications about earned tokens, stars and league promotions");

            // 4. Other Channel
            NotificationChannel otherChannel = new NotificationChannel(
                    CHANNEL_OTHER,
                    "Other Notifications",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            otherChannel.setDescription("Friend requests and other system alerts");

            manager.createNotificationChannel(chatChannel);
            manager.createNotificationChannel(rankingChannel);
            manager.createNotificationChannel(rewardsChannel);
            manager.createNotificationChannel(otherChannel);
        }
    }
}
