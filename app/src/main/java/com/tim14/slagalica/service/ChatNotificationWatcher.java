package com.tim14.slagalica.service;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.HashSet;
import java.util.Set;

public final class ChatNotificationWatcher {

    private static ChatNotificationWatcher instance;

    private final Set<String> knownNotificationIds = new HashSet<>();

    private ListenerRegistration listenerRegistration;
    private String listeningUserId = "";
    private boolean initialSnapshotConsumed;
    private boolean appInForeground;

    private ChatNotificationWatcher() {
    }

    public static synchronized ChatNotificationWatcher getInstance() {
        if (instance == null) {
            instance = new ChatNotificationWatcher();
        }
        return instance;
    }

    public synchronized void ensureStarted(@NonNull Context context) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            stop();
            return;
        }

        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (TextUtils.isEmpty(currentUserId)) {
            stop();
            return;
        }

        if (listenerRegistration != null && currentUserId.equals(listeningUserId)) {
            return;
        }

        stop();
        listeningUserId = currentUserId;
        initialSnapshotConsumed = false;

        listenerRegistration = FirebaseFirestore.getInstance()
                .collection("notifications")
                .whereEqualTo("userId", currentUserId)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null || snapshot == null) {
                        return;
                    }

                    if (!initialSnapshotConsumed) {
                        for (com.google.firebase.firestore.DocumentSnapshot document : snapshot.getDocuments()) {
                            knownNotificationIds.add(document.getId());
                        }
                        initialSnapshotConsumed = true;
                        return;
                    }

                    for (DocumentChange change : snapshot.getDocumentChanges()) {
                        if (change.getType() != DocumentChange.Type.ADDED) {
                            continue;
                        }

                        String notificationId = change.getDocument().getId();
                        if (!knownNotificationIds.add(notificationId)) {
                            continue;
                        }

                        String type = change.getDocument().getString("typeString");
                        boolean isChat = "CHAT".equalsIgnoreCase(type);
                        boolean read = Boolean.TRUE.equals(change.getDocument().getBoolean("read"));
                        if (!isChat || read || appInForeground) {
                            continue;
                        }

                        String title = change.getDocument().getString("title");
                        String message = change.getDocument().getString("message");

                        Intent broadcastIntent = new Intent("com.tim14.slagalica.SHOW_NOTIFICATION");
                        broadcastIntent.setPackage(context.getPackageName());
                        broadcastIntent.putExtra(
                                "title",
                                TextUtils.isEmpty(title)
                                        ? "Regional chat"
                                        : title
                        );
                        broadcastIntent.putExtra(
                                "message",
                                TextUtils.isEmpty(message)
                                        ? "You have a new message from your region."
                                        : message
                        );
                        broadcastIntent.putExtra("type", "CHAT");
                        context.sendBroadcast(broadcastIntent);
                    }
                });
    }

    public synchronized void stop() {
        if (listenerRegistration != null) {
            listenerRegistration.remove();
            listenerRegistration = null;
        }

        listeningUserId = "";
        initialSnapshotConsumed = false;
        knownNotificationIds.clear();
    }

    public synchronized void setAppInForeground(boolean appInForeground) {
        this.appInForeground = appInForeground;
    }
}
