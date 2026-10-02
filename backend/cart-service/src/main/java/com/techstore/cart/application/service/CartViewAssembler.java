package com.techstore.cart.application.service;

import com.techstore.cart.application.model.CartView;
import com.techstore.cart.application.port.out.ProductCatalogPort;
import com.techstore.cart.domain.CartItem;
import com.techstore.cart.domain.CartItemRules;
import java.util.List;

public class CartViewAssembler {
    private final ProductCatalogPort productCatalog;
    private final CartItemRules cartItemRules;

    public CartViewAssembler(ProductCatalogPort productCatalog, CartItemRules cartItemRules) {
        this.productCatalog = productCatalog;
        this.cartItemRules = cartItemRules;
    }

    public CartView assemble(List<CartItem> cartItems) {
        List<CartView.Item> items = cartItems.stream()
                .map(this::toResponse)
                .toList();
        return new CartView(items, cartItemRules.maxQuantity());
    }

    private CartView.Item toResponse(CartItem cartItem) {
        return productCatalog.findActiveById(cartItem.productId())
                .map(product -> new CartView.Item(cartItem.productId(), cartItem.quantity(), true,
                new CartView.ProductSummary(product.name(), cartItem.unitPriceSnapshot().amount(),
                    product.brand(), product.imageUrl()), cartItem.unitPriceSnapshot()))
            .orElseGet(() -> new CartView.Item(cartItem.productId(), cartItem.quantity(), false, null,
                cartItem.unitPriceSnapshot()));
    }
}