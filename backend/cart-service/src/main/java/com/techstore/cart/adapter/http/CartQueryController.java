package com.techstore.cart.adapter.http;

import com.techstore.cart.adapter.http.CartApiModels.CartItemResponse;
import com.techstore.cart.adapter.http.CartApiModels.CartResponse;
import com.techstore.cart.adapter.http.CartApiModels.ProductSummaryResponse;
import com.techstore.cart.adapter.security.CartOwnerResolver;
import com.techstore.cart.application.model.CartView;
import com.techstore.cart.application.service.GetCartService;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cart")
public class CartQueryController {
    private static final Logger log = LoggerFactory.getLogger(CartQueryController.class);

    private final GetCartService getCartService;
    private final CartOwnerResolver cartOwnerResolver;

    public CartQueryController(GetCartService getCartService, CartOwnerResolver cartOwnerResolver) {
        this.getCartService = getCartService;
        this.cartOwnerResolver = cartOwnerResolver;
    }

    @GetMapping
        @Operation(summary = "Consultar carrinho do usuário autenticado")
        @ApiResponses({
                @ApiResponse(responseCode = "200", description = "Carrinho atual, possivelmente vazio",
                    content = @Content(schema = @Schema(implementation = CartResponse.class))),
            @ApiResponse(responseCode = "401", description = "JWT ausente, inválido ou expirado"),
            @ApiResponse(responseCode = "503", description = "Product Service indisponível")
        })
    public CartResponse getCart(Authentication authentication) {
        UUID ownerUserId = cartOwnerResolver.resolve(authentication);
        CartView view = getCartService.getCart(ownerUserId);
        log.info("cart_operation=read ownerUserId={} itemCount={}", ownerUserId, view.items().size());
        return new CartResponse(view.items().stream().map(this::toResponse).toList());
    }

    private CartItemResponse toResponse(CartView.Item item) {
        CartView.ProductSummary summary = item.product();
        ProductSummaryResponse product = summary == null ? null : new ProductSummaryResponse(
                summary.name(), summary.price(), summary.brand(), summary.imageUrl());
        return new CartItemResponse(item.productId(), item.quantity(), item.available(), product);
    }
}