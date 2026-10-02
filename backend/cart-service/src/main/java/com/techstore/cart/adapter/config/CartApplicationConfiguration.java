package com.techstore.cart.adapter.config;

import com.techstore.cart.application.port.out.CartStorePort;
import com.techstore.cart.application.port.out.ProductCatalogPort;
import com.techstore.cart.application.service.AddCartItemService;
import com.techstore.cart.application.service.CartViewAssembler;
import com.techstore.cart.application.service.ClearCartService;
import com.techstore.cart.application.service.GetCartService;
import com.techstore.cart.application.service.InitializeLegacyPriceSnapshotsService;
import com.techstore.cart.application.service.RemoveCartItemService;
import com.techstore.cart.application.service.SetCartItemQuantityService;
import com.techstore.cart.domain.CartItemRules;
import org.springframework.boot.ApplicationRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CartApplicationConfiguration {
    @Bean
    CartItemRules cartItemRules(@Value("${techstore.cart.max-item-quantity:99}") int maxQuantity) {
        return new CartItemRules(maxQuantity);
    }

    @Bean
    CartViewAssembler cartViewAssembler(ProductCatalogPort productCatalog, CartItemRules cartItemRules) {
        return new CartViewAssembler(productCatalog, cartItemRules);
    }

    @Bean
    GetCartService getCartService(CartStorePort cartStore, CartViewAssembler cartViewAssembler) {
        return new GetCartService(cartStore, cartViewAssembler);
    }

    @Bean
    AddCartItemService addCartItemService(CartStorePort cartStore, ProductCatalogPort productCatalog,
                                          CartViewAssembler cartViewAssembler, CartItemRules cartItemRules) {
        return new AddCartItemService(cartStore, productCatalog, cartViewAssembler, cartItemRules);
    }

    @Bean
    SetCartItemQuantityService setCartItemQuantityService(CartStorePort cartStore,
                                                          ProductCatalogPort productCatalog,
                                                          CartViewAssembler cartViewAssembler,
                                                          CartItemRules cartItemRules) {
        return new SetCartItemQuantityService(cartStore, productCatalog, cartViewAssembler, cartItemRules);
    }

    @Bean
    RemoveCartItemService removeCartItemService(CartStorePort cartStore) {
        return new RemoveCartItemService(cartStore);
    }

    @Bean
    ClearCartService clearCartService(CartStorePort cartStore) {
        return new ClearCartService(cartStore);
    }

    @Bean
    InitializeLegacyPriceSnapshotsService initializeLegacyPriceSnapshotsService(
            CartStorePort cartStore, ProductCatalogPort productCatalog) {
        return new InitializeLegacyPriceSnapshotsService(cartStore, productCatalog);
    }

    @Bean
    ApplicationRunner initializeLegacyPriceSnapshots(InitializeLegacyPriceSnapshotsService initializer) {
        return args -> initializer.initialize();
    }
}