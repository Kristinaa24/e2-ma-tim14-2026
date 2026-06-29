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

import com.tim14.slagalica.GameHostActivity;
import com.tim14.slagalica.R;
import com.tim14.slagalica.game.BaseGameFragment;
import com.tim14.slagalica.model.SharedMatchState;
import com.tim14.slagalica.model.SharedSkockoRound;
import com.tim14.slagalica.repository.FirestoreRepository;
import com.tim14.slagalica.service.SkockoService;

import java.util.ArrayList;
import java.util.List;

public class SkockoFragment extends BaseGameFragment {

    private FirestoreRepository firestoreRepository;
    private SkockoService skockoService;
    private int currentRound = 1;
    private int playerOneScore = 0;
    private int playerTwoScore = 0;
    private boolean isOpponentTurn;
    private boolean remoteMode;
    private int lastRemoteStatsRound = -1;
    private int lastScheduledRemoteRound = -1;
    private final SkockoService.Symbol[] currentAttemptSymbols = new SkockoService.Symbol[4];
    private final Handler handler = new Handler(Looper.getMainLooper());

    private View[] rows;
    private Button btnSubmit;
    private View palette;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.fragment_skocko, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        firestoreRepository = new FirestoreRepository(requireContext());
        remoteMode = ((GameHostActivity) requireActivity()).isRemoteMatchMode();

        if (!remoteMode) {
            playerOneScore = host().getPlayerOneScore();
            playerTwoScore = host().getPlayerTwoScore();
        }

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
        btnSubmit.setOnClickListener(v -> {
            if (remoteMode) {
                submitRemoteAttempt();
            } else {
                checkAttempt();
            }
        });

        if (remoteMode) {
            renderRemoteRound();
            return;
        }

