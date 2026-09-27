package com.techstore.cart.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.techstore.cart.application.model.CartView;
import com.techstore.cart.application.exception.ProductCatalogUnavailableException;
import com.techstore.cart.application.port.out.ProductCatalogPort;
import com.techstore.cart.application.port.out.ProductCatalogPort.ProductSummary;
import com.techstore.cart.domain.CartItem;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CartViewAssemblerTest {
    private static final UUID PRODUCT_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

    @Mock
    private ProductCatalogPort productCatalog;

    @InjectMocks
    private CartViewAssembler assembler;

    @Test
    void enrichesCartLineWithCurrentProductSummary() {
        when(productCatalog.findActiveById(PRODUCT_ID)).thenReturn(Optional.of(
                new ProductSummary("Keyboard", new BigDecimal("49.90"), "Acme", "https://example.test/item.jpg")));

        CartView response = assembler.assemble(List.of(new CartItem(PRODUCT_ID, 2)));
        CartView.Item item = response.items().getFirst();

        assertTrue(item.available());
        assertEquals(PRODUCT_ID, item.productId());
        assertEquals(2, item.quantity());
        assertEquals("Keyboard", item.product().name());
        assertEquals(new BigDecimal("49.90"), item.product().price());
    }

    @Test
    void representsInactiveProductWithoutPersistedSummary() {
        when(productCatalog.findActiveById(PRODUCT_ID)).thenReturn(Optional.empty());

        CartView.Item item = assembler.assemble(List.of(new CartItem(PRODUCT_ID, 1))).items().getFirst();

        assertEquals(false, item.available());
        assertNull(item.product());
    }

    @Test
    void propagatesCatalogFailureForServiceUnavailableResponse() {
        when(productCatalog.findActiveById(PRODUCT_ID))
                .thenThrow(new ProductCatalogUnavailableException("catalog unavailable"));

        assertThrows(ProductCatalogUnavailableException.class,
                () -> assembler.assemble(List.of(new CartItem(PRODUCT_ID, 1))));
    }
}