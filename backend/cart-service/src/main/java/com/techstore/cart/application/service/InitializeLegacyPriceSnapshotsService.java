package com.techstore.cart.application.service;

import com.techstore.cart.application.port.out.CartStorePort;
import com.techstore.cart.application.port.out.CartStorePort.LegacyCartItem;
import com.techstore.cart.application.port.out.ProductCatalogPort;
import com.techstore.cart.domain.UnitPriceSnapshot;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class InitializeLegacyPriceSnapshotsService {
    private final CartStorePort cartStore;
    private final ProductCatalogPort productCatalog;

    public InitializeLegacyPriceSnapshotsService(CartStorePort cartStore, ProductCatalogPort productCatalog) {
        this.cartStore = cartStore;
        this.productCatalog = productCatalog;
    }

    public void initialize() {
        List<LegacyCartItem> pendingItems = cartStore.findPendingPriceSnapshots();
        Map<UUID, List<LegacyCartItem>> itemsByProduct = new LinkedHashMap<>();
        for (LegacyCartItem item : pendingItems) {
            itemsByProduct.computeIfAbsent(item.productId(), ignored -> new ArrayList<>()).add(item);
        }

        for (Map.Entry<UUID, List<LegacyCartItem>> entry : itemsByProduct.entrySet()) {
            UnitPriceSnapshot snapshot = productCatalog.findActiveById(entry.getKey())
                    .map(product -> UnitPriceSnapshot.known(product.price()))
                    .orElseGet(UnitPriceSnapshot::unknown);
            for (LegacyCartItem item : entry.getValue()) {
                cartStore.resolvePendingPriceSnapshot(item.ownerUserId(), item.productId(), snapshot);
            }
        }
    }
}
