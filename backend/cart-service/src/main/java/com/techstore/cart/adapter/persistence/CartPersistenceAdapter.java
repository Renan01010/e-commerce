package com.techstore.cart.adapter.persistence;

import com.techstore.cart.application.port.out.CartStorePort;
import com.techstore.cart.domain.CartItem;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional
public class CartPersistenceAdapter implements CartStorePort {
    private static final String ADD_ITEM_SQL = """
            insert into cart_items (owner_user_id, product_id, quantity)
            values (?, ?, ?)
            on conflict (owner_user_id, product_id)
            do update set quantity = cart_items.quantity + excluded.quantity
            returning quantity, (xmax = 0) as created
            """;

    private final CartItemJpaRepository cartItems;
    private final JdbcTemplate jdbcTemplate;

    public CartPersistenceAdapter(CartItemJpaRepository cartItems, JdbcTemplate jdbcTemplate) {
        this.cartItems = cartItems;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CartItem> findByOwner(UUID ownerUserId) {
        return cartItems.findAllByIdOwnerUserId(ownerUserId).stream()
                .map(CartItemEntity::toDomain)
                .toList();
    }

    @Override
    public AddResult add(UUID ownerUserId, UUID productId, int quantity) {
        return jdbcTemplate.queryForObject(ADD_ITEM_SQL,
                (resultSet, rowNumber) -> new AddResult(
                        new CartItem(productId, resultSet.getInt("quantity")),
                        resultSet.getBoolean("created")),
                ownerUserId, productId, quantity);
    }

    @Override
    public boolean setQuantity(UUID ownerUserId, UUID productId, int quantity) {
        return cartItems.updateOwnedQuantity(ownerUserId, productId, quantity) == 1;
    }

    @Override
    public boolean remove(UUID ownerUserId, UUID productId) {
        return cartItems.deleteOwnedItem(ownerUserId, productId) == 1;
    }

    @Override
    public void clear(UUID ownerUserId) {
        cartItems.deleteAllOwnedItems(ownerUserId);
    }
}