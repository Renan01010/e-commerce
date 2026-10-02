package com.techstore.cart.adapter.http;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.techstore.cart.adapter.config.OpenApiConfiguration;
import com.techstore.cart.adapter.security.CartOwnerResolver;
import com.techstore.cart.adapter.security.SecurityConfig;
import com.techstore.cart.application.service.AddCartItemService;
import com.techstore.cart.application.service.ClearCartService;
import com.techstore.cart.application.service.GetCartService;
import com.techstore.cart.application.service.RemoveCartItemService;
import com.techstore.cart.application.service.SetCartItemQuantityService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springdoc.core.configuration.SpringDocConfiguration;
import org.springdoc.webmvc.core.configuration.SpringDocWebMvcConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({CartController.class, CartQueryController.class, CartItemController.class})
@ImportAutoConfiguration({SpringDocConfiguration.class, SpringDocWebMvcConfiguration.class})
@EnableConfigurationProperties(SpringDocConfigProperties.class)
@Import({OpenApiConfiguration.class, SecurityConfig.class, CartOwnerResolver.class,
        CartExceptionHandler.class, CartAuthenticationErrorWriter.class})
@TestPropertySource(properties = "techstore.jwt.secret=01234567890123456789012345678901")
class CartOpenApiContractTest {
    private static final UUID OWNER_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440010");

    @Autowired
    private MockMvc mockMvc;

    @MockBean private AddCartItemService addCartItemService;
    @MockBean private SetCartItemQuantityService setCartItemQuantityService;
    @MockBean private RemoveCartItemService removeCartItemService;
    @MockBean private GetCartService getCartService;
    @MockBean private ClearCartService clearCartService;

    @Test
    void publishesGatewayPathsAndBearerAuthentication() throws Exception {
        mockMvc.perform(get("/v3/api-docs").with(jwt().jwt(token -> token.subject(OWNER_ID.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.servers[0].url").value("http://localhost:8080/api"))
                .andExpect(jsonPath("$.paths['/cart'].get").exists())
                .andExpect(jsonPath("$.paths['/cart'].get.responses['200']").exists())
                .andExpect(jsonPath("$.paths['/cart'].get.responses['401']").exists())
                .andExpect(jsonPath("$.paths['/cart'].get.responses['503']").exists())
                .andExpect(jsonPath("$.paths['/cart'].delete").exists())
                .andExpect(jsonPath("$.paths['/cart'].delete.responses['204']").exists())
                .andExpect(jsonPath("$.paths['/cart/items'].post").exists())
                .andExpect(jsonPath("$.paths['/cart/items'].post.responses['200']").exists())
                .andExpect(jsonPath("$.paths['/cart/items'].post.responses['201']").exists())
                .andExpect(jsonPath("$.paths['/cart/items'].post.responses['400']").exists())
                .andExpect(jsonPath("$.paths['/cart/items'].post.responses['404']").exists())
                .andExpect(jsonPath("$.paths['/cart/items'].post.responses['409']").exists())
                .andExpect(jsonPath("$.paths['/cart/items'].post.requestBody.content['application/json'].schema.$ref").exists())
                .andExpect(jsonPath("$.paths['/cart/items/{productId}'].put").exists())
                .andExpect(jsonPath("$.paths['/cart/items/{productId}'].put.responses['200']").exists())
                .andExpect(jsonPath("$.paths['/cart/items/{productId}'].put.responses['404']").exists())
                .andExpect(jsonPath("$.paths['/cart/items/{productId}'].put.responses['409']").exists())
                .andExpect(jsonPath("$.paths['/cart/items/{productId}'].delete").exists())
                .andExpect(jsonPath("$.paths['/cart/items/{productId}'].delete.responses['204']").exists())
                .andExpect(jsonPath("$.components.securitySchemes.BearerAuth.scheme").value("bearer"))
                .andExpect(jsonPath("$.components.schemas.CartResponse").exists())
                .andExpect(jsonPath("$.components.schemas.CartResponse.properties.maxItemQuantity").exists())
                .andExpect(jsonPath("$.components.schemas.CartResponse.properties.total").exists())
                .andExpect(jsonPath("$.components.schemas.CartResponse.properties.totalAvailable").exists())
                .andExpect(jsonPath("$.components.schemas.CartItemResponse.properties.unitPriceSnapshot").exists())
                .andExpect(jsonPath("$.components.schemas.CartItemResponse.properties.priceAvailable").exists())
                .andExpect(jsonPath("$.components.schemas.CartItemResponse.properties.subtotal").exists())
                .andExpect(jsonPath("$.security[0].BearerAuth").isArray());
    }
}