package com.techstore.cart.adapter.persistence;

import com.techstore.cart.domain.CartItem;
import com.techstore.cart.domain.UnitPriceSnapshot;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "cart_items")
public class CartItemEntity {
    @EmbeddedId
    private CartItemId id;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "unit_price", precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "price_snapshot_status", nullable = false, length = 16)
    private String priceSnapshotStatus;

    protected CartItemEntity() {}

    public CartItemEntity(UUID ownerUserId, UUID productId, int quantity) {
        this(ownerUserId, productId, quantity, UnitPriceSnapshot.unknown());
    }

    public CartItemEntity(UUID ownerUserId, UUID productId, int quantity, UnitPriceSnapshot priceSnapshot) {
        this.id = new CartItemId(ownerUserId, productId);
        this.quantity = quantity;
        this.unitPrice = priceSnapshot.amount();
        this.priceSnapshotStatus = priceSnapshot.status().name();
    }

    public CartItemId getId() {
        return id;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public String getPriceSnapshotStatus() {
        return priceSnapshotStatus;
    }

    public CartItem toDomain() {
        UnitPriceSnapshot snapshot = switch (priceSnapshotStatus) {
            case "KNOWN" -> UnitPriceSnapshot.known(unitPrice);
            case "UNKNOWN" -> UnitPriceSnapshot.unknown();
            case "PENDING" -> throw new IllegalStateException("Legacy price snapshot is still pending");
            default -> throw new IllegalStateException("Unknown price snapshot status: " + priceSnapshotStatus);
        };
        return new CartItem(id.getProductId(), quantity, snapshot);
    }
}