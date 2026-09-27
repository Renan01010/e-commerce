package com.techstore.product.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.techstore.product.application.port.CategoryRepository;
import com.techstore.product.application.port.ProductRepository;
import com.techstore.product.domain.Product;
import java.math.BigDecimal;
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
class ProductServiceTest {
    @Mock ProductRepository products;
    @Mock CategoryRepository categories;
    private ProductService service;
    private final UUID categoryId = UUID.randomUUID();
    private final UUID productId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new ProductService(products, categories,
                Clock.fixed(Instant.parse("2026-09-27T12:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void createRejectsUnknownCategory() {
        when(categories.existsActiveById(categoryId)).thenReturn(false);
        assertThrows(NotFoundException.class, () -> service.create("Phone", null, BigDecimal.TEN,
                null, null, "P-1", categoryId, 2, null, "admin"));
        verify(products, never()).save(any());
    }

    @Test
    void createRejectsDuplicateSku() {
        when(categories.existsActiveById(categoryId)).thenReturn(true);
        when(products.existsBySku("P-1")).thenReturn(true);
        assertThrows(ConflictException.class, () -> service.create("Phone", null, BigDecimal.TEN,
                null, null, "P-1", categoryId, 2, null, "admin"));
        verify(products, never()).save(any());
    }

    @Test
    void createPersistsAuditedActiveProduct() {
        when(categories.existsActiveById(categoryId)).thenReturn(true);
        when(products.existsBySku("P-1")).thenReturn(false);
        when(products.existsByNameAndCategoryId("Phone", categoryId)).thenReturn(false);
        when(products.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Product result = service.create("Phone", null, BigDecimal.TEN, null, "Maker", "P-1",
                categoryId, 2, null, "admin");

        assertEquals("admin", result.createdBy());
        assertEquals(true, result.active());
        assertEquals("2026-09-27T12:00", result.createdAt().toString());
    }

    @Test
    void searchRejectsInvertedPriceRange() {
        assertThrows(IllegalArgumentException.class, () -> service.search(null, null,
                BigDecimal.TEN, BigDecimal.ONE, null, null, "price", "asc", 0, 20));
        verify(products, never()).search(any());
    }

    @Test
    void getHidesInactiveProducts() {
        when(products.findActiveById(productId)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> service.get(productId));
    }
}