package com.techstore.user.domain;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

public final class User {
    private static final int MAX_EMAIL_LENGTH = 254;
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final UUID id;
    private final String email;
    private final String passwordHash;
    private final Role role;
    private final boolean active;
    private final Instant createdAt;
    private final Instant updatedAt;

    private User(UUID id, String email, String passwordHash, Role role, boolean active,
                 Instant createdAt, Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "User ID is required");
        this.email = normalizeEmail(email);
        this.passwordHash = requireText(passwordHash, "Password hash is required");
        this.role = Objects.requireNonNull(role, "User role is required");
        this.active = active;
        this.createdAt = Objects.requireNonNull(createdAt, "Creation timestamp is required");
        this.updatedAt = Objects.requireNonNull(updatedAt, "Update timestamp is required");
        if (updatedAt.isBefore(createdAt)) {
            throw new IllegalArgumentException("Update timestamp cannot be before creation timestamp");
        }
    }

    public static User create(UUID id, String email, String passwordHash, Role role, Instant now) {
        Objects.requireNonNull(now, "Creation timestamp is required");
        return new User(id, email, passwordHash, role, true, now, now);
    }

    public static User restore(UUID id, String email, String passwordHash, Role role, boolean active,
                               Instant createdAt, Instant updatedAt) {
        return new User(id, email, passwordHash, role, active, createdAt, updatedAt);
    }

    public User updateRoleAndActive(Role role, boolean active, Instant now) {
        Objects.requireNonNull(now, "Update timestamp is required");
        return new User(id, email, passwordHash, role, active, createdAt, now);
    }

    private static String normalizeEmail(String email) {
        String normalized = requireText(email, "Email is required").trim().toLowerCase(Locale.ROOT);
        if (normalized.length() > MAX_EMAIL_LENGTH || !EMAIL_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Email format is invalid");
        }
        return normalized;
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    public UUID id() { return id; }
    public String email() { return email; }
    public Role role() { return role; }
    public boolean active() { return active; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }

    public String passwordHashForPersistence() { return passwordHash; }

    @Override
    public String toString() {
        return "User[id=" + id + ", email=" + email + ", passwordHash=[REDACTED], role=" + role
                + ", active=" + active + ", createdAt=" + createdAt + ", updatedAt=" + updatedAt + "]";
    }
}