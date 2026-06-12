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
import com.tim14.slagalica.service.StatisticsService;

public class StatisticsFragment extends Fragment {

    private TextView gamesPlayedValue;
    private TextView winsValue;
    private TextView lossesValue;
    private TextView koZnaZnaPercentValue;
    private TextView koZnaZnaDetailsValue;
    private TextView spojnicePercentValue;
    private TextView spojniceDetailsValue;
    private TextView mojBrojPercentValue;
    private TextView mojBrojDetailsValue;
    private TextView korakPoKorakPercentValue;
    private TextView korakPoKorakDetailsValue;
    private TextView koChartLabel;
    private TextView spojniceChartLabel;
    private TextView mojBrojChartLabel;
    private TextView korakPoKorakChartLabel;

    private ProgressBar koZnaZnaProgress;
    private ProgressBar spojniceProgress;
    private ProgressBar mojBrojProgress;
    private ProgressBar korakPoKorakProgress;
    private ProgressBar koChartProgress;
    private ProgressBar spojniceChartProgress;
    private ProgressBar mojBrojChartProgress;
    private ProgressBar korakPoKorakChartProgress;

    private FirestoreRepository firestoreRepository;
    private StatisticsService statisticsService;

    public StatisticsFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_statistics, container, false);

        firestoreRepository = new FirestoreRepository(requireContext());
        statisticsService = new StatisticsService();

        gamesPlayedValue = view.findViewById(R.id.gamesPlayedValue);
        winsValue = view.findViewById(R.id.winsValue);
        lossesValue = view.findViewById(R.id.lossesValue);
        koZnaZnaPercentValue = view.findViewById(R.id.koZnaZnaPercentValue);
        koZnaZnaDetailsValue = view.findViewById(R.id.koZnaZnaDetailsValue);
        spojnicePercentValue = view.findViewById(R.id.spojnicePercentValue);
        spojniceDetailsValue = view.findViewById(R.id.spojniceDetailsValue);
        mojBrojPercentValue = view.findViewById(R.id.mojBrojPercentValue);
        mojBrojDetailsValue = view.findViewById(R.id.mojBrojDetailsValue);
        korakPoKorakPercentValue = view.findViewById(R.id.korakPoKorakPercentValue);
        korakPoKorakDetailsValue = view.findViewById(R.id.korakPoKorakDetailsValue);
        koChartLabel = view.findViewById(R.id.koChartLabel);
        spojniceChartLabel = view.findViewById(R.id.spojniceChartLabel);
        mojBrojChartLabel = view.findViewById(R.id.mojBrojChartLabel);
        korakPoKorakChartLabel = view.findViewById(R.id.korakPoKorakChartLabel);
        koZnaZnaProgress = view.findViewById(R.id.koZnaZnaProgress);
        spojniceProgress = view.findViewById(R.id.spojniceProgress);
        mojBrojProgress = view.findViewById(R.id.mojBrojProgress);
        korakPoKorakProgress = view.findViewById(R.id.korakPoKorakProgress);
        koChartProgress = view.findViewById(R.id.koChartProgress);
        spojniceChartProgress = view.findViewById(R.id.spojniceChartProgress);
        mojBrojChartProgress = view.findViewById(R.id.mojBrojChartProgress);
        korakPoKorakChartProgress = view.findViewById(R.id.korakPoKorakChartProgress);

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
        mojBrojPercentValue.setText(getString(R.string.statistics_percent_zero));
        mojBrojDetailsValue.setText(getString(R.string.statistics_loading));
        korakPoKorakPercentValue.setText(getString(R.string.statistics_percent_zero));
        korakPoKorakDetailsValue.setText(getString(R.string.statistics_loading));
        koChartLabel.setText(getString(R.string.statistics_ko_chart_format, 0));
        spojniceChartLabel.setText(getString(R.string.statistics_spojnice_chart_format, 0));
        mojBrojChartLabel.setText(getString(R.string.statistics_moj_broj_chart_format, 0));
        korakPoKorakChartLabel.setText(getString(R.string.statistics_korak_po_korak_chart_format, 0));
        koZnaZnaProgress.setProgress(0);
        spojniceProgress.setProgress(0);
        mojBrojProgress.setProgress(0);
        korakPoKorakProgress.setProgress(0);
        koChartProgress.setProgress(0);
        spojniceChartProgress.setProgress(0);
        mojBrojChartProgress.setProgress(0);
        korakPoKorakChartProgress.setProgress(0);
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
        StatisticsService.StatisticsUiData statisticsUiData =
                statisticsService.prepareStatistics(statistics);

        gamesPlayedValue.setText(String.valueOf(statisticsUiData.getGamesPlayed()));
        winsValue.setText(String.valueOf(statisticsUiData.getWins()));
        lossesValue.setText(String.valueOf(statisticsUiData.getLosses()));

        koZnaZnaPercentValue.setText(
                getString(
                        R.string.statistics_percent_format,
                        statisticsUiData.getKoZnaZnaSuccessPercent()
                )
        );
        koZnaZnaDetailsValue.setText(buildBasicDetails(
                "Correct",
                statisticsUiData.getKoZnaZnaCorrect(),
                "Wrong",
                statisticsUiData.getKoZnaZnaWrong(),
                statisticsUiData.getKoZnaZnaTotalScore()
        ));

        spojnicePercentValue.setText(
                getString(
                        R.string.statistics_percent_format,
                        statisticsUiData.getSpojniceSuccessPercent()
                )
        );
        spojniceDetailsValue.setText(buildBasicDetails(
                "Correct",
                statisticsUiData.getSpojniceCorrectPairs(),
                "Wrong",
                statisticsUiData.getSpojniceWrongPairs(),
                statisticsUiData.getSpojniceTotalScore()
        ));
        mojBrojPercentValue.setText(
                getString(
                        R.string.statistics_percent_format,
                        statisticsUiData.getMojBrojSuccessPercent()
                )
        );
        mojBrojDetailsValue.setText(buildBasicDetails(
                "Exact hits",
                statisticsUiData.getMojBrojExactHits(),
                "Close hits",
                statisticsUiData.getMojBrojCloseHits(),
                statisticsUiData.getMojBrojTotalScore()
        ));
        int[] korakPoKorakStepPercents = statisticsUiData.getKorakPoKorakStepPercents();
        korakPoKorakPercentValue.setText(
                getString(
                        R.string.statistics_percent_format,
                        statisticsUiData.getKorakPoKorakSuccessPercent()
                )
        );
        korakPoKorakDetailsValue.setText(buildKorakPoKorakDetails(
                statisticsUiData,
                korakPoKorakStepPercents
        ));

        koZnaZnaProgress.setProgress(statisticsUiData.getKoZnaZnaSuccessPercent());
        spojniceProgress.setProgress(statisticsUiData.getSpojniceSuccessPercent());
        mojBrojProgress.setProgress(statisticsUiData.getMojBrojSuccessPercent());
        korakPoKorakProgress.setProgress(statisticsUiData.getKorakPoKorakSuccessPercent());
        koChartProgress.setProgress(statisticsUiData.getKoZnaZnaSuccessPercent());
        spojniceChartProgress.setProgress(statisticsUiData.getSpojniceSuccessPercent());
        mojBrojChartProgress.setProgress(statisticsUiData.getMojBrojSuccessPercent());
        korakPoKorakChartProgress.setProgress(statisticsUiData.getKorakPoKorakSuccessPercent());

        koChartLabel.setText(
                getString(
                        R.string.statistics_ko_chart_format,
                        statisticsUiData.getKoZnaZnaSuccessPercent()
                )
        );
        spojniceChartLabel.setText(
                getString(
                        R.string.statistics_spojnice_chart_format,
                        statisticsUiData.getSpojniceSuccessPercent()
                )
        );
        mojBrojChartLabel.setText(
                getString(
                        R.string.statistics_moj_broj_chart_format,
                        statisticsUiData.getMojBrojSuccessPercent()
                )
        );
        korakPoKorakChartLabel.setText(
                getString(
                        R.string.statistics_korak_po_korak_chart_format,
                        statisticsUiData.getKorakPoKorakSuccessPercent()
                )
        );
    }

    private String buildBasicDetails(
            String firstLabel,
            int firstValue,
            String secondLabel,
            int secondValue,
            int score
    ) {
        return firstLabel + ": " + firstValue
                + "\n" + secondLabel + ": " + secondValue
                + "\nScore: " + score;
    }

    private String buildKorakPoKorakDetails(
            StatisticsService.StatisticsUiData statisticsUiData,
            int[] stepPercents
    ) {
        return "Solved rounds: " + statisticsUiData.getKorakPoKorakSolved()
                + "\nBest step: " + statisticsUiData.getKorakPoKorakBestStep()
                + "\nScore: " + statisticsUiData.getKorakPoKorakTotalScore()
                + "\n\nStep 1: " + stepPercents[0] + "%   Step 2: " + stepPercents[1] + "%"
                + "\nStep 3: " + stepPercents[2] + "%   Step 4: " + stepPercents[3] + "%"
                + "\nStep 5: " + stepPercents[4] + "%   Step 6: " + stepPercents[5] + "%"
                + "\nStep 7: " + stepPercents[6] + "%";
    }

}
