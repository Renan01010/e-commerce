package com.techstore.user.adapter.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.techstore.user.adapter.config.OpenApiConfig;
import com.techstore.user.adapter.config.SecurityConfig;
import com.techstore.user.adapter.http.GlobalExceptionHandler;
import com.techstore.user.adapter.http.SecurityErrorResponseWriter;
import com.techstore.user.application.service.UserManagementService;
import com.techstore.user.domain.Role;
import com.techstore.user.domain.User;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(com.techstore.user.adapter.http.UserController.class)
@AutoConfigureMockMvc
@Import({SecurityConfig.class, GlobalExceptionHandler.class, SecurityErrorResponseWriter.class,
        OpenApiConfig.class})
@TestPropertySource(properties = "techstore.jwt.secret=01234567890123456789012345678901")
class JwtAuthorizationTest {
    private static final String SECRET = "01234567890123456789012345678901";
    private static final String OTHER_SECRET = "abcdefghijklmnopqrstuvwxyz123456";
    private static final UUID ADMIN_ID = UUID.fromString("1bd2e8b3-cfea-4c20-b620-f8bb8a65ea2c");

    @Autowired MockMvc mvc;
    @MockBean UserManagementService userManagementService;

    @BeforeEach
    void setup() {
        when(userManagementService.create(any(), any(), any())).thenAnswer(invocation ->
                User.create(UUID.randomUUID(), invocation.getArgument(0), "$2a$12$hash",
                        invocation.getArgument(2), Instant.now()));
    }

    @Test
    void validAdminJwtCanAccessAdministrativeEndpoint() throws Exception {
        mvc.perform(post("/users").header("Authorization", bearerToken("ADMIN", SECRET, 3600))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"new@example.com","password":"CustomerPassword123","role":"USER"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void userRoleGetsForbiddenAndMissingTokenGetsUnauthorized() throws Exception {
        String body = "{\"email\":\"new@example.com\",\"password\":\"CustomerPassword123\",\"role\":\"USER\"}";

        mvc.perform(post("/users").header("Authorization", bearerToken("USER", SECRET, 3600))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
        mvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void expiredAndWrongSignatureTokensAreUnauthorized() throws Exception {
        String body = "{\"email\":\"new@example.com\",\"password\":\"CustomerPassword123\",\"role\":\"USER\"}";

        mvc.perform(post("/users").header("Authorization", bearerToken("ADMIN", SECRET, -60))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/users").header("Authorization", bearerToken("ADMIN", OTHER_SECRET, 3600))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
    }

    private static String bearerToken(String role, String secret, long lifetimeSeconds) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(lifetimeSeconds);
        Instant issuedAt = lifetimeSeconds < 0 ? expiresAt.minusSeconds(60) : now;
        var key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        var encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
        var claims = JwtClaimsSet.builder().subject(ADMIN_ID.toString()).claim("roles", List.of(role))
                .issuedAt(issuedAt).expiresAt(expiresAt).build();
        String token = encoder.encode(JwtEncoderParameters.from(
                org.springframework.security.oauth2.jwt.JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
        return "Bearer " + token;
    }
}