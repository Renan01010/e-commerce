package com.techstore.cart.adapter.security;

import com.techstore.cart.application.exception.InvalidCartOwnerException;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class CartOwnerResolver {
    public UUID resolve(Authentication authentication) {
        if (!(authentication instanceof JwtAuthenticationToken jwtAuthentication)) {
            throw new InvalidCartOwnerException();
        }
        String subject = jwtAuthentication.getToken().getSubject();
        try {
            return UUID.fromString(subject);
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new InvalidCartOwnerException();
        }
    }
}