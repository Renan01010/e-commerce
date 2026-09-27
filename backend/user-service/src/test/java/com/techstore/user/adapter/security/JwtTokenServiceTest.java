package com.techstore.user.adapter.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.techstore.user.domain.Role;
import com.techstore.user.domain.User;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.jwt.JwsHeader;

class JwtTokenServiceTest {
    private static final Instant NOW = Instant.parse("2026-09-27T12:00:00Z");
    private static final String SECRET = "01234567890123456789012345678901";
    private static final UUID USER_ID = UUID.fromString("1bd2e8b3-cfea-4c20-b620-f8bb8a65ea2c");

    private JwtEncoder encoder;
    private JwtDecoder decoder;
    private JwtTokenService tokenService;

    @BeforeEach
    void setUp() {
        var key = new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
        decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        tokenService = new JwtTokenService(encoder, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void issuesHs256TokenWithIdentityRolesAndTwentyFourHourLifetime() {
        User user = User.create(USER_ID, "admin@example.com", "$2a$12$hash", Role.ADMIN, NOW);

        var issued = tokenService.issue(user);
        Jwt decoded = decoder.decode(issued.value());

        assertEquals(86400, issued.expiresIn());
        assertEquals("HS256", decoded.getHeaders().get("alg"));
        assertEquals(USER_ID.toString(), decoded.getSubject());
        assertEquals(List.of("ADMIN"), decoded.getClaimAsStringList("roles"));
        assertEquals(NOW, decoded.getIssuedAt());
        assertEquals(NOW.plusSeconds(86400), decoded.getExpiresAt());
    }

    @Test
    void decoderRejectsExpiredToken() {
        Instant issuedAt = NOW.minusSeconds(172800);
        JwtEncoderParameters parameters = JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(),
                JwtClaimsSet.builder().subject(USER_ID.toString()).claim("roles", List.of("USER"))
                        .issuedAt(issuedAt).expiresAt(NOW.minusSeconds(1)).build());
        String expired = encoder.encode(parameters).getTokenValue();

        assertThrows(JwtValidationException.class, () -> decoder.decode(expired));
    }

    @Test
    void decoderRejectsTamperedSignature() {
        User user = User.create(USER_ID, "admin@example.com", "$2a$12$hash", Role.ADMIN, NOW);
        String token = tokenService.issue(user).value();
        String[] parts = token.split("\\.");
        char replacement = parts[2].charAt(0) == 'a' ? 'b' : 'a';
        parts[2] = replacement + parts[2].substring(1);

        assertThrows(JwtException.class, () -> decoder.decode(String.join(".", parts)));
    }
}