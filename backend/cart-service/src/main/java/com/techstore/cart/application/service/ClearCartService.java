package com.techstore.cart.application.service;

import com.techstore.cart.application.port.out.CartStorePort;
import java.util.UUID;

public class ClearCartService {
    private final CartStorePort cartStore;

    public ClearCartService(CartStorePort cartStore) {
        this.cartStore = cartStore;
    }

    public void clear(UUID ownerUserId) {
        cartStore.clear(ownerUserId);
    }
}