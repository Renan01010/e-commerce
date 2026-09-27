package com.techstore.cart.application.service;

import static org.mockito.Mockito.verify;

import com.techstore.cart.application.port.out.CartStorePort;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClearCartServiceTest {
    private static final UUID OWNER_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440010");

    @Mock
    private CartStorePort cartStore;

    @InjectMocks
    private ClearCartService service;

    @Test
    void clearsOnlyRowsForAuthenticatedOwnerAndAllowsRepeatedCalls() {
        service.clear(OWNER_ID);
        service.clear(OWNER_ID);

        verify(cartStore, org.mockito.Mockito.times(2)).clear(OWNER_ID);
    }
}