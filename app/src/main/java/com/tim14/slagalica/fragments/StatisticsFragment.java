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
    private TextView asocijacijePercentValue;
    private TextView asocijacijeDetailsValue;
    private TextView skockoPercentValue;
    private TextView skockoDetailsValue;

    private ProgressBar koZnaZnaProgress;
    private ProgressBar spojniceProgress;
    private ProgressBar mojBrojProgress;
    private ProgressBar korakPoKorakProgress;
    private ProgressBar asocijacijeProgress;
    private ProgressBar skockoProgress;

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
        asocijacijePercentValue = view.findViewById(R.id.asocijacijePercentValue);
        asocijacijeDetailsValue = view.findViewById(R.id.asocijacijeDetailsValue);
        skockoPercentValue = view.findViewById(R.id.skockoPercentValue);
        skockoDetailsValue = view.findViewById(R.id.skockoDetailsValue);
        koZnaZnaProgress = view.findViewById(R.id.koZnaZnaProgress);
        spojniceProgress = view.findViewById(R.id.spojniceProgress);
        mojBrojProgress = view.findViewById(R.id.mojBrojProgress);
        korakPoKorakProgress = view.findViewById(R.id.korakPoKorakProgress);
        asocijacijeProgress = view.findViewById(R.id.asocijacijeProgress);
        skockoProgress = view.findViewById(R.id.skockoProgress);

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
        winsValue.setText(getString(R.string.statistics_percent_zero));
        lossesValue.setText(getString(R.string.statistics_percent_zero));
        koZnaZnaPercentValue.setText(getString(R.string.statistics_percent_zero));
        koZnaZnaDetailsValue.setText(getString(R.string.statistics_loading));
        spojnicePercentValue.setText(getString(R.string.statistics_percent_zero));
        spojniceDetailsValue.setText(getString(R.string.statistics_loading));
        mojBrojPercentValue.setText(getString(R.string.statistics_percent_zero));
        mojBrojDetailsValue.setText(getString(R.string.statistics_loading));
        korakPoKorakPercentValue.setText(getString(R.string.statistics_percent_zero));
        korakPoKorakDetailsValue.setText(getString(R.string.statistics_loading));
        asocijacijePercentValue.setText(getString(R.string.statistics_percent_zero));
        asocijacijeDetailsValue.setText(getString(R.string.statistics_loading));
        skockoPercentValue.setText(getString(R.string.statistics_percent_zero));
        skockoDetailsValue.setText(getString(R.string.statistics_loading));
        koZnaZnaProgress.setProgress(0);
        spojniceProgress.setProgress(0);
        mojBrojProgress.setProgress(0);
        korakPoKorakProgress.setProgress(0);
        asocijacijeProgress.setProgress(0);
        skockoProgress.setProgress(0);
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
        winsValue.setText(getString(
                R.string.statistics_percent_format,
                statisticsUiData.getWinPercent()
        ));
        lossesValue.setText(getString(
                R.string.statistics_percent_format,
                statisticsUiData.getLossPercent()
        ));

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
        asocijacijePercentValue.setText(
                getString(
                        R.string.statistics_percent_format,
                        statisticsUiData.getAsocijacijeSuccessPercent()
                )
        );
        asocijacijeDetailsValue.setText(buildBasicDetails(
                "Solved",
                statisticsUiData.getAsocijacijeSolved(),
                "Unsolved",
                statisticsUiData.getAsocijacijeUnsolved(),
                statisticsUiData.getAsocijacijeTotalScore()
        ));
        int[] skockoAttemptPercents = statisticsUiData.getSkockoAttemptPercents();
        skockoPercentValue.setText(
                getString(
                        R.string.statistics_percent_format,
                        statisticsUiData.getSkockoSuccessPercent()
                )
        );
        skockoDetailsValue.setText(buildSkockoDetails(
                statisticsUiData,
                skockoAttemptPercents
        ));

        koZnaZnaProgress.setProgress(statisticsUiData.getKoZnaZnaSuccessPercent());
        spojniceProgress.setProgress(statisticsUiData.getSpojniceSuccessPercent());
        mojBrojProgress.setProgress(statisticsUiData.getMojBrojSuccessPercent());
        korakPoKorakProgress.setProgress(statisticsUiData.getKorakPoKorakSuccessPercent());
        asocijacijeProgress.setProgress(statisticsUiData.getAsocijacijeSuccessPercent());
        skockoProgress.setProgress(statisticsUiData.getSkockoSuccessPercent());
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

    private String buildSkockoDetails(
            StatisticsService.StatisticsUiData statisticsUiData,
            int[] attemptPercents
    ) {
        return "Solved: " + statisticsUiData.getSkockoSolvedCount()
                + "\nScore: " + statisticsUiData.getSkockoTotalScore()
                + "\n\nAttempt 1: " + attemptPercents[0] + "%   Attempt 2: " + attemptPercents[1] + "%"
                + "\nAttempt 3: " + attemptPercents[2] + "%   Attempt 4: " + attemptPercents[3] + "%"
                + "\nAttempt 5: " + attemptPercents[4] + "%   Attempt 6: " + attemptPercents[5] + "%";
    }

}
