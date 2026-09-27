package com.techstore.cart.adapter.http;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.techstore.cart.adapter.security.CartOwnerResolver;
import com.techstore.cart.adapter.security.SecurityConfig;
import com.techstore.cart.application.model.AddCartItemResult;
import com.techstore.cart.application.model.CartView;
import com.techstore.cart.application.service.AddCartItemService;
import com.techstore.cart.application.service.RemoveCartItemService;
import com.techstore.cart.application.service.SetCartItemQuantityService;
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

@WebMvcTest(CartItemController.class)
@Import({SecurityConfig.class, CartOwnerResolver.class, CartExceptionHandler.class,
        CartAuthenticationErrorWriter.class})
@TestPropertySource(properties = "techstore.jwt.secret=01234567890123456789012345678901")
class CartItemControllerTest {
    private static final UUID OWNER_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440010");
    private static final UUID PRODUCT_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

    @Autowired
    private MockMvc mockMvc;

    @MockBean private AddCartItemService addCartItemService;
    @MockBean private SetCartItemQuantityService setCartItemQuantityService;
    @MockBean private RemoveCartItemService removeCartItemService;

    @Test
    void returnsCreatedForNewLineAndUpdatedCart() throws Exception {
        when(addCartItemService.add(OWNER_ID, PRODUCT_ID, 2))
                .thenReturn(new AddCartItemResult(view(2), true));

        mockMvc.perform(post("/api/cart/items").with(jwt().jwt(token -> token.subject(OWNER_ID.toString())))
                        .contentType(APPLICATION_JSON)
                        .content("""{"productId":"550e8400-e29b-41d4-a716-446655440000","quantity":2}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.items[0].quantity").value(2));
    }

    @Test
    void returnsOkWhenAddConsolidatesExistingLine() throws Exception {
        when(addCartItemService.add(OWNER_ID, PRODUCT_ID, 2))
                .thenReturn(new AddCartItemResult(view(5), false));

        mockMvc.perform(post("/api/cart/items").with(jwt().jwt(token -> token.subject(OWNER_ID.toString())))
                        .contentType(APPLICATION_JSON)
                        .content("""{"productId":"550e8400-e29b-41d4-a716-446655440000","quantity":2}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].quantity").value(5));
    }

    @Test
    void rejectsUnknownOwnerFieldAndInvalidQuantity() throws Exception {
        mockMvc.perform(post("/api/cart/items").with(jwt().jwt(token -> token.subject(OWNER_ID.toString())))
                        .contentType(APPLICATION_JSON)
                        .content("""{"productId":"550e8400-e29b-41d4-a716-446655440000","quantity":1,"userId":"550e8400-e29b-41d4-a716-446655440010"}"""))
                .andExpect(status().isBadRequest());
        mockMvc.perform(put("/api/cart/items/{productId}", PRODUCT_ID)
                        .with(jwt().jwt(token -> token.subject(OWNER_ID.toString())))
                        .contentType(APPLICATION_JSON)
                        .content("""{"quantity":0}"""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updatesAndRemovesOwnedItem() throws Exception {
        when(setCartItemQuantityService.setQuantity(OWNER_ID, PRODUCT_ID, 4)).thenReturn(view(4));

        mockMvc.perform(put("/api/cart/items/{productId}", PRODUCT_ID)
                        .with(jwt().jwt(token -> token.subject(OWNER_ID.toString())))
                        .contentType(APPLICATION_JSON).content("""{"quantity":4}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].quantity").value(4));

        doNothing().when(removeCartItemService).remove(OWNER_ID, PRODUCT_ID);
        mockMvc.perform(delete("/api/cart/items/{productId}", PRODUCT_ID)
                        .with(jwt().jwt(token -> token.subject(OWNER_ID.toString()))))
                .andExpect(status().isNoContent());
    }

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/cart/items").contentType(APPLICATION_JSON)
                        .content("""{"productId":"550e8400-e29b-41d4-a716-446655440000","quantity":1}"""))
                .andExpect(status().isUnauthorized());
    }

    private static CartView view(int quantity) {
        return new CartView(List.of(new CartView.Item(PRODUCT_ID, quantity, true,
                new CartView.ProductSummary("Keyboard", new BigDecimal("49.90"), "Acme", null))));
    }
}