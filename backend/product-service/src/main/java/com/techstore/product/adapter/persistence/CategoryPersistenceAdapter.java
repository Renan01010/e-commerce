package com.techstore.product.adapter.persistence;

import com.techstore.product.application.port.CategoryRepository;
import com.techstore.product.domain.Category;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class CategoryPersistenceAdapter implements CategoryRepository {
    private final CategoryJpaRepository repository;

    public CategoryPersistenceAdapter(CategoryJpaRepository repository) { this.repository = repository; }

    @Override public List<Category> findAllActive() {
        return repository.findAllByActiveTrueOrderByDisplayOrderAscNameAsc().stream().map(CategoryEntity::toDomain).toList();
    }
    @Override public Optional<Category> findActiveById(UUID id) {
        return repository.findByIdAndActiveTrue(id).map(CategoryEntity::toDomain);
    }
    @Override public boolean existsByName(String name) { return repository.existsByName(name); }
    @Override public boolean existsByNameAndIdNot(String name, UUID id) { return repository.existsByNameAndIdNot(name, id); }
    @Override public boolean existsActiveById(UUID id) { return repository.existsByIdAndActiveTrue(id); }
    @Override public Category save(Category category) { return repository.save(CategoryEntity.from(category)).toDomain(); }
}