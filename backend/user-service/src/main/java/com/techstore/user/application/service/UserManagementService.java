package com.techstore.user.application.service;

import com.techstore.user.application.exception.DuplicateEmailException;
import com.techstore.user.application.exception.LastActiveAdminException;
import com.techstore.user.application.exception.UserNotFoundException;
import com.techstore.user.application.port.in.ManageUsersUseCase;
import com.techstore.user.application.port.out.PasswordHasher;
import com.techstore.user.application.port.out.TransactionRunner;
import com.techstore.user.application.port.out.UserRepository;
import com.techstore.user.domain.PasswordPolicy;
import com.techstore.user.domain.Role;
import com.techstore.user.domain.User;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class UserManagementService implements ManageUsersUseCase {
    private final UserRepository users;
    private final PasswordHasher passwordHasher;
    private final TransactionRunner transactions;
    private final Clock clock;

    public UserManagementService(UserRepository users, PasswordHasher passwordHasher,
                                 TransactionRunner transactions, Clock clock) {
        this.users = users;
        this.passwordHasher = passwordHasher;
        this.transactions = transactions;
        this.clock = clock;
    }

    public Optional<User> bootstrapInitialAdmin(String email, String rawPassword) {
        boolean emailProvided = email != null && !email.isBlank();
        boolean passwordProvided = rawPassword != null && !rawPassword.isBlank();
        if (!emailProvided && !passwordProvided) return Optional.empty();
        if (emailProvided != passwordProvided) {
            throw new IllegalStateException("INITIAL_ADMIN_EMAIL and INITIAL_ADMIN_PASSWORD must be configured together");
        }

        PasswordPolicy.validate(rawPassword);
        String normalizedEmail = normalizeEmail(email);
        return transactions.runInTransaction(() -> users.findByEmail(normalizedEmail)
                .map(Optional::of)
                .orElseGet(() -> Optional.of(createUser(normalizedEmail, rawPassword, Role.ADMIN))));
    }

    @Override
    public User create(String email, String rawPassword, Role role) {
        Objects.requireNonNull(role, "User role is required");
        PasswordPolicy.validate(rawPassword);
        String normalizedEmail = normalizeEmail(email);
        return transactions.runInTransaction(() -> createUser(normalizedEmail, rawPassword, role));
    }

    @Override
    public User update(UUID userId, UUID actorId, Role requestedRole, Boolean requestedActive) {
        Objects.requireNonNull(userId, "User ID is required");
        Objects.requireNonNull(actorId, "Actor ID is required");
        if (requestedRole == null && requestedActive == null) {
            throw new IllegalArgumentException("At least one user field must be updated");
        }

        return transactions.runInTransaction(() -> {
            List<User> activeAdmins = users.findActiveByRoleForUpdate(Role.ADMIN);
            User current = users.findById(userId).orElseThrow(UserNotFoundException::new);
            Role nextRole = requestedRole == null ? current.role() : requestedRole;
            boolean nextActive = requestedActive == null ? current.active() : requestedActive;
            boolean removingAdmin = current.role() == Role.ADMIN
                    && (nextRole != Role.ADMIN || !nextActive);

            if (removingAdmin && actorId.equals(current.id())) {
                throw new LastActiveAdminException();
            }
            if (removingAdmin && current.active() && activeAdmins.size() <= 1) {
                throw new LastActiveAdminException();
            }

            User updated = current.updateRoleAndActive(nextRole, nextActive, clock.instant());
            return users.save(updated);
        });
    }

    private User createUser(String normalizedEmail, String rawPassword, Role role) {
        if (users.findByEmail(normalizedEmail).isPresent()) throw new DuplicateEmailException();
        Instant now = clock.instant();
        User user = User.create(UUID.randomUUID(), normalizedEmail, passwordHasher.hash(rawPassword), role, now);
        return users.save(user);
    }

    private static String normalizeEmail(String email) {
        if (email == null) throw new IllegalArgumentException("Email is required");
        return email.trim().toLowerCase(Locale.ROOT);
    }
}