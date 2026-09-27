package com.techstore.user.domain;

import java.nio.charset.StandardCharsets;

public final class PasswordPolicy {
    private static final int MIN_CHARACTERS = 8;
    private static final int MAX_CHARACTERS = 72;
    private static final int MAX_UTF8_BYTES = 72;

    private PasswordPolicy() {}

    public static void validate(String password) {
        if (password == null) {
            throw new IllegalArgumentException("Password is required");
        }

        int characterCount = password.codePointCount(0, password.length());
        if (characterCount < MIN_CHARACTERS || characterCount > MAX_CHARACTERS) {
            throw new IllegalArgumentException("Password must contain 8 to 72 characters");
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > MAX_UTF8_BYTES) {
            throw new IllegalArgumentException("Password must not exceed 72 UTF-8 bytes");
        }
    }
}