package com.techstore.user.adapter.http;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.techstore.user.application.exception.DuplicateEmailException;
import com.techstore.user.application.service.UserManagementService;
import com.techstore.user.adapter.config.OpenApiConfig;
import com.techstore.user.adapter.config.SecurityConfig;
import com.techstore.user.adapter.http.GlobalExceptionHandler;
import com.techstore.user.adapter.http.SecurityErrorResponseWriter;
import com.techstore.user.domain.Role;
import com.techstore.user.domain.User;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc
@Import({SecurityConfig.class, GlobalExceptionHandler.class, SecurityErrorResponseWriter.class,
        OpenApiConfig.class})
@TestPropertySource(properties = {
        "techstore.jwt.secret=mockmvc-test-secret-at-least-thirty-two-bytes",
        "spring.jackson.deserialization.fail-on-unknown-properties=true"
})
class UserControllerTest {
    @Autowired MockMvc mvc;
    @MockBean UserManagementService userManagementService;

    @Test
    void anonymousCannotCreateUsersAndRegularUserCannotManageUsers() throws Exception {
        String request = """
                {"email":"new@example.com","password":"InitialPassword123","role":"USER"}
                """;

        mvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/users").with(user("customer").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isForbidden());
        mvc.perform(put("/users/{id}", UUID.randomUUID()).with(user("customer").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"active\":false}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanCreateUserAndResponseDoesNotExposeHash() throws Exception {
        User user = User.create(UUID.fromString("1bd2e8b3-cfea-4c20-b620-f8bb8a65ea2c"),
                "new@example.com", "$2a$12$private-hash", Role.USER,
                Instant.parse("2026-09-27T12:00:00Z"));
        when(userManagementService.create("new@example.com", "InitialPassword123", Role.USER))
                .thenReturn(user);

        mvc.perform(post("/users").with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"new@example.com","password":"InitialPassword123","role":"USER"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void adminRequestRejectsInvalidRoleAndDuplicateEmail() throws Exception {
        mvc.perform(post("/users").with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"new@example.com","password":"InitialPassword123","role":"ROOT"}
                                """))
                .andExpect(status().isBadRequest());

        when(userManagementService.create(any(), any(), any())).thenThrow(new DuplicateEmailException());
        mvc.perform(post("/users").with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"new@example.com","password":"InitialPassword123","role":"USER"}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void adminUpdateAcceptsOnlyRoleAndActiveFields() throws Exception {
        UUID id = UUID.fromString("3d6d8a6e-3909-4fd4-ae1a-35fe1aafef6c");
        User updated = User.restore(id, "new@example.com", "$2a$12$private-hash", Role.USER,
                false, Instant.parse("2026-09-27T12:00:00Z"), Instant.parse("2026-09-27T13:00:00Z"));
        when(userManagementService.update(eq(id), any(UUID.class), eq(null), eq(false))).thenReturn(updated);

        mvc.perform(put("/users/{id}", id).with(user(id.toString()).roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"active\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void publicRegistrationCannotSetRoleAndUpdateRequiresAtLeastOneField() throws Exception {
        mvc.perform(put("/users/{id}", UUID.randomUUID()).with(user(UUID.randomUUID().toString()).roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }
}