package com.techstore.cart.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CartItemRulesTest {
    @Test
    void acceptsOnlyPositiveQuantitiesUpToConfiguredMaximum() {
        CartItemRules rules = new CartItemRules(99);

        assertFalse(rules.isQuantityAllowed(0));
        assertFalse(rules.isQuantityAllowed(-1));
        assertTrue(rules.isQuantityAllowed(1));
        assertTrue(rules.isQuantityAllowed(99));
        assertFalse(rules.isQuantityAllowed(100));
    }

    @Test
    void rejectsNonPositiveMaximum() {
        assertThrows(IllegalArgumentException.class, () -> new CartItemRules(0));
        assertThrows(IllegalArgumentException.class, () -> new CartItemRules(-1));
    }

    @Test
    void calculatesRepeatedAddWithoutIntegerOverflow() {
        CartItemRules rules = new CartItemRules(99);

        assertEquals(99L, rules.resultingQuantity(98, 1));
        assertEquals(100L, rules.resultingQuantity(99, 1));
        assertEquals(4_294_967_294L, rules.resultingQuantity(Integer.MAX_VALUE, Integer.MAX_VALUE));
    }

    @Test
    void checksCurrentStockAgainstRequestedResultingQuantity() {
        CartItemRules rules = new CartItemRules(99);

        assertTrue(rules.hasStockFor(5, 5));
        assertFalse(rules.hasStockFor(6, 5));
        assertFalse(rules.hasStockFor(1, 0));
    }
}
