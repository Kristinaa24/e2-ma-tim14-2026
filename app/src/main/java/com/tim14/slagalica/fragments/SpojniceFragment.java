package com.tim14.slagalica.fragments;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.tim14.slagalica.R;
import com.tim14.slagalica.game.BaseGameFragment;
import com.tim14.slagalica.model.SpojniceRound;
import com.tim14.slagalica.repository.FirebaseCallback;
import com.tim14.slagalica.repository.FirestoreRepository;
import com.tim14.slagalica.service.SpojniceService;

import java.util.ArrayList;
import java.util.List;

public class SpojniceFragment extends BaseGameFragment {

    private static final String TAG = "SpojniceFragment";

    private TextView roundText;
    private TextView currentPlayerText;
    private TextView selectedPairText;
    private TextView secondChanceInfoText;

    private Button left1Button;
    private Button left2Button;
    private Button left3Button;
    private Button left4Button;
    private Button left5Button;
    private Button right1Button;
    private Button right2Button;
    private Button right3Button;
    private Button right4Button;
    private Button right5Button;
    private Button confirmConnectionButton;

    private FirestoreRepository firestoreRepository;
    private SpojniceService spojniceService;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private int round = 1;
    private boolean gameFinished = false;

    private CountDownTimer roundTimer;

    private List<SpojniceRound> rounds = new ArrayList<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_spojnice, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        firestoreRepository = new FirestoreRepository();
        spojniceService = new SpojniceService();

        roundText = view.findViewById(R.id.roundText);
        currentPlayerText = view.findViewById(R.id.currentPlayerText);
        selectedPairText = view.findViewById(R.id.selectedPairText);
        secondChanceInfoText = view.findViewById(R.id.secondChanceInfoText);

        left1Button = view.findViewById(R.id.left1Button);
        left2Button = view.findViewById(R.id.left2Button);
        left3Button = view.findViewById(R.id.left3Button);
        left4Button = view.findViewById(R.id.left4Button);
        left5Button = view.findViewById(R.id.left5Button);

        right1Button = view.findViewById(R.id.right1Button);
        right2Button = view.findViewById(R.id.right2Button);
        right3Button = view.findViewById(R.id.right3Button);
        right4Button = view.findViewById(R.id.right4Button);
        right5Button = view.findViewById(R.id.right5Button);

        confirmConnectionButton = view.findViewById(R.id.confirmConnectionButton);

        spojniceService.startGame(host().getPlayerOneScore(), host().getPlayerTwoScore());

        host().setPhaseText(getString(R.string.phase_spojnice));
        host().setTimerValue(30);
        host().setScores(
                spojniceService.getPlayerOneScore(),
                spojniceService.getPlayerTwoScore()
        );

        confirmConnectionButton.setOnClickListener(v -> confirmConnection());

        disableAllPairButtons();
        confirmConnectionButton.setEnabled(false);
        secondChanceInfoText.setText(getString(R.string.loading_spojnice));
        updateScores();

        loadRoundsFromFirestore();
    }

    private void loadRoundsFromFirestore() {
        firestoreRepository.getSpojniceRounds(new FirebaseCallback<List<SpojniceRound>>() {
            @Override
            public void onSuccess(List<SpojniceRound> result) {
                if (!isAdded() || gameFinished) {
                    return;
                }

                if (result == null || result.size() < 2) {
                    secondChanceInfoText.setText(getString(R.string.not_enough_spojnice_rounds));
                    Toast.makeText(requireContext(),
                            R.string.need_at_least_2_spojnice_rounds,
                            Toast.LENGTH_LONG).show();
                    return;
                }

                rounds = result;

                Log.d(TAG, "Spojnice rounds loaded from Firestore: " + rounds.size());

                startRound();
            }

            @Override
            public void onError(String error) {
                if (!isAdded() || gameFinished) {
                    return;
                }

                secondChanceInfoText.setText(getString(R.string.spojnice_load_failed));
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Error loading Spojnice rounds: " + error);
            }
        });
    }

    private void startRound() {
        if (gameFinished || !startRoundService()) {
            return;
        }

        setRoundItems();
        enableAllPairButtons();

        confirmConnectionButton.setEnabled(true);
        secondChanceInfoText.setText(getString(R.string.spojnice_start_info));

        updateHeader();
        updateScores();
        updateSelectedPairText();
        startRoundTimer();
    }

    private void setRoundItems() {
        int index = round - 1;
        SpojniceRound currentRound = rounds.get(index);

        left1Button.setText(currentRound.getLeftItems().get(0));
        left2Button.setText(currentRound.getLeftItems().get(1));
        left3Button.setText(currentRound.getLeftItems().get(2));
        left4Button.setText(currentRound.getLeftItems().get(3));
        left5Button.setText(currentRound.getLeftItems().get(4));

        right1Button.setText(currentRound.getDisplayedRightItems().get(0));
        right2Button.setText(currentRound.getDisplayedRightItems().get(1));
        right3Button.setText(currentRound.getDisplayedRightItems().get(2));
        right4Button.setText(currentRound.getDisplayedRightItems().get(3));
        right5Button.setText(currentRound.getDisplayedRightItems().get(4));

        left1Button.setOnClickListener(v -> selectLeft(left1Button.getText().toString()));
        left2Button.setOnClickListener(v -> selectLeft(left2Button.getText().toString()));
        left3Button.setOnClickListener(v -> selectLeft(left3Button.getText().toString()));
        left4Button.setOnClickListener(v -> selectLeft(left4Button.getText().toString()));
        left5Button.setOnClickListener(v -> selectLeft(left5Button.getText().toString()));

        right1Button.setOnClickListener(v -> selectRight(right1Button.getText().toString()));
        right2Button.setOnClickListener(v -> selectRight(right2Button.getText().toString()));
        right3Button.setOnClickListener(v -> selectRight(right3Button.getText().toString()));
        right4Button.setOnClickListener(v -> selectRight(right4Button.getText().toString()));
        right5Button.setOnClickListener(v -> selectRight(right5Button.getText().toString()));
    }

    private void startRoundTimer() {
        cancelRoundTimer();

        roundTimer = new CountDownTimer(31000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long seconds = millisUntilFinished / 1000;
                host().setTimerValue((int) seconds);
            }

            @Override
            public void onFinish() {
                roundTimer = null;
                host().setTimerValue(0);

                if (!spojniceService.isSecondChance()
                        && spojniceService.getRemainingPairsCount() > 0) {
                    switchToSecondPlayer();
                } else {
                    prepareNextRoundOrEnd();
                }
            }
        };

        roundTimer.start();
    }

    private void selectLeft(String value) {
        spojniceService.selectLeft(value);
        updateSelectedPairText();
    }

    private void selectRight(String value) {
        spojniceService.selectRight(value);
        updateSelectedPairText();
    }

    private void updateSelectedPairText() {
        String selectedLeft = spojniceService.getSelectedLeft();
        String selectedRight = spojniceService.getSelectedRight();

        if (selectedLeft.isEmpty() && selectedRight.isEmpty()) {
            selectedPairText.setText(getString(R.string.selected_pair_none));
        } else {
            selectedPairText.setText(getString(R.string.selected_pair_format, selectedLeft, selectedRight));
        }
    }

    private void confirmConnection() {
        if (gameFinished) {
            return;
        }

        SpojniceService.ConnectionResult result = spojniceService.confirmConnection();

        if (result.getType() == SpojniceService.ConnectionType.MISSING_SELECTION) {
            Toast.makeText(requireContext(), R.string.select_both_columns, Toast.LENGTH_SHORT).show();
            return;
        }

        if (result.getType() == SpojniceService.ConnectionType.NO_OP) {
            return;
        }

        if (result.getType() == SpojniceService.ConnectionType.CORRECT
                || result.getType() == SpojniceService.ConnectionType.ROUND_SOLVED) {
            disableSolvedPair(result.getSelectedLeft(), result.getSelectedRight());
            Toast.makeText(requireContext(), R.string.correct_connection_points, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(requireContext(), R.string.wrong_connection, Toast.LENGTH_SHORT).show();
        }

        updateSelectedPairText();
        updateScores();

        if (result.getType() == SpojniceService.ConnectionType.ROUND_SOLVED) {
            Toast.makeText(requireContext(), R.string.all_pairs_solved, Toast.LENGTH_SHORT).show();
            handler.postDelayed(() -> {
                if (isAdded()) {
                    prepareNextRoundOrEnd();
                }
            }, 900);
            return;
        }

        if (spojniceService.shouldSwitchToSecondChance()) {
            handler.postDelayed(() -> {
                if (isAdded()) {
                    switchToSecondPlayer();
                }
            }, 900);
            return;
        }

        if (spojniceService.shouldFinishSecondChance()) {
            handler.postDelayed(() -> {
                if (isAdded()) {
                    prepareNextRoundOrEnd();
                }
            }, 900);
        }
    }

    private void switchToSecondPlayer() {
        cancelRoundTimer();

        if (!spojniceService.startSecondChance()) {
            prepareNextRoundOrEnd();
            return;
        }

        updateHeader();
        updateSelectedPairText();

        secondChanceInfoText.setText(
                getString(
                        R.string.second_chance_player_format,
                        spojniceService.getCurrentPlayer(),
                        spojniceService.getSecondChancePairsCount()
                )
        );

        Toast.makeText(requireContext(),
                R.string.second_player_remaining_time,
                Toast.LENGTH_LONG).show();

        handler.postDelayed(() -> {
            if (isAdded()) {
                startRoundTimer();
            }
        }, 2000);
    }

    private void prepareNextRoundOrEnd() {
        cancelRoundTimer();
        spojniceService.finishRound();

        confirmConnectionButton.setEnabled(false);

        if (round == 1) {
            Toast.makeText(requireContext(),
                    R.string.round_one_finished,
                    Toast.LENGTH_LONG).show();

            handler.postDelayed(() -> {
                if (isAdded()) {
                    round = 2;
                    startRound();
                }
            }, 2500);
        } else {
            endGame();
        }
    }

    private void disableSolvedPair(String left, String right) {
        disableLeftButton(left);
        disableRightButton(right);
    }

    private void disableLeftButton(String left) {
        if (left.equals(left1Button.getText().toString())) fadeSolvedButton(left1Button);
        if (left.equals(left2Button.getText().toString())) fadeSolvedButton(left2Button);
        if (left.equals(left3Button.getText().toString())) fadeSolvedButton(left3Button);
        if (left.equals(left4Button.getText().toString())) fadeSolvedButton(left4Button);
        if (left.equals(left5Button.getText().toString())) fadeSolvedButton(left5Button);
    }

    private void disableRightButton(String right) {
        if (right.equals(right1Button.getText().toString())) fadeSolvedButton(right1Button);
        if (right.equals(right2Button.getText().toString())) fadeSolvedButton(right2Button);
        if (right.equals(right3Button.getText().toString())) fadeSolvedButton(right3Button);
        if (right.equals(right4Button.getText().toString())) fadeSolvedButton(right4Button);
        if (right.equals(right5Button.getText().toString())) fadeSolvedButton(right5Button);
    }

    private void fadeSolvedButton(Button button) {
        button.setEnabled(false);
        button.setAlpha(0.45f);
    }

    private void enableAllPairButtons() {
        left1Button.setEnabled(true);
        left2Button.setEnabled(true);
        left3Button.setEnabled(true);
        left4Button.setEnabled(true);
        left5Button.setEnabled(true);

        right1Button.setEnabled(true);
        right2Button.setEnabled(true);
        right3Button.setEnabled(true);
        right4Button.setEnabled(true);
        right5Button.setEnabled(true);

        left1Button.setAlpha(1f);
        left2Button.setAlpha(1f);
        left3Button.setAlpha(1f);
        left4Button.setAlpha(1f);
        left5Button.setAlpha(1f);

        right1Button.setAlpha(1f);
        right2Button.setAlpha(1f);
        right3Button.setAlpha(1f);
        right4Button.setAlpha(1f);
        right5Button.setAlpha(1f);
    }

    private void disableAllPairButtons() {
        left1Button.setEnabled(false);
        left2Button.setEnabled(false);
        left3Button.setEnabled(false);
        left4Button.setEnabled(false);
        left5Button.setEnabled(false);

        right1Button.setEnabled(false);
        right2Button.setEnabled(false);
        right3Button.setEnabled(false);
        right4Button.setEnabled(false);
        right5Button.setEnabled(false);
    }

    private void endGame() {
        if (gameFinished) {
            return;
        }

        gameFinished = true;
        spojniceService.finishGame();

        cancelRoundTimer();

        disableAllPairButtons();
        confirmConnectionButton.setEnabled(false);

        secondChanceInfoText.setText(getString(R.string.end_of_spojnice));

        firestoreRepository.updateSpojniceStatistics(
                spojniceService.getCorrectPairs(),
                spojniceService.getTotalPairs(),
                spojniceService.getTotalScore()
        );

        host().setScores(
                spojniceService.getPlayerOneScore(),
                spojniceService.getPlayerTwoScore()
        );
        host().setTimerValue(0);
        host().setPhaseText(getString(R.string.phase_spojnice_finished));

        Toast.makeText(requireContext(),
                getString(
                        R.string.spojnice_end_format,
                        spojniceService.getPlayerOneScore(),
                        spojniceService.getPlayerTwoScore()
                ),
                Toast.LENGTH_LONG).show();

        handler.postDelayed(() -> {
            if (isAdded()) {
                host().goToNextRound();
            }
        }, 3000);
    }

    private void updateHeader() {
        roundText.setText(getString(R.string.round_counter_format, round));
        currentPlayerText.setText(
                getString(R.string.current_player_format, spojniceService.getCurrentPlayer())
        );
    }

    private void updateScores() {
        host().setScores(
                spojniceService.getPlayerOneScore(),
                spojniceService.getPlayerTwoScore()
        );
    }

    private boolean startRoundService() {
        if (spojniceService.startRound(rounds.get(round - 1), round)) {
            return true;
        }

        Toast.makeText(requireContext(), R.string.invalid_spojnice_data, Toast.LENGTH_LONG).show();
        endGame();
        return false;
    }

    private void cancelRoundTimer() {
        if (roundTimer != null) {
            roundTimer.cancel();
            roundTimer = null;
        }
    }

    @Override
    public void onDestroyView() {
        cancelRoundTimer();
        handler.removeCallbacksAndMessages(null);
        super.onDestroyView();
    }
}
