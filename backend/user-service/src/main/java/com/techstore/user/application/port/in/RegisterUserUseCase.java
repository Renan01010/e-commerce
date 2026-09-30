package com.techstore.user.application.port.in;

import com.techstore.user.domain.User;

public interface RegisterUserUseCase {
    User register(String email, String rawPassword);
}