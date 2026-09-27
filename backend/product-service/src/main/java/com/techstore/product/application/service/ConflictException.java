package com.techstore.product.application.service;

public class ConflictException extends RuntimeException {
    public ConflictException(String message) { super(message); }
}