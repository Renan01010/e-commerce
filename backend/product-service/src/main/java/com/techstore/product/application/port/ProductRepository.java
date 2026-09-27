package com.techstore.product.application.port;

import com.techstore.product.domain.Product;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository {
    record Search(String query, UUID categoryId, BigDecimal minPrice, BigDecimal maxPrice,
                  Boolean inStock, String brand, String sortBy, String sortOrder,
                  int page, int pageSize) {}

    record ProductPage(List<Product> content, long totalElements, int totalPages,
                       int currentPage, int pageSize, boolean hasMore) {}

    ProductPage search(Search search);
    Optional<Product> findActiveById(UUID id);
    boolean existsBySku(String sku);
    boolean existsBySkuAndIdNot(String sku, UUID id);
    boolean existsByNameAndCategoryId(String name, UUID categoryId);
    boolean existsByNameAndCategoryIdAndIdNot(String name, UUID categoryId, UUID id);
    boolean existsInCategory(UUID categoryId);
    Product save(Product product);
}