package com.techstore.product.adapter.persistence;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface ProductJpaRepository extends JpaRepository<ProductEntity, UUID> {
    @Query(value = """
            SELECT p.* FROM products p
            WHERE p.is_active = true
              AND (:query IS NULL OR p.search_vector @@ plainto_tsquery('english', :query))
              AND (:categoryId IS NULL OR p.category_id = :categoryId)
              AND (:minPrice IS NULL OR p.price >= :minPrice)
              AND (:maxPrice IS NULL OR p.price <= :maxPrice)
              AND (:inStock IS NULL OR (:inStock = true AND p.quantity > 0) OR (:inStock = false AND p.quantity = 0))
              AND (:brand IS NULL OR lower(p.brand) = lower(:brand))
            ORDER BY
              CASE WHEN :sortBy = 'price' AND :sortOrder = 'asc' THEN p.price END ASC,
              CASE WHEN :sortBy = 'price' AND :sortOrder = 'desc' THEN p.price END DESC,
              CASE WHEN :sortBy = 'name' AND :sortOrder = 'asc' THEN p.name END ASC,
              CASE WHEN :sortBy = 'name' AND :sortOrder = 'desc' THEN p.name END DESC,
              CASE WHEN :sortBy = 'newest' AND :sortOrder = 'asc' THEN p.created_at END ASC,
              CASE WHEN :sortBy = 'newest' AND :sortOrder = 'desc' THEN p.created_at END DESC,
              CASE WHEN :sortBy = 'relevance' AND :query IS NOT NULL
                THEN ts_rank(ARRAY[0, 1, 2, 3]::real[], p.search_vector, plainto_tsquery('english', :query)) END DESC,
              p.created_at DESC, p.id ASC
            """,
            countQuery = """
            SELECT count(*) FROM products p
            WHERE p.is_active = true
              AND (:query IS NULL OR p.search_vector @@ plainto_tsquery('english', :query))
              AND (:categoryId IS NULL OR p.category_id = :categoryId)
              AND (:minPrice IS NULL OR p.price >= :minPrice)
              AND (:maxPrice IS NULL OR p.price <= :maxPrice)
              AND (:inStock IS NULL OR (:inStock = true AND p.quantity > 0) OR (:inStock = false AND p.quantity = 0))
              AND (:brand IS NULL OR lower(p.brand) = lower(:brand))
            """, nativeQuery = true)
    Page<ProductEntity> search(@Param("query") String query,
                               @Param("categoryId") UUID categoryId,
                               @Param("minPrice") BigDecimal minPrice,
                               @Param("maxPrice") BigDecimal maxPrice,
                               @Param("inStock") Boolean inStock,
                               @Param("brand") String brand,
                               @Param("sortBy") String sortBy,
                               @Param("sortOrder") String sortOrder,
                               Pageable pageable);

    Optional<ProductEntity> findByIdAndActiveTrue(UUID id);
    boolean existsBySku(String sku);
    boolean existsBySkuAndIdNot(String sku, UUID id);
    boolean existsByNameAndCategoryId(String name, UUID categoryId);
    boolean existsByNameAndCategoryIdAndIdNot(String name, UUID categoryId, UUID id);
    boolean existsByCategoryId(UUID categoryId);
}