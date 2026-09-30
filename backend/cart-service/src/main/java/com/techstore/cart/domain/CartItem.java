package com.techstore.cart.domain;

import java.util.UUID;

public record CartItem(UUID productId, int quantity) {
    public CartItem {
        if (productId == null) throw new IllegalArgumentException("Product ID is required");
        if (quantity <= 0) throw new IllegalArgumentException("Quantity must be positive");
    }
}