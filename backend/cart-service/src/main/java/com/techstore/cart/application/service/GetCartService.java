package com.techstore.cart.application.service;

import com.techstore.cart.application.model.CartView;
import com.techstore.cart.application.port.out.CartStorePort;
import com.techstore.cart.domain.CartItem;
import java.util.List;
import java.util.UUID;

public class GetCartService {
    private final CartStorePort cartStore;
    private final CartViewAssembler cartViewAssembler;

    public GetCartService(CartStorePort cartStore, CartViewAssembler cartViewAssembler) {
        this.cartStore = cartStore;
        this.cartViewAssembler = cartViewAssembler;
    }

    public CartView getCart(UUID ownerUserId) {
        List<CartItem> items = cartStore.findByOwner(ownerUserId);
        return cartViewAssembler.assemble(items);
    }
}