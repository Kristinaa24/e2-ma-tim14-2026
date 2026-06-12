package com.tim14.slagalica.service;

import com.tim14.slagalica.model.KoZnaZnaQuestion;

import java.util.ArrayList;
import java.util.List;

public class KoZnaZnaService {

    private static final int REQUIRED_QUESTIONS = 5;
    private static final int CORRECT_POINTS = 10;
    private static final int WRONG_POINTS = -5;

    public enum AnswerType {
        NO_OP,
        CORRECT,
        WRONG,
        PLAYER_TWO_FIRST,
        TIMEOUT,
        GAME_FINISHED
    }

    public static final class AnswerResult {
        private final AnswerType type;
        private final int awardedPlayer;
        private final int awardedPoints;
        private final boolean gameFinished;

        public AnswerResult(
                AnswerType type,
                int awardedPlayer,
                int awardedPoints,
                boolean gameFinished
        ) {
            this.type = type;
            this.awardedPlayer = awardedPlayer;
            this.awardedPoints = awardedPoints;
            this.gameFinished = gameFinished;
        }

        public AnswerType getType() {
            return type;
        }

        public int getAwardedPlayer() {
            return awardedPlayer;
        }

        public int getAwardedPoints() {
            return awardedPoints;
        }

        public boolean isGameFinished() {
            return gameFinished;
        }
    }

    private List<KoZnaZnaQuestion> questions = new ArrayList<>();
    private int questionIndex;
    private int playerOneScore;
    private int playerTwoScore;
    private int correctAnswers;
    private int wrongAnswers;
    private int totalScore;
    private boolean questionAnswered;
    private boolean gameFinished;

    public boolean startGame(List<KoZnaZnaQuestion> questions, int playerOneScore, int playerTwoScore) {
        if (questions == null || questions.size() < REQUIRED_QUESTIONS) {
            return false;
        }

        this.questions = new ArrayList<>(questions);
        this.playerOneScore = playerOneScore;
        this.playerTwoScore = playerTwoScore;
        questionIndex = 0;
        correctAnswers = 0;
        wrongAnswers = 0;
        totalScore = 0;
        questionAnswered = false;
        gameFinished = false;
        return true;
    }

    public boolean startCurrentQuestion() {
        if (gameFinished || !hasCurrentQuestion()) {
            return false;
        }

        questionAnswered = false;
        return true;
    }

    public AnswerResult answerCurrentQuestion(int selectedIndex) {
        if (questionAnswered || gameFinished || !hasCurrentQuestion()) {
            return new AnswerResult(AnswerType.NO_OP, 0, 0, gameFinished);
        }

        questionAnswered = true;
        KoZnaZnaQuestion currentQuestion = getCurrentQuestion();

        if (selectedIndex == currentQuestion.correctIndex) {
            playerOneScore += CORRECT_POINTS;
            totalScore += CORRECT_POINTS;
            correctAnswers++;
            return new AnswerResult(AnswerType.CORRECT, 1, CORRECT_POINTS, false);
        }

        playerOneScore += WRONG_POINTS;
        totalScore += WRONG_POINTS;
        wrongAnswers++;
        return new AnswerResult(AnswerType.WRONG, 1, WRONG_POINTS, false);
    }

    public AnswerResult playerTwoAnswersFirst() {
        if (questionAnswered || gameFinished) {
            return new AnswerResult(AnswerType.NO_OP, 0, 0, gameFinished);
        }

        questionAnswered = true;
        playerTwoScore += CORRECT_POINTS;
        return new AnswerResult(AnswerType.PLAYER_TWO_FIRST, 2, CORRECT_POINTS, false);
    }

    public AnswerResult timeoutCurrentQuestion() {
        if (questionAnswered || gameFinished) {
            return new AnswerResult(AnswerType.NO_OP, 0, 0, gameFinished);
        }

        questionAnswered = true;
        return new AnswerResult(AnswerType.TIMEOUT, 0, 0, false);
    }

    public boolean goToNextQuestion() {
        if (gameFinished) {
            return false;
        }

        if (questionIndex < REQUIRED_QUESTIONS - 1) {
            questionIndex++;
            questionAnswered = false;
            return true;
        }

        finishGame();
        return false;
    }

    public void finishGame() {
        gameFinished = true;
        questionAnswered = true;
    }

    public boolean isCurrentQuestionValid() {
        KoZnaZnaQuestion currentQuestion = getCurrentQuestion();
        return currentQuestion != null
                && currentQuestion.answers != null
                && currentQuestion.answers.size() >= 4;
    }

    public KoZnaZnaQuestion getCurrentQuestion() {
        if (!hasCurrentQuestion()) {
            return null;
        }

        return questions.get(questionIndex);
    }

    public int getQuestionNumber() {
        return questionIndex + 1;
    }

    public int getPlayerOneScore() {
        return playerOneScore;
    }

    public int getPlayerTwoScore() {
        return playerTwoScore;
    }

    public int getCorrectAnswers() {
        return correctAnswers;
    }

    public int getWrongAnswers() {
        return wrongAnswers;
    }

    public int getTotalScore() {
        return totalScore;
    }

    public boolean isQuestionAnswered() {
        return questionAnswered;
    }

    public boolean isGameFinished() {
        return gameFinished;
    }

    private boolean hasCurrentQuestion() {
        return questionIndex >= 0 && questionIndex < questions.size();
    }
}
