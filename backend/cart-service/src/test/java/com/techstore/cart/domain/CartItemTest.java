package com.techstore.cart.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CartItemTest {
    private static final UUID PRODUCT_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

    @Test
    void acceptsPositiveQuantity() {
        CartItem item = new CartItem(PRODUCT_ID, 2);

        assertEquals(PRODUCT_ID, item.productId());
        assertEquals(2, item.quantity());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void rejectsNonPositiveQuantity(int quantity) {
        assertThrows(IllegalArgumentException.class, () -> new CartItem(PRODUCT_ID, quantity));
    }

    @Test
    void rejectsMissingProductId() {
        assertThrows(IllegalArgumentException.class, () -> new CartItem(null, 1));
    }

    @Test
    void acceptsKnownPriceSnapshot() {
        UnitPriceSnapshot snapshot = UnitPriceSnapshot.known(new BigDecimal("12.34"));
        CartItem item = new CartItem(PRODUCT_ID, 2, snapshot);

        assertEquals(snapshot, item.unitPriceSnapshot());
        assertEquals(new BigDecimal("12.34"), item.unitPriceSnapshot().amount());
    }

    @Test
    void acceptsUnknownPriceWithoutInventingAnAmount() {
        CartItem item = new CartItem(PRODUCT_ID, 1, UnitPriceSnapshot.unknown());

        assertEquals(UnitPriceSnapshot.Status.UNKNOWN, item.unitPriceSnapshot().status());
        assertEquals(null, item.unitPriceSnapshot().amount());
    }

    @Test
    void rejectsMissingPriceSnapshot() {
        assertThrows(IllegalArgumentException.class, () -> new CartItem(PRODUCT_ID, 1, null));
    }
}