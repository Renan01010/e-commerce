package com.techstore.user.adapter.http;

import com.techstore.user.application.port.in.LoginUseCase.LoginResult;
import com.techstore.user.domain.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public final class AuthModels {
    private AuthModels() {}

    public record RegisterRequest(
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(min = 8, max = 72) String password) {
        public RegisterRequest {
            email = email == null ? null : email.trim();
        }
    }

    public record LoginRequest(
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(min = 8, max = 72) String password) {
        public LoginRequest {
            email = email == null ? null : email.trim();
        }
    }

    public record UserResponse(UUID id, String email, String role, boolean active,
                               Instant createdAt, Instant updatedAt) {
        public static UserResponse from(User user) {
            return new UserResponse(user.id(), user.email(), user.role().name(), user.active(),
                    user.createdAt(), user.updatedAt());
        }
    }

    public record LoginResponse(String accessToken, String tokenType, long expiresIn) {
        public static LoginResponse from(LoginResult result) {
            return new LoginResponse(result.accessToken(), result.tokenType(), result.expiresIn());
        }
    }
}