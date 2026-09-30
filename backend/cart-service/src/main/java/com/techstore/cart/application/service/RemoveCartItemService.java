package com.techstore.cart.application.service;

import com.techstore.cart.application.exception.CartItemNotFoundException;
import com.techstore.cart.application.port.out.CartStorePort;
import java.util.UUID;

public class RemoveCartItemService {
    private final CartStorePort cartStore;

    public RemoveCartItemService(CartStorePort cartStore) {
        this.cartStore = cartStore;
    }

    public void remove(UUID ownerUserId, UUID productId) {
        if (!cartStore.remove(ownerUserId, productId)) throw new CartItemNotFoundException(productId);
    }
}