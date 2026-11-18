package com.devsam.urbantrend.Util;

/**
 * Minimum rules for passwords an admin sets when creating a staff account. Staff sign in with a
 * password plus a one-time code, so the rules stay simple: long enough, and not a single
 * repeated word or all digits.
 */
public final class PasswordPolicy {

    public static final int MIN_LENGTH = 8;
    public static final String MESSAGE =
            "Password must be at least " + MIN_LENGTH + " characters and include a letter and a number";

    private PasswordPolicy() {
    }

    public static boolean isAcceptable(String password) {
        if (password == null || password.trim().length() < MIN_LENGTH) {
            return false;
        }
        boolean hasLetter = false;
        boolean hasDigit = false;
        for (char c : password.toCharArray()) {
            if (Character.isLetter(c)) {
                hasLetter = true;
            } else if (Character.isDigit(c)) {
                hasDigit = true;
            }
        }
        return hasLetter && hasDigit;
    }

    public static void require(String password) {
        if (!isAcceptable(password)) {
            throw new IllegalArgumentException(MESSAGE);
        }
    }
}
