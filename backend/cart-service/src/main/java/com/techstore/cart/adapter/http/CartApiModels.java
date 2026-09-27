package com.techstore.cart.adapter.http;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public final class CartApiModels {
    private CartApiModels() {}

    public record CartResponse(List<CartItemResponse> items) {}

    public record CartItemResponse(UUID productId, int quantity, boolean available, ProductSummaryResponse product) {}

    public record ProductSummaryResponse(String name, BigDecimal price, String brand, String imageUrl) {}

    public record ErrorResponse(int status, String message, String details, String correlationId,
                                LocalDateTime timestamp) {}
}