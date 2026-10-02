package com.techstore.cart.application.port.out;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public interface ProductCatalogPort {
    Optional<ProductSummary> findActiveById(UUID productId);

    record ProductSummary(String name, BigDecimal price, String brand, String imageUrl, int availableStock) {}
}