package com.tim14.slagalica;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.tim14.slagalica.model.HomeRankingItem;
import com.tim14.slagalica.model.RankingCycleInfo;
import com.tim14.slagalica.repository.FirebaseCallback;
import com.tim14.slagalica.repository.FirestoreRepository;

import java.util.ArrayList;
import java.util.List;

public class RankingActivity extends AppCompatActivity {

    private TextView weeklyTabButton;
    private TextView monthlyTabButton;
    private TextView rankingTitleText;
    private TextView rankingCycleText;
    private ListView rankingListView;

    private FirestoreRepository firestoreRepository;
    private HomeRankingAdapter rankingAdapter;
    private String activeType = FirestoreRepository.RANKING_WEEKLY;
    private final Handler refreshHandler = new Handler(Looper.getMainLooper());
    private final Runnable refreshRunnable = new Runnable() {
        @Override
        public void run() {
            loadActiveRanking();
            refreshHandler.postDelayed(this, 120000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ranking);

        firestoreRepository = new FirestoreRepository(this);
        weeklyTabButton = findViewById(R.id.weeklyTabButton);
        monthlyTabButton = findViewById(R.id.monthlyTabButton);
        rankingTitleText = findViewById(R.id.rankingTitleText);
        rankingCycleText = findViewById(R.id.rankingCycleText);
        rankingListView = findViewById(R.id.rankingListView);
        TextView backButton = findViewById(R.id.rankingBackButton);

        rankingAdapter = new HomeRankingAdapter(this, new ArrayList<>());
        rankingListView.setAdapter(rankingAdapter);

        weeklyTabButton.setOnClickListener(v -> switchRanking(FirestoreRepository.RANKING_WEEKLY));
        monthlyTabButton.setOnClickListener(v -> switchRanking(FirestoreRepository.RANKING_MONTHLY));
        backButton.setOnClickListener(v -> finish());

        switchRanking(FirestoreRepository.RANKING_WEEKLY);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshHandler.removeCallbacks(refreshRunnable);
        refreshHandler.post(refreshRunnable);
    }

    @Override
    protected void onPause() {
        refreshHandler.removeCallbacks(refreshRunnable);
        super.onPause();
    }

    private void switchRanking(String type) {
        activeType = type;
        updateTabState();
        loadActiveRanking();
    }

    private void updateTabState() {
        boolean weekly = FirestoreRepository.RANKING_WEEKLY.equals(activeType);
        weeklyTabButton.setBackgroundResource(weekly ? R.drawable.bg_lobby_tab_active : R.drawable.bg_lobby_tab_inactive);
        monthlyTabButton.setBackgroundResource(weekly ? R.drawable.bg_lobby_tab_inactive : R.drawable.bg_lobby_tab_active);
        weeklyTabButton.setTextColor(ContextCompat.getColor(this, weekly ? R.color.white : R.color.slagalica_dark_blue));
        monthlyTabButton.setTextColor(ContextCompat.getColor(this, weekly ? R.color.slagalica_dark_blue : R.color.white));
        rankingTitleText.setText(weekly ? R.string.weekly_ranking_title : R.string.monthly_ranking_title);
    }

    private void loadActiveRanking() {
        RankingCycleInfo info = firestoreRepository.getCurrentRankingCycleInfo(activeType);
        rankingCycleText.setText(getString(R.string.ranking_cycle_format, info.dateRange));

        firestoreRepository.getPlayerRanking(activeType, new FirebaseCallback<List<HomeRankingItem>>() {
            @Override
            public void onSuccess(List<HomeRankingItem> items) {
                rankingAdapter.clear();
                if (items == null || items.isEmpty()) {
                    rankingAdapter.add(new HomeRankingItem(0, getString(R.string.ranking_empty), "", 0, 0));
                } else {
                    rankingAdapter.addAll(items);
                }
                rankingAdapter.notifyDataSetChanged();
                rankingListView.post(() -> setListViewHeightBasedOnChildren(rankingListView));
            }

            @Override
            public void onError(String error) {
                Toast.makeText(RankingActivity.this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setListViewHeightBasedOnChildren(ListView listView) {
        ListAdapter adapter = listView.getAdapter();
        if (adapter == null) {
            return;
        }

        int totalHeight = 0;
        int widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(listView.getWidth(), View.MeasureSpec.AT_MOST);
        for (int index = 0; index < adapter.getCount(); index++) {
            View listItem = adapter.getView(index, null, listView);
            listItem.measure(widthMeasureSpec, View.MeasureSpec.UNSPECIFIED);
            totalHeight += listItem.getMeasuredHeight();
        }

        ViewGroup.LayoutParams params = listView.getLayoutParams();
        params.height = totalHeight + (listView.getDividerHeight() * Math.max(0, adapter.getCount() - 1));
        listView.setLayoutParams(params);
        listView.requestLayout();
    }
}