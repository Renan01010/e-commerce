package com.techstore.cart.application.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.techstore.cart.application.exception.CartItemNotFoundException;
import com.techstore.cart.application.port.out.CartStorePort;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RemoveCartItemServiceTest {
    private static final UUID OWNER_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440010");
    private static final UUID PRODUCT_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

    @Mock private CartStorePort cartStore;
    @InjectMocks private RemoveCartItemService service;

    @Test
    void removesOnlyItemForAuthenticatedOwner() {
        when(cartStore.remove(OWNER_ID, PRODUCT_ID)).thenReturn(true);

        service.remove(OWNER_ID, PRODUCT_ID);

        verify(cartStore).remove(OWNER_ID, PRODUCT_ID);
    }

    @Test
    void returnsNotFoundWhenOwnedItemDoesNotExist() {
        when(cartStore.remove(OWNER_ID, PRODUCT_ID)).thenReturn(false);

        assertThrows(CartItemNotFoundException.class, () -> service.remove(OWNER_ID, PRODUCT_ID));
        verify(cartStore).remove(OWNER_ID, PRODUCT_ID);
    }
}