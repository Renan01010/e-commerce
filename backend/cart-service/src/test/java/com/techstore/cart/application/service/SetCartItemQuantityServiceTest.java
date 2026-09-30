package com.techstore.cart.application.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.techstore.cart.application.exception.CartItemNotFoundException;
import com.techstore.cart.application.exception.ProductCatalogUnavailableException;
import com.techstore.cart.application.exception.ProductNotFoundException;
import com.techstore.cart.application.model.CartView;
import com.techstore.cart.application.port.out.CartStorePort;
import com.techstore.cart.domain.CartItem;
import java.util.List;
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
    @Mock private CartViewAssembler cartViewAssembler;
    @InjectMocks private SetCartItemQuantityService service;

    @Test
    void replacesQuantityForOwnedItemAfterCatalogValidation() {
        when(cartStore.findByOwner(OWNER_ID)).thenReturn(List.of(new CartItem(PRODUCT_ID, 3)));
        CartView currentView = new CartView(List.of(new CartView.Item(PRODUCT_ID, 3, true,
            new CartView.ProductSummary("Keyboard", java.math.BigDecimal.valueOf(49.90), null, null))));
        when(cartViewAssembler.assemble(List.of(new CartItem(PRODUCT_ID, 3)))).thenReturn(currentView);
        when(cartStore.setQuantity(OWNER_ID, PRODUCT_ID, 2)).thenReturn(true);

        CartView updatedView = service.setQuantity(OWNER_ID, PRODUCT_ID, 2);

        org.junit.jupiter.api.Assertions.assertEquals(2, updatedView.items().getFirst().quantity());
        verify(cartStore).setQuantity(OWNER_ID, PRODUCT_ID, 2);
    }

    @Test
    void returnsNotFoundWhenItemIsNotOwned() {
        when(cartStore.findByOwner(OWNER_ID)).thenReturn(List.of());

        assertThrows(CartItemNotFoundException.class, () -> service.setQuantity(OWNER_ID, PRODUCT_ID, 2));
        verify(cartViewAssembler, never()).assemble(List.of());
        verify(cartStore, never()).setQuantity(OWNER_ID, PRODUCT_ID, 2);
    }

    @Test
    void rejectsInactiveProductWithoutReplacingQuantity() {
        when(cartStore.findByOwner(OWNER_ID)).thenReturn(List.of(new CartItem(PRODUCT_ID, 3)));
        when(cartViewAssembler.assemble(List.of(new CartItem(PRODUCT_ID, 3))))
            .thenReturn(new CartView(List.of(new CartView.Item(PRODUCT_ID, 3, false, null))));

        assertThrows(ProductNotFoundException.class, () -> service.setQuantity(OWNER_ID, PRODUCT_ID, 2));
        verify(cartStore, never()).setQuantity(OWNER_ID, PRODUCT_ID, 2);
    }

    @Test
    void doesNotReplaceQuantityWhenCatalogIsUnavailable() {
        when(cartStore.findByOwner(OWNER_ID)).thenReturn(List.of(new CartItem(PRODUCT_ID, 3)));
        when(cartViewAssembler.assemble(List.of(new CartItem(PRODUCT_ID, 3))))
                .thenThrow(new ProductCatalogUnavailableException("catalog unavailable"));

        assertThrows(ProductCatalogUnavailableException.class,
                () -> service.setQuantity(OWNER_ID, PRODUCT_ID, 2));
        verify(cartStore, never()).setQuantity(OWNER_ID, PRODUCT_ID, 2);
    }
}