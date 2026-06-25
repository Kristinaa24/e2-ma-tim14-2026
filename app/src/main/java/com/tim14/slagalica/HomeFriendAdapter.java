package com.tim14.slagalica;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.tim14.slagalica.model.HomeFriendItem;

import java.util.List;

public class HomeFriendAdapter extends ArrayAdapter<HomeFriendItem> {

    public interface OnInviteClickListener {
        void onInviteClick(HomeFriendItem item);
    }

    private final OnInviteClickListener onInviteClickListener;

    public HomeFriendAdapter(
            @NonNull Context context,
            @NonNull List<HomeFriendItem> items,
            @Nullable OnInviteClickListener onInviteClickListener
    ) {
        super(context, 0, items);
        this.onInviteClickListener = onInviteClickListener;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View view = convertView;

        if (view == null) {
            view = LayoutInflater.from(getContext())
                    .inflate(R.layout.item_home_friend, parent, false);
        }

        HomeFriendItem item = getItem(position);

        if (item == null) {
            return view;
        }

        LinearLayout regularContent = view.findViewById(R.id.friendRegularContent);
        LinearLayout inviteContent = view.findViewById(R.id.friendInviteContent);

        if (item.isInviteTile()) {
            regularContent.setVisibility(View.GONE);
            inviteContent.setVisibility(View.VISIBLE);
            return view;
        }

        regularContent.setVisibility(View.VISIBLE);
        inviteContent.setVisibility(View.GONE);

        TextView rankText = view.findViewById(R.id.friendRankText);
        TextView initialText = view.findViewById(R.id.friendInitialText);
        TextView nameText = view.findViewById(R.id.friendNameText);
        TextView statusText = view.findViewById(R.id.friendStatusText);
        TextView starsText = view.findViewById(R.id.friendStarsText);
        Button inviteButton = view.findViewById(R.id.friendInviteButton);

        rankText.setText(String.valueOf(item.getRank()));
        initialText.setText(item.getInitial());
        nameText.setText(item.getName());
        if (item.isInMatch()) {
            statusText.setText(R.string.friend_status_in_match);
        } else {
            statusText.setText(item.isOnline()
                    ? getContext().getString(R.string.friend_status_online)
                    : getContext().getString(R.string.friend_status_offline)
            );
        }
        starsText.setText(String.valueOf(item.getStars()));

        inviteButton.setEnabled(item.canInvite());
        inviteButton.setAlpha(item.canInvite() ? 1f : 0.6f);
        inviteButton.setOnClickListener(v -> {
            if (onInviteClickListener != null) {
                onInviteClickListener.onInviteClick(item);
            }
        });

        return view;
    }
}
