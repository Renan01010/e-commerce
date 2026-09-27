package com.techstore.cart.application.port.out;

import com.techstore.cart.domain.CartItem;
import java.util.List;
import java.util.UUID;

public interface CartStorePort {
    List<CartItem> findByOwner(UUID ownerUserId);

    AddResult add(UUID ownerUserId, UUID productId, int quantity);

    boolean setQuantity(UUID ownerUserId, UUID productId, int quantity);

    boolean remove(UUID ownerUserId, UUID productId);

    void clear(UUID ownerUserId);

    record AddResult(CartItem item, boolean created) {}
}