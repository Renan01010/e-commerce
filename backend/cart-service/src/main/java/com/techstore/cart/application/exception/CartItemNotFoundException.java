package com.techstore.cart.application.exception;

import java.util.UUID;

public class CartItemNotFoundException extends RuntimeException {
    public CartItemNotFoundException(UUID productId) {
        super("Cart item not found: " + productId);
    }
}