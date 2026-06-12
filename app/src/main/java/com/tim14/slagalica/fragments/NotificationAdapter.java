package com.tim14.slagalica.fragments;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.tim14.slagalica.R;
import com.tim14.slagalica.model.Notification;

import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {

    private List<Notification> notifications;
    private OnNotificationActionListener listener;

    public interface OnNotificationActionListener {
        void onMarkAsRead(Notification notification);
    }

    public NotificationAdapter(List<Notification> notifications, OnNotificationActionListener listener) {
        this.notifications = notifications;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Notification n = notifications.get(position);
        holder.tvType.setText(n.title);
        holder.tvDate.setText(n.timestamp);
        holder.tvMessage.setText(n.message);
        
        holder.btnMarkRead.setVisibility(n.read ? View.GONE : View.VISIBLE);
        holder.btnMarkRead.setOnClickListener(v -> {
            if (listener != null) listener.onMarkAsRead(n);
        });
        
        holder.itemView.setAlpha(n.read ? 0.7f : 1.0f);
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvType, tvDate, tvMessage, btnMarkRead;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvType = itemView.findViewById(R.id.notificationType);
            tvDate = itemView.findViewById(R.id.notificationDate);
            tvMessage = itemView.findViewById(R.id.notificationMessage);
            btnMarkRead = itemView.findViewById(R.id.markAsRead);
        }
    }
}
