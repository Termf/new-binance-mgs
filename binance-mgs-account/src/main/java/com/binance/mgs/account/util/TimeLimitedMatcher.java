package com.binance.mgs.account.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Allows to create timeoutable regular expressions.
 */
public class TimeLimitedMatcher {

    public static Matcher createMatcherWithTimeout(String stringToMatch, String regularExpression, long timeoutMillis,
                                                   int checkInterval) {
        Pattern pattern = Pattern.compile(regularExpression);
        return createMatcherWithTimeout(stringToMatch, pattern, timeoutMillis, checkInterval);
    }

    public static Matcher createMatcherWithTimeout(String stringToMatch, Pattern regularExpressionPattern,
                                                   long timeoutMillis, int checkInterval) {
        if (timeoutMillis < 0) {
            return regularExpressionPattern.matcher(stringToMatch);
        }
        CharSequence charSequence = new TimeoutRegexCharSequence(stringToMatch, timeoutMillis, stringToMatch,
                regularExpressionPattern.pattern(), checkInterval);
        return regularExpressionPattern.matcher(charSequence);
    }

    public static class RegExpTimeoutException extends RuntimeException {
        public RegExpTimeoutException(String message) {
            super(message);
        }
    }
    private static class TimeoutRegexCharSequence implements CharSequence {

        private final CharSequence inner;

        private final long timeoutMillis;

        private final long timeoutTime;

        private final String stringToMatch;

        private final String regularExpression;

        private int checkInterval;

        private int attemps;

        TimeoutRegexCharSequence(CharSequence inner, long timeoutMillis, String stringToMatch,
                                 String regularExpression, int checkInterval) {
            super();
            this.inner = inner;
            this.timeoutMillis = timeoutMillis;
            this.stringToMatch = stringToMatch;
            this.regularExpression = regularExpression;
            timeoutTime = System.currentTimeMillis() + timeoutMillis;
            this.checkInterval = checkInterval;
            this.attemps = 0;
        }

        public char charAt(int index) {
            if (this.attemps == this.checkInterval) {
                if (System.currentTimeMillis() > timeoutTime) {
                    throw new RegExpTimeoutException("Regular expression timeout after " + timeoutMillis
                            + "ms for string" + stringToMatch + " ]");
                }
                this.attemps = 0;
            } else {
                this.attemps++;
            }

            return inner.charAt(index);
        }

        public int length() {
            return inner.length();
        }

        public CharSequence subSequence(int start, int end) {
            return new TimeoutRegexCharSequence(inner.subSequence(start, end), timeoutMillis, stringToMatch,
                    regularExpression, checkInterval);
        }

        @Override
        public String toString() {
            return inner.toString();
        }
    }

}
