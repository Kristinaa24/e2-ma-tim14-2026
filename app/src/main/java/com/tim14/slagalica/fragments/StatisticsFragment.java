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

        firestoreRepository = new FirestoreRepository();

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
        statisticsText.setText("Loading statistics...");

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

                statisticsText.setText(
                        "Summary\n" +
                                "Games played: " + statistics.gamesPlayed +
                                "\nWins: " + statistics.wins +
                                "\nLosses: " + statistics.losses +
                                "\n\nKo zna zna\n" +
                                "Correct answers: " + statistics.koZnaZnaCorrect +
                                "\nWrong answers: " + statistics.koZnaZnaWrong +
                                "\nSuccess: " + koZnaZnaSuccess + "%" +
                                "\nTotal score: " + statistics.koZnaZnaTotalScore +
                                "\n\nSpojnice\n" +
                                "Correct pairs: " + statistics.spojnicaCorrectPairs +
                                "\nTotal pairs: " + statistics.spojnicaTotalPairs +
                                "\nSuccess: " + spojnicaSuccess + "%" +
                                "\nTotal score: " + statistics.spojnicaTotalScore
                );
            }

            @Override
            public void onError(String error) {
                statisticsText.setText("Statistics could not be loaded.");
                Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show();
            }
        });
    }
}