package com.techstore.cart.adapter.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.techstore.cart.application.port.out.CartStorePort;
import com.techstore.cart.application.port.out.ProductCatalogPort;
import com.techstore.cart.application.service.InitializeLegacyPriceSnapshotsService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

class CartApplicationConfigurationTest {
    @Test
    void registersOneInitializerAndOneRunnerThatInvokesItOnce() {
        CartStorePort cartStore = mock(CartStorePort.class);
        ProductCatalogPort productCatalog = mock(ProductCatalogPort.class);
        when(cartStore.findPendingPriceSnapshots()).thenReturn(List.of());

        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(CartApplicationConfiguration.class)
                .web(WebApplicationType.NONE)
                .initializers(applicationContext -> {
                    applicationContext.getBeanFactory().registerSingleton("cartStorePort", cartStore);
                    applicationContext.getBeanFactory().registerSingleton("productCatalogPort", productCatalog);
                })
                .run("--techstore.cart.max-item-quantity=99")) {
            assertThat(context.getBeansOfType(InitializeLegacyPriceSnapshotsService.class)).hasSize(1);
            assertThat(context.getBeansOfType(org.springframework.boot.ApplicationRunner.class)).hasSize(1);
            verify(cartStore, times(1)).findPendingPriceSnapshots();
        }
    }
}
