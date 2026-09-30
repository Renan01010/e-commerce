package com.techstore.cart.adapter.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CartItemJpaRepository extends JpaRepository<CartItemEntity, CartItemId> {
    List<CartItemEntity> findAllByIdOwnerUserId(UUID ownerUserId);

    @Modifying
    @Query("update CartItemEntity item set item.quantity = :quantity "
            + "where item.id.ownerUserId = :ownerUserId and item.id.productId = :productId")
    int updateOwnedQuantity(@Param("ownerUserId") UUID ownerUserId,
                            @Param("productId") UUID productId,
                            @Param("quantity") int quantity);

    @Modifying
    @Query(value = "delete from cart_items where owner_user_id = :ownerUserId and product_id = :productId",
            nativeQuery = true)
    int deleteOwnedItem(@Param("ownerUserId") UUID ownerUserId, @Param("productId") UUID productId);

    @Modifying
    @Query(value = "delete from cart_items where owner_user_id = :ownerUserId", nativeQuery = true)
    int deleteAllOwnedItems(@Param("ownerUserId") UUID ownerUserId);
}