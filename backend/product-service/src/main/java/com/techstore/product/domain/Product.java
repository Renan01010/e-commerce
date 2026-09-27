package com.techstore.product.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record Product(
        UUID id,
        String name,
        String description,
        BigDecimal price,
        BigDecimal cost,
        String brand,
        String sku,
        UUID categoryId,
        int quantity,
        String imageUrl,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String createdBy,
        String updatedBy) {

    public Product {
        if (id == null || categoryId == null) throw new IllegalArgumentException("Product and category IDs are required");
        if (name == null || name.isBlank() || name.length() > 255) throw new IllegalArgumentException("Name must contain 1 to 255 characters");
        if (sku == null || sku.isBlank() || sku.length() > 50) throw new IllegalArgumentException("SKU must contain 1 to 50 characters");
        if (price == null || price.signum() < 0) throw new IllegalArgumentException("Price must be zero or greater");
        if (cost != null && cost.signum() < 0) throw new IllegalArgumentException("Cost must be zero or greater");
        if (quantity < 0) throw new IllegalArgumentException("Quantity must be zero or greater");
        if (description != null && description.length() > 5000) throw new IllegalArgumentException("Description must be at most 5000 characters");
        if (brand != null && brand.length() > 100) throw new IllegalArgumentException("Brand must be at most 100 characters");
        if (imageUrl != null && imageUrl.length() > 2000) throw new IllegalArgumentException("Image URL must be at most 2000 characters");
    }

    public Product update(String name, String description, BigDecimal price, BigDecimal cost,
                          String brand, int quantity, String imageUrl, String actor, LocalDateTime now) {
        return new Product(id, name == null ? this.name : name,
                description == null ? this.description : description,
                price == null ? this.price : price,
                cost == null ? this.cost : cost,
                brand == null ? this.brand : brand, sku, categoryId,
                quantity < 0 ? this.quantity : quantity,
                imageUrl == null ? this.imageUrl : imageUrl, active,
                createdAt, now, createdBy, actor);
    }

    public Product deactivate(String actor, LocalDateTime now) {
        return new Product(id, name, description, price, cost, brand, sku, categoryId,
                quantity, imageUrl, false, createdAt, now, createdBy, actor);
    }
}