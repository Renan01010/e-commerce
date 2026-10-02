package com.techstore.cart.domain;

public final class CartItemRules {
    private final int maxQuantity;

    public CartItemRules(int maxQuantity) {
        if (maxQuantity <= 0) throw new IllegalArgumentException("Maximum cart quantity must be positive");
        this.maxQuantity = maxQuantity;
    }

    public int maxQuantity() {
        return maxQuantity;
    }

    public boolean isQuantityAllowed(int quantity) {
        return quantity > 0 && quantity <= maxQuantity;
    }

    public boolean isResultingQuantityAllowed(long quantity) {
        return quantity > 0 && quantity <= maxQuantity;
    }

    public long resultingQuantity(int currentQuantity, int increment) {
        if (currentQuantity < 0 || increment < 0) {
            throw new IllegalArgumentException("Cart quantities cannot be negative");
        }
        return (long) currentQuantity + increment;
    }

    public boolean hasStockFor(long requestedQuantity, int availableStock) {
        return requestedQuantity > 0 && availableStock >= requestedQuantity;
    }
}
