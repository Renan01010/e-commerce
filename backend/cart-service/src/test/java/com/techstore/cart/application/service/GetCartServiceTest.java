package com.techstore.cart.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
class GetCartServiceTest {
    private static final UUID OWNER_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440010");
    private static final UUID PRODUCT_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

    @Mock
    private CartStorePort cartStore;

    @Mock
    private CartViewAssembler cartViewAssembler;

    @InjectMocks
    private GetCartService service;

    @Test
    void returnsEmptyViewWithoutCreatingCartRows() {
        when(cartStore.findByOwner(OWNER_ID)).thenReturn(List.of());
        when(cartViewAssembler.assemble(List.of())).thenReturn(new CartView(List.of()));

        CartView response = service.getCart(OWNER_ID);

        assertEquals(List.of(), response.items());
        assertEquals(java.math.BigDecimal.ZERO, response.total());
        assertTrue(response.totalAvailable());
        verify(cartStore).findByOwner(OWNER_ID);
        verify(cartStore, never()).add(any(), any(), anyInt(), any(), anyInt(), anyInt());
        verify(cartStore, never()).clear(OWNER_ID);
    }

    @Test
    void readsOnlyTheAuthenticatedOwnerRows() {
        List<CartItem> rows = List.of(new CartItem(PRODUCT_ID, 3));
        when(cartStore.findByOwner(OWNER_ID)).thenReturn(rows);
        when(cartViewAssembler.assemble(rows)).thenReturn(
                new CartView(List.of(new CartView.Item(PRODUCT_ID, 3, false, null))));

        CartView response = service.getCart(OWNER_ID);

        assertEquals(PRODUCT_ID, response.items().getFirst().productId());
        verify(cartStore).findByOwner(OWNER_ID);
    }

    @Test
    void reportsUnavailableTotalWhenAnyLineHasUnknownPrice() {
        List<CartItem> rows = List.of(new CartItem(PRODUCT_ID, 3));
        CartView assembled = new CartView(List.of(new CartView.Item(
                PRODUCT_ID, 3, true, new CartView.ProductSummary("Keyboard", null, "Acme", null),
                com.techstore.cart.domain.UnitPriceSnapshot.unknown())));
        when(cartStore.findByOwner(OWNER_ID)).thenReturn(rows);
        when(cartViewAssembler.assemble(rows)).thenReturn(assembled);

        CartView response = service.getCart(OWNER_ID);

        assertFalse(response.totalAvailable());
        assertNull(response.total());
        assertNull(response.items().getFirst().subtotal());
    }
}