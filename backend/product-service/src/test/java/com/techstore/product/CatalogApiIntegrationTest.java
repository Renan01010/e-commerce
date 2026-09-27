package com.techstore.product;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
class CatalogApiIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("techstore.jwt.secret", () -> "integration-test-secret-at-least-thirty-two-bytes");
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired JdbcTemplate jdbc;
    private String categoryId;

    @BeforeEach
    void cleanDatabase() throws Exception {
        jdbc.update("DELETE FROM products");
        jdbc.update("DELETE FROM categories");
        String category = mvc.perform(post("/api/categories")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Computers\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode result = objectMapper.readTree(category);
        categoryId = result.get("id").asText();
    }

    @Test
    void readsArePublicAndWritesRequireAdmin() throws Exception {
        mvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));
        mvc.perform(post("/api/categories").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Audio\"}"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.status").value(401));
        mvc.perform(post("/api/categories").with(user("customer").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Audio\"}"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void productCanBeCreatedSearchedUpdatedAndDeactivated() throws Exception {
        String body = """
                {"name":"Laptop Ultra","description":"Light laptop","price":1299.99,
                 "brand":"Acme","sku":"LAP-1","categoryId":"%s","quantity":4}
                """.formatted(categoryId);
        String created = mvc.perform(post("/api/products").with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String productId = objectMapper.readTree(created).get("id").asText();

        mvc.perform(get("/api/products").param("query", "laptop").param("minPrice", "1000")
                        .param("inStock", "true").param("categoryId", categoryId))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content", hasSize(1)));
        mvc.perform(get("/api/products/{id}", productId)).andExpect(status().isOk())
                .andExpect(jsonPath("$.cost").value(org.hamcrest.Matchers.nullValue()));
        mvc.perform(put("/api/products/{id}", productId).with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"price\":1399.99}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.price").value(1399.99));
        mvc.perform(delete("/api/products/{id}", productId).with(user("admin").roles("ADMIN")))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/products/{id}", productId)).andExpect(status().isNotFound());
        mvc.perform(delete("/api/categories/{id}", categoryId).with(user("admin").roles("ADMIN")))
                .andExpect(status().isConflict());
    }

    @Test
    void invalidProductDataReturnsBadRequest() throws Exception {
        String body = """
                {"name":"Invalid","price":-1,"sku":"BAD-1","categoryId":"%s","quantity":0}
                """.formatted(categoryId);
        mvc.perform(post("/api/products").with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
    }

        @Test
        void openApiDocumentsCatalogRoutes() throws Exception {
                mvc.perform(get("/v3/api-docs"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.paths['/api/products'].get").exists())
                                .andExpect(jsonPath("$.paths['/api/categories'].post").exists());
        }
}