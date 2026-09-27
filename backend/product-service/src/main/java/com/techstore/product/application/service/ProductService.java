package com.techstore.product.application.service;

import com.techstore.product.application.port.CategoryRepository;
import com.techstore.product.application.port.ProductRepository;
import com.techstore.product.application.port.ProductRepository.ProductPage;
import com.techstore.product.application.port.ProductRepository.Search;
import com.techstore.product.domain.Product;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ProductService {
    private static final Logger log = LoggerFactory.getLogger(ProductService.class);
    private final ProductRepository products;
    private final CategoryRepository categories;
    private final Clock clock;

    public ProductService(ProductRepository products, CategoryRepository categories, Clock clock) {
        this.products = products;
        this.categories = categories;
        this.clock = clock;
    }

    public ProductPage search(String query, UUID categoryId, BigDecimal minPrice, BigDecimal maxPrice,
                              Boolean inStock, String brand, String sortBy, String sortOrder,
                              int page, int pageSize) {
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new IllegalArgumentException("minPrice must not exceed maxPrice");
        }
        String normalizedQuery = query == null || query.isBlank() ? null : query.trim();
        String normalizedBrand = brand == null || brand.isBlank() ? null : brand.trim();
        return products.search(new Search(normalizedQuery, categoryId, minPrice, maxPrice,
                inStock, normalizedBrand, sortBy, sortOrder, page, pageSize));
    }

    public Product get(UUID id) {
        return products.findActiveById(id).orElseThrow(() -> new NotFoundException("Product not found"));
    }

    @Transactional
    public Product create(String name, String description, BigDecimal price, BigDecimal cost,
                          String brand, String sku, UUID categoryId, int quantity,
                          String imageUrl, String actor) {
        if (!categories.existsActiveById(categoryId)) throw new NotFoundException("Category not found");
        if (products.existsBySku(sku)) throw new ConflictException("SKU already exists");
        if (products.existsByNameAndCategoryId(name, categoryId)) throw new ConflictException("Product name already exists in category");
        LocalDateTime now = now();
        Product created = products.save(new Product(UUID.randomUUID(), name, description, price, cost, brand,
            sku, categoryId, quantity, imageUrl, true, now, now, actor, actor));
        log.info("catalog_operation=product_created productId={} actor={}", created.id(), actor);
        return created;
    }

    @Transactional
    public Product update(UUID id, String name, String description, BigDecimal price, BigDecimal cost,
                          String brand, Integer quantity, String imageUrl, String actor) {
        Product current = get(id);
        String nextName = name == null ? current.name() : name;
        if (products.existsByNameAndCategoryIdAndIdNot(nextName, current.categoryId(), id)) {
            throw new ConflictException("Product name already exists in category");
        }
        Product updated = products.save(current.update(name, description, price, cost, brand,
            quantity == null ? -1 : quantity, imageUrl, actor, now()));
        log.info("catalog_operation=product_updated productId={} actor={}", updated.id(), actor);
        return updated;
    }

    @Transactional
    public void deactivate(UUID id, String actor) {
        Product current = get(id);
        products.save(current.deactivate(actor, now()));
        log.info("catalog_operation=product_deactivated productId={} actor={}", id, actor);
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}