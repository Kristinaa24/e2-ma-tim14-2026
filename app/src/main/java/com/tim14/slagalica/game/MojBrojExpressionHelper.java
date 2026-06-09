package com.tim14.slagalica.game;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MojBrojExpressionHelper {

    public enum ErrorType {
        NONE,
        EMPTY,
        INVALID_CHARACTERS,
        INVALID_NUMBERS,
        INVALID_EXPRESSION
    }

    public static class ValidationResult {
        private final ErrorType errorType;
        private final Integer value;

        private ValidationResult(ErrorType errorType, Integer value) {
            this.errorType = errorType;
            this.value = value;
        }

        public static ValidationResult success(int value) {
            return new ValidationResult(ErrorType.NONE, value);
        }

        public static ValidationResult failure(ErrorType errorType) {
            return new ValidationResult(errorType, null);
        }

        public boolean isValid() {
            return errorType == ErrorType.NONE && value != null;
        }

        public ErrorType getErrorType() {
            return errorType;
        }

        public Integer getValue() {
            return value;
        }
    }

    public ValidationResult validateAndEvaluate(String expression, int[] offeredNumbers) {
        String cleanExpression = expression == null ? "" : expression.replace(" ", "");

        if (cleanExpression.isEmpty()) {
            return ValidationResult.failure(ErrorType.EMPTY);
        }

        if (!cleanExpression.matches("[0-9+\\-*/()]+")) {
            return ValidationResult.failure(ErrorType.INVALID_CHARACTERS);
        }

        if (!usesOnlyOfferedNumbers(cleanExpression, offeredNumbers)) {
            return ValidationResult.failure(ErrorType.INVALID_NUMBERS);
        }

        Integer value = evaluateExpression(cleanExpression);

        if (value == null) {
            return ValidationResult.failure(ErrorType.INVALID_EXPRESSION);
        }

        return ValidationResult.success(value);
    }

    private boolean usesOnlyOfferedNumbers(String expression, int[] offeredNumbers) {
        List<Integer> availableNumbers = new ArrayList<>();

        for (int number : offeredNumbers) {
            availableNumbers.add(number);
        }

        Matcher matcher = Pattern.compile("\\d+").matcher(expression);

        while (matcher.find()) {
            int usedNumber = Integer.parseInt(matcher.group());

            if (!availableNumbers.remove(Integer.valueOf(usedNumber))) {
                return false;
            }
        }

        return true;
    }

    private Integer evaluateExpression(String expression) {
        ArrayDeque<Integer> values = new ArrayDeque<>();
        ArrayDeque<Character> operators = new ArrayDeque<>();

        int index = 0;

        while (index < expression.length()) {
            char current = expression.charAt(index);

            if (Character.isDigit(current)) {
                int number = 0;

                while (index < expression.length() && Character.isDigit(expression.charAt(index))) {
                    number = number * 10 + (expression.charAt(index) - '0');
                    index++;
                }

                values.push(number);
                continue;
            }

            if (current == '(') {
                operators.push(current);
                index++;
                continue;
            }

            if (current == ')') {
                while (!operators.isEmpty() && operators.peek() != '(') {
                    if (!applyTopOperation(values, operators)) {
                        return null;
                    }
                }

                if (operators.isEmpty() || operators.pop() != '(') {
                    return null;
                }

                index++;
                continue;
            }

            if (!isOperator(current)) {
                return null;
            }

            while (!operators.isEmpty() && precedence(operators.peek()) >= precedence(current)) {
                if (!applyTopOperation(values, operators)) {
                    return null;
                }
            }

            operators.push(current);
            index++;
        }

        while (!operators.isEmpty()) {
            if (operators.peek() == '(' || !applyTopOperation(values, operators)) {
                return null;
            }
        }

        if (values.size() != 1) {
            return null;
        }

        return values.pop();
    }

    private boolean applyTopOperation(
            ArrayDeque<Integer> values,
            ArrayDeque<Character> operators
    ) {
        if (values.size() < 2 || operators.isEmpty()) {
            return false;
        }

        int right = values.pop();
        int left = values.pop();
        char operator = operators.pop();

        Integer result = applyOperation(left, right, operator);

        if (result == null) {
            return false;
        }

        values.push(result);
        return true;
    }

    private Integer applyOperation(int left, int right, char operator) {
        switch (operator) {
            case '+':
                return left + right;
            case '-':
                return left - right;
            case '*':
                return left * right;
            case '/':
                if (right == 0 || left % right != 0) {
                    return null;
                }
                return left / right;
            default:
                return null;
        }
    }

    private boolean isOperator(char value) {
        return value == '+' || value == '-' || value == '*' || value == '/';
    }

    private int precedence(char operator) {
        if (operator == '+' || operator == '-') {
            return 1;
        }

        if (operator == '*' || operator == '/') {
            return 2;
        }

        return 0;
    }
}
