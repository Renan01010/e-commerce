package com.techstore.cart.application.service;

import com.techstore.cart.application.model.CartView;
import com.techstore.cart.application.port.out.ProductCatalogPort;
import com.techstore.cart.domain.CartItem;
import java.util.List;

public class CartViewAssembler {
    private final ProductCatalogPort productCatalog;

    public CartViewAssembler(ProductCatalogPort productCatalog) {
        this.productCatalog = productCatalog;
    }

    public CartView assemble(List<CartItem> cartItems) {
        List<CartView.Item> items = cartItems.stream()
                .map(this::toResponse)
                .toList();
        return new CartView(items);
    }

    private CartView.Item toResponse(CartItem cartItem) {
        return productCatalog.findActiveById(cartItem.productId())
                .map(product -> new CartView.Item(cartItem.productId(), cartItem.quantity(), true,
                        new CartView.ProductSummary(product.name(), product.price(), product.brand(), product.imageUrl())))
                .orElseGet(() -> new CartView.Item(cartItem.productId(), cartItem.quantity(), false, null));
    }
}