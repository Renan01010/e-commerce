package com.techstore.product.application.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.techstore.product.application.port.CategoryRepository;
import com.techstore.product.application.port.ProductRepository;
import com.techstore.product.domain.Category;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {
    @Mock CategoryRepository categories;
    @Mock ProductRepository products;
    private CategoryService service;
    private final UUID id = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new CategoryService(categories, products,
                Clock.fixed(Instant.parse("2026-09-27T12:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void createRejectsDuplicateName() {
        when(categories.existsByName("Phones")).thenReturn(true);
        assertThrows(ConflictException.class, () -> service.create("Phones", null, null, "admin"));
        verify(categories, never()).save(any());
    }

    @Test
    void deactivateRejectsCategoryWithProducts() {
        Category category = new Category(id, "Phones", null, null, 0, true,
                java.time.LocalDateTime.now(), java.time.LocalDateTime.now(), "admin", "admin");
        when(categories.findActiveById(id)).thenReturn(Optional.of(category));
        when(products.existsInCategory(id)).thenReturn(true);
        assertThrows(ConflictException.class, () -> service.deactivate(id, "admin"));
        verify(categories, never()).save(any());
    }
}