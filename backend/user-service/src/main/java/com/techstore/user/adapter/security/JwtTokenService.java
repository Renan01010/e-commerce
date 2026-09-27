package com.techstore.user.adapter.security;

import com.techstore.user.application.port.out.AccessTokenIssuer;
import com.techstore.user.domain.User;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenService implements AccessTokenIssuer {
    private static final long TOKEN_LIFETIME_SECONDS = 24 * 60 * 60;

    private final JwtEncoder jwtEncoder;
    private final Clock clock;

    public JwtTokenService(JwtEncoder jwtEncoder, Clock clock) {
        this.jwtEncoder = jwtEncoder;
        this.clock = clock;
    }

    @Override
    public IssuedAccessToken issue(User user) {
        Instant issuedAt = clock.instant();
        Instant expiresAt = issuedAt.plusSeconds(TOKEN_LIFETIME_SECONDS);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(user.id().toString())
                .claim("roles", List.of(user.role().name()))
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new IssuedAccessToken(token, TOKEN_LIFETIME_SECONDS);
    }
}