package com.techstore.product.adapter.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

class UserServiceJwtCompatibilityTest {
    private static final String SHARED_SECRET = "01234567890123456789012345678901";

    @Test
    @SuppressWarnings("unchecked")
    void acceptsUserServiceHs256RoleArrayAndMapsAdminAuthority() throws Exception {
        var key = new SecretKeySpec(SHARED_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        var encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
        Instant issuedAt = Instant.parse("2026-09-27T12:00:00Z");
        var claims = JwtClaimsSet.builder()
                .subject("1bd2e8b3-cfea-4c20-b620-f8bb8a65ea2c")
                .claim("roles", List.of("ADMIN"))
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plusSeconds(86400))
                .build();
        String token = encoder.encode(JwtEncoderParameters.from(
                org.springframework.security.oauth2.jwt.JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();

        SecurityConfig config = new SecurityConfig();
        JwtDecoder decoder = config.jwtDecoder(SHARED_SECRET);
        Jwt decoded = decoder.decode(token);
        Method converterMethod = SecurityConfig.class.getDeclaredMethod("jwtAuthenticationConverter");
        converterMethod.setAccessible(true);
        Converter<Jwt, AbstractAuthenticationToken> converter =
                (Converter<Jwt, AbstractAuthenticationToken>) converterMethod.invoke(config);
        AbstractAuthenticationToken authentication = converter.convert(decoded);

        assertEquals("1bd2e8b3-cfea-4c20-b620-f8bb8a65ea2c", decoded.getSubject());
        assertTrue(authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals));
    }
}