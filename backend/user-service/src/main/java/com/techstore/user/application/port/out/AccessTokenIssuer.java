package com.techstore.user.application.port.out;

import com.techstore.user.domain.User;

public interface AccessTokenIssuer {
    IssuedAccessToken issue(User user);

    record IssuedAccessToken(String value, long expiresIn) {}
}