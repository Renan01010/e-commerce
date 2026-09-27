package com.techstore.product.adapter.persistence;

import com.techstore.product.domain.Product;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "products")
public class ProductEntity {
    @Id
    private UUID id;
    @Column(nullable = false, length = 255)
    private String name;
    @Column(columnDefinition = "text")
    private String description;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;
    @Column(precision = 12, scale = 2)
    private BigDecimal cost;
    @Column(length = 100)
    private String brand;
    @Column(nullable = false, unique = true, length = 50)
    private String sku;
    @Column(name = "category_id", nullable = false)
    private UUID categoryId;
    @Column(nullable = false)
    private int quantity;
    @Column(name = "image_url", length = 2000)
    private String imageUrl;
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

    protected ProductEntity() {}

    public static ProductEntity from(Product product) {
        ProductEntity entity = new ProductEntity();
        entity.id = product.id();
        entity.name = product.name();
        entity.description = product.description();
        entity.price = product.price();
        entity.cost = product.cost();
        entity.brand = product.brand();
        entity.sku = product.sku();
        entity.categoryId = product.categoryId();
        entity.quantity = product.quantity();
        entity.imageUrl = product.imageUrl();
        entity.active = product.active();
        entity.createdAt = product.createdAt();
        entity.updatedAt = product.updatedAt();
        entity.createdBy = product.createdBy();
        entity.updatedBy = product.updatedBy();
        return entity;
    }

    public Product toDomain() {
        return new Product(id, name, description, price, cost, brand, sku, categoryId,
                quantity, imageUrl, active, createdAt, updatedAt, createdBy, updatedBy);
    }
}