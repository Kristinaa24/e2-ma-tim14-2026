package com.tim14.slagalica.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.tim14.slagalica.R;
import com.tim14.slagalica.model.PlayerStatistics;
import com.tim14.slagalica.repository.FirebaseCallback;
import com.tim14.slagalica.repository.FirestoreRepository;

public class StatisticsFragment extends Fragment {

    private TextView statisticsText;
    private FirestoreRepository firestoreRepository;

    public StatisticsFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_statistics, container, false);

        firestoreRepository = new FirestoreRepository(requireContext());

        statisticsText = view.findViewById(R.id.statisticsText);
        Button closeButton = view.findViewById(R.id.closeStatisticsButton);

        closeButton.setOnClickListener(v ->
                requireActivity()
                        .getSupportFragmentManager()
                        .popBackStack()
        );

        loadStatistics();

        return view;
    }

    private void loadStatistics() {
        statisticsText.setText(getString(R.string.statistics_loading_runtime));

        firestoreRepository.getStatistics(new FirebaseCallback<PlayerStatistics>() {
            @Override
            public void onSuccess(PlayerStatistics statistics) {
                int totalKoZnaZnaAnswers =
                        statistics.koZnaZnaCorrect + statistics.koZnaZnaWrong;

                int koZnaZnaSuccess = 0;

                if (totalKoZnaZnaAnswers > 0) {
                    koZnaZnaSuccess =
                            statistics.koZnaZnaCorrect * 100 / totalKoZnaZnaAnswers;
                }

                int spojnicaSuccess = 0;

                if (statistics.spojnicaTotalPairs > 0) {
                    spojnicaSuccess =
                            statistics.spojnicaCorrectPairs * 100 / statistics.spojnicaTotalPairs;
                }

                String bestStepText = statistics.korakPoKorakBestStep == 0
                        ? "-"
                        : String.valueOf(statistics.korakPoKorakBestStep);

                statisticsText.setText(getString(
                        R.string.statistics_summary_format,
                        statistics.gamesPlayed,
                        statistics.wins,
                        statistics.losses,
                        statistics.koZnaZnaCorrect,
                        statistics.koZnaZnaWrong,
                        koZnaZnaSuccess,
                        statistics.koZnaZnaTotalScore,
                        statistics.spojnicaCorrectPairs,
                        statistics.spojnicaTotalPairs,
                        spojnicaSuccess,
                        statistics.spojnicaTotalScore,
                        statistics.korakPoKorakSolved,
                        bestStepText,
                        statistics.korakPoKorakTotalScore,
                        statistics.mojBrojExactHits,
                        statistics.mojBrojCloseHits,
                        statistics.mojBrojTotalScore
                ));
            }

            @Override
            public void onError(String error) {
                statisticsText.setText(getString(R.string.statistics_load_failed));
                Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show();
            }
        });
    }
}
