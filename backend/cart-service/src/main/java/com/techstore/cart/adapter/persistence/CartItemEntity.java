package com.techstore.cart.adapter.persistence;

import com.techstore.cart.domain.CartItem;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "cart_items")
public class CartItemEntity {
    @EmbeddedId
    private CartItemId id;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    protected CartItemEntity() {}

    public CartItemEntity(UUID ownerUserId, UUID productId, int quantity) {
        this.id = new CartItemId(ownerUserId, productId);
        this.quantity = quantity;
    }

    public CartItemId getId() {
        return id;
    }

    public int getQuantity() {
        return quantity;
    }

    public CartItem toDomain() {
        return new CartItem(id.getProductId(), quantity);
    }
}