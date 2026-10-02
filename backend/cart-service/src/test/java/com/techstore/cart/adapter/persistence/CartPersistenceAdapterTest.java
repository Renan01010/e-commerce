package com.techstore.cart.adapter.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.techstore.cart.application.port.out.CartStorePort;
import com.techstore.cart.domain.UnitPriceSnapshot;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(CartPersistenceAdapter.class)
@Testcontainers(disabledWithoutDocker = true)
class CartPersistenceAdapterTest {
    private static final UUID OWNER_A = UUID.fromString("550e8400-e29b-41d4-a716-446655440010");
    private static final UUID OWNER_B = UUID.fromString("550e8400-e29b-41d4-a716-446655440011");
    private static final UUID PRODUCT_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    private static final UnitPriceSnapshot PRICE = UnitPriceSnapshot.known(new java.math.BigDecimal("12.34"));

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    @Autowired
    private CartStorePort cartStore;

    @Autowired
    private CartItemJpaRepository cartItemJpaRepository;

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Test
    void upsertsOneLineAndAddsQuantitiesForTheSameOwner() {
        CartStorePort.AddResult inserted = cartStore.add(OWNER_A, PRODUCT_ID, 2, PRICE, 99, 10);
        CartStorePort.AddResult consolidated = cartStore.add(OWNER_A, PRODUCT_ID, 3, PRICE, 99, 10);

        assertTrue(inserted.created());
        assertFalse(consolidated.created());
        assertEquals(5, consolidated.item().quantity());
        assertEquals(1, cartStore.findByOwner(OWNER_A).size());
    }

    @Test
    void isolatesIdenticalProductAcrossOwners() {
        cartStore.add(OWNER_A, PRODUCT_ID, 2, PRICE, 99, 10);
        cartStore.add(OWNER_B, PRODUCT_ID, 4, PRICE, 99, 10);

        assertEquals(2, cartStore.findByOwner(OWNER_A).getFirst().quantity());
        assertEquals(4, cartStore.findByOwner(OWNER_B).getFirst().quantity());
    }

    @Test
    void databaseRejectsNonPositiveQuantity() {
        CartItemEntity invalidItem = new CartItemEntity(OWNER_A, PRODUCT_ID, 0);

        assertThrows(DataIntegrityViolationException.class,
                () -> cartItemJpaRepository.saveAndFlush(invalidItem));
    }

    @Test
    void rejectsConcurrentAddAboveConfiguredMaximumWithoutChangingLine() {
        cartStore.add(OWNER_A, PRODUCT_ID, 98, PRICE, 99, 100);

        CartStorePort.AddResult rejected = cartStore.add(OWNER_A, PRODUCT_ID, 2, PRICE, 99, 100);

        assertEquals(CartStorePort.WriteRejection.MAX_QUANTITY, rejected.rejection());
        assertEquals(98, cartStore.findByOwner(OWNER_A).getFirst().quantity());
    }

    @Test
    void rejectsAddAboveStockWithoutChangingLine() {
        cartStore.add(OWNER_A, PRODUCT_ID, 3, PRICE, 99, 5);

        CartStorePort.AddResult rejected = cartStore.add(OWNER_A, PRODUCT_ID, 3, PRICE, 99, 5);

        assertEquals(CartStorePort.WriteRejection.INSUFFICIENT_STOCK, rejected.rejection());
        assertEquals(3, cartStore.findByOwner(OWNER_A).getFirst().quantity());
    }
}