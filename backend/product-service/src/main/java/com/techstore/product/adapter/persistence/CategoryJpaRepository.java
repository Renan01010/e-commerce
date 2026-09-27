package com.techstore.product.adapter.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface CategoryJpaRepository extends JpaRepository<CategoryEntity, UUID> {
    List<CategoryEntity> findAllByActiveTrueOrderByDisplayOrderAscNameAsc();
    Optional<CategoryEntity> findByIdAndActiveTrue(UUID id);
    boolean existsByName(String name);
    boolean existsByNameAndIdNot(String name, UUID id);
    boolean existsByIdAndActiveTrue(UUID id);
}