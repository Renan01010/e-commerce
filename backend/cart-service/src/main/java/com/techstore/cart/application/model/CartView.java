package com.techstore.cart.application.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record CartView(List<Item> items) {
    public CartView {
        items = List.copyOf(items);
    }

    public record Item(UUID productId, int quantity, boolean available, ProductSummary product) {}

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
        return new CartView(updatedItems);
    }
}