package com.techstore.user.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.techstore.user.application.exception.DuplicateEmailException;
import com.techstore.user.application.exception.LastActiveAdminException;
import com.techstore.user.application.port.out.PasswordHasher;
import com.techstore.user.application.port.out.TransactionRunner;
import com.techstore.user.application.port.out.UserRepository;
import com.techstore.user.domain.Role;
import com.techstore.user.domain.User;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserManagementServiceTest {
    private static final Instant NOW = Instant.parse("2026-09-27T12:00:00Z");

    @Mock UserRepository users;
    @Mock PasswordHasher passwordHasher;
    private final TransactionRunner transactions = new TransactionRunner() {
        @Override
        public <T> T runInTransaction(Supplier<T> work) {
            return work.get();
        }
    };
    private UserManagementService service;

    @BeforeEach
    void setUp() {
        service = new UserManagementService(users, passwordHasher, transactions,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void bootstrapIsSkippedWhenBothCredentialsAreAbsent() {
        assertTrue(service.bootstrapInitialAdmin(null, null).isEmpty());
        verify(users, never()).save(any());
    }

    @Test
    void bootstrapRejectsPartialCredentials() {
        assertThrows(IllegalStateException.class,
                () -> service.bootstrapInitialAdmin("admin@example.com", null));
        assertThrows(IllegalStateException.class,
                () -> service.bootstrapInitialAdmin(null, "InitialPassword123"));
        verify(users, never()).save(any());
    }

    @Test
    void bootstrapDoesNotOverwriteAnExistingUser() {
        User existing = user("admin@example.com", Role.USER, true);
        when(users.findByEmail("admin@example.com")).thenReturn(Optional.of(existing));

        User result = service.bootstrapInitialAdmin(" Admin@Example.com ", "InitialPassword123").orElseThrow();

        assertEquals(existing, result);
        verify(passwordHasher, never()).hash(any());
        verify(users, never()).save(any());
    }

    @Test
    void createRejectsDuplicateEmailAndCreatesRequestedRole() {
        when(users.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(passwordHasher.hash("InitialPassword123")).thenReturn("bcrypt-hash");
        when(users.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User created = service.create(" New@Example.com ", "InitialPassword123", Role.ADMIN);

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(users).save(saved.capture());
        assertEquals(Role.ADMIN, saved.getValue().role());
        assertEquals("new@example.com", created.email());
        when(users.findByEmail("new@example.com")).thenReturn(Optional.of(created));
        assertThrows(DuplicateEmailException.class,
                () -> service.create("new@example.com", "InitialPassword123", Role.USER));
    }

    @Test
    void updateCannotDemoteOrDeactivateTheLastActiveAdmin() {
        User admin = user("admin@example.com", Role.ADMIN, true);
        when(users.findById(admin.id())).thenReturn(Optional.of(admin));
        when(users.findActiveByRoleForUpdate(Role.ADMIN)).thenReturn(java.util.List.of(admin));

        assertThrows(LastActiveAdminException.class,
            () -> service.update(admin.id(), UUID.randomUUID(), Role.USER, null));
        assertThrows(LastActiveAdminException.class,
            () -> service.update(admin.id(), UUID.randomUUID(), null, false));
        verify(users, never()).save(any());
    }

    private static User user(String email, Role role, boolean active) {
        return User.restore(UUID.randomUUID(), email, "bcrypt-hash", role, active, NOW, NOW);
    }
}