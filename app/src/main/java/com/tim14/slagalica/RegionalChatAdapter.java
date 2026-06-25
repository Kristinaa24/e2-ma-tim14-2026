package com.tim14.slagalica;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.tim14.slagalica.model.ChatMessage;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class RegionalChatAdapter extends RecyclerView.Adapter<RegionalChatAdapter.ChatViewHolder> {

    private static final int VIEW_TYPE_RECEIVED = 0;
    private static final int VIEW_TYPE_SENT = 1;

    private final List<ChatMessage> messages;
    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

    private String currentUserId = "";

    public RegionalChatAdapter(List<ChatMessage> messages) {
        this.messages = messages;
    }

    public void setCurrentUserId(String currentUserId) {
        this.currentUserId = currentUserId == null ? "" : currentUserId;
    }

    @Override
    public int getItemViewType(int position) {
        ChatMessage message = messages.get(position);
        return message != null
                && message.senderId != null
                && message.senderId.equals(currentUserId)
                ? VIEW_TYPE_SENT
                : VIEW_TYPE_RECEIVED;
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layoutRes = viewType == VIEW_TYPE_SENT
                ? R.layout.item_regional_chat_sent
                : R.layout.item_regional_chat_received;
        View view = LayoutInflater.from(parent.getContext()).inflate(layoutRes, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        ChatMessage message = messages.get(position);
        if (message == null) {
            return;
        }

        String senderName = message.senderName == null || message.senderName.trim().isEmpty()
                ? holder.itemView.getContext().getString(R.string.chat_player_fallback)
                : message.senderName.trim();
        holder.senderNameText.setText(senderName);
        holder.messageText.setText(message.text == null ? "" : message.text);
        holder.sentAtText.setText(dateFormat.format(message.createdAt));
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static final class ChatViewHolder extends RecyclerView.ViewHolder {
        private final TextView senderNameText;
        private final TextView messageText;
        private final TextView sentAtText;

        private ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            senderNameText = itemView.findViewById(R.id.chatSenderNameText);
            messageText = itemView.findViewById(R.id.chatMessageText);
            sentAtText = itemView.findViewById(R.id.chatSentAtText);
        }
    }
}
