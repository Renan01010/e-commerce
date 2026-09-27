package com.techstore.cart.application.service;

import com.techstore.cart.application.exception.CartItemNotFoundException;
import com.techstore.cart.application.exception.ProductNotFoundException;
import com.techstore.cart.application.model.CartView;
import com.techstore.cart.application.port.out.CartStorePort;
import com.techstore.cart.domain.CartItem;
import java.util.List;
import java.util.UUID;

public class SetCartItemQuantityService {
    private final CartStorePort cartStore;
    private final CartViewAssembler cartViewAssembler;

    public SetCartItemQuantityService(CartStorePort cartStore, CartViewAssembler cartViewAssembler) {
        this.cartStore = cartStore;
        this.cartViewAssembler = cartViewAssembler;
    }

    public CartView setQuantity(UUID ownerUserId, UUID productId, int quantity) {
        if (quantity <= 0) throw new IllegalArgumentException("Quantity must be positive");
        List<CartItem> existingItems = cartStore.findByOwner(ownerUserId);
        CartItem existingItem = existingItems.stream()
                .filter(item -> item.productId().equals(productId))
                .findFirst()
                .orElse(null);
        if (existingItem == null) throw new CartItemNotFoundException(productId);

        CartView currentView = cartViewAssembler.assemble(existingItems);
        CartView.Item currentLine = currentView.items().stream()
                .filter(item -> item.productId().equals(productId))
                .findFirst()
                .orElseThrow(() -> new CartItemNotFoundException(productId));
        if (!currentLine.available()) throw new ProductNotFoundException(productId);

        if (!cartStore.setQuantity(ownerUserId, productId, quantity)) {
            throw new CartItemNotFoundException(productId);
        }
        return currentView.withItem(new CartView.Item(productId, quantity, true, currentLine.product()));
    }
}