package com.tim14.slagalica;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.tim14.slagalica.model.HomeRankingItem;

import java.util.List;

public class HomeRankingAdapter extends ArrayAdapter<HomeRankingItem> {

    public HomeRankingAdapter(@NonNull Context context, @NonNull List<HomeRankingItem> items) {
        super(context, 0, items);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View view = convertView;

        if (view == null) {
            view = LayoutInflater.from(getContext())
                    .inflate(R.layout.item_home_ranking, parent, false);
        }

        HomeRankingItem item = getItem(position);

        if (item == null) {
            return view;
        }

        TextView positionText = view.findViewById(R.id.rankingPositionText);
        TextView playerNameText = view.findViewById(R.id.rankingPlayerNameText);
        TextView leagueText = view.findViewById(R.id.rankingLeagueText);
        TextView starsText = view.findViewById(R.id.rankingStarsText);

        positionText.setText(String.valueOf(item.getPosition()));
        playerNameText.setText(item.getPlayerName());
        leagueText.setText(LeagueUtils.getLeagueIcon(item.getLeague()) + " " + item.getLeagueName());
        starsText.setText(String.valueOf(item.getStars()));

        return view;
    }
}