package com.tim14.slagalica.fragments;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.tim14.slagalica.R;
import com.tim14.slagalica.model.Notification;

import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {

    private List<Notification> notifications;
    private OnNotificationActionListener listener;

    public interface OnNotificationActionListener {
        void onMarkAsRead(Notification notification);
        void onAction(Notification notification, boolean positive);
        void onItemClick(Notification notification);
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

        // Visual distinction with a left stripe
        int stripeColor;
        switch (n.type) {
            case CHAT: stripeColor = ContextCompat.getColor(holder.itemView.getContext(), R.color.slagalica_blue); break;
            case RANKING: stripeColor = ContextCompat.getColor(holder.itemView.getContext(), R.color.slagalica_yellow); break;
            case REWARD: stripeColor = Color.parseColor("#4CAF50"); break; // Emerald Green
            case INVITE: stripeColor = ContextCompat.getColor(holder.itemView.getContext(), R.color.slagalica_red); break;
            default: stripeColor = ContextCompat.getColor(holder.itemView.getContext(), R.color.light_gray); break;
        }
        holder.typeStripe.setBackgroundColor(stripeColor);

        // Handle Actions for INVITE and REWARD types
        if (n.type == Notification.Type.INVITE || n.type == Notification.Type.REWARD) {
            holder.actionsContainer.setVisibility(View.VISIBLE);
            
            if (n.type == Notification.Type.INVITE) {
                holder.btnPositive.setText(R.string.notification_accept);
                holder.btnNegative.setVisibility(View.VISIBLE);
                holder.btnNegative.setText(R.string.notification_decline);
            } else {
                holder.btnPositive.setText(R.string.notification_claim);
                holder.btnNegative.setVisibility(View.GONE);
            }

            holder.btnPositive.setOnClickListener(v -> {
                if (listener != null) listener.onAction(n, true);
            });
            holder.btnNegative.setOnClickListener(v -> {
                if (listener != null) listener.onAction(n, false);
            });
        } else {
            holder.actionsContainer.setVisibility(View.GONE);
        }
        
        holder.itemView.setAlpha(n.read ? 0.7f : 1.0f);
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(n);
        });
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvType, tvDate, tvMessage, btnMarkRead;
        LinearLayout actionsContainer;
        Button btnPositive, btnNegative;
        View typeStripe;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvType = itemView.findViewById(R.id.notificationType);
            tvDate = itemView.findViewById(R.id.notificationDate);
            tvMessage = itemView.findViewById(R.id.notificationMessage);
            btnMarkRead = itemView.findViewById(R.id.markAsRead);
            actionsContainer = itemView.findViewById(R.id.actionButtonsContainer);
            btnPositive = itemView.findViewById(R.id.btnActionPositive);
            btnNegative = itemView.findViewById(R.id.btnActionNegative);
            typeStripe = itemView.findViewById(R.id.typeAccentStripe);
        }
    }
}
