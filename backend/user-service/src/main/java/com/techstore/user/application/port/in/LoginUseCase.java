package com.techstore.user.application.port.in;

public interface LoginUseCase {
    LoginResult login(String email, String rawPassword);

    record LoginResult(String accessToken, String tokenType, long expiresIn) {}
}