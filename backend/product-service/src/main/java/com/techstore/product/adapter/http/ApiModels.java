package com.techstore.product.adapter.http;

import com.techstore.product.application.port.ProductRepository.ProductPage;
import com.techstore.product.domain.Category;
import com.techstore.product.domain.Product;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public final class ApiModels {
    private ApiModels() {}

    public record CreateProductRequest(
            @NotBlank @Size(max = 255) String name,
            @Size(max = 5000) String description,
            @NotNull @DecimalMin("0.00") BigDecimal price,
            @DecimalMin("0.00") BigDecimal cost,
            @Size(max = 100) String brand,
            @NotBlank @Size(max = 50) String sku,
            @NotNull UUID categoryId,
            @NotNull @Min(0) Integer quantity,
            @Size(max = 2000) @Pattern(regexp = "https?://.+") String imageUrl) {}

    public record UpdateProductRequest(
            @Size(min = 1, max = 255) String name,
            @Size(max = 5000) String description,
            @DecimalMin("0.00") BigDecimal price,
            @DecimalMin("0.00") BigDecimal cost,
            @Size(max = 100) String brand,
            @Min(0) Integer quantity,
            @Size(max = 2000) @Pattern(regexp = "https?://.+") String imageUrl) {}

    public record CreateCategoryRequest(
            @NotBlank @Size(max = 100) String name,
            @Size(max = 500) String description,
            UUID parentCategoryId) {}

    public record UpdateCategoryRequest(
            @Size(min = 1, max = 100) String name,
            @Size(max = 500) String description,
            Integer displayOrder) {}

    public record ProductResponse(UUID id, String name, String description, BigDecimal price,
                                  BigDecimal cost, String brand, String sku, UUID categoryId,
                                  int quantity, String imageUrl, boolean isActive,
                                  LocalDateTime createdAt, LocalDateTime updatedAt) {
        public static ProductResponse from(Product product, boolean includeCost) {
            return new ProductResponse(product.id(), product.name(), product.description(), product.price(),
                    includeCost ? product.cost() : null, product.brand(), product.sku(), product.categoryId(),
                    product.quantity(), product.imageUrl(), product.active(), product.createdAt(), product.updatedAt());
        }
    }

    public record CategoryResponse(UUID id, String name, String slug, String description, UUID parentCategoryId,
                                   int displayOrder, boolean isActive,
                                   LocalDateTime createdAt, LocalDateTime updatedAt) {
        public static CategoryResponse from(Category category) {
            return new CategoryResponse(category.id(), category.name(), category.slug(), category.description(),
                    category.parentCategoryId(), category.displayOrder(), category.active(),
                    category.createdAt(), category.updatedAt());
        }
    }

    public record ProductPageResponse(List<ProductResponse> content, long totalElements, int totalPages,
                                      int currentPage, int pageSize, boolean hasMore) {
        public static ProductPageResponse from(ProductPage page, boolean includeCost) {
            return new ProductPageResponse(page.content().stream().map(p -> ProductResponse.from(p, includeCost)).toList(),
                    page.totalElements(), page.totalPages(), page.currentPage(), page.pageSize(), page.hasMore());
        }
    }

    public record ErrorResponse(int status, String message, String details,
                                String correlationId, LocalDateTime timestamp) {}
}