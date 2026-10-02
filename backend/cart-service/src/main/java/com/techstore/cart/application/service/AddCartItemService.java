package com.techstore.cart.application.service;

import com.techstore.cart.application.exception.ProductNotFoundException;
import com.techstore.cart.application.exception.CartQuantityLimitExceededException;
import com.techstore.cart.application.exception.InsufficientProductStockException;
import com.techstore.cart.application.model.AddCartItemResult;
import com.techstore.cart.application.model.CartView;
import com.techstore.cart.application.port.out.CartStorePort;
import com.techstore.cart.application.port.out.ProductCatalogPort;
import com.techstore.cart.domain.CartItem;
import com.techstore.cart.domain.CartItemRules;
import com.techstore.cart.domain.UnitPriceSnapshot;
import java.util.List;
import java.util.UUID;

public class AddCartItemService {
    private final CartStorePort cartStore;
    private final ProductCatalogPort productCatalog;
    private final CartViewAssembler cartViewAssembler;
    private final CartItemRules cartItemRules;

    public AddCartItemService(CartStorePort cartStore, ProductCatalogPort productCatalog,
                              CartViewAssembler cartViewAssembler, CartItemRules cartItemRules) {
        this.cartStore = cartStore;
        this.productCatalog = productCatalog;
        this.cartViewAssembler = cartViewAssembler;
        this.cartItemRules = cartItemRules;
    }

    public AddCartItemResult add(UUID ownerUserId, UUID productId, int quantity) {
        if (!cartItemRules.isQuantityAllowed(quantity)) {
            throw new CartQuantityLimitExceededException(cartItemRules.maxQuantity());
        }
        List<CartItem> existingItems = cartStore.findByOwner(ownerUserId);
        CartView currentView = cartViewAssembler.assemble(existingItems);
        CartItem existingLine = existingItems.stream()
                .filter(item -> item.productId().equals(productId))
                .findFirst()
                .orElse(null);

        long resultingQuantity = cartItemRules.resultingQuantity(
                existingLine == null ? 0 : existingLine.quantity(), quantity);
        if (!cartItemRules.isResultingQuantityAllowed(resultingQuantity)) {
            throw new CartQuantityLimitExceededException(cartItemRules.maxQuantity());
        }

        ProductCatalogPort.ProductSummary product = productCatalog.findActiveById(productId)
            .orElseThrow(() -> new ProductNotFoundException(productId));
        if (!cartItemRules.hasStockFor(resultingQuantity, product.availableStock())) {
            throw new InsufficientProductStockException(product.availableStock());
        }

        UnitPriceSnapshot priceSnapshot = UnitPriceSnapshot.known(product.price());
        CartView.ProductSummary currentProduct = new CartView.ProductSummary(
                product.name(), product.price(), product.brand(), product.imageUrl());

        CartStorePort.AddResult result = cartStore.add(ownerUserId, productId, quantity, priceSnapshot,
                cartItemRules.maxQuantity(), product.availableStock());
        if (result.rejection() == CartStorePort.WriteRejection.MAX_QUANTITY) {
            throw new CartQuantityLimitExceededException(cartItemRules.maxQuantity());
        }
        if (result.rejection() == CartStorePort.WriteRejection.INSUFFICIENT_STOCK) {
            throw new InsufficientProductStockException(product.availableStock());
        }
        CartView updatedView = currentView.withItem(new CartView.Item(
                productId, result.item().quantity(), true, currentProduct));
        return new AddCartItemResult(updatedView, result.created());
    }
}