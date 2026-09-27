package com.techstore.cart.application.exception;

public class InvalidCartOwnerException extends RuntimeException {
    public InvalidCartOwnerException() {
        super("Authenticated subject must be a UUID");
    }
}