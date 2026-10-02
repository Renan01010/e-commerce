package com.techstore.cart.application.service;

import com.techstore.cart.application.exception.CartItemNotFoundException;
import com.techstore.cart.application.exception.CartQuantityLimitExceededException;
import com.techstore.cart.application.exception.InsufficientProductStockException;
import com.techstore.cart.application.exception.ProductNotFoundException;
import com.techstore.cart.application.model.CartView;
import com.techstore.cart.application.port.out.CartStorePort;
import com.techstore.cart.application.port.out.ProductCatalogPort;
import com.techstore.cart.domain.CartItem;
import com.techstore.cart.domain.CartItemRules;
import com.techstore.cart.domain.UnitPriceSnapshot;
import java.util.List;
import java.util.UUID;

public class SetCartItemQuantityService {
    private final CartStorePort cartStore;
    private final ProductCatalogPort productCatalog;
    private final CartViewAssembler cartViewAssembler;
    private final CartItemRules cartItemRules;

    public SetCartItemQuantityService(CartStorePort cartStore, ProductCatalogPort productCatalog,
                                      CartViewAssembler cartViewAssembler, CartItemRules cartItemRules) {
        this.cartStore = cartStore;
        this.productCatalog = productCatalog;
        this.cartViewAssembler = cartViewAssembler;
        this.cartItemRules = cartItemRules;
    }

    public CartView setQuantity(UUID ownerUserId, UUID productId, int quantity) {
        if (!cartItemRules.isQuantityAllowed(quantity)) {
            throw new CartQuantityLimitExceededException(cartItemRules.maxQuantity());
        }
        List<CartItem> existingItems = cartStore.findByOwner(ownerUserId);
        CartItem existingItem = existingItems.stream()
                .filter(item -> item.productId().equals(productId))
                .findFirst()
                .orElse(null);
        if (existingItem == null) throw new CartItemNotFoundException(productId);

        CartView currentView = cartViewAssembler.assemble(existingItems);
        CartView.Item currentLine = currentView.items().stream()
                .filter(item -> item.productId().equals(productId))
                .findFirst()
                .orElseThrow(() -> new CartItemNotFoundException(productId));
        if (!currentLine.available()) throw new ProductNotFoundException(productId);

        ProductCatalogPort.ProductSummary product = productCatalog.findActiveById(productId)
            .orElseThrow(() -> new ProductNotFoundException(productId));
        if (!cartItemRules.hasStockFor(quantity, product.availableStock())) {
            throw new InsufficientProductStockException(product.availableStock());
        }
        UnitPriceSnapshot priceSnapshot = UnitPriceSnapshot.known(product.price());

        CartStorePort.SetQuantityResult writeResult = cartStore.setQuantity(ownerUserId, productId, quantity,
                priceSnapshot, cartItemRules.maxQuantity(), product.availableStock());
        if (writeResult.rejection() == CartStorePort.WriteRejection.MAX_QUANTITY) {
            throw new CartQuantityLimitExceededException(cartItemRules.maxQuantity());
        }
        if (writeResult.rejection() == CartStorePort.WriteRejection.INSUFFICIENT_STOCK) {
            throw new InsufficientProductStockException(product.availableStock());
        }
        if (!writeResult.updated()) {
            throw new CartItemNotFoundException(productId);
        }
        CartView.ProductSummary productSummary = new CartView.ProductSummary(
            product.name(), product.price(), product.brand(), product.imageUrl());
        return currentView.withItem(new CartView.Item(productId, quantity, true, productSummary));
    }
}