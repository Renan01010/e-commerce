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
class UserServiceRouteTest {
    private static final List<CapturedRequest> FORWARDED_REQUESTS = new CopyOnWriteArrayList<>();
    private static HttpServer userServiceStub;

    @Value("${local.server.port}")
    private int gatewayPort;

    @DynamicPropertySource
    static void userServiceUri(DynamicPropertyRegistry registry) {
        registry.add("USER_SERVICE_URL", UserServiceRouteTest::startUserServiceStub);
    }

    @AfterAll
    static void stopUserServiceStub() {
        if (userServiceStub != null) userServiceStub.stop(0);
    }

    @BeforeEach
    void clearForwardedRequests() {
        FORWARDED_REQUESTS.clear();
    }

    @Test
    void gatewayStripsOnlyApiPrefixAndPreservesMethodAndAuthorization() throws Exception {
        HttpResponse<String> loginResponse = post("/api/auth/login");
        HttpResponse<String> userResponse = post("/api/users");

        assertEquals(200, loginResponse.statusCode());
        assertEquals(200, userResponse.statusCode());
        assertEquals(2, FORWARDED_REQUESTS.size());
        assertEquals("POST", FORWARDED_REQUESTS.get(0).method());
        assertEquals("/auth/login", FORWARDED_REQUESTS.get(0).path());
        assertEquals("Bearer integration-token", FORWARDED_REQUESTS.get(0).authorization());
        assertEquals("POST", FORWARDED_REQUESTS.get(1).method());
        assertEquals("/users", FORWARDED_REQUESTS.get(1).path());
        assertEquals("Bearer integration-token", FORWARDED_REQUESTS.get(1).authorization());
    }

    private HttpResponse<String> post(String path) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + gatewayPort + path))
                .header("Authorization", "Bearer integration-token")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{}"))
                .build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }

    private static String startUserServiceStub() {
        if (userServiceStub != null) return baseUrl(userServiceStub);
        try {
            userServiceStub = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            userServiceStub.createContext("/", exchange -> {
                FORWARDED_REQUESTS.add(new CapturedRequest(exchange.getRequestMethod(),
                        exchange.getRequestURI().getPath(), exchange.getRequestHeaders().getFirst("Authorization")));
                byte[] response = "{}".getBytes(java.nio.charset.StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, response.length);
                try (var output = exchange.getResponseBody()) {
                    output.write(response);
                }
            });
            userServiceStub.start();
            return baseUrl(userServiceStub);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to start User Service route test stub", exception);
        }
    }

    private static String baseUrl(HttpServer server) {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    private record CapturedRequest(String method, String path, String authorization) {}
}