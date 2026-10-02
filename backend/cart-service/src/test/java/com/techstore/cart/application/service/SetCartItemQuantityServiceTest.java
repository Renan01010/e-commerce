package com.techstore.cart.application.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.techstore.cart.application.exception.CartQuantityLimitExceededException;
import com.techstore.cart.application.exception.CartItemNotFoundException;
import com.techstore.cart.application.exception.InsufficientProductStockException;
import com.techstore.cart.application.exception.ProductCatalogUnavailableException;
import com.techstore.cart.application.exception.ProductNotFoundException;
import com.techstore.cart.application.model.CartView;
import com.techstore.cart.application.port.out.CartStorePort;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SetCartItemQuantityServiceTest {
    private static final UUID OWNER_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440010");
    private static final UUID PRODUCT_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

    @Mock private CartStorePort cartStore;
    @Mock private ProductCatalogPort productCatalog;
    @Mock private CartViewAssembler cartViewAssembler;
    private SetCartItemQuantityService service;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        service = new SetCartItemQuantityService(cartStore, productCatalog, cartViewAssembler,
                new CartItemRules(99));
    }

    @Test
    void replacesQuantityForOwnedItemAfterCatalogValidation() {
        when(cartStore.findByOwner(OWNER_ID)).thenReturn(List.of(new CartItem(PRODUCT_ID, 3)));
        when(productCatalog.findActiveById(PRODUCT_ID)).thenReturn(Optional.of(product(5)));
        CartView currentView = new CartView(List.of(new CartView.Item(PRODUCT_ID, 3, true,
            new CartView.ProductSummary("Keyboard", java.math.BigDecimal.valueOf(49.90), null, null))));
        when(cartViewAssembler.assemble(List.of(new CartItem(PRODUCT_ID, 3)))).thenReturn(currentView);
        UnitPriceSnapshot snapshot = UnitPriceSnapshot.known(new BigDecimal("49.90"));
        when(cartStore.setQuantity(OWNER_ID, PRODUCT_ID, 2, snapshot, 99, 5))
            .thenReturn(CartStorePort.SetQuantityResult.success());

        CartView updatedView = service.setQuantity(OWNER_ID, PRODUCT_ID, 2);

        org.junit.jupiter.api.Assertions.assertEquals(2, updatedView.items().getFirst().quantity());
        verify(cartStore).setQuantity(OWNER_ID, PRODUCT_ID, 2, snapshot, 99, 5);
    }

    @Test
    void returnsNotFoundWhenItemIsNotOwned() {
        when(cartStore.findByOwner(OWNER_ID)).thenReturn(List.of());

        assertThrows(CartItemNotFoundException.class, () -> service.setQuantity(OWNER_ID, PRODUCT_ID, 2));
        verify(cartViewAssembler, never()).assemble(List.of());
        verify(cartStore, never()).setQuantity(org.mockito.ArgumentMatchers.eq(OWNER_ID),
            org.mockito.ArgumentMatchers.eq(PRODUCT_ID), org.mockito.ArgumentMatchers.eq(2),
            org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyInt(),
            org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void rejectsInactiveProductWithoutReplacingQuantity() {
        List<CartItem> existing = List.of(new CartItem(PRODUCT_ID, 3));
        when(cartStore.findByOwner(OWNER_ID)).thenReturn(existing);
        when(cartViewAssembler.assemble(List.of(new CartItem(PRODUCT_ID, 3))))
            .thenReturn(new CartView(List.of(new CartView.Item(PRODUCT_ID, 3, false, null))));

        assertThrows(ProductNotFoundException.class, () -> service.setQuantity(OWNER_ID, PRODUCT_ID, 2));
        verify(cartStore, never()).setQuantity(org.mockito.ArgumentMatchers.eq(OWNER_ID),
            org.mockito.ArgumentMatchers.eq(PRODUCT_ID), org.mockito.ArgumentMatchers.eq(2),
            org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyInt(),
            org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void doesNotReplaceQuantityWhenCatalogIsUnavailable() {
        List<CartItem> existing = List.of(new CartItem(PRODUCT_ID, 3));
        when(cartStore.findByOwner(OWNER_ID)).thenReturn(existing);
        when(cartViewAssembler.assemble(existing)).thenReturn(activeView(3));
        when(productCatalog.findActiveById(PRODUCT_ID))
                .thenThrow(new ProductCatalogUnavailableException("catalog unavailable"));

        assertThrows(ProductCatalogUnavailableException.class,
                () -> service.setQuantity(OWNER_ID, PRODUCT_ID, 2));
        verify(cartStore, never()).setQuantity(org.mockito.ArgumentMatchers.eq(OWNER_ID),
            org.mockito.ArgumentMatchers.eq(PRODUCT_ID), org.mockito.ArgumentMatchers.eq(2),
            org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyInt(),
            org.mockito.ArgumentMatchers.anyInt());
        }

        @Test
        void rejectsQuantityAboveConfiguredMaximum() {
        assertThrows(CartQuantityLimitExceededException.class,
            () -> service.setQuantity(OWNER_ID, PRODUCT_ID, 100));
        verify(productCatalog, never()).findActiveById(PRODUCT_ID);
        }

        @Test
        void rejectsQuantityAboveAvailableStockWithoutReplacingLine() {
            List<CartItem> existing = List.of(new CartItem(PRODUCT_ID, 3));
            when(cartStore.findByOwner(OWNER_ID)).thenReturn(existing);
            when(cartViewAssembler.assemble(existing)).thenReturn(activeView(3));
        when(productCatalog.findActiveById(PRODUCT_ID)).thenReturn(Optional.of(product(4)));

        assertThrows(InsufficientProductStockException.class,
            () -> service.setQuantity(OWNER_ID, PRODUCT_ID, 5));
        verify(cartStore, never()).setQuantity(org.mockito.ArgumentMatchers.eq(OWNER_ID),
            org.mockito.ArgumentMatchers.eq(PRODUCT_ID), org.mockito.ArgumentMatchers.eq(5),
            org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyInt(),
            org.mockito.ArgumentMatchers.anyInt());
        }

        private static ProductSummary product(int stock) {
        return new ProductSummary("Keyboard", new BigDecimal("49.90"), "Acme", null, stock);
    }

        private static CartView activeView(int quantity) {
            return new CartView(List.of(new CartView.Item(PRODUCT_ID, quantity, true,
                    new CartView.ProductSummary("Keyboard", new BigDecimal("49.90"), "Acme", null))));
        }
}