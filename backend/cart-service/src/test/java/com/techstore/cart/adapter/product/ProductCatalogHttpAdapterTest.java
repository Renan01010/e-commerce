package com.techstore.cart.adapter.product;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.techstore.cart.application.exception.ProductCatalogUnavailableException;
import com.techstore.cart.application.port.out.ProductCatalogPort.ProductSummary;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.SocketTimeoutException;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class ProductCatalogHttpAdapterTest {
    private static final UUID PRODUCT_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    private static final String PRODUCT_URL = "http://product-service/api/products/" + PRODUCT_ID;

    private MockRestServiceServer server;
    private ProductCatalogHttpAdapter adapter;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://product-service");
        server = MockRestServiceServer.bindTo(builder).build();
        adapter = new ProductCatalogHttpAdapter(builder.build());
    }

    @Test
    void mapsActiveProductSummaryFromPrivateProductEndpoint() {
        server.expect(requestTo(PRODUCT_URL))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {"id":"550e8400-e29b-41d4-a716-446655440000","name":"Keyboard","price":49.90,
                         "brand":"Acme","quantity":0,"imageUrl":"https://example.test/keyboard.jpg","isActive":true}
                        """, MediaType.APPLICATION_JSON));

        ProductSummary product = adapter.findActiveById(PRODUCT_ID).orElseThrow();

        assertEquals("Keyboard", product.name());
        assertEquals(new BigDecimal("49.90"), product.price());
        assertEquals("Acme", product.brand());
        assertEquals("https://example.test/keyboard.jpg", product.imageUrl());
        server.verify();
    }

    @Test
    void returnsEmptyWhenProductIsMissingOrInactive() {
        server.expect(requestTo(PRODUCT_URL))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertTrue(adapter.findActiveById(PRODUCT_ID).isEmpty());
        server.verify();
    }

    @Test
    void mapsServerFailureToCatalogUnavailable() {
        server.expect(requestTo(PRODUCT_URL))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertThrows(ProductCatalogUnavailableException.class, () -> adapter.findActiveById(PRODUCT_ID));
        server.verify();
    }

    @Test
    void mapsReadTimeoutToCatalogUnavailable() throws IOException {
        server.expect(requestTo(PRODUCT_URL))
                .andRespond(withException(new SocketTimeoutException("read timed out")));

        assertThrows(ProductCatalogUnavailableException.class, () -> adapter.findActiveById(PRODUCT_ID));
        server.verify();
    }
}