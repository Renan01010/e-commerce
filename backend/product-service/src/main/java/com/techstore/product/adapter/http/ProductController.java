package com.techstore.product.adapter.http;

import static com.techstore.product.adapter.http.ApiModels.*;

import com.techstore.product.application.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
@Validated
@Tag(name = "Produtos")
public class ProductController {
    private final ProductService products;

    public ProductController(ProductService products) { this.products = products; }

    @GetMapping
    @Operation(summary = "Listar, pesquisar, filtrar e ordenar produtos")
    public ProductPageResponse list(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) @DecimalMin("0.00") BigDecimal minPrice,
            @RequestParam(required = false) @DecimalMin("0.00") BigDecimal maxPrice,
            @RequestParam(required = false) Boolean inStock,
            @RequestParam(required = false) String brand,
            @RequestParam(defaultValue = "relevance") @Pattern(regexp = "relevance|price|name|newest") String sortBy,
            @RequestParam(defaultValue = "desc") @Pattern(regexp = "asc|desc") String sortOrder,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize,
            Authentication authentication) {
        var result = products.search(query, categoryId, minPrice, maxPrice, inStock, brand,
                sortBy, sortOrder, page, pageSize);
        return ProductPageResponse.from(result, isAdmin(authentication));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar detalhes de produto ativo")
    public ProductResponse get(@PathVariable UUID id, Authentication authentication) {
        return ProductResponse.from(products.get(id), isAdmin(authentication));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Criar produto (ADMIN)")
    @SecurityRequirement(name = "bearerAuth")
    public ProductResponse create(@Valid @RequestBody CreateProductRequest request, Authentication authentication) {
        var product = products.create(request.name(), request.description(), request.price(), request.cost(),
                request.brand(), request.sku(), request.categoryId(), request.quantity(), request.imageUrl(), actor(authentication));
        return ProductResponse.from(product, true);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar produto (ADMIN)")
    @SecurityRequirement(name = "bearerAuth")
    public ProductResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateProductRequest request,
                                  Authentication authentication) {
        var product = products.update(id, request.name(), request.description(), request.price(), request.cost(),
                request.brand(), request.quantity(), request.imageUrl(), actor(authentication));
        return ProductResponse.from(product, true);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Desativar produto (ADMIN)")
    @SecurityRequirement(name = "bearerAuth")
    public void deactivate(@PathVariable UUID id, Authentication authentication) {
        products.deactivate(id, actor(authentication));
    }

    private static String actor(Authentication authentication) { return authentication.getName(); }
    private static boolean isAdmin(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
    }
}