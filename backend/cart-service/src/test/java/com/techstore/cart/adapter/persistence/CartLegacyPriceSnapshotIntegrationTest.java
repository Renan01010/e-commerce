package com.techstore.cart.adapter.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.techstore.cart.application.exception.ProductCatalogUnavailableException;
import com.techstore.cart.application.port.out.CartStorePort;
import com.techstore.cart.application.port.out.ProductCatalogPort;
import com.techstore.cart.application.port.out.ProductCatalogPort.ProductSummary;
import com.techstore.cart.application.service.InitializeLegacyPriceSnapshotsService;
import com.techstore.cart.domain.UnitPriceSnapshot;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.context.annotation.Import;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(CartPersistenceAdapter.class)
@Testcontainers(disabledWithoutDocker = true)
class CartLegacyPriceSnapshotIntegrationTest {
    private static final UUID OWNER_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440010");
    private static final UUID ACTIVE_PRODUCT_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    private static final UUID MISSING_PRODUCT_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    @Autowired private CartStorePort cartStore;
    @Autowired private JdbcTemplate jdbcTemplate;
    private ProductCatalogPort productCatalog;

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("techstore.jwt.secret", () -> "integration-test-secret-at-least-thirty-two-bytes");
    }

    @BeforeEach
    void insertLegacyRows() {
        jdbcTemplate.update("delete from cart_items");
        jdbcTemplate.update("insert into cart_items (owner_user_id, product_id, quantity, unit_price, price_snapshot_status) "
                        + "values (?, ?, ?, null, 'PENDING'), (?, ?, ?, null, 'PENDING')",
                OWNER_ID, ACTIVE_PRODUCT_ID, 2, OWNER_ID, MISSING_PRODUCT_ID, 1);
        productCatalog = org.mockito.Mockito.mock(ProductCatalogPort.class);
    }

    @Test
    void resolvesActiveRowsAndRetainsMissingRowsAsUnknownAcrossRetry() {
        when(productCatalog.findActiveById(ACTIVE_PRODUCT_ID)).thenReturn(Optional.of(
                new ProductSummary("Keyboard", new BigDecimal("49.90"), "Acme", null, 10)));
        when(productCatalog.findActiveById(MISSING_PRODUCT_ID)).thenReturn(Optional.empty());
        InitializeLegacyPriceSnapshotsService initializer = initializer();

        initializer.initialize();
        initializer.initialize();

        List<com.techstore.cart.domain.CartItem> lines = cartStore.findByOwner(OWNER_ID);
        assertEquals(2, lines.size());
        assertEquals(UnitPriceSnapshot.known(new BigDecimal("49.90")), lines.stream()
                .filter(line -> line.productId().equals(ACTIVE_PRODUCT_ID)).findFirst().orElseThrow().unitPriceSnapshot());
        assertEquals(UnitPriceSnapshot.unknown(), lines.stream()
                .filter(line -> line.productId().equals(MISSING_PRODUCT_ID)).findFirst().orElseThrow().unitPriceSnapshot());
        verify(productCatalog, times(1)).findActiveById(ACTIVE_PRODUCT_ID);
        verify(productCatalog, times(1)).findActiveById(MISSING_PRODUCT_ID);
        assertNull(jdbcTemplate.queryForObject(
                "select unit_price from cart_items where owner_user_id = ? and product_id = ?",
                BigDecimal.class, OWNER_ID, MISSING_PRODUCT_ID));
    }

    @Test
    void transientCatalogFailureLeavesRowPendingForRetry() {
        when(productCatalog.findActiveById(ACTIVE_PRODUCT_ID))
                .thenThrow(new ProductCatalogUnavailableException("catalog unavailable"));

        assertThrows(ProductCatalogUnavailableException.class, initializer()::initialize);

        assertEquals("PENDING", jdbcTemplate.queryForObject(
                "select price_snapshot_status from cart_items where owner_user_id = ? and product_id = ?",
                String.class, OWNER_ID, ACTIVE_PRODUCT_ID));
        assertNull(jdbcTemplate.queryForObject(
                "select unit_price from cart_items where owner_user_id = ? and product_id = ?",
                BigDecimal.class, OWNER_ID, ACTIVE_PRODUCT_ID));
    }

        @Test
        void removesUnknownLegacyLineWithoutRemovingKnownLines() {
                when(productCatalog.findActiveById(ACTIVE_PRODUCT_ID)).thenReturn(Optional.of(
                                new ProductSummary("Keyboard", new BigDecimal("49.90"), "Acme", null, 10)));
                when(productCatalog.findActiveById(MISSING_PRODUCT_ID)).thenReturn(Optional.empty());
                initializer().initialize();

                assertTrue(cartStore.remove(OWNER_ID, MISSING_PRODUCT_ID));
                List<com.techstore.cart.domain.CartItem> remaining = cartStore.findByOwner(OWNER_ID);

                assertEquals(1, remaining.size());
                assertEquals(ACTIVE_PRODUCT_ID, remaining.getFirst().productId());
                assertEquals(UnitPriceSnapshot.known(new BigDecimal("49.90")), remaining.getFirst().unitPriceSnapshot());
        }

    private InitializeLegacyPriceSnapshotsService initializer() {
        return new InitializeLegacyPriceSnapshotsService(cartStore, productCatalog);
    }
}
