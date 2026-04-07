package com.pos.identity_service.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class PasswordPolicy {

    private PasswordPolicy() {
    }

    public static void validateOrThrow(String username, String password) {
        List<String> problems = validate(username, password);
        if (!problems.isEmpty()) {
            throw new IllegalArgumentException(String.join("; ", problems));
        }
    }

    public static List<String> validate(String username, String password) {
        List<String> problems = new ArrayList<>();

        if (password == null || password.isBlank()) {
            problems.add("password must be provided");
            return problems;
        }

        if (password.length() < 12) {
            problems.add("password must be at least 12 characters");
        }

        boolean hasUpper = false;
        boolean hasLower = false;
        boolean hasDigit = false;
        boolean hasSpecial = false;

        for (int i = 0; i < password.length(); i++) {
            char c = password.charAt(i);
            if (Character.isUpperCase(c)) {
                hasUpper = true;
            } else if (Character.isLowerCase(c)) {
                hasLower = true;
            } else if (Character.isDigit(c)) {
                hasDigit = true;
            } else {
                hasSpecial = true;
            }
        }

        if (!hasUpper) {
            problems.add("password must include an uppercase letter");
        }
        if (!hasLower) {
            problems.add("password must include a lowercase letter");
        }
        if (!hasDigit) {
            problems.add("password must include a digit");
        }
        if (!hasSpecial) {
            problems.add("password must include a symbol");
        }

        if (username != null && !username.isBlank()) {
            String u = username.toLowerCase(Locale.ROOT);
            String p = password.toLowerCase(Locale.ROOT);
            if (p.contains(u)) {
                problems.add("password must not contain the username");
            }
        }

        return problems;
    }
}

