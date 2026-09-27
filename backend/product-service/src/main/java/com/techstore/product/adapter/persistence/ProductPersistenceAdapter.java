package com.techstore.product.adapter.persistence;

import com.techstore.product.application.port.ProductRepository;
import com.techstore.product.domain.Product;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

@Repository
public class ProductPersistenceAdapter implements ProductRepository {
    private final ProductJpaRepository repository;

    public ProductPersistenceAdapter(ProductJpaRepository repository) { this.repository = repository; }

    @Override
    public ProductPage search(Search search) {
        var page = repository.search(search.query(), search.categoryId(), search.minPrice(), search.maxPrice(),
                search.inStock(), search.brand(), search.sortBy(), search.sortOrder(),
                PageRequest.of(search.page(), search.pageSize()));
        return new ProductPage(page.getContent().stream().map(ProductEntity::toDomain).toList(),
                page.getTotalElements(), page.getTotalPages(), page.getNumber(), page.getSize(), page.hasNext());
    }

    @Override
    public Optional<Product> findActiveById(UUID id) {
        return repository.findByIdAndActiveTrue(id).map(ProductEntity::toDomain);
    }

    @Override public boolean existsBySku(String sku) { return repository.existsBySku(sku); }
    @Override public boolean existsBySkuAndIdNot(String sku, UUID id) { return repository.existsBySkuAndIdNot(sku, id); }
    @Override public boolean existsByNameAndCategoryId(String name, UUID categoryId) { return repository.existsByNameAndCategoryId(name, categoryId); }
    @Override public boolean existsByNameAndCategoryIdAndIdNot(String name, UUID categoryId, UUID id) {
        return repository.existsByNameAndCategoryIdAndIdNot(name, categoryId, id);
    }
    @Override public boolean existsInCategory(UUID categoryId) { return repository.existsByCategoryId(categoryId); }
    @Override public Product save(Product product) { return repository.save(ProductEntity.from(product)).toDomain(); }
}