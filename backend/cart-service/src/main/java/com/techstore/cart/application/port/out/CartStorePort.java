package com.techstore.cart.application.port.out;

import com.techstore.cart.domain.CartItem;
import com.techstore.cart.domain.UnitPriceSnapshot;
import java.util.List;
import java.util.UUID;

public interface CartStorePort {
    List<CartItem> findByOwner(UUID ownerUserId);

    List<LegacyCartItem> findPendingPriceSnapshots();

    boolean resolvePendingPriceSnapshot(UUID ownerUserId, UUID productId, UnitPriceSnapshot priceSnapshot);

    AddResult add(UUID ownerUserId, UUID productId, int quantity, UnitPriceSnapshot priceSnapshot,
                  int maxQuantity, int availableStock);

    SetQuantityResult setQuantity(UUID ownerUserId, UUID productId, int quantity, UnitPriceSnapshot priceSnapshot,
                                  int maxQuantity, int availableStock);

    boolean remove(UUID ownerUserId, UUID productId);

    void clear(UUID ownerUserId);

    record LegacyCartItem(UUID ownerUserId, UUID productId, int quantity) {}

    enum WriteRejection {
        MAX_QUANTITY,
        INSUFFICIENT_STOCK
    }

    record AddResult(CartItem item, boolean created, WriteRejection rejection) {
        public AddResult(CartItem item, boolean created) {
            this(item, created, null);
        }

        public static AddResult rejected(WriteRejection rejection) {
            return new AddResult(null, false, rejection);
        }
    }

    record SetQuantityResult(boolean updated, WriteRejection rejection) {
        public static SetQuantityResult success() {
            return new SetQuantityResult(true, null);
        }

        public static SetQuantityResult rejected(WriteRejection rejection) {
            return new SetQuantityResult(false, rejection);
        }

        public static SetQuantityResult missing() {
            return new SetQuantityResult(false, null);
        }
    }
}