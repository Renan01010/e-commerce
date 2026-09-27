package com.techstore.cart.adapter.http;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.UUID;

public final class CartRequests {
    private CartRequests() {}

    public record AddCartItemRequest(@NotNull UUID productId, @NotNull @Positive Integer quantity) {}

    public record SetCartItemQuantityRequest(@NotNull @Positive Integer quantity) {}
}