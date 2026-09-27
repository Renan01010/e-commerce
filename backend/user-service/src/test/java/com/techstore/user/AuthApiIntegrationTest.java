package com.techstore.user;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.junit.jupiter.api.Assertions;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class AuthApiIntegrationTest {
    private static final String TEST_SECRET = "integration-test-secret-at-least-thirty-two-bytes";

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("techstore.jwt.secret", () -> TEST_SECRET);
    }

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper objectMapper;

    @BeforeEach
    void clearUsers() {
        jdbc.update("DELETE FROM users");
        Assertions.assertEquals(1, jdbc.queryForObject(
                "SELECT count(*) FROM flyway_schema_history WHERE version = '1'", Integer.class));
    }

    @Test
    void registrationPersistsOnlyBcryptHashAndDuplicateEmailConflicts() throws Exception {
        String password = "SenhaForte123";
        String response = mvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":" Alice@Example.com ","password":"%s"}
                                """.formatted(password)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("alice@example.com"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andReturn().getResponse().getContentAsString();

        UUID id = UUID.fromString(objectMapper.readTree(response).get("id").asText());
        String passwordHash = jdbc.queryForObject(
                "SELECT password_hash FROM users WHERE id = ?", String.class, id);
        Assertions.assertNotEquals(password, passwordHash);
        Assertions.assertTrue(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder(12)
                .matches(password, passwordHash));

        mvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"ALICE@example.com","password":"OutraSenha123"}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void loginIssuesTokenAndRejectsUnknownWrongAndInactiveAccountsGenerically() throws Exception {
        String password = "SenhaForte123";
        String userResponse = mvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"active@example.com","password":"%s"}
                                """.formatted(password)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        JsonNode tokenResponse = objectMapper.readTree(mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"active@example.com","password":"%s"}
                                """.formatted(password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(86400))
                .andReturn().getResponse().getContentAsString());
        org.junit.jupiter.api.Assertions.assertFalse(tokenResponse.get("accessToken").asText().isBlank());

        String unknownError = loginFailure("missing@example.com", password);
        String wrongPasswordError = loginFailure("active@example.com", "SenhaErrada123");
        UUID id = UUID.fromString(objectMapper.readTree(userResponse).get("id").asText());
        jdbc.update("UPDATE users SET active = FALSE WHERE id = ?", id);
        String inactiveError = loginFailure("active@example.com", password);
        Assertions.assertEquals(unknownError, wrongPasswordError);
        Assertions.assertEquals(unknownError, inactiveError);
    }

    private String loginFailure(String email, String password) throws Exception {
        String response = mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "email", email, "password", password))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"))
                .andExpect(jsonPath("$.details").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        JsonNode error = objectMapper.readTree(response);
        return error.get("status").asText() + ":" + error.get("message").asText();
    }
}