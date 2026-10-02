package com.techstore.cart.application.model;

import com.techstore.cart.domain.UnitPriceSnapshot;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record CartView(List<Item> items, int maxItemQuantity) {
    public CartView {
        items = List.copyOf(items);
        if (maxItemQuantity <= 0) throw new IllegalArgumentException("Maximum cart quantity must be positive");
    }

    public CartView(List<Item> items) {
        this(items, 99);
    }

    public boolean totalAvailable() {
        return items.stream().allMatch(Item::priceAvailable);
    }

    public BigDecimal total() {
        if (!totalAvailable()) return null;
        return items.stream().map(Item::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public record Item(UUID productId, int quantity, boolean available, ProductSummary product,
                       UnitPriceSnapshot priceSnapshot) {
        public Item {
            if (productId == null) throw new IllegalArgumentException("Product ID is required");
            if (quantity <= 0) throw new IllegalArgumentException("Quantity must be positive");
            if (priceSnapshot == null) throw new IllegalArgumentException("Price snapshot is required");
        }

        public Item(UUID productId, int quantity, boolean available, ProductSummary product) {
            this(productId, quantity, available, product,
                    product == null || product.price() == null
                            ? UnitPriceSnapshot.unknown() : UnitPriceSnapshot.known(product.price()));
        }

        public boolean priceAvailable() {
            return priceSnapshot.status() == UnitPriceSnapshot.Status.KNOWN;
        }

        public BigDecimal unitPriceSnapshot() {
            return priceSnapshot.amount();
        }

        public BigDecimal subtotal() {
            return priceAvailable()
                    ? priceSnapshot.amount().multiply(BigDecimal.valueOf(quantity)) : null;
        }
    }

    public record ProductSummary(String name, BigDecimal price, String brand, String imageUrl) {}

    public CartView withItem(Item updatedItem) {
        List<Item> updatedItems = new ArrayList<>(items.size() + 1);
        boolean replaced = false;
        for (Item item : items) {
            if (item.productId().equals(updatedItem.productId())) {
                updatedItems.add(updatedItem);
                replaced = true;
            } else {
                updatedItems.add(item);
            }
        }
        if (!replaced) updatedItems.add(updatedItem);
        return new CartView(updatedItems, maxItemQuantity);
    }
}