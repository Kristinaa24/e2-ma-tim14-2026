package com.tim14.slagalica.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.tabs.TabLayout;
import com.tim14.slagalica.R;
import com.tim14.slagalica.model.Notification;

import java.util.ArrayList;
import java.util.List;

public class NotificationsFragment extends Fragment {

    private final List<Notification> allNotifications = new ArrayList<>();
    private final List<Notification> filteredNotifications = new ArrayList<>();
    private NotificationAdapter adapter;
    private TabLayout tabLayout;
    private String currentFilter = "all";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_notifications, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView recyclerView = view.findViewById(R.id.notificationsRecyclerView);
        tabLayout = view.findViewById(R.id.notificationsTabLayout);
        Button btnBack = view.findViewById(R.id.btnBackFromNotif);
        Button btnTestSystem = view.findViewById(R.id.btnTestSystemNotif);

        setupMockData();

        adapter = new NotificationAdapter(filteredNotifications, new NotificationAdapter.OnNotificationActionListener() {
            @Override
            public void onMarkAsRead(Notification notification) {
                notification.read = true;
                applyFilter(currentFilter);
            }

            @Override
            public void onAction(Notification notification, boolean positive) {
                String actionText = positive ? "Accepted/Claimed" : "Declined";
                Toast.makeText(getContext(), actionText + ": " + notification.title, Toast.LENGTH_SHORT).show();
                notification.read = true;
                applyFilter(currentFilter);
            }

            @Override
            public void onItemClick(Notification notification) {
                notification.read = true;
                applyFilter(currentFilter);

                android.content.Intent intent = new android.content.Intent(getActivity(), com.tim14.slagalica.HomeActivity.class);
                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP | android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP);

                if (notification.type == Notification.Type.RANKING) {
                    intent.putExtra("TARGET_SECTION", "ranking");
                } else if (notification.type == Notification.Type.REWARD) {
                    intent.putExtra("TARGET_SECTION", "profile");
                } else if (notification.type == Notification.Type.INVITE) {
                    intent.putExtra("TARGET_SECTION", "friends");
                } else if (notification.type == Notification.Type.CHAT) {
                    intent.putExtra("TARGET_SECTION", "chat");
                }

                startActivity(intent);
                if (getActivity() != null) getActivity().finish();
            }
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 0: applyFilter("all"); break;
                    case 1: applyFilter("read"); break;
                    case 2: applyFilter("unread"); break;
                }
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                if (getActivity() != null) getActivity().finish();
            });
        }

        if (btnTestSystem != null) {
            btnTestSystem.setOnClickListener(v -> {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    if (androidx.core.content.ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                        requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 101);
                        return;
                    }
                }
                sendTestBroadcast();
            });
        }

        applyFilter("all");
    }

    private void sendTestBroadcast() {
        android.content.Intent broadcastIntent = new android.content.Intent("com.tim14.slagalica.SHOW_NOTIFICATION");
        broadcastIntent.setPackage(requireContext().getPackageName());
        broadcastIntent.putExtra("title", "Ranking Update!");
        broadcastIntent.putExtra("message", "Click to see your position.");
        broadcastIntent.putExtra("type", "RANKING");
        requireContext().sendBroadcast(broadcastIntent);
        Toast.makeText(getContext(), "Broadcast sent! Check the top of your screen.", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 101 && grantResults.length > 0 && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            sendTestBroadcast();
        }
    }

    private void setupMockData() {
        allNotifications.clear();
        allNotifications.add(new Notification("1", "League Reward", "You reached Silver League! Claim your reward.", "Today, 10:30", false, Notification.Type.REWARD));
        allNotifications.add(new Notification("2", "Game Invite", "Ana invited you to a match.", "Today, 09:15", false, Notification.Type.INVITE));
        allNotifications.add(new Notification("3", "Ranking", "You are 3rd in the Weekly Ranking!", "Yesterday, 14:20", true, Notification.Type.RANKING));
        allNotifications.add(new Notification("4", "Chat Message", "Mina: Good game!", "Yesterday, 13:05", true, Notification.Type.CHAT));
        allNotifications.add(new Notification("5", "Friend Request", "Ivan wants to be your friend.", "05/05/2024", false, Notification.Type.INVITE));
    }

    private void applyFilter(String filter) {
        currentFilter = filter;
        filteredNotifications.clear();
        for (Notification n : allNotifications) {
            if (filter.equals("all")) filteredNotifications.add(n);
            else if (filter.equals("read") && n.read) filteredNotifications.add(n);
            else if (filter.equals("unread") && !n.read) filteredNotifications.add(n);
        }
        adapter.notifyDataSetChanged();
    }
}
