package com.techstore.cart.application.service;

import com.techstore.cart.application.exception.ProductNotFoundException;
import com.techstore.cart.application.model.AddCartItemResult;
import com.techstore.cart.application.model.CartView;
import com.techstore.cart.application.port.out.CartStorePort;
import com.techstore.cart.application.port.out.ProductCatalogPort;
import com.techstore.cart.domain.CartItem;
import java.util.List;
import java.util.UUID;

public class AddCartItemService {
    private final CartStorePort cartStore;
    private final ProductCatalogPort productCatalog;
    private final CartViewAssembler cartViewAssembler;

    public AddCartItemService(CartStorePort cartStore, ProductCatalogPort productCatalog,
                              CartViewAssembler cartViewAssembler) {
        this.cartStore = cartStore;
        this.productCatalog = productCatalog;
        this.cartViewAssembler = cartViewAssembler;
    }

    public AddCartItemResult add(UUID ownerUserId, UUID productId, int quantity) {
        if (quantity <= 0) throw new IllegalArgumentException("Quantity must be positive");
        List<CartItem> existingItems = cartStore.findByOwner(ownerUserId);
        CartView currentView = cartViewAssembler.assemble(existingItems);
        CartView.Item existingLine = currentView.items().stream()
                .filter(item -> item.productId().equals(productId))
                .findFirst()
                .orElse(null);

        CartView.ProductSummary currentProduct;
        if (existingLine != null) {
            if (!existingLine.available()) throw new ProductNotFoundException(productId);
            currentProduct = existingLine.product();
        } else {
            ProductCatalogPort.ProductSummary product = productCatalog.findActiveById(productId)
                    .orElseThrow(() -> new ProductNotFoundException(productId));
            currentProduct = new CartView.ProductSummary(
                    product.name(), product.price(), product.brand(), product.imageUrl());
        }

        CartStorePort.AddResult result = cartStore.add(ownerUserId, productId, quantity);
        CartView updatedView = currentView.withItem(new CartView.Item(
                productId, result.item().quantity(), true, currentProduct));
        return new AddCartItemResult(updatedView, result.created());
    }
}