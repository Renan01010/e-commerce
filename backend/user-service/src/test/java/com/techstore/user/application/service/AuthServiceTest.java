package com.techstore.user.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.techstore.user.application.exception.DuplicateEmailException;
import com.techstore.user.application.exception.InvalidCredentialsException;
import com.techstore.user.application.port.out.AccessTokenIssuer;
import com.techstore.user.application.port.out.PasswordHasher;
import com.techstore.user.application.port.out.UserRepository;
import com.techstore.user.domain.Role;
import com.techstore.user.domain.User;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    private static final Instant NOW = Instant.parse("2026-09-27T12:00:00Z");
    private static final String PASSWORD = "SenhaForte123";
    private static final String PASSWORD_HASH = "$2a$12$encoded-password-hash";

    @Mock UserRepository users;
    @Mock PasswordHasher passwordHasher;
    @Mock AccessTokenIssuer tokenIssuer;

    private AuthService service;

    @BeforeEach
    void setUp() {
        service = new AuthService(users, passwordHasher, tokenIssuer,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void registerNormalizesEmailHashesPasswordAndAlwaysCreatesActiveUserRole() {
        when(users.findByEmail("alice@example.com")).thenReturn(Optional.empty());
        when(passwordHasher.hash(PASSWORD)).thenReturn(PASSWORD_HASH);
        when(users.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User registered = service.register("  Alice@Example.COM ", PASSWORD);

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(users).save(saved.capture());
        assertEquals("alice@example.com", saved.getValue().email());
        assertEquals(PASSWORD_HASH, saved.getValue().passwordHashForPersistence());
        assertEquals(Role.USER, saved.getValue().role());
        assertEquals(true, saved.getValue().active());
        assertEquals(registered.id(), saved.getValue().id());
    }

    @Test
    void registerRejectsDuplicateEmailBeforeHashingOrPersisting() {
        when(users.findByEmail("alice@example.com")).thenReturn(Optional.of(existingUser(true)));

        assertThrows(DuplicateEmailException.class,
                () -> service.register("Alice@Example.com", PASSWORD));

        verify(passwordHasher, never()).hash(any());
        verify(users, never()).save(any());
    }

    @Test
    void loginReturnsIssuedBearerTokenForActiveUserWithMatchingPassword() {
        User user = existingUser(true);
        when(users.findByEmail(user.email())).thenReturn(Optional.of(user));
        when(passwordHasher.matches(PASSWORD, PASSWORD_HASH)).thenReturn(true);
        when(tokenIssuer.issue(user)).thenReturn(new AccessTokenIssuer.IssuedAccessToken("jwt-value", 86400));

        var result = service.login(" ALICE@EXAMPLE.COM ", PASSWORD);

        assertEquals("jwt-value", result.accessToken());
        assertEquals("Bearer", result.tokenType());
        assertEquals(86400, result.expiresIn());
    }

    @Test
    void loginUsesSameGenericFailureForUnknownWrongPasswordAndInactiveAccount() {
        User active = existingUser(true);
        User inactive = existingUser(false);
        when(users.findByEmail("missing@example.com")).thenReturn(Optional.empty());
        when(users.findByEmail("alice@example.com")).thenReturn(Optional.of(active));
        when(users.findByEmail("inactive@example.com")).thenReturn(Optional.of(inactive));
        when(passwordHasher.matches("wrong", PASSWORD_HASH)).thenReturn(false);

        InvalidCredentialsException unknown = assertThrows(InvalidCredentialsException.class,
                () -> service.login("missing@example.com", PASSWORD));
        InvalidCredentialsException wrongPassword = assertThrows(InvalidCredentialsException.class,
                () -> service.login("alice@example.com", "wrong"));
        InvalidCredentialsException inactiveAccount = assertThrows(InvalidCredentialsException.class,
                () -> service.login("inactive@example.com", PASSWORD));

        assertEquals(unknown.getMessage(), wrongPassword.getMessage());
        assertEquals(unknown.getMessage(), inactiveAccount.getMessage());
    }

    private User existingUser(boolean active) {
        return User.restore(UUID.randomUUID(), active ? "alice@example.com" : "inactive@example.com",
                PASSWORD_HASH, Role.USER, active, NOW, NOW);
    }
}