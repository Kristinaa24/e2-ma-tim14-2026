package com.tim14.slagalica.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.tim14.slagalica.R;
import com.tim14.slagalica.model.PlayerStatistics;
import com.tim14.slagalica.repository.FirebaseCallback;
import com.tim14.slagalica.repository.FirestoreRepository;

public class StatisticsFragment extends Fragment {

    private TextView gamesPlayedValue;
    private TextView winsValue;
    private TextView lossesValue;
    private TextView koZnaZnaPercentValue;
    private TextView koZnaZnaDetailsValue;
    private TextView spojnicePercentValue;
    private TextView spojniceDetailsValue;
    private TextView koChartLabel;
    private TextView spojniceChartLabel;

    private ProgressBar koZnaZnaProgress;
    private ProgressBar spojniceProgress;
    private ProgressBar koChartProgress;
    private ProgressBar spojniceChartProgress;

    private FirestoreRepository firestoreRepository;

    public StatisticsFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_statistics, container, false);

        firestoreRepository = new FirestoreRepository();

        gamesPlayedValue = view.findViewById(R.id.gamesPlayedValue);
        winsValue = view.findViewById(R.id.winsValue);
        lossesValue = view.findViewById(R.id.lossesValue);
        koZnaZnaPercentValue = view.findViewById(R.id.koZnaZnaPercentValue);
        koZnaZnaDetailsValue = view.findViewById(R.id.koZnaZnaDetailsValue);
        spojnicePercentValue = view.findViewById(R.id.spojnicePercentValue);
        spojniceDetailsValue = view.findViewById(R.id.spojniceDetailsValue);
        koChartLabel = view.findViewById(R.id.koChartLabel);
        spojniceChartLabel = view.findViewById(R.id.spojniceChartLabel);
        koZnaZnaProgress = view.findViewById(R.id.koZnaZnaProgress);
        spojniceProgress = view.findViewById(R.id.spojniceProgress);
        koChartProgress = view.findViewById(R.id.koChartProgress);
        spojniceChartProgress = view.findViewById(R.id.spojniceChartProgress);

        Button closeButton = view.findViewById(R.id.closeStatisticsButton);

        closeButton.setOnClickListener(v ->
                requireActivity()
                        .getSupportFragmentManager()
                        .popBackStack()
        );

        setLoadingState();
        loadStatistics();

        return view;
    }

    private void setLoadingState() {
        gamesPlayedValue.setText("0");
        winsValue.setText("0");
        lossesValue.setText("0");
        koZnaZnaPercentValue.setText(getString(R.string.statistics_percent_zero));
        koZnaZnaDetailsValue.setText(getString(R.string.statistics_loading));
        spojnicePercentValue.setText(getString(R.string.statistics_percent_zero));
        spojniceDetailsValue.setText(getString(R.string.statistics_loading));
        koChartLabel.setText(getString(R.string.statistics_ko_chart_format, 0));
        spojniceChartLabel.setText(getString(R.string.statistics_spojnice_chart_format, 0));
        koZnaZnaProgress.setProgress(0);
        spojniceProgress.setProgress(0);
        koChartProgress.setProgress(0);
        spojniceChartProgress.setProgress(0);
    }

    private void loadStatistics() {
        firestoreRepository.getStatistics(new FirebaseCallback<PlayerStatistics>() {
            @Override
            public void onSuccess(PlayerStatistics statistics) {
                bindStatistics(statistics);
            }

            @Override
            public void onError(String error) {
                Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void bindStatistics(PlayerStatistics statistics) {
        int totalKoZnaZnaAnswers =
                statistics.koZnaZnaCorrect + statistics.koZnaZnaWrong;
        int koZnaZnaSuccess = calculatePercent(statistics.koZnaZnaCorrect, totalKoZnaZnaAnswers);
        int spojnicaSuccess = calculatePercent(
                statistics.spojnicaCorrectPairs,
                statistics.spojnicaTotalPairs
        );
        int spojnicaWrongPairs = Math.max(
                0,
                statistics.spojnicaTotalPairs - statistics.spojnicaCorrectPairs
        );

        gamesPlayedValue.setText(String.valueOf(statistics.gamesPlayed));
        winsValue.setText(String.valueOf(statistics.wins));
        lossesValue.setText(String.valueOf(statistics.losses));

        koZnaZnaPercentValue.setText(getString(R.string.statistics_percent_format, koZnaZnaSuccess));
        koZnaZnaDetailsValue.setText(
                getString(
                        R.string.statistics_details_format,
                        statistics.koZnaZnaCorrect,
                        statistics.koZnaZnaWrong,
                        statistics.koZnaZnaTotalScore
                )
        );

        spojnicePercentValue.setText(getString(R.string.statistics_percent_format, spojnicaSuccess));
        spojniceDetailsValue.setText(
                getString(
                        R.string.statistics_details_format,
                        statistics.spojnicaCorrectPairs,
                        spojnicaWrongPairs,
                        statistics.spojnicaTotalScore
                )
        );

        koZnaZnaProgress.setProgress(koZnaZnaSuccess);
        spojniceProgress.setProgress(spojnicaSuccess);
        koChartProgress.setProgress(koZnaZnaSuccess);
        spojniceChartProgress.setProgress(spojnicaSuccess);

        koChartLabel.setText(getString(R.string.statistics_ko_chart_format, koZnaZnaSuccess));
        spojniceChartLabel.setText(getString(R.string.statistics_spojnice_chart_format, spojnicaSuccess));
    }

    private int calculatePercent(int value, int total) {
        if (total <= 0) {
            return 0;
        }

        return value * 100 / total;
    }

}
