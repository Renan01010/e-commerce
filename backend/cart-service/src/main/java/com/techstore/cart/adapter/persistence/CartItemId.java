package com.techstore.cart.adapter.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class CartItemId implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "owner_user_id", nullable = false)
    private UUID ownerUserId;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    protected CartItemId() {}

    public CartItemId(UUID ownerUserId, UUID productId) {
        this.ownerUserId = Objects.requireNonNull(ownerUserId);
        this.productId = Objects.requireNonNull(productId);
    }

    public UUID getOwnerUserId() {
        return ownerUserId;
    }

    public UUID getProductId() {
        return productId;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof CartItemId that)) return false;
        return Objects.equals(ownerUserId, that.ownerUserId) && Objects.equals(productId, that.productId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ownerUserId, productId);
    }
}