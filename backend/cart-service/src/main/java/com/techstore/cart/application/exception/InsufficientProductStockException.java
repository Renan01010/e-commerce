package com.techstore.cart.application.exception;

public class InsufficientProductStockException extends RuntimeException {
    public InsufficientProductStockException(int availableStock) {
        super("Requested quantity exceeds available stock (" + availableStock + ")");
    }
}
