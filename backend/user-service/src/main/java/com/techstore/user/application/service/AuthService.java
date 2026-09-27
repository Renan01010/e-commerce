package com.techstore.user.application.service;

import com.techstore.user.application.exception.DuplicateEmailException;
import com.techstore.user.application.exception.InvalidCredentialsException;
import com.techstore.user.application.port.in.LoginUseCase;
import com.techstore.user.application.port.in.RegisterUserUseCase;
import com.techstore.user.application.port.out.AccessTokenIssuer;
import com.techstore.user.application.port.out.PasswordHasher;
import com.techstore.user.application.port.out.UserRepository;
import com.techstore.user.domain.PasswordPolicy;
import com.techstore.user.domain.Role;
import com.techstore.user.domain.User;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

public class AuthService implements RegisterUserUseCase, LoginUseCase {
    private static final String INVALID_CREDENTIALS = "Invalid email or password";

    private final UserRepository users;
    private final PasswordHasher passwordHasher;
    private final AccessTokenIssuer tokenIssuer;
    private final Clock clock;

    public AuthService(UserRepository users, PasswordHasher passwordHasher,
                       AccessTokenIssuer tokenIssuer, Clock clock) {
        this.users = users;
        this.passwordHasher = passwordHasher;
        this.tokenIssuer = tokenIssuer;
        this.clock = clock;
    }

    @Override
    public User register(String email, String rawPassword) {
        PasswordPolicy.validate(rawPassword);
        String normalizedEmail = normalizeEmail(email);
        if (users.findByEmail(normalizedEmail).isPresent()) {
            throw new DuplicateEmailException();
        }

        Instant now = clock.instant();
        User user = User.create(UUID.randomUUID(), normalizedEmail,
                passwordHasher.hash(rawPassword), Role.USER, now);
        return users.save(user);
    }

    @Override
    public LoginResult login(String email, String rawPassword) {
        if (rawPassword == null) throw new InvalidCredentialsException();

        User user;
        try {
            user = users.findByEmail(normalizeEmail(email))
                    .filter(User::active)
                    .orElseThrow(InvalidCredentialsException::new);
        } catch (IllegalArgumentException exception) {
            throw new InvalidCredentialsException();
        }

        if (!passwordHasher.matches(rawPassword, user.passwordHashForPersistence())) {
            throw new InvalidCredentialsException();
        }

        AccessTokenIssuer.IssuedAccessToken token = tokenIssuer.issue(user);
        return new LoginResult(token.value(), "Bearer", token.expiresIn());
    }

    private static String normalizeEmail(String email) {
        if (email == null) throw new IllegalArgumentException("Email is required");
        return email.trim().toLowerCase(Locale.ROOT);
    }
}