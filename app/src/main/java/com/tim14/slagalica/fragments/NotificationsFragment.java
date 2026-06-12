package com.tim14.slagalica.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.tim14.slagalica.R;
import com.tim14.slagalica.model.Notification;

import java.util.ArrayList;
import java.util.List;

public class NotificationsFragment extends Fragment {

    private List<Notification> allNotifications = new ArrayList<>();
    private List<Notification> filteredNotifications = new ArrayList<>();
    private NotificationAdapter adapter;
    private RecyclerView recyclerView;
    private Button btnAll, btnRead, btnUnread;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_notifications, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.notificationsRecyclerView);
        btnAll = view.findViewById(R.id.btnAllNotifs);
        btnRead = view.findViewById(R.id.btnReadNotifs);
        btnUnread = view.findViewById(R.id.btnUnreadNotifs);
        Button btnBack = view.findViewById(R.id.btnBackFromNotif);

        setupDummyData();

        adapter = new NotificationAdapter(filteredNotifications, notification -> {
            notification.read = true;
            applyFilter(getCurrentFilter());
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        btnAll.setOnClickListener(v -> applyFilter("all"));
        btnRead.setOnClickListener(v -> applyFilter("read"));
        btnUnread.setOnClickListener(v -> applyFilter("unread"));

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                if (getActivity() != null) getActivity().finish();
            });
        }

        applyFilter("all");
    }

    private void setupDummyData() {
        allNotifications.add(new Notification("1", "League Reward", "You reached Silver League! +10 tokens.", "Today, 10:30", false));
        allNotifications.add(new Notification("2", "Friend Request", "Ana wants to be your friend.", "Yesterday, 14:20", true));
        allNotifications.add(new Notification("3", "Tournament", "New tournament starting tomorrow.", "05/05/2024", false));
    }

    private String currentFilter = "all";

    private void applyFilter(String filter) {
        currentFilter = filter;
        filteredNotifications.clear();
        for (Notification n : allNotifications) {
            if (filter.equals("all")) filteredNotifications.add(n);
            else if (filter.equals("read") && n.read) filteredNotifications.add(n);
            else if (filter.equals("unread") && !n.read) filteredNotifications.add(n);
        }
        adapter.notifyDataSetChanged();
        
        // Update button styles
        btnAll.setAlpha(filter.equals("all") ? 1.0f : 0.6f);
        btnRead.setAlpha(filter.equals("read") ? 1.0f : 0.6f);
        btnUnread.setAlpha(filter.equals("unread") ? 1.0f : 0.6f);
    }

    private String getCurrentFilter() {
        return currentFilter;
    }
}
