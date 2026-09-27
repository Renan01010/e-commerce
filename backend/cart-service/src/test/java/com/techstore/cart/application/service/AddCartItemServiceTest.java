package com.techstore.cart.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.techstore.cart.application.exception.ProductCatalogUnavailableException;
import com.techstore.cart.application.exception.ProductNotFoundException;
import com.techstore.cart.application.model.AddCartItemResult;
import com.techstore.cart.application.model.CartView;
import com.techstore.cart.application.port.out.CartStorePort;
import com.techstore.cart.application.port.out.ProductCatalogPort;
import com.techstore.cart.application.port.out.ProductCatalogPort.ProductSummary;
import com.techstore.cart.domain.CartItem;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AddCartItemServiceTest {
    private static final UUID OWNER_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440010");
    private static final UUID PRODUCT_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

    @Mock private CartStorePort cartStore;
    @Mock private ProductCatalogPort productCatalog;
    @Mock private CartViewAssembler cartViewAssembler;
    @InjectMocks private AddCartItemService service;

    @Test
    void validatesActiveProductBeforeAdding() {
        when(cartStore.findByOwner(OWNER_ID)).thenReturn(java.util.List.of());
        when(cartViewAssembler.assemble(java.util.List.of())).thenReturn(new CartView(java.util.List.of()));
        when(productCatalog.findActiveById(PRODUCT_ID)).thenReturn(Optional.of(activeProduct()));
        when(cartStore.add(OWNER_ID, PRODUCT_ID, 2))
                .thenReturn(new CartStorePort.AddResult(new CartItem(PRODUCT_ID, 2), true));

        AddCartItemResult result = service.add(OWNER_ID, PRODUCT_ID, 2);

        assertEquals(2, result.cart().items().getFirst().quantity());
        assertEquals(true, result.created());
        verify(productCatalog).findActiveById(PRODUCT_ID);
        verify(cartStore).add(OWNER_ID, PRODUCT_ID, 2);
    }

    @Test
    void rejectsInactiveProductWithoutWriting() {
        when(cartStore.findByOwner(OWNER_ID)).thenReturn(java.util.List.of());
        when(cartViewAssembler.assemble(java.util.List.of())).thenReturn(new CartView(java.util.List.of()));
        when(productCatalog.findActiveById(PRODUCT_ID)).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class, () -> service.add(OWNER_ID, PRODUCT_ID, 1));
        verify(cartStore, never()).add(OWNER_ID, PRODUCT_ID, 1);
    }

    @Test
    void doesNotWriteWhenCatalogIsUnavailable() {
        when(cartStore.findByOwner(OWNER_ID)).thenReturn(java.util.List.of());
        when(cartViewAssembler.assemble(java.util.List.of())).thenReturn(new CartView(java.util.List.of()));
        when(productCatalog.findActiveById(PRODUCT_ID))
                .thenThrow(new ProductCatalogUnavailableException("catalog unavailable"));

        assertThrows(ProductCatalogUnavailableException.class, () -> service.add(OWNER_ID, PRODUCT_ID, 1));
        verify(cartStore, never()).add(OWNER_ID, PRODUCT_ID, 1);
    }

    private static ProductSummary activeProduct() {
        return new ProductSummary("Keyboard", new BigDecimal("49.90"), "Acme", null);
    }
}