package com.techstore.cart.adapter.persistence;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CartItemJpaRepository extends JpaRepository<CartItemEntity, CartItemId> {
    List<CartItemEntity> findAllByIdOwnerUserId(UUID ownerUserId);

    List<CartItemEntity> findAllByPriceSnapshotStatus(String priceSnapshotStatus);

        @Query("select item.quantity from CartItemEntity item "
                        + "where item.id.ownerUserId = :ownerUserId and item.id.productId = :productId")
        Integer findOwnedQuantity(@Param("ownerUserId") UUID ownerUserId, @Param("productId") UUID productId);

    @Modifying
        @Query("update CartItemEntity item set item.quantity = :quantity, item.unitPrice = :unitPrice, "
            + "item.priceSnapshotStatus = 'KNOWN' "
            + "where item.id.ownerUserId = :ownerUserId and item.id.productId = :productId "
            + "and :quantity <= :maxQuantity and :quantity <= :availableStock")
    int updateOwnedQuantity(@Param("ownerUserId") UUID ownerUserId,
                            @Param("productId") UUID productId,
                            @Param("quantity") int quantity,
                            @Param("unitPrice") BigDecimal unitPrice,
                            @Param("maxQuantity") int maxQuantity,
                            @Param("availableStock") int availableStock);

    @Modifying
    @Query(value = "update cart_items set unit_price = :unitPrice, price_snapshot_status = :status "
            + "where owner_user_id = :ownerUserId and product_id = :productId "
            + "and price_snapshot_status = 'PENDING'", nativeQuery = true)
    int resolvePendingPriceSnapshot(@Param("ownerUserId") UUID ownerUserId,
                                    @Param("productId") UUID productId,
                                    @Param("unitPrice") BigDecimal unitPrice,
                                    @Param("status") String status);

    @Modifying
    @Query(value = "delete from cart_items where owner_user_id = :ownerUserId and product_id = :productId",
            nativeQuery = true)
    int deleteOwnedItem(@Param("ownerUserId") UUID ownerUserId, @Param("productId") UUID productId);

    @Modifying
    @Query(value = "delete from cart_items where owner_user_id = :ownerUserId", nativeQuery = true)
    int deleteAllOwnedItems(@Param("ownerUserId") UUID ownerUserId);
}