package com.techstore.user.adapter.http;

import com.techstore.user.adapter.http.AuthModels.LoginRequest;
import com.techstore.user.adapter.http.AuthModels.LoginResponse;
import com.techstore.user.adapter.http.AuthModels.RegisterRequest;
import com.techstore.user.adapter.http.AuthModels.UserResponse;
import com.techstore.user.application.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registrar usuário público com role USER")
    public UserResponse register(@Valid @RequestBody RegisterRequest request) {
        return UserResponse.from(authService.register(request.email(), request.password()));
    }

    @PostMapping("/login")
    @Operation(summary = "Autenticar usuário e emitir JWT")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return LoginResponse.from(authService.login(request.email(), request.password()));
    }
}