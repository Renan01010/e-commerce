package com.techstore.cart.adapter.http;

import com.techstore.cart.adapter.security.CartOwnerResolver;
import com.techstore.cart.application.service.ClearCartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cart")
public class CartController {
    private static final Logger log = LoggerFactory.getLogger(CartController.class);

    private final ClearCartService clearCartService;
    private final CartOwnerResolver cartOwnerResolver;

    public CartController(ClearCartService clearCartService, CartOwnerResolver cartOwnerResolver) {
        this.clearCartService = clearCartService;
        this.cartOwnerResolver = cartOwnerResolver;
    }

    @DeleteMapping
        @Operation(summary = "Limpar carrinho do usuário autenticado")
        @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Carrinho limpo ou já vazio"),
            @ApiResponse(responseCode = "401", description = "JWT ausente, inválido ou expirado")
        })
    public ResponseEntity<Void> clear(Authentication authentication) {
        UUID ownerUserId = cartOwnerResolver.resolve(authentication);
        clearCartService.clear(ownerUserId);
        log.info("cart_operation=cleared ownerUserId={}", ownerUserId);
        return ResponseEntity.noContent().build();
    }
}