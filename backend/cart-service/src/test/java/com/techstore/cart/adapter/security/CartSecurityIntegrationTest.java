package com.techstore.cart.adapter.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.techstore.cart.adapter.http.CartExceptionHandler;
import com.techstore.cart.adapter.http.CartAuthenticationErrorWriter;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import com.nimbusds.jose.jwk.source.ImmutableSecret;

@WebMvcTest(CartSecurityIntegrationTest.ProtectedEndpoint.class)
@Import({SecurityConfig.class, CartOwnerResolver.class, CartExceptionHandler.class, CartAuthenticationErrorWriter.class})
@TestPropertySource(properties = "techstore.jwt.secret=01234567890123456789012345678901")
class CartSecurityIntegrationTest {
    private static final String SECRET = "01234567890123456789012345678901";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void rejectsRequestWithoutToken() throws Exception {
        mockMvc.perform(get("/api/cart")).andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsInvalidAndExpiredTokens() throws Exception {
        mockMvc.perform(get("/api/cart").header(HttpHeaders.AUTHORIZATION, "Bearer invalid"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/cart").header(HttpHeaders.AUTHORIZATION,
                        "Bearer " + token("550e8400-e29b-41d4-a716-446655440000", Instant.now().minusSeconds(60))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsTokenWhoseSubjectIsNotUuid() throws Exception {
        mockMvc.perform(get("/api/cart").header(HttpHeaders.AUTHORIZATION,
                        "Bearer " + token("not-a-uuid", Instant.now().plusSeconds(300))))
                .andExpect(status().isUnauthorized());
    }

    private static String token(String subject, Instant expiration) {
        byte[] key = SECRET.getBytes(StandardCharsets.UTF_8);
        JwtEncoder encoder = new NimbusJwtEncoder(new ImmutableSecret<>(new SecretKeySpec(key, "HmacSHA256")));
        Instant issuedAt = Instant.now().minusSeconds(1);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(subject)
                .issuedAt(issuedAt)
                .expiresAt(expiration)
                .claim("roles", List.of("USER"))
                .build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }

    @RestController
    static class ProtectedEndpoint {
        private final CartOwnerResolver ownerResolver;

        ProtectedEndpoint(CartOwnerResolver ownerResolver) {
            this.ownerResolver = ownerResolver;
        }

        @GetMapping("/api/cart")
        String getCart(org.springframework.security.core.Authentication authentication) {
            return ownerResolver.resolve(authentication).toString();
        }
    }
}