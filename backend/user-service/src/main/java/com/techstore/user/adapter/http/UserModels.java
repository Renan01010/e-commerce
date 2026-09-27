package com.techstore.user.adapter.http;

import com.techstore.user.domain.Role;
import com.techstore.user.domain.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public final class UserModels {
    private UserModels() {}

    public record AdminCreateUserRequest(
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(min = 8, max = 72) String password,
            @NotNull Role role) {
        public AdminCreateUserRequest {
            email = email == null ? null : email.trim();
        }
    }

    public record UpdateUserRequest(Role role, Boolean active) {
        public boolean hasChanges() {
            return role != null || active != null;
        }
    }

    public record UserResponse(UUID id, String email, Role role, boolean active,
                               Instant createdAt, Instant updatedAt) {
        public static UserResponse from(User user) {
            return new UserResponse(user.id(), user.email(), user.role(), user.active(),
                    user.createdAt(), user.updatedAt());
        }
    }
}