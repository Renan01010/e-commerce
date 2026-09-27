package com.techstore.cart.adapter.http;

import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.techstore.cart.adapter.security.CartOwnerResolver;
import com.techstore.cart.adapter.security.SecurityConfig;
import com.techstore.cart.application.service.ClearCartService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CartController.class)
@Import({SecurityConfig.class, CartOwnerResolver.class, CartExceptionHandler.class,
        CartAuthenticationErrorWriter.class})
@TestPropertySource(properties = "techstore.jwt.secret=01234567890123456789012345678901")
class CartControllerTest {
    private static final UUID OWNER_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440010");

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ClearCartService clearCartService;

    @Test
    void clearsCartAndReturnsNoContentForValidOwner() throws Exception {
        mockMvc.perform(delete("/api/cart").with(jwt().jwt(token -> token.subject(OWNER_ID.toString()))))
                .andExpect(status().isNoContent());

        verify(clearCartService).clear(OWNER_ID);
    }

    @Test
    void requiresAuthenticationToClearCart() throws Exception {
        mockMvc.perform(delete("/api/cart")).andExpect(status().isUnauthorized());
    }
}