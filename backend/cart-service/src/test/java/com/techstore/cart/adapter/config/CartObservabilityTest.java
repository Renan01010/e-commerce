package com.techstore.cart.adapter.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.techstore.cart.adapter.http.CartCorrelationIdFilter;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers(disabledWithoutDocker = true)
class CartObservabilityTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("techstore.jwt.secret", () -> "integration-test-secret-at-least-thirty-two-bytes");
    }

    @Test
    void exposesHealthAndMetricsWithoutEchoingAuthorization() {
        String secretToken = "Bearer do-not-echo-this-token";
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, secretToken);
        headers.set(CartCorrelationIdFilter.HEADER, "cart-observability-test");

        ResponseEntity<String> health = restTemplate.exchange("http://localhost:" + port + "/actuator/health",
                HttpMethod.GET, new HttpEntity<>(headers), String.class);
        ResponseEntity<String> metrics = restTemplate.exchange("http://localhost:" + port + "/actuator/metrics",
                HttpMethod.GET, new HttpEntity<>(headers), String.class);

        assertTrue(health.getStatusCode().is2xxSuccessful());
        assertTrue(metrics.getStatusCode().is2xxSuccessful());
        assertEquals("cart-observability-test", health.getHeaders().getFirst(CartCorrelationIdFilter.HEADER));
        assertFalse(health.getBody().contains(secretToken));
        assertFalse(metrics.getBody().contains(secretToken));
    }

    @Test
    void createsCorrelationIdWhenRequestDoesNotSupplyOne() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/actuator/health", String.class);

        String correlationId = response.getHeaders().getFirst(CartCorrelationIdFilter.HEADER);
        assertNotNull(correlationId);
        assertEquals(UUID.fromString(correlationId).toString(), correlationId);
    }
}