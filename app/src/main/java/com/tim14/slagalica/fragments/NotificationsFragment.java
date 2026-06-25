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
import com.tim14.slagalica.GameHostActivity;
import com.tim14.slagalica.R;
import com.tim14.slagalica.model.Notification;
import com.tim14.slagalica.repository.FirebaseCallback;
import com.tim14.slagalica.repository.FirestoreRepository;
import com.tim14.slagalica.repository.SharedMatchRepository;

import java.util.ArrayList;
import java.util.List;

public class NotificationsFragment extends Fragment {

    private final List<Notification> allNotifications = new ArrayList<>();
    private final List<Notification> filteredNotifications = new ArrayList<>();
    private NotificationAdapter adapter;
    private TabLayout tabLayout;
    private String currentFilter = "all";
    private FirestoreRepository firestoreRepository;
    private SharedMatchRepository sharedMatchRepository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_notifications, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        firestoreRepository = new FirestoreRepository(requireContext());
        sharedMatchRepository = new SharedMatchRepository();

        RecyclerView recyclerView = view.findViewById(R.id.notificationsRecyclerView);
        tabLayout = view.findViewById(R.id.notificationsTabLayout);
        Button btnBack = view.findViewById(R.id.btnBackFromNotif);
        Button btnTestSystem = view.findViewById(R.id.btnTestSystemNotif);

        adapter = new NotificationAdapter(filteredNotifications, new NotificationAdapter.OnNotificationActionListener() {
            @Override
            public void onMarkAsRead(Notification notification) {
                notification.read = true;
                if (notification.id != null) firestoreRepository.markNotificationAsRead(notification.id);
                applyFilter(currentFilter);
            }

            @Override
            public void onAction(Notification notification, boolean positive) {
                if (notification.type == Notification.Type.INVITE) {
                    handleInviteAction(notification, positive);
                    return;
                }

                String actionText = positive ? "Accepted/Claimed" : "Declined";
                Toast.makeText(getContext(), actionText + ": " + notification.title, Toast.LENGTH_SHORT).show();
                notification.read = true;
                if (notification.id != null) firestoreRepository.markNotificationAsRead(notification.id);
                applyFilter(currentFilter);
            }

            @Override
            public void onItemClick(Notification notification) {
                notification.read = true;
                if (notification.id != null) firestoreRepository.markNotificationAsRead(notification.id);
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

        loadNotifications();
    }

    private void loadNotifications() {
        firestoreRepository.getNotifications(new FirebaseCallback<List<Notification>>() {
            @Override
            public void onSuccess(List<Notification> notifications) {
                if (!isAdded()) return;
                allNotifications.clear();
                if (notifications.isEmpty()) {
                    allNotifications.addAll(getSampleNotifications());
                } else {
                    allNotifications.addAll(notifications);
                }
                applyFilter(currentFilter);
            }

            @Override
            public void onError(String error) {
                if (!isAdded()) return;
                allNotifications.addAll(getSampleNotifications());
                applyFilter(currentFilter);
            }
        });
    }

    private List<Notification> getSampleNotifications() {
        List<Notification> samples = new ArrayList<>();
        samples.add(new Notification("s1", "League Reward", "You reached Silver League! Claim your reward.", "Today, 10:30", false, Notification.Type.REWARD));
        samples.add(new Notification("s2", "Game Invite", "Ana invited you to a match.", "Today, 09:15", false, Notification.Type.INVITE));
        samples.add(new Notification("s3", "Ranking Update", "You are in 3rd place on the Weekly Leaderboard!", "Yesterday, 14:20", true, Notification.Type.RANKING));
        samples.add(new Notification("s4", "Chat Message", "Mina: Great game! Well played.", "Yesterday, 13:05", true, Notification.Type.CHAT));
        samples.add(new Notification("s5", "Game Invite", "Ivan invited you to a match.", "13/06/2025", false, Notification.Type.INVITE));
        return samples;
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

    private void handleInviteAction(Notification notification, boolean positive) {
        if (notification == null) {
            return;
        }

        if (!positive) {
            sharedMatchRepository.declineFriendlyInvite(notification);
            notification.read = true;
            notification.invitationStatus = "DECLINED";
            applyFilter(currentFilter);
            Toast.makeText(getContext(), R.string.friendly_invite_declined_message, Toast.LENGTH_SHORT).show();
            return;
        }

        sharedMatchRepository.acceptFriendlyInvite(notification, new FirebaseCallback<SharedMatchRepository.MatchJoinResult>() {
            @Override
            public void onSuccess(SharedMatchRepository.MatchJoinResult result) {
                if (!isAdded()) {
                    return;
                }

                notification.read = true;
                notification.invitationStatus = "ACCEPTED";
                applyFilter(currentFilter);

                android.content.Intent intent =
                        new android.content.Intent(requireContext(), GameHostActivity.class);
                intent.putExtra("IS_GUEST", false);
                intent.putExtra(GameHostActivity.EXTRA_REMOTE_MATCH, true);
                intent.putExtra(GameHostActivity.EXTRA_REMOTE_MATCH_ID, result.matchId);
                intent.putExtra(GameHostActivity.EXTRA_LOCAL_PLAYER_NUMBER, result.localPlayerNumber);
                startActivity(intent);

                if (getActivity() != null) {
                    getActivity().finish();
                }
            }

            @Override
            public void onError(String error) {
                if (isAdded()) {
                    Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show();
                }
            }
        });
    }
}
