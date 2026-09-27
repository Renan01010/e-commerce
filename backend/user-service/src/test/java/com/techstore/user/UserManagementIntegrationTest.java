package com.techstore.user;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.techstore.user.application.service.UserManagementService;
import com.techstore.user.domain.Role;
import com.techstore.user.domain.User;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class UserManagementIntegrationTest {
    private static final String TEST_SECRET = "integration-test-secret-at-least-thirty-two-bytes";

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("techstore.jwt.secret", () -> TEST_SECRET);
        registry.add("INITIAL_ADMIN_EMAIL", () -> "");
        registry.add("INITIAL_ADMIN_PASSWORD", () -> "");
    }

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserManagementService userManagementService;

    @BeforeEach
    void clearUsers() {
        jdbc.update("DELETE FROM users");
    }

    @Test
    void bootstrapIsIdempotentAndDoesNotOverwriteExistingUser() {
        User first = userManagementService.bootstrapInitialAdmin(" Admin@Example.com ", "FirstAdminPassword123")
                .orElseThrow();
        String originalHash = jdbc.queryForObject(
                "SELECT password_hash FROM users WHERE id = ?", String.class, first.id());

        User second = userManagementService.bootstrapInitialAdmin("admin@example.com", "DifferentPassword123")
                .orElseThrow();

        String retainedHash = jdbc.queryForObject(
                "SELECT password_hash FROM users WHERE id = ?", String.class, first.id());
        org.junit.jupiter.api.Assertions.assertEquals(first.id(), second.id());
        org.junit.jupiter.api.Assertions.assertEquals(Role.ADMIN, second.role());
        org.junit.jupiter.api.Assertions.assertEquals(originalHash, retainedHash);
        org.junit.jupiter.api.Assertions.assertEquals(1,
                jdbc.queryForObject("SELECT count(*) FROM users", Integer.class));
    }

    @Test
    void adminCanCreateAndUpdateUserButLastAdminCannotBeRemoved() throws Exception {
        User admin = userManagementService.bootstrapInitialAdmin("admin@example.com", "AdminPassword123")
                .orElseThrow();
        String createBody = objectMapper.writeValueAsString(Map.of(
                "email", "customer@example.com",
                "password", "CustomerPassword123",
                "role", "USER"));

        String response = mvc.perform(post("/users")
                        .with(user(admin.id().toString()).roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        String customerId = objectMapper.readTree(response).get("id").asText();

        mvc.perform(put("/users/{id}", customerId)
                        .with(user(admin.id().toString()).roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        mvc.perform(put("/users/{id}", admin.id())
                        .with(user(admin.id().toString()).roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"USER\"}"))
                .andExpect(status().isConflict());

        mvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/users")
                        .with(user("customer-id").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isForbidden());
    }

    @Test
    void duplicateEmailReturnsConflict() throws Exception {
        String request = """
                {"email":"same@example.com","password":"CustomerPassword123","role":"USER"}
                """;
        User admin = userManagementService.bootstrapInitialAdmin("admin@example.com", "AdminPassword123")
                .orElseThrow();

        mvc.perform(post("/users").with(user(admin.id().toString()).roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isCreated());
        mvc.perform(post("/users").with(user(admin.id().toString()).roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isConflict());
    }
}