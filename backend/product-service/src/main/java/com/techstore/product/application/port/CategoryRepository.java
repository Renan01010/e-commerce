package com.techstore.product.application.port;

import com.techstore.product.domain.Category;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository {
    List<Category> findAllActive();
    Optional<Category> findActiveById(UUID id);
    boolean existsByName(String name);
    boolean existsByNameAndIdNot(String name, UUID id);
    boolean existsActiveById(UUID id);
    Category save(Category category);
}