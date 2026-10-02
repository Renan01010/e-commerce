package com.techstore.cart.domain;

import java.util.UUID;

public record CartItem(UUID productId, int quantity, UnitPriceSnapshot unitPriceSnapshot) {
    public CartItem {
        if (productId == null) throw new IllegalArgumentException("Product ID is required");
        if (quantity <= 0) throw new IllegalArgumentException("Quantity must be positive");
        if (unitPriceSnapshot == null) throw new IllegalArgumentException("Price snapshot is required");
    }

    public CartItem(UUID productId, int quantity) {
        this(productId, quantity, UnitPriceSnapshot.unknown());
    }
}