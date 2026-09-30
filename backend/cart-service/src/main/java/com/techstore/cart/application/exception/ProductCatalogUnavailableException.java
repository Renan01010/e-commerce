package com.techstore.cart.application.exception;

public class ProductCatalogUnavailableException extends RuntimeException {
    public ProductCatalogUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }

    public ProductCatalogUnavailableException(String message) {
        super(message);
    }
}