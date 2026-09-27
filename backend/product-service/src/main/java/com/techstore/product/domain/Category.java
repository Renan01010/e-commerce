package com.techstore.product.domain;

import java.time.LocalDateTime;
import java.util.UUID;

public record Category(
        UUID id,
        String name,
        String description,
        UUID parentCategoryId,
        int displayOrder,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String createdBy,
        String updatedBy) {

    public Category {
        if (id == null) throw new IllegalArgumentException("Category ID is required");
        if (name == null || name.isBlank() || name.length() > 100) throw new IllegalArgumentException("Name must contain 1 to 100 characters");
        if (description != null && description.length() > 500) throw new IllegalArgumentException("Description must be at most 500 characters");
        if (id.equals(parentCategoryId)) throw new IllegalArgumentException("A category cannot be its own parent");
    }

    public Category update(String name, String description, Integer displayOrder,
                           UUID parentCategoryId, String actor, LocalDateTime now) {
        UUID parent = parentCategoryId == null ? this.parentCategoryId : parentCategoryId;
        return new Category(id, name == null ? this.name : name,
                description == null ? this.description : description, parent,
                displayOrder == null ? this.displayOrder : displayOrder, active,
                createdAt, now, createdBy, actor);
    }

    public Category deactivate(String actor, LocalDateTime now) {
        return new Category(id, name, description, parentCategoryId, displayOrder,
                false, createdAt, now, createdBy, actor);
    }
}