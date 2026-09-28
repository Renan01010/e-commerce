package com.techstore.product.adapter.persistence;

import com.techstore.product.domain.Category;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "categories")
public class CategoryEntity {
    @Id
    private UUID id;
    @Column(nullable = false, unique = true, length = 100)
    private String name;
    @Column(nullable = false, unique = true, length = 120)
    private String slug;
    @Column(length = 500)
    private String description;
    @Column(name = "parent_category_id")
    private UUID parentCategoryId;
    @Column(name = "display_order", nullable = false)
    private int displayOrder;
    @Column(name = "is_active", nullable = false)
    private boolean active;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    @Column(name = "created_by", nullable = false, length = 100)
    private String createdBy;
    @Column(name = "updated_by", nullable = false, length = 100)
    private String updatedBy;

    protected CategoryEntity() {}

    public static CategoryEntity from(Category category) {
        CategoryEntity entity = new CategoryEntity();
        entity.id = category.id();
        entity.name = category.name();
        entity.slug = category.slug();
        entity.description = category.description();
        entity.parentCategoryId = category.parentCategoryId();
        entity.displayOrder = category.displayOrder();
        entity.active = category.active();
        entity.createdAt = category.createdAt();
        entity.updatedAt = category.updatedAt();
        entity.createdBy = category.createdBy();
        entity.updatedBy = category.updatedBy();
        return entity;
    }

    public Category toDomain() {
        return new Category(id, name, slug, description, parentCategoryId, displayOrder,
                active, createdAt, updatedAt, createdBy, updatedBy);
    }
}