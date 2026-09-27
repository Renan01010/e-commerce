package com.techstore.user.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UserTest {
    private static final Instant NOW = Instant.parse("2026-09-27T12:00:00Z");

    @Test
    void creationNormalizesEmailAndDefaultsToActive() {
        User user = User.create(UUID.randomUUID(), "  Alice@Example.COM ", "$2a$12$hash", Role.USER, NOW);

        assertEquals("alice@example.com", user.email());
        assertEquals(Role.USER, user.role());
        assertTrue(user.active());
        assertEquals(NOW, user.createdAt());
        assertEquals(NOW, user.updatedAt());
    }

    @Test
    void emailMustBePresentAndValid() {
        assertThrows(IllegalArgumentException.class,
                () -> User.create(UUID.randomUUID(), " ", "$2a$12$hash", Role.USER, NOW));
        assertThrows(IllegalArgumentException.class,
                () -> User.create(UUID.randomUUID(), "invalid-email", "$2a$12$hash", Role.USER, NOW));
    }

    @Test
    void restoredInactiveUserKeepsItsState() {
        User user = User.restore(UUID.randomUUID(), "alice@example.com", "$2a$12$hash", Role.ADMIN,
                false, NOW, NOW);

        assertFalse(user.active());
        assertEquals(Role.ADMIN, user.role());
    }

    @Test
    void userStringRepresentationDoesNotExposePasswordHash() {
        User user = User.create(UUID.randomUUID(), "alice@example.com", "sensitive-hash", Role.USER, NOW);

        assertTrue(user.toString().contains("[REDACTED]"));
        assertFalse(user.toString().contains("sensitive-hash"));
    }

    @Test
    void passwordPolicyAcceptsCharacterAndByteLimits() {
        PasswordPolicy.validate("12345678");
        PasswordPolicy.validate("áááááááá");
    }

    @Test
    void passwordPolicyRejectsInvalidCharacterAndUtf8ByteLengths() {
        assertThrows(IllegalArgumentException.class, () -> PasswordPolicy.validate("short7!"));
        assertThrows(IllegalArgumentException.class, () -> PasswordPolicy.validate("a".repeat(73)));
        assertThrows(IllegalArgumentException.class, () -> PasswordPolicy.validate("😀".repeat(19)));
        assertThrows(IllegalArgumentException.class, () -> PasswordPolicy.validate(null));
    }
}