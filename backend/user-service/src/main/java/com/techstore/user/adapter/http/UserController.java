package com.techstore.user.adapter.http;

import com.techstore.user.adapter.http.UserModels.AdminCreateUserRequest;
import com.techstore.user.adapter.http.UserModels.UpdateUserRequest;
import com.techstore.user.adapter.http.UserModels.UserResponse;
import com.techstore.user.application.service.UserManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
@Tag(name = "Users")
public class UserController {
    private final UserManagementService users;

    public UserController(UserManagementService users) {
        this.users = users;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Criar usuário (ADMIN)")
    @SecurityRequirement(name = "BearerAuth")
    public UserResponse create(@Valid @RequestBody AdminCreateUserRequest request) {
        return UserResponse.from(users.create(request.email(), request.password(), request.role()));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar role ou estado ativo do usuário (ADMIN)")
    @SecurityRequirement(name = "BearerAuth")
    public UserResponse update(@PathVariable(name = "id") UUID id,
                               @Valid @RequestBody UpdateUserRequest request,
                               Authentication authentication) {
        if (!request.hasChanges()) {
            throw new IllegalArgumentException("At least one user field must be updated");
        }
        UUID actorId = UUID.fromString(authentication.getName());
        return UserResponse.from(users.update(id, actorId, request.role(), request.active()));
    }
}