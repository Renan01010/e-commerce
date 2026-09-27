package com.techstore.product.application.service;

import com.techstore.product.application.port.CategoryRepository;
import com.techstore.product.application.port.ProductRepository;
import com.techstore.product.domain.Category;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CategoryService {
    private static final Logger log = LoggerFactory.getLogger(CategoryService.class);
    private final CategoryRepository categories;
    private final ProductRepository products;
    private final Clock clock;

    public CategoryService(CategoryRepository categories, ProductRepository products, Clock clock) {
        this.categories = categories;
        this.products = products;
        this.clock = clock;
    }

    public List<Category> list() { return categories.findAllActive(); }

    public Category get(UUID id) {
        return categories.findActiveById(id).orElseThrow(() -> new NotFoundException("Category not found"));
    }

    @Transactional
    public Category create(String name, String description, UUID parentCategoryId, String actor) {
        if (categories.existsByName(name)) throw new ConflictException("Category name already exists");
        if (parentCategoryId != null && !categories.existsActiveById(parentCategoryId)) throw new NotFoundException("Parent category not found");
        LocalDateTime now = now();
        Category created = categories.save(new Category(UUID.randomUUID(), name, description, parentCategoryId,
            0, true, now, now, actor, actor));
        log.info("catalog_operation=category_created categoryId={} actor={}", created.id(), actor);
        return created;
    }

    @Transactional
    public Category update(UUID id, String name, String description, Integer displayOrder,
                           UUID parentCategoryId, String actor) {
        Category current = get(id);
        if (name != null && categories.existsByNameAndIdNot(name, id)) throw new ConflictException("Category name already exists");
        if (parentCategoryId != null && !categories.existsActiveById(parentCategoryId)) throw new NotFoundException("Parent category not found");
        Category updated = categories.save(current.update(name, description, displayOrder, parentCategoryId, actor, now()));
        log.info("catalog_operation=category_updated categoryId={} actor={}", updated.id(), actor);
        return updated;
    }

    @Transactional
    public void deactivate(UUID id, String actor) {
        Category current = get(id);
        if (products.existsInCategory(id)) throw new ConflictException("Category contains products");
        categories.save(current.deactivate(actor, now()));
        log.info("catalog_operation=category_deactivated categoryId={} actor={}", id, actor);
    }

    private LocalDateTime now() { return LocalDateTime.now(clock); }
}