package com.tim14.slagalica.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.tim14.slagalica.R;
import com.tim14.slagalica.game.BaseGameFragment;
import com.tim14.slagalica.game.GameNavigator;
import com.tim14.slagalica.repository.FirestoreRepository;
import com.tim14.slagalica.service.SkockoService;

import java.util.ArrayList;
import java.util.List;

public class SkockoFragment extends BaseGameFragment {

    private GameNavigator navigator;
    private FirestoreRepository firestoreRepository;
    private SkockoService skockoService;
    private int currentRound = 1;
    private int playerOneScore = 0;
    private int playerTwoScore = 0;
    private boolean isOpponentTurn = false;
    private SkockoService.Symbol[] currentAttemptSymbols = new SkockoService.Symbol[4];
    private final Handler handler = new Handler(Looper.getMainLooper());

    private View[] rows;
    private Button btnSubmit;
    private View palette;

    @Override
    public void onAttach(@NonNull android.content.Context context) {
        super.onAttach(context);
        if (context instanceof GameNavigator) {
            navigator = (GameNavigator) context;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_skocko, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        firestoreRepository = new FirestoreRepository(requireContext());
        playerOneScore = navigator.getPlayerOneScore();
        playerTwoScore = navigator.getPlayerTwoScore();

        rows = new View[]{
                view.findViewById(R.id.row1),
                view.findViewById(R.id.row2),
                view.findViewById(R.id.row3),
                view.findViewById(R.id.row4),
                view.findViewById(R.id.row5),
                view.findViewById(R.id.row6),
                view.findViewById(R.id.rowOpponent),
                view.findViewById(R.id.rowSolution)
        };

        palette = (View) view.findViewById(R.id.btnSymbolSkocko).getParent();
        btnSubmit = view.findViewById(R.id.btnSubmitSkocko);

        setupPalette(view);
        btnSubmit.setOnClickListener(v -> checkAttempt());

        startNewRound();
    }

    private void setupPalette(View view) {
        view.findViewById(R.id.btnSymbolSkocko).setOnClickListener(v -> addSymbol(SkockoService.Symbol.SKOCKO));
        view.findViewById(R.id.btnSymbolClub).setOnClickListener(v -> addSymbol(SkockoService.Symbol.CLUB));
        view.findViewById(R.id.btnSymbolSpade).setOnClickListener(v -> addSymbol(SkockoService.Symbol.SPADE));
        view.findViewById(R.id.btnSymbolHeart).setOnClickListener(v -> addSymbol(SkockoService.Symbol.HEART));
        view.findViewById(R.id.btnSymbolDiamond).setOnClickListener(v -> addSymbol(SkockoService.Symbol.DIAMOND));
        view.findViewById(R.id.btnSymbolStar).setOnClickListener(v -> addSymbol(SkockoService.Symbol.STAR));
    }

    private void startNewRound() {
        skockoService = new SkockoService();
        isOpponentTurn = false;
        clearCurrentAttempt();
        clearBoard();
        navigator.setPhaseText(getString(R.string.round_counter_format, currentRound) + " - Player " + (currentRound == 1 ? "1" : "2"));
        startRoundTimer(30, this::handleTimeout);
        palette.setVisibility(View.VISIBLE);
        btnSubmit.setEnabled(true);
    }

    private void clearCurrentAttempt() {
        for (int i = 0; i < 4; i++) currentAttemptSymbols[i] = null;
    }

    private void addSymbol(SkockoService.Symbol symbol) {
        for (int i = 0; i < 4; i++) {
            if (currentAttemptSymbols[i] == null) {
                currentAttemptSymbols[i] = symbol;
                updateCurrentRowUI();
                return;
            }
        }
    }

    private void updateCurrentRowUI() {
        int rowIndex = isOpponentTurn ? 6 : skockoService.getCurrentAttempt();
        View row = rows[rowIndex];
        for (int i = 0; i < 4; i++) {
            TextView slot = row.findViewById(getSlotId(i));
            if (currentAttemptSymbols[i] != null) {
                slot.setText(getSymbolEmoji(currentAttemptSymbols[i]));
                final int index = i;
                slot.setOnClickListener(v -> {
                    currentAttemptSymbols[index] = null;
                    updateCurrentRowUI();
                });
            } else {
                slot.setText("");
                slot.setOnClickListener(null);
            }
        }
    }

    private void checkAttempt() {
        List<SkockoService.Symbol> attempt = new ArrayList<>();
        for (SkockoService.Symbol s : currentAttemptSymbols) {
            if (s != null) attempt.add(s);
        }

        if (attempt.size() != 4) {
            Toast.makeText(requireContext(), "Select 4 symbols", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isOpponentTurn) {
            handleOpponentAttempt(attempt);
            return;
        }

        int rowIndexBeforeCheck = skockoService.getCurrentAttempt();
        SkockoService.Result result = skockoService.checkAttempt(attempt);
        updateIndicators(rowIndexBeforeCheck, result);
        clearCurrentAttempt();

        if (result.isWin) {
            handleWin();
        } else if (skockoService.isFinished()) {
            startOpponentChance();
        }
    }

    private void handleWin() {
        stopRoundTimer();
        int points = skockoService.getScore();
        awardPoints(currentRound == 1 ? 1 : 2, points);
        firestoreRepository.updateSkockoStatistics(skockoService.getCurrentAttempt(), points, true);
        revealSolution();
        Toast.makeText(requireContext(), getString(R.string.msg_correct, points), Toast.LENGTH_SHORT).show();
        finishRoundAfterDelay();
    }

    private void startOpponentChance() {
        stopRoundTimer();
        isOpponentTurn = true;
        clearCurrentAttempt();
        navigator.setPhaseText(getString(R.string.msg_opponent_chance) + " - Player " + (currentRound == 1 ? "2" : "1"));
        startRoundTimer(10, this::handleTimeout);
        Toast.makeText(requireContext(), "Opponent has 10 seconds!", Toast.LENGTH_SHORT).show();
    }

    private void handleOpponentAttempt(List<SkockoService.Symbol> attempt) {
        stopRoundTimer();
        SkockoService.Result result = skockoService.checkAttempt(attempt);
        updateIndicators(6, result);
        if (result.isWin) {
            awardPoints(currentRound == 1 ? 2 : 1, 10);
            Toast.makeText(requireContext(), getString(R.string.msg_opponent_success), Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(requireContext(), getString(R.string.msg_opponent_failed), Toast.LENGTH_SHORT).show();
        }
        revealSolution();
        finishRoundAfterDelay();
    }

    private void handleTimeout() {
        if (!isOpponentTurn) {
            startOpponentChance();
        } else {
            revealSolution();
            finishRoundAfterDelay();
        }
    }

    private void revealSolution() {
        View solutionRow = rows[7];
        List<SkockoService.Symbol> secret = skockoService.getSecretCombination();
        for (int i = 0; i < 4; i++) {
            TextView slot = solutionRow.findViewById(getSlotId(i));
            slot.setText(getSymbolEmoji(secret.get(i)));
            slot.setBackgroundResource(R.drawable.avatar_silver);
        }
        palette.setVisibility(View.GONE);
        btnSubmit.setEnabled(false);
    }

    private void finishRoundAfterDelay() {
        handler.postDelayed(() -> {
            if (currentRound == 1) {
                currentRound = 2;
                startNewRound();
            } else {
                navigator.goToNextRound();
            }
        }, 3000);
    }

    private void awardPoints(int player, int pts) {
        if (player == 1) playerOneScore += pts;
        else playerTwoScore += pts;
        navigator.setScores(playerOneScore, playerTwoScore);
    }

    private void updateIndicators(int rowIndex, SkockoService.Result result) {
        View row = rows[rowIndex];
        int red = result.red;
        int yellow = result.yellow;
        for (int i = 0; i < 4; i++) {
            ImageView indicator = row.findViewById(getIndicatorId(i));
            if (red > 0) {
                indicator.setColorFilter(Color.RED);
                red--;
            } else if (yellow > 0) {
                indicator.setColorFilter(Color.YELLOW);
                yellow--;
            }
        }
    }

    private void clearBoard() {
        for (View row : rows) {
            for (int i = 0; i < 4; i++) {
                TextView slot = row.findViewById(getSlotId(i));
                slot.setText("");
                slot.setBackgroundResource(R.drawable.bg_skocko_slot);
                slot.setOnClickListener(null);
                ImageView ind = row.findViewById(getIndicatorId(i));
                if (ind != null) ind.setColorFilter(null);
            }
        }
    }

    private int getSlotId(int index) {
        switch (index) {
            case 0: return R.id.slot1;
            case 1: return R.id.slot2;
            case 2: return R.id.slot3;
            case 3: return R.id.slot4;
            default: return -1;
        }
    }

    private int getIndicatorId(int index) {
        switch (index) {
            case 0: return R.id.indicator1;
            case 1: return R.id.indicator2;
            case 2: return R.id.indicator3;
            case 3: return R.id.indicator4;
            default: return -1;
        }
    }

    private String getSymbolEmoji(SkockoService.Symbol s) {
        switch (s) {
            case SKOCKO: return getString(R.string.skocko_symbol);
            case CLUB: return getString(R.string.club_symbol);
            case SPADE: return getString(R.string.spade_symbol);
            case HEART: return getString(R.string.heart_symbol);
            case DIAMOND: return getString(R.string.diamond_symbol);
            case STAR: return getString(R.string.star_symbol);
            default: return "";
        }
    }
}