        startNewRound();
    }

    private void setupPalette(View view) {
        view.findViewById(R.id.btnSymbolSkocko).setOnClickListener(
                v -> addSymbol(SkockoService.Symbol.SKOCKO)
        );
        view.findViewById(R.id.btnSymbolClub).setOnClickListener(
                v -> addSymbol(SkockoService.Symbol.CLUB)
        );
        view.findViewById(R.id.btnSymbolSpade).setOnClickListener(
                v -> addSymbol(SkockoService.Symbol.SPADE)
        );
        view.findViewById(R.id.btnSymbolHeart).setOnClickListener(
                v -> addSymbol(SkockoService.Symbol.HEART)
        );
        view.findViewById(R.id.btnSymbolDiamond).setOnClickListener(
                v -> addSymbol(SkockoService.Symbol.DIAMOND)
        );
        view.findViewById(R.id.btnSymbolStar).setOnClickListener(
                v -> addSymbol(SkockoService.Symbol.STAR)
        );
    }

    private void startNewRound() {
        skockoService = new SkockoService();
        isOpponentTurn = false;
        clearCurrentAttempt();
        clearBoard();
        host().setPhaseText(isChallengeMode()
                ? getString(R.string.challenge_skocko_round_phase_format, currentRound)
                : getString(R.string.round_counter_format, currentRound)
                + " - Player "
                + (currentRound == 1 ? "1" : "2"));
        startRoundTimer(30, this::handleTimeout);
        palette.setVisibility(View.VISIBLE);
        btnSubmit.setEnabled(true);
    }

    private void clearCurrentAttempt() {
        for (int index = 0; index < currentAttemptSymbols.length; index++) {
            currentAttemptSymbols[index] = null;
        }
    }

    private void addSymbol(SkockoService.Symbol symbol) {
        if (remoteMode && !isRemoteInputAllowed()) {
            return;
        }

        for (int index = 0; index < currentAttemptSymbols.length; index++) {
            if (currentAttemptSymbols[index] == null) {
                currentAttemptSymbols[index] = symbol;
                updateCurrentRowUI();
                return;
            }
        }
    }

    private void updateCurrentRowUI() {
        if (remoteMode) {
            updateRemoteDraftRow();
            return;
        }

        int rowIndex = isOpponentTurn ? 6 : skockoService.getCurrentAttempt();
        View row = rows[rowIndex];

        for (int index = 0; index < 4; index++) {
            TextView slot = row.findViewById(getSlotId(index));

            if (currentAttemptSymbols[index] != null) {
                slot.setText(getSymbolEmoji(currentAttemptSymbols[index]));
                int currentIndex = index;
                slot.setOnClickListener(v -> {
                    currentAttemptSymbols[currentIndex] = null;
                    updateCurrentRowUI();
                });
            } else {
                slot.setText("");
                slot.setOnClickListener(null);
            }
        }
    }

    private void checkAttempt() {
        List<SkockoService.Symbol> attempt = collectCurrentAttempt();
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
        updateIndicators(rowIndexBeforeCheck, result.red, result.yellow);
        clearCurrentAttempt();
        updateCurrentRowUI();

        if (result.isWin) {
            handleWin();
        } else if (skockoService.isFinished()) {
            if (isChallengeMode()) {
                revealSolution(skockoService.getSecretCombination());
                finishRoundAfterDelay();
            } else {
                startOpponentChance();
            }
        }
    }

    private void handleWin() {
        stopRoundTimer();
        int points = skockoService.getScore();
        awardPoints(currentRound == 1 ? 1 : 2, points);
        if (host().shouldPersistStatistics()) {
            firestoreRepository.updateSkockoStatistics(skockoService.getCurrentAttempt(), points, true);
        }
        revealSolution(skockoService.getSecretCombination());
        Toast.makeText(requireContext(), getString(R.string.msg_correct, points), Toast.LENGTH_SHORT).show();
        finishRoundAfterDelay();
    }

    private void startOpponentChance() {
        stopRoundTimer();
        isOpponentTurn = true;
        clearCurrentAttempt();
        host().setPhaseText(
                getString(R.string.msg_opponent_chance)
                        + " - Player "
                        + (currentRound == 1 ? "2" : "1")
        );
        startRoundTimer(10, this::handleTimeout);
        Toast.makeText(requireContext(), "Opponent has 10 seconds!", Toast.LENGTH_SHORT).show();
    }

    private void handleOpponentAttempt(List<SkockoService.Symbol> attempt) {
        stopRoundTimer();
        SkockoService.Result result = skockoService.checkAttempt(attempt);
        updateIndicators(6, result.red, result.yellow);
        if (result.isWin) {
            awardPoints(currentRound == 1 ? 2 : 1, 10);
            Toast.makeText(requireContext(), getString(R.string.msg_opponent_success), Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(requireContext(), getString(R.string.msg_opponent_failed), Toast.LENGTH_SHORT).show();
        }
        revealSolution(skockoService.getSecretCombination());
        finishRoundAfterDelay();
    }

    private void handleTimeout() {
        if (!isOpponentTurn && !isChallengeMode()) {
            startOpponentChance();
        } else {
            revealSolution(skockoService.getSecretCombination());
            finishRoundAfterDelay();
        }
    }

    private void revealSolution(List<SkockoService.Symbol> secret) {
        View solutionRow = rows[7];
        for (int index = 0; index < 4; index++) {
            TextView slot = solutionRow.findViewById(getSlotId(index));
            slot.setText(getSymbolEmoji(secret.get(index)));
            slot.setBackgroundResource(R.drawable.avatar_silver);
        }
        palette.setVisibility(View.GONE);
        btnSubmit.setEnabled(false);
    }

    private void finishRoundAfterDelay() {
        handler.postDelayed(() -> {
            if (currentRound == 1 && !isChallengeMode()) {
                currentRound = 2;
                startNewRound();
            } else {
                host().goToNextRound();
            }
        }, 3000);
    }

    private void awardPoints(int player, int points) {
        if (player == 1) {
            playerOneScore += points;
        } else {
            playerTwoScore += points;
        }

        host().setScores(playerOneScore, playerTwoScore);
    }

    private void updateIndicators(int rowIndex, int redCount, int yellowCount) {
        View row = rows[rowIndex];
        int red = redCount;
        int yellow = yellowCount;

        for (int index = 0; index < 4; index++) {
            ImageView indicator = row.findViewById(getIndicatorId(index));
            indicator.setColorFilter(null);
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
            for (int index = 0; index < 4; index++) {
                TextView slot = row.findViewById(getSlotId(index));
                slot.setText("");
                slot.setBackgroundResource(R.drawable.bg_skocko_slot);
                slot.setOnClickListener(null);
                ImageView indicator = row.findViewById(getIndicatorId(index));
                if (indicator != null) {
                    indicator.setColorFilter(null);
                }
            }
        }
    }

    private int getSlotId(int index) {
        switch (index) {
            case 0:
                return R.id.slot1;
            case 1:
                return R.id.slot2;
            case 2:
                return R.id.slot3;
            case 3:
                return R.id.slot4;
            default:
                return -1;
        }
    }

    private int getIndicatorId(int index) {
        switch (index) {
            case 0:
                return R.id.indicator1;
            case 1:
                return R.id.indicator2;
            case 2:
                return R.id.indicator3;
            case 3:
                return R.id.indicator4;
            default:
                return -1;
        }
    }

    private String getSymbolEmoji(SkockoService.Symbol symbol) {
        switch (symbol) {
            case SKOCKO:
                return getString(R.string.skocko_symbol);
            case CLUB:
                return getString(R.string.club_symbol);
            case SPADE:
                return getString(R.string.spade_symbol);
            case HEART:
                return getString(R.string.heart_symbol);
            case DIAMOND:
                return getString(R.string.diamond_symbol);
            case STAR:
                return getString(R.string.star_symbol);
            default:
                return "";
        }
    }

    private void renderRemoteRound() {
        stopRoundTimer();
        GameHostActivity activity = (GameHostActivity) requireActivity();
        SharedMatchState state = activity.getSharedMatchState();
        SharedSkockoRound roundState =
                state == null ? null : activity.getSharedSkockoRound(state.currentTurnIndex);

        if (state == null || roundState == null) {
            return;
        }

        if (maybeContinueAfterForfeit(activity, state, roundState)) {
            return;
        }

        clearBoard();
        renderRemoteStarterAttempts(roundState);
        renderRemoteBonusAttempt(roundState);

        if (SharedMatchState.PHASE_SKOCKO_DONE.equals(state.phase)) {
            host().setPhaseText(state.phaseMessage);
            host().setTimerValue(0);
            renderRemoteSolution(roundState);
            palette.setVisibility(View.GONE);
            btnSubmit.setEnabled(false);
            maybePersistRemoteSkockoStatistics(state, roundState);
            scheduleRemoteAdvanceIfCoordinator(state);
            return;
        }

        host().setTimerValue(getRemoteRemainingSeconds(state));
        host().setPhaseText(state.phaseMessage);

        boolean localTurn = activity.getLocalPlayerNumber() == state.activePlayer;
        palette.setVisibility(localTurn ? View.VISIBLE : View.INVISIBLE);
        btnSubmit.setEnabled(localTurn);
        updateRemoteDraftRow();

        if (getRemoteRemainingSeconds(state) > 0) {
            startRoundTimer(getRemoteRemainingSeconds(state), this::handleRemoteTimeout);
        }
    }

    private boolean maybeContinueAfterForfeit(
            GameHostActivity activity,
            SharedMatchState state,
            SharedSkockoRound roundState
    ) {
        if (!activity.hasOpponentForfeited()
                || state.activePlayer != state.forfeitedPlayer
                || (!SharedMatchState.PHASE_SKOCKO_PLAY.equals(state.phase)
                && !SharedMatchState.PHASE_SKOCKO_BONUS.equals(state.phase))) {
            return false;
        }

        SharedSkockoRound updatedRound = copyRound(roundState);
        List<SharedSkockoRound> allRounds = new ArrayList<>(state.skockoRounds);
        allRounds.set(state.currentTurnIndex, updatedRound);
        int continuingPlayer = activity.getRemainingRemotePlayerNumber();

        if (SharedMatchState.PHASE_SKOCKO_PLAY.equals(state.phase)) {
            activity.updateSharedMatch(new java.util.HashMap<String, Object>() {{
                put("skockoRounds", allRounds);
                put("phase", SharedMatchState.PHASE_SKOCKO_BONUS);
                put("activePlayer", continuingPlayer);
                put("phaseStartedAt", System.currentTimeMillis());
                put("phaseDurationSeconds", 10);
                put(
                        "phaseMessage",
                        getString(R.string.shared_match_skocko_bonus_phase_format, continuingPlayer)
                );
            }});
            return true;
        }

        updatedRound.finished = true;
        activity.updateSharedMatch(new java.util.HashMap<String, Object>() {{
            put("skockoRounds", allRounds);
            put("phase", SharedMatchState.PHASE_SKOCKO_DONE);
            put("activePlayer", 0);
            put("phaseStartedAt", System.currentTimeMillis());
            put("phaseDurationSeconds", 1);
            put("phaseMessage", getString(R.string.shared_match_skocko_unsolved_phase));
        }});
        return true;
    }

    private void renderRemoteStarterAttempts(SharedSkockoRound roundState) {
        for (int index = 0; index < roundState.starterAttempts.size() && index < 6; index++) {
            renderAttempt(rows[index], deserializeAttempt(roundState.starterAttempts.get(index)));
            updateIndicators(
                    index,
                    roundState.starterRedCounts.get(index),
                    roundState.starterYellowCounts.get(index)
            );
        }
    }

    private void renderRemoteBonusAttempt(SharedSkockoRound roundState) {
        if (roundState.bonusAttempt == null || roundState.bonusAttempt.isEmpty()) {
            return;
        }

        renderAttempt(rows[6], deserializeAttempt(roundState.bonusAttempt));
        updateIndicators(6, roundState.bonusRedCount, roundState.bonusYellowCount);
    }

    private void renderRemoteSolution(SharedSkockoRound roundState) {
        View solutionRow = rows[7];
        List<SkockoService.Symbol> secret = deserializeAttempt(roundState.secretCombination);

        for (int index = 0; index < 4 && index < secret.size(); index++) {
            TextView slot = solutionRow.findViewById(getSlotId(index));
            slot.setText(getSymbolEmoji(secret.get(index)));
            slot.setBackgroundResource(R.drawable.avatar_silver);
        }
    }

    private void renderAttempt(View row, List<SkockoService.Symbol> attempt) {
        for (int index = 0; index < 4; index++) {
            TextView slot = row.findViewById(getSlotId(index));
            if (index < attempt.size()) {
                slot.setText(getSymbolEmoji(attempt.get(index)));
            } else {
                slot.setText("");
            }
        }
    }

    private void updateRemoteDraftRow() {
        if (!remoteMode) {
            return;
        }

        GameHostActivity activity = (GameHostActivity) requireActivity();
        SharedMatchState state = activity.getSharedMatchState();
        if (state == null) {
            return;
        }

        int rowIndex = SharedMatchState.PHASE_SKOCKO_BONUS.equals(state.phase)
                ? 6
                : 0;

        if (SharedMatchState.PHASE_SKOCKO_PLAY.equals(state.phase)) {
            SharedSkockoRound roundState = activity.getSharedSkockoRound(state.currentTurnIndex);
            rowIndex = roundState == null ? 0 : Math.min(roundState.starterAttempts.size(), 5);
        }

        View row = rows[rowIndex];
        for (int index = 0; index < 4; index++) {
            TextView slot = row.findViewById(getSlotId(index));
            if (currentAttemptSymbols[index] != null) {
                slot.setText(getSymbolEmoji(currentAttemptSymbols[index]));
                int currentIndex = index;
                slot.setOnClickListener(v -> {
                    if (isRemoteInputAllowed()) {
                        currentAttemptSymbols[currentIndex] = null;
                        updateRemoteDraftRow();
                    }
                });
            } else {
                slot.setText("");
                slot.setOnClickListener(null);
            }
        }
    }

    private boolean isRemoteInputAllowed() {
        GameHostActivity activity = (GameHostActivity) requireActivity();
        SharedMatchState state = activity.getSharedMatchState();
        return state != null
                && (SharedMatchState.PHASE_SKOCKO_PLAY.equals(state.phase)
                || SharedMatchState.PHASE_SKOCKO_BONUS.equals(state.phase))
                && activity.getLocalPlayerNumber() == state.activePlayer;
    }

    private void submitRemoteAttempt() {
        GameHostActivity activity = (GameHostActivity) requireActivity();
        SharedMatchState state = activity.getSharedMatchState();
        SharedSkockoRound roundState =
                state == null ? null : activity.getSharedSkockoRound(state.currentTurnIndex);

        if (state == null || roundState == null || !isRemoteInputAllowed()) {
            return;
        }

        List<SkockoService.Symbol> attempt = collectCurrentAttempt();
        if (attempt.size() != 4) {
            Toast.makeText(requireContext(), "Select 4 symbols", Toast.LENGTH_SHORT).show();
            return;
        }

        SharedSkockoRound updatedRound = copyRound(roundState);
        SkockoEvaluation evaluation = evaluateAttempt(
                deserializeAttempt(updatedRound.secretCombination),
                attempt
        );
        List<SharedSkockoRound> allRounds = new ArrayList<>(state.skockoRounds);

        if (SharedMatchState.PHASE_SKOCKO_PLAY.equals(state.phase)) {
            updatedRound.starterAttempts.add(serializeAttempt(attempt));
            updatedRound.starterRedCounts.add(evaluation.redCount);
            updatedRound.starterYellowCounts.add(evaluation.yellowCount);

            if (evaluation.win) {
                int awardedPoints = calculateStarterPoints(updatedRound.starterAttempts.size() - 1);
                updatedRound.finished = true;
                updatedRound.solved = true;
                updatedRound.solvedByPlayer = updatedRound.starterPlayer;
                updatedRound.solvedAttemptIndex = updatedRound.starterAttempts.size() - 1;
                updatedRound.awardedPoints = awardedPoints;

                int playerOneScoreUpdate = state.playerOneScore;
                int playerTwoScoreUpdate = state.playerTwoScore;
                if (updatedRound.starterPlayer == 1) {
                    updatedRound.playerOneRoundScore = awardedPoints;
                    playerOneScoreUpdate += awardedPoints;
                } else {
                    updatedRound.playerTwoRoundScore = awardedPoints;
                    playerTwoScoreUpdate += awardedPoints;
                }

                allRounds.set(state.currentTurnIndex, updatedRound);
                clearCurrentAttempt();
                java.util.Map<String, Object> updates = new java.util.HashMap<>();
                updates.put("skockoRounds", allRounds);
                updates.put("playerOneScore", playerOneScoreUpdate);
                updates.put("playerTwoScore", playerTwoScoreUpdate);
                updates.put("phase", SharedMatchState.PHASE_SKOCKO_DONE);
                updates.put("activePlayer", 0);
                updates.put("phaseStartedAt", System.currentTimeMillis());
                updates.put("phaseDurationSeconds", 1);
                updates.put(
                        "phaseMessage",
                        getString(
                                R.string.shared_match_skocko_done_phase_format,
                                updatedRound.solvedByPlayer,
                                awardedPoints
                        )
                );
                activity.updateSharedMatch(updates);
                return;
            }

            if (updatedRound.starterAttempts.size() >= 6) {
                allRounds.set(state.currentTurnIndex, updatedRound);
                clearCurrentAttempt();
                activity.updateSharedMatch(new java.util.HashMap<String, Object>() {{
                    put("skockoRounds", allRounds);
                    put("phase", SharedMatchState.PHASE_SKOCKO_BONUS);
                    put("activePlayer", updatedRound.starterPlayer == 1 ? 2 : 1);
                    put("phaseStartedAt", System.currentTimeMillis());
                    put("phaseDurationSeconds", 10);
                    put(
                            "phaseMessage",
                            getString(
                                    R.string.shared_match_skocko_bonus_phase_format,
                                    updatedRound.starterPlayer == 1 ? 2 : 1
                            )
                    );
                }});
                return;
            }

            allRounds.set(state.currentTurnIndex, updatedRound);
            clearCurrentAttempt();
            activity.updateSharedMatch(new java.util.HashMap<String, Object>() {{
                put("skockoRounds", allRounds);
                put(
                        "phaseMessage",
                        getString(
                                R.string.shared_match_skocko_attempt_phase_format,
                                updatedRound.starterAttempts.size() + 1,
                                6
                        )
                );
            }});
            return;
        }

        updatedRound.bonusAttempt = serializeAttempt(attempt);
        updatedRound.bonusRedCount = evaluation.redCount;
        updatedRound.bonusYellowCount = evaluation.yellowCount;
        updatedRound.finished = true;
        int playerOneScoreUpdate = state.playerOneScore;
        int playerTwoScoreUpdate = state.playerTwoScore;

        if (evaluation.win) {
            updatedRound.solved = true;
            updatedRound.solvedByPlayer = state.activePlayer;
            updatedRound.solvedAttemptIndex = 5;
            updatedRound.awardedPoints = 10;
            if (state.activePlayer == 1) {
                updatedRound.playerOneRoundScore = 10;
                playerOneScoreUpdate += 10;
            } else {
                updatedRound.playerTwoRoundScore = 10;
                playerTwoScoreUpdate += 10;
            }
        }

        allRounds.set(state.currentTurnIndex, updatedRound);
        clearCurrentAttempt();
        java.util.Map<String, Object> updates = new java.util.HashMap<>();
        updates.put("skockoRounds", allRounds);
        updates.put("playerOneScore", playerOneScoreUpdate);
        updates.put("playerTwoScore", playerTwoScoreUpdate);
        updates.put("phase", SharedMatchState.PHASE_SKOCKO_DONE);
        updates.put("activePlayer", 0);
        updates.put("phaseStartedAt", System.currentTimeMillis());
        updates.put("phaseDurationSeconds", 1);
        updates.put(
                "phaseMessage",
                evaluation.win
                        ? getString(R.string.shared_match_skocko_done_phase_format, state.activePlayer, 10)
                        : getString(R.string.shared_match_skocko_unsolved_phase)
        );
        activity.updateSharedMatch(updates);
    }

    private void handleRemoteTimeout() {
        GameHostActivity activity = (GameHostActivity) requireActivity();
        SharedMatchState state = activity.getSharedMatchState();
        SharedSkockoRound roundState =
                state == null ? null : activity.getSharedSkockoRound(state.currentTurnIndex);

        if (state == null || roundState == null || !isRemoteInputAllowed()) {
            return;
        }

        List<SharedSkockoRound> allRounds = new ArrayList<>(state.skockoRounds);
        SharedSkockoRound updatedRound = copyRound(roundState);
        allRounds.set(state.currentTurnIndex, updatedRound);

        if (SharedMatchState.PHASE_SKOCKO_PLAY.equals(state.phase)) {
            activity.updateSharedMatch(new java.util.HashMap<String, Object>() {{
                put("skockoRounds", allRounds);
                put("phase", SharedMatchState.PHASE_SKOCKO_BONUS);
                put("activePlayer", updatedRound.starterPlayer == 1 ? 2 : 1);
                put("phaseStartedAt", System.currentTimeMillis());
                put("phaseDurationSeconds", 10);
                put(
                        "phaseMessage",
                        getString(
                                R.string.shared_match_skocko_bonus_phase_format,
                                updatedRound.starterPlayer == 1 ? 2 : 1
                        )
                );
            }});
            return;
        }

        updatedRound.finished = true;
        activity.updateSharedMatch(new java.util.HashMap<String, Object>() {{
            put("skockoRounds", allRounds);
            put("phase", SharedMatchState.PHASE_SKOCKO_DONE);
            put("activePlayer", 0);
            put("phaseStartedAt", System.currentTimeMillis());
            put("phaseDurationSeconds", 1);
            put("phaseMessage", getString(R.string.shared_match_skocko_unsolved_phase));
        }});
    }

    private void scheduleRemoteAdvanceIfCoordinator(SharedMatchState state) {
        GameHostActivity activity = (GameHostActivity) requireActivity();
        if (!activity.isRemoteProgressCoordinator() || lastScheduledRemoteRound == state.currentTurnIndex) {
            return;
        }

        lastScheduledRemoteRound = state.currentTurnIndex;

        handler.postDelayed(() -> {
            if (!isAdded()) {
                return;
            }

            SharedMatchState currentState = activity.getSharedMatchState();
            if (currentState == null
                    || currentState.currentTurnIndex != state.currentTurnIndex
                    || !SharedMatchState.PHASE_SKOCKO_DONE.equals(currentState.phase)) {
                return;
            }

            if (currentState.currentTurnIndex == 0) {
                activity.updateSharedMatch(new java.util.HashMap<String, Object>() {{
                    put("currentTurnIndex", 1);
                    put("phase", SharedMatchState.PHASE_SKOCKO_PLAY);
                    put("activePlayer", 2);
                    put("phaseStartedAt", System.currentTimeMillis());
                    put("phaseDurationSeconds", 30);
                    put("phaseMessage", getString(R.string.shared_match_skocko_round_two_phase));
                }});
            } else {
                host().goToNextRound();
            }
        }, 1200L);
    }

    private void maybePersistRemoteSkockoStatistics(SharedMatchState state, SharedSkockoRound roundState) {
        if (lastRemoteStatsRound == state.currentTurnIndex) {
            return;
        }

        int localPlayer = ((GameHostActivity) requireActivity()).getLocalPlayerNumber();
        int localRoundScore = localPlayer == 1
                ? roundState.playerOneRoundScore
                : roundState.playerTwoRoundScore;
        boolean localSolved = roundState.solved && roundState.solvedByPlayer == localPlayer;

        if (localRoundScore > 0 || localSolved) {
            if (host().shouldPersistStatistics()) {
                firestoreRepository.updateSkockoStatistics(
                        roundState.solvedAttemptIndex < 0 ? 5 : roundState.solvedAttemptIndex,
                        localRoundScore,
                        localSolved
                );
            }
        }

        lastRemoteStatsRound = state.currentTurnIndex;
    }

    private List<SkockoService.Symbol> collectCurrentAttempt() {
        List<SkockoService.Symbol> attempt = new ArrayList<>();
        for (SkockoService.Symbol symbol : currentAttemptSymbols) {
            if (symbol != null) {
                attempt.add(symbol);
            }
        }
        return attempt;
    }

    private int calculateStarterPoints(int attemptIndex) {
        if (attemptIndex < 2) {
            return 20;
        }
        if (attemptIndex < 4) {
            return 15;
        }
        return 10;
    }

    private SkockoEvaluation evaluateAttempt(
            List<SkockoService.Symbol> secretCombination,
            List<SkockoService.Symbol> attempt
    ) {
        int red = 0;
        int yellow = 0;
        List<SkockoService.Symbol> secretCopy = new ArrayList<>(secretCombination);
        List<SkockoService.Symbol> attemptCopy = new ArrayList<>(attempt);

        for (int index = 3; index >= 0; index--) {
            if (attemptCopy.get(index) == secretCopy.get(index)) {
                red++;
                attemptCopy.remove(index);
                secretCopy.remove(index);
            }
        }

        for (SkockoService.Symbol symbol : attemptCopy) {
            if (secretCopy.contains(symbol)) {
                yellow++;
                secretCopy.remove(symbol);
            }
        }

        return new SkockoEvaluation(red, yellow, red == 4);
    }

    private String serializeAttempt(List<SkockoService.Symbol> symbols) {
        List<String> names = new ArrayList<>();
        for (SkockoService.Symbol symbol : symbols) {
            names.add(symbol.name());
        }
        return android.text.TextUtils.join("|", names);
    }

    private List<SkockoService.Symbol> deserializeAttempt(String serialized) {
        List<SkockoService.Symbol> result = new ArrayList<>();
        if (serialized == null || serialized.trim().isEmpty()) {
            return result;
        }

        for (String part : serialized.split("\\|")) {
            result.add(SkockoService.Symbol.valueOf(part));
        }
        return result;
    }

    private List<SkockoService.Symbol> deserializeAttempt(List<String> serializedValues) {
        List<SkockoService.Symbol> result = new ArrayList<>();
        if (serializedValues == null) {
            return result;
        }

        for (String value : serializedValues) {
            result.add(SkockoService.Symbol.valueOf(value));
        }
        return result;
    }

    private SharedSkockoRound copyRound(SharedSkockoRound source) {
        SharedSkockoRound copy = new SharedSkockoRound();
        copy.secretCombination = new ArrayList<>(source.secretCombination);
        copy.starterAttempts = new ArrayList<>(source.starterAttempts);
        copy.starterRedCounts = new ArrayList<>(source.starterRedCounts);
        copy.starterYellowCounts = new ArrayList<>(source.starterYellowCounts);
        copy.bonusAttempt = source.bonusAttempt;
        copy.bonusRedCount = source.bonusRedCount;
        copy.bonusYellowCount = source.bonusYellowCount;
        copy.starterPlayer = source.starterPlayer;
        copy.finished = source.finished;
        copy.solved = source.solved;
        copy.solvedByPlayer = source.solvedByPlayer;
        copy.solvedAttemptIndex = source.solvedAttemptIndex;
        copy.awardedPoints = source.awardedPoints;
        copy.playerOneRoundScore = source.playerOneRoundScore;
        copy.playerTwoRoundScore = source.playerTwoRoundScore;
        return copy;
    }

    private int getRemoteRemainingSeconds(SharedMatchState state) {
        if (state == null || state.phaseDurationSeconds <= 0) {
            return 0;
        }

        long elapsedMs = System.currentTimeMillis() - state.phaseStartedAt;
        int remaining = state.phaseDurationSeconds - (int) Math.floor(elapsedMs / 1000d);
        return Math.max(0, remaining);
    }

    @Override
    public void onDestroyView() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroyView();
    }

    private static final class SkockoEvaluation {
        final int redCount;
        final int yellowCount;
        final boolean win;

        SkockoEvaluation(int redCount, int yellowCount, boolean win) {
            this.redCount = redCount;
            this.yellowCount = yellowCount;
            this.win = win;
        }
    }
}
