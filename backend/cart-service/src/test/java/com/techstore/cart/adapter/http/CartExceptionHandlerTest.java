package com.techstore.cart.adapter.http;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.techstore.cart.application.exception.CartItemNotFoundException;
import com.techstore.cart.application.exception.CartQuantityLimitExceededException;
import com.techstore.cart.application.exception.InsufficientProductStockException;
import com.techstore.cart.application.exception.ProductCatalogUnavailableException;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(CartExceptionHandlerTest.FailingEndpoint.class)
@Import({CartExceptionHandler.class, CartExceptionHandlerTest.FailingEndpoint.class})
@WithMockUser
class CartExceptionHandlerTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void mapsValidationFailureToBadRequest() throws Exception {
        mockMvc.perform(get("/test/validation"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void mapsMissingItemToNotFound() throws Exception {
        mockMvc.perform(get("/test/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void mapsCatalogFailureToServiceUnavailable() throws Exception {
        mockMvc.perform(get("/test/catalog"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503));
    }

            @Test
            void mapsCartRuleConflictsToConflict() throws Exception {
            mockMvc.perform(get("/test/quantity-limit"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
            mockMvc.perform(get("/test/stock"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
            }

    @RestController
    public static class FailingEndpoint {
        @GetMapping("/test/validation")
        public void validation() {
            throw new IllegalArgumentException("invalid request");
        }

        @GetMapping("/test/missing")
        public void missing() {
            throw new CartItemNotFoundException(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"));
        }

        @GetMapping("/test/catalog")
        public void catalog() {
            throw new ProductCatalogUnavailableException("catalog unavailable");
        }

        @GetMapping("/test/quantity-limit")
        public void quantityLimit() {
            throw new CartQuantityLimitExceededException(99);
        }

        @GetMapping("/test/stock")
        public void stock() {
            throw new InsufficientProductStockException(3);
        }
    }
}