package com.techstore.cart.adapter.http;

import com.techstore.cart.adapter.http.CartApiModels.CartItemResponse;
import com.techstore.cart.adapter.http.CartApiModels.CartResponse;
import com.techstore.cart.adapter.http.CartApiModels.ProductSummaryResponse;
import com.techstore.cart.adapter.http.CartRequests.AddCartItemRequest;
import com.techstore.cart.adapter.http.CartRequests.SetCartItemQuantityRequest;
import com.techstore.cart.adapter.security.CartOwnerResolver;
import com.techstore.cart.application.model.CartView;
import com.techstore.cart.application.model.AddCartItemResult;
import com.techstore.cart.application.service.AddCartItemService;
import com.techstore.cart.application.service.RemoveCartItemService;
import com.techstore.cart.application.service.SetCartItemQuantityService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cart/items")
public class CartItemController {
    private static final Logger log = LoggerFactory.getLogger(CartItemController.class);

    private final AddCartItemService addCartItemService;
    private final SetCartItemQuantityService setCartItemQuantityService;
    private final RemoveCartItemService removeCartItemService;
    private final CartOwnerResolver cartOwnerResolver;

    public CartItemController(AddCartItemService addCartItemService,
                              SetCartItemQuantityService setCartItemQuantityService,
                              RemoveCartItemService removeCartItemService,
                              CartOwnerResolver cartOwnerResolver) {
        this.addCartItemService = addCartItemService;
        this.setCartItemQuantityService = setCartItemQuantityService;
        this.removeCartItemService = removeCartItemService;
        this.cartOwnerResolver = cartOwnerResolver;
    }

    @PostMapping
        @Operation(summary = "Adicionar produto ou somar quantidade existente")
        @ApiResponses({
                @ApiResponse(responseCode = "200", description = "Quantidade consolidada; carrinho atualizado",
                    content = @Content(schema = @Schema(implementation = CartResponse.class))),
                @ApiResponse(responseCode = "201", description = "Nova linha criada; carrinho atualizado",
                    content = @Content(schema = @Schema(implementation = CartResponse.class))),
            @ApiResponse(responseCode = "400", description = "Request inválido"),
            @ApiResponse(responseCode = "401", description = "JWT ausente, inválido ou expirado"),
            @ApiResponse(responseCode = "404", description = "Produto inexistente ou inativo"),
            @ApiResponse(responseCode = "503", description = "Product Service indisponível")
        })
    public ResponseEntity<CartResponse> add(@Valid @RequestBody AddCartItemRequest request,
                                            Authentication authentication) {
        UUID ownerUserId = cartOwnerResolver.resolve(authentication);
        AddCartItemResult result = addCartItemService.add(ownerUserId, request.productId(), request.quantity());
        log.info("cart_operation=item_added ownerUserId={} productId={} quantityAdded={} quantityTotal={} created={}",
            ownerUserId, request.productId(), request.quantity(),
            result.cart().items().stream().filter(item -> item.productId().equals(request.productId()))
                .findFirst().orElseThrow().quantity(), result.created());
        HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(cartResponse(result.cart()));
    }

    @PutMapping("/{productId}")
        @Operation(summary = "Substituir quantidade de item próprio")
        @ApiResponses({
                @ApiResponse(responseCode = "200", description = "Quantidade substituída; carrinho atualizado",
                    content = @Content(schema = @Schema(implementation = CartResponse.class))),
            @ApiResponse(responseCode = "400", description = "Request inválido"),
            @ApiResponse(responseCode = "401", description = "JWT ausente, inválido ou expirado"),
            @ApiResponse(responseCode = "404", description = "Item ou produto ausente/inativo"),
            @ApiResponse(responseCode = "503", description = "Product Service indisponível")
        })
    public CartResponse setQuantity(@PathVariable UUID productId,
                                    @Valid @RequestBody SetCartItemQuantityRequest request,
                                    Authentication authentication) {
        UUID ownerUserId = cartOwnerResolver.resolve(authentication);
        CartView updatedView = setCartItemQuantityService.setQuantity(ownerUserId, productId, request.quantity());
        log.info("cart_operation=item_quantity_replaced ownerUserId={} productId={} quantity={}",
            ownerUserId, productId, request.quantity());
        return cartResponse(updatedView);
    }

    @DeleteMapping("/{productId}")
        @Operation(summary = "Remover item próprio")
        @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Item removido"),
            @ApiResponse(responseCode = "401", description = "JWT ausente, inválido ou expirado"),
            @ApiResponse(responseCode = "404", description = "Item não pertence ao carrinho")
        })
    public ResponseEntity<Void> remove(@PathVariable UUID productId, Authentication authentication) {
        UUID ownerUserId = cartOwnerResolver.resolve(authentication);
        removeCartItemService.remove(ownerUserId, productId);
        log.info("cart_operation=item_removed ownerUserId={} productId={}", ownerUserId, productId);
        return ResponseEntity.noContent().build();
    }

    private CartResponse cartResponse(CartView view) {
        return new CartResponse(view.items().stream().map(this::toResponse).toList());
    }

    private CartItemResponse toResponse(CartView.Item item) {
        CartView.ProductSummary summary = item.product();
        ProductSummaryResponse product = summary == null ? null : new ProductSummaryResponse(
                summary.name(), summary.price(), summary.brand(), summary.imageUrl());
        return new CartItemResponse(item.productId(), item.quantity(), item.available(), product);
    }
}