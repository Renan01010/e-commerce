package com.techstore.cart.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.techstore.cart.application.exception.CartQuantityLimitExceededException;
import com.techstore.cart.application.exception.InsufficientProductStockException;
import com.techstore.cart.application.exception.ProductCatalogUnavailableException;
import com.techstore.cart.application.exception.ProductNotFoundException;
import com.techstore.cart.application.model.AddCartItemResult;
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
import org.junit.jupiter.api.BeforeEach;
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
    private AddCartItemService service;

    @BeforeEach
    void setUp() {
        service = new AddCartItemService(cartStore, productCatalog, cartViewAssembler, new CartItemRules(99));
    }

    @Test
    void validatesActiveProductBeforeAdding() {
        when(cartStore.findByOwner(OWNER_ID)).thenReturn(List.of());
        when(cartViewAssembler.assemble(List.of())).thenReturn(new CartView(List.of()));
        when(productCatalog.findActiveById(PRODUCT_ID)).thenReturn(Optional.of(activeProduct()));
        UnitPriceSnapshot snapshot = UnitPriceSnapshot.known(new BigDecimal("49.90"));
        when(cartStore.add(OWNER_ID, PRODUCT_ID, 2, snapshot, 99, 10))
                .thenReturn(new CartStorePort.AddResult(new CartItem(PRODUCT_ID, 2, snapshot), true));

        AddCartItemResult result = service.add(OWNER_ID, PRODUCT_ID, 2);

        assertEquals(2, result.cart().items().getFirst().quantity());
        assertEquals(true, result.created());
        verify(productCatalog).findActiveById(PRODUCT_ID);
        verify(cartStore).add(OWNER_ID, PRODUCT_ID, 2, snapshot, 99, 10);
    }

    @Test
    void consolidatesRepeatedAddWithinMaximum() {
        List<CartItem> existing = List.of(new CartItem(PRODUCT_ID, 98,
                UnitPriceSnapshot.known(new BigDecimal("49.90"))));
        when(cartStore.findByOwner(OWNER_ID)).thenReturn(existing);
        when(cartViewAssembler.assemble(existing)).thenReturn(new CartView(List.of()));
        when(productCatalog.findActiveById(PRODUCT_ID)).thenReturn(Optional.of(productWithStock(100)));
        UnitPriceSnapshot snapshot = UnitPriceSnapshot.known(new BigDecimal("49.90"));
        when(cartStore.add(OWNER_ID, PRODUCT_ID, 1, snapshot, 99, 100))
                .thenReturn(new CartStorePort.AddResult(new CartItem(PRODUCT_ID, 99, snapshot), false));

        AddCartItemResult result = service.add(OWNER_ID, PRODUCT_ID, 1);

        assertEquals(99, result.cart().items().getFirst().quantity());
        assertEquals(false, result.created());
    }

    @Test
    void rejectsRepeatedAddThatWouldExceedMaximumWithoutWriting() {
        List<CartItem> existing = List.of(new CartItem(PRODUCT_ID, 99,
                UnitPriceSnapshot.known(new BigDecimal("49.90"))));
        when(cartStore.findByOwner(OWNER_ID)).thenReturn(existing);
        when(cartViewAssembler.assemble(existing)).thenReturn(new CartView(List.of()));

        assertThrows(CartQuantityLimitExceededException.class, () -> service.add(OWNER_ID, PRODUCT_ID, 1));
        verify(cartStore, never()).add(eq(OWNER_ID), eq(PRODUCT_ID), eq(1),
                eq(UnitPriceSnapshot.known(new BigDecimal("49.90"))), eq(99), eq(10));
    }

    @Test
    void rejectsRequestedQuantityAboveMaximumWithoutWriting() {
        assertThrows(CartQuantityLimitExceededException.class, () -> service.add(OWNER_ID, PRODUCT_ID, 100));
        verify(productCatalog, never()).findActiveById(PRODUCT_ID);
        verify(cartStore, never()).add(eq(OWNER_ID), eq(PRODUCT_ID), eq(100),
                eq(UnitPriceSnapshot.known(new BigDecimal("49.90"))), eq(99), eq(10));
    }

    @Test
    void rejectsQuantityAboveAvailableStockWithoutWriting() {
        when(cartStore.findByOwner(OWNER_ID)).thenReturn(List.of());
        when(cartViewAssembler.assemble(List.of())).thenReturn(new CartView(List.of()));
        when(productCatalog.findActiveById(PRODUCT_ID)).thenReturn(Optional.of(productWithStock(3)));

        assertThrows(InsufficientProductStockException.class, () -> service.add(OWNER_ID, PRODUCT_ID, 4));
        verify(cartStore, never()).add(eq(OWNER_ID), eq(PRODUCT_ID), eq(4),
                eq(UnitPriceSnapshot.known(new BigDecimal("49.90"))), eq(99), eq(3));
    }

    @Test
    void rejectsInactiveProductWithoutWriting() {
        when(cartStore.findByOwner(OWNER_ID)).thenReturn(List.of());
        when(cartViewAssembler.assemble(List.of())).thenReturn(new CartView(List.of()));
        when(productCatalog.findActiveById(PRODUCT_ID)).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class, () -> service.add(OWNER_ID, PRODUCT_ID, 1));
        verify(cartStore, never()).add(eq(OWNER_ID), eq(PRODUCT_ID), eq(1),
                eq(UnitPriceSnapshot.known(new BigDecimal("49.90"))), eq(99), eq(10));
    }

    @Test
    void doesNotWriteWhenCatalogIsUnavailable() {
        when(cartStore.findByOwner(OWNER_ID)).thenReturn(List.of());
        when(cartViewAssembler.assemble(List.of())).thenReturn(new CartView(List.of()));
        when(productCatalog.findActiveById(PRODUCT_ID))
                .thenThrow(new ProductCatalogUnavailableException("catalog unavailable"));

        assertThrows(ProductCatalogUnavailableException.class, () -> service.add(OWNER_ID, PRODUCT_ID, 1));
        verify(cartStore, never()).add(eq(OWNER_ID), eq(PRODUCT_ID), eq(1),
                eq(UnitPriceSnapshot.known(new BigDecimal("49.90"))), eq(99), eq(10));
    }

    private static ProductSummary activeProduct() {
        return productWithStock(10);
    }

    private static ProductSummary productWithStock(int stock) {
        return new ProductSummary("Keyboard", new BigDecimal("49.90"), "Acme", null, stock);
    }
}