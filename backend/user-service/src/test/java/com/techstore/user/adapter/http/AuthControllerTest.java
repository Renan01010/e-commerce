package com.techstore.user.adapter.http;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.techstore.user.application.exception.InvalidCredentialsException;
import com.techstore.user.application.service.AuthService;
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

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class})
@TestPropertySource(properties = "spring.jackson.deserialization.fail-on-unknown-properties=true")
class AuthControllerTest {
    @Autowired MockMvc mvc;
    @MockBean AuthService authService;

    @Test
    void registerReturnsPublicUserWithoutPasswordOrHash() throws Exception {
        Instant createdAt = Instant.parse("2026-09-27T12:00:00Z");
        User user = User.create(UUID.fromString("1bd2e8b3-cfea-4c20-b620-f8bb8a65ea2c"),
                "user@example.com", "$2a$12$private-hash", Role.USER, createdAt);
        when(authService.register("User@Example.com", "SenhaForte123")).thenReturn(user);

        mvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":" User@Example.com ","password":"SenhaForte123"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(user.id().toString()))
                .andExpect(jsonPath("$.email").value("user@example.com"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void registerRejectsInvalidEmailAndPassword() throws Exception {
        mvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"not-an-email","password":"short"}
                                """))
                .andExpect(status().isBadRequest());
    }

        @Test
        void publicRegistrationRejectsRoleAssignment() throws Exception {
                mvc.perform(post("/auth/register")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content("""
                                                                {"email":"new@example.com","password":"InitialPassword123","role":"ADMIN"}
                                                                """))
                                .andExpect(status().isBadRequest());
        }

    @Test
    void loginReturnsBearerTokenAndTwentyFourHourLifetime() throws Exception {
        when(authService.login("user@example.com", "SenhaForte123"))
                .thenReturn(new com.techstore.user.application.port.in.LoginUseCase.LoginResult(
                        "signed-jwt", "Bearer", 86400));

        mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"user@example.com","password":"SenhaForte123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("signed-jwt"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(86400));
    }

    @Test
    void loginFailureUsesGenericUnauthorizedResponse() throws Exception {
        when(authService.login(any(), any())).thenThrow(new InvalidCredentialsException());

        mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"unknown@example.com","password":"SenhaForte123"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"))
                .andExpect(jsonPath("$.details").doesNotExist());
    }
}