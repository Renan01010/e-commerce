package com.techstore.cart.adapter.config;

import com.techstore.cart.application.port.out.CartStorePort;
import com.techstore.cart.application.port.out.ProductCatalogPort;
import com.techstore.cart.application.service.AddCartItemService;
import com.techstore.cart.application.service.CartViewAssembler;
import com.techstore.cart.application.service.ClearCartService;
import com.techstore.cart.application.service.GetCartService;
import com.techstore.cart.application.service.RemoveCartItemService;
import com.techstore.cart.application.service.SetCartItemQuantityService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CartApplicationConfiguration {
    @Bean
    CartViewAssembler cartViewAssembler(ProductCatalogPort productCatalog) {
        return new CartViewAssembler(productCatalog);
    }

    @Bean
    GetCartService getCartService(CartStorePort cartStore, CartViewAssembler cartViewAssembler) {
        return new GetCartService(cartStore, cartViewAssembler);
    }

    @Bean
    AddCartItemService addCartItemService(CartStorePort cartStore, ProductCatalogPort productCatalog,
                                          CartViewAssembler cartViewAssembler) {
        return new AddCartItemService(cartStore, productCatalog, cartViewAssembler);
    }

    @Bean
    SetCartItemQuantityService setCartItemQuantityService(CartStorePort cartStore,
                                                          CartViewAssembler cartViewAssembler) {
        return new SetCartItemQuantityService(cartStore, cartViewAssembler);
    }

    @Bean
    RemoveCartItemService removeCartItemService(CartStorePort cartStore) {
        return new RemoveCartItemService(cartStore);
    }

    @Bean
    ClearCartService clearCartService(CartStorePort cartStore) {
        return new ClearCartService(cartStore);
    }
}