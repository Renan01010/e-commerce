package com.techstore.cart.application.exception;

public class CartQuantityLimitExceededException extends RuntimeException {
    public CartQuantityLimitExceededException(int maxQuantity) {
        super("Maximum quantity per product is " + maxQuantity);
    }
}
