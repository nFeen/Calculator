package calculator;

public final class Calculator {

    private static final int MAX_NESTING_DEPTH = 256;

    private Calculator() {
        throw new AssertionError("Utility class");
    }

    public static int evaluate(String expression) {
        if (expression == null) {
            throw new IllegalArgumentException("Expression must not be null");
        }
        return new Parser(expression).parse();
    }

    private static final class Parser {

        private final String input;
        private int pos;
        private int depth;

        Parser(String input) {
            this.input = input;
        }

        int parse() {
            skipWhitespace();
            if (atEnd()) {
                throw error("Expression is empty");
            }
            int result = parseExpression();
            skipWhitespace();
            if (!atEnd()) {
                throw unexpected();
            }
            return result;
        }

        private int parseExpression() {
            int result = parseTerm();
            while (true) {
                skipWhitespace();
                if (match('+')) {
                    result = toInt((long) result + parseTerm(), "Integer overflow");
                } else if (match('-')) {
                    result = toInt((long) result - parseTerm(), "Integer overflow");
                } else {
                    return result;
                }
            }
        }

        private int parseTerm() {
            int result = parseUnary();
            while (true) {
                skipWhitespace();
                if (match('*')) {
                    result = toInt((long) result * parseUnary(), "Integer overflow");
                } else if (peekIs('/')) {
                    int operatorPos = pos;
                    pos++;
                    int divisor = parseUnary();
                    if (divisor == 0) {
                        throw new IllegalArgumentException(
                                "Division by zero at position " + (operatorPos + 1));
                    }
                    result = toInt((long) result / divisor, "Integer overflow");
                } else {
                    return result;
                }
            }
        }

        private int parseUnary() {
            enterNesting();
            try {
                skipWhitespace();
                if (match('-')) {
                    skipWhitespace();
                    if (!atEnd() && isDigit(input.charAt(pos))) {
                        return parseNumber(true);
                    }
                    return toInt(-(long) parseUnary(), "Integer overflow");
                }
                if (match('+')) {
                    return parseUnary();
                }
                return parsePrimary();
            } finally {
                depth--;
            }
        }

        private int parsePrimary() {
            skipWhitespace();
            if (atEnd()) {
                throw error("Unexpected end of expression, operand expected");
            }
            char c = input.charAt(pos);
            if (c == '(') {
                int openPos = pos;
                pos++;
                enterNesting();
                try {
                    int value = parseExpression();
                    skipWhitespace();
                    if (!match(')')) {
                        throw new IllegalArgumentException(
                                "Missing closing parenthesis for '(' at position " + (openPos + 1));
                    }
                    return value;
                } finally {
                    depth--;
                }
            }
            if (isDigit(c)) {
                return parseNumber(false);
            }
            throw unexpected();
        }

        private int parseNumber(boolean negative) {
            int start = pos;
            long value = 0;
            while (!atEnd() && isDigit(input.charAt(pos))) {
                value = value * 10 + (input.charAt(pos) - '0');
                if (value > (long) Integer.MAX_VALUE + 1) {
                    throw new IllegalArgumentException(
                            "Number is too large at position " + (start + 1));
                }
                pos++;
            }
            long signed = negative ? -value : value;
            if (signed > Integer.MAX_VALUE) {
                throw new IllegalArgumentException("Number is too large at position " + (start + 1));
            }
            return (int) signed;
        }

        private void enterNesting() {
            if (++depth > MAX_NESTING_DEPTH) {
                throw error("Expression is nested too deeply (limit is " + MAX_NESTING_DEPTH + ")");
            }
        }

        private boolean atEnd() {
            return pos >= input.length();
        }

        private boolean peekIs(char expected) {
            return !atEnd() && input.charAt(pos) == expected;
        }

        private boolean match(char expected) {
            if (peekIs(expected)) {
                pos++;
                return true;
            }
            return false;
        }

        private void skipWhitespace() {
            while (!atEnd() && Character.isWhitespace(input.charAt(pos))) {
                pos++;
            }
        }

        private static boolean isDigit(char c) {
            return c >= '0' && c <= '9';
        }

        private static int toInt(long value, String message) {
            if (value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) {
                throw new IllegalArgumentException(message);
            }
            return (int) value;
        }

        private IllegalArgumentException unexpected() {
            return new IllegalArgumentException(
                    "Unexpected character '" + input.charAt(pos) + "' at position " + (pos + 1));
        }

        private IllegalArgumentException error(String message) {
            return new IllegalArgumentException(message);
        }
    }
}