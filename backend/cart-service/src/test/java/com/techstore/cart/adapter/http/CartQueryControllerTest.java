package com.techstore.cart.adapter.http;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.techstore.cart.adapter.security.CartOwnerResolver;
import com.techstore.cart.adapter.security.SecurityConfig;
import com.techstore.cart.application.model.CartView;
import com.techstore.cart.application.service.GetCartService;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CartQueryController.class)
@Import({SecurityConfig.class, CartOwnerResolver.class, CartExceptionHandler.class,
        CartAuthenticationErrorWriter.class})
@TestPropertySource(properties = "techstore.jwt.secret=01234567890123456789012345678901")
class CartQueryControllerTest {
    private static final UUID OWNER_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440010");
    private static final UUID PRODUCT_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GetCartService getCartService;

    @Test
    void returnsEmptyCartForAuthenticatedOwner() throws Exception {
        when(getCartService.getCart(OWNER_ID)).thenReturn(new CartView(List.of()));

        mockMvc.perform(get("/api/cart").with(jwt().jwt(token -> token.subject(OWNER_ID.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items").isEmpty());
    }

    @Test
    void returnsCurrentProductSummary() throws Exception {
        when(getCartService.getCart(OWNER_ID)).thenReturn(new CartView(List.of(
                new CartView.Item(PRODUCT_ID, 2, true,
                        new CartView.ProductSummary("Keyboard", new BigDecimal("49.90"), "Acme", null)))));

        mockMvc.perform(get("/api/cart").with(jwt().jwt(token -> token.subject(OWNER_ID.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].productId").value(PRODUCT_ID.toString()))
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andExpect(jsonPath("$.items[0].available").value(true))
                .andExpect(jsonPath("$.items[0].product.name").value("Keyboard"))
                .andExpect(jsonPath("$.items[0].product.price").value(49.90));
    }

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/cart")).andExpect(status().isUnauthorized());
    }
}