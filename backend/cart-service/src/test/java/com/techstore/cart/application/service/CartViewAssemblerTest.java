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
import com.techstore.cart.domain.CartItemRules;
import com.techstore.cart.domain.UnitPriceSnapshot;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CartViewAssemblerTest {
    private static final UUID PRODUCT_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

    @Mock
    private ProductCatalogPort productCatalog;

    private CartViewAssembler assembler;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        assembler = new CartViewAssembler(productCatalog, new CartItemRules(99));
    }

    @Test
    void enrichesCartLineWithCurrentProductSummary() {
        when(productCatalog.findActiveById(PRODUCT_ID)).thenReturn(Optional.of(
            new ProductSummary("Keyboard", new BigDecimal("49.90"), "Acme",
                "https://example.test/item.jpg", 10)));

        CartView response = assembler.assemble(List.of(new CartItem(PRODUCT_ID, 2,
            UnitPriceSnapshot.known(new BigDecimal("49.90")))));
        CartView.Item item = response.items().getFirst();

        assertTrue(item.available());
        assertEquals(PRODUCT_ID, item.productId());
        assertEquals(2, item.quantity());
        assertEquals("Keyboard", item.product().name());
        assertEquals(new BigDecimal("49.90"), item.product().price());
        assertEquals(new BigDecimal("99.80"), item.subtotal());
        assertEquals(new BigDecimal("99.80"), response.total());
    }

        @Test
        void keepsSnapshotAndSubtotalWhenCatalogPriceChanges() {
        when(productCatalog.findActiveById(PRODUCT_ID)).thenReturn(Optional.of(
            new ProductSummary("Keyboard", new BigDecimal("79.90"), "Acme", null, 10)));

        CartView.Item item = assembler.assemble(List.of(new CartItem(PRODUCT_ID, 2,
            UnitPriceSnapshot.known(new BigDecimal("49.90"))))).items().getFirst();

        assertEquals(new BigDecimal("49.90"), item.product().price());
        assertEquals(new BigDecimal("99.80"), item.subtotal());
        }

        @Test
        void keepsUnknownPriceEvenWhenCatalogCurrentlyHasAnActivePrice() {
        when(productCatalog.findActiveById(PRODUCT_ID)).thenReturn(Optional.of(
            new ProductSummary("Keyboard", new BigDecimal("79.90"), "Acme", null, 10)));

        CartView response = assembler.assemble(List.of(new CartItem(PRODUCT_ID, 2,
            UnitPriceSnapshot.unknown())));
        CartView.Item item = response.items().getFirst();

        assertTrue(item.available());
        assertEquals(false, item.priceAvailable());
        assertEquals(null, item.product().price());
        assertEquals(null, item.subtotal());
        assertEquals(null, response.total());
        assertEquals(false, response.totalAvailable());
        }

    @Test
    void representsInactiveProductWithoutPersistedSummary() {
        when(productCatalog.findActiveById(PRODUCT_ID)).thenReturn(Optional.empty());

        CartView response = assembler.assemble(List.of(new CartItem(PRODUCT_ID, 1,
            UnitPriceSnapshot.unknown())));
        CartView.Item item = response.items().getFirst();

        assertEquals(false, item.available());
        assertNull(item.product());
        assertEquals(null, item.subtotal());
        assertEquals(false, response.totalAvailable());
    }

    @Test
    void propagatesCatalogFailureForServiceUnavailableResponse() {
        when(productCatalog.findActiveById(PRODUCT_ID))
                .thenThrow(new ProductCatalogUnavailableException("catalog unavailable"));

        assertThrows(ProductCatalogUnavailableException.class,
            () -> assembler.assemble(List.of(new CartItem(PRODUCT_ID, 1, UnitPriceSnapshot.unknown()))));
    }
}