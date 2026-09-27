package com.techstore.gateway;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CartServiceRouteTest {
    private static final List<CapturedRequest> FORWARDED_REQUESTS = new CopyOnWriteArrayList<>();
    private static HttpServer cartServiceStub;

    @Value("${local.server.port}")
    private int gatewayPort;

    @DynamicPropertySource
    static void cartServiceUri(DynamicPropertyRegistry registry) {
        registry.add("CART_SERVICE_URL", CartServiceRouteTest::startCartServiceStub);
    }

    @AfterAll
    static void stopCartServiceStub() {
        if (cartServiceStub != null) cartServiceStub.stop(0);
    }

    @BeforeEach
    void clearForwardedRequests() {
        FORWARDED_REQUESTS.clear();
    }

    @Test
    void forwardsCartPathMethodBodyAuthorizationAndCorrelationIdUnchanged() throws Exception {
        String correlationId = "cart-route-integration-test";
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + gatewayPort + "/api/cart/items"))
                .header("Authorization", "Bearer integration-token")
                .header("X-Correlation-ID", correlationId)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"productId\":\"550e8400-e29b-41d4-a716-446655440000\",\"quantity\":2}"))
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertEquals(1, FORWARDED_REQUESTS.size());
        CapturedRequest forwarded = FORWARDED_REQUESTS.getFirst();
        assertEquals("POST", forwarded.method());
        assertEquals("/api/cart/items", forwarded.path());
        assertEquals("Bearer integration-token", forwarded.authorization());
        assertEquals(correlationId, forwarded.correlationId());
        assertEquals("{\"productId\":\"550e8400-e29b-41d4-a716-446655440000\",\"quantity\":2}", forwarded.body());
        assertEquals(correlationId, response.headers().firstValue("X-Correlation-ID").orElseThrow());
    }

    private static String startCartServiceStub() {
        if (cartServiceStub != null) return baseUrl(cartServiceStub);
        try {
            cartServiceStub = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            cartServiceStub.createContext("/", exchange -> {
                byte[] requestBody = exchange.getRequestBody().readAllBytes();
                FORWARDED_REQUESTS.add(new CapturedRequest(exchange.getRequestMethod(),
                        exchange.getRequestURI().getPath(), exchange.getRequestHeaders().getFirst("Authorization"),
                        exchange.getRequestHeaders().getFirst("X-Correlation-ID"),
                        new String(requestBody, java.nio.charset.StandardCharsets.UTF_8)));
                byte[] response = "{\"items\":[]}".getBytes(java.nio.charset.StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, response.length);
                try (var output = exchange.getResponseBody()) {
                    output.write(response);
                }
            });
            cartServiceStub.start();
            return baseUrl(cartServiceStub);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to start Cart Service route test stub", exception);
        }
    }

    private static String baseUrl(HttpServer server) {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    private record CapturedRequest(String method, String path, String authorization,
                                   String correlationId, String body) {}
}