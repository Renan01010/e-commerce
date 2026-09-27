package com.techstore.product.adapter.http;

import static com.techstore.product.adapter.http.ApiModels.*;

import com.techstore.product.application.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/categories")
@Tag(name = "Categorias")
public class CategoryController {
    private final CategoryService categories;

    public CategoryController(CategoryService categories) { this.categories = categories; }

    @GetMapping
    @Operation(summary = "Listar categorias ativas")
    public List<CategoryResponse> list() { return categories.list().stream().map(CategoryResponse::from).toList(); }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar categoria")
    public CategoryResponse get(@PathVariable(name = "id") UUID id) { return CategoryResponse.from(categories.get(id)); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Criar categoria (ADMIN)")
    @SecurityRequirement(name = "bearerAuth")
    public CategoryResponse create(@Valid @RequestBody CreateCategoryRequest request, Authentication authentication) {
        return CategoryResponse.from(categories.create(request.name(), request.description(),
                request.parentCategoryId(), authentication.getName()));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar categoria (ADMIN)")
    @SecurityRequirement(name = "bearerAuth")
    public CategoryResponse update(@PathVariable(name = "id") UUID id, @Valid @RequestBody UpdateCategoryRequest request,
                                   Authentication authentication) {
        return CategoryResponse.from(categories.update(id, request.name(), request.description(),
                request.displayOrder(), null, authentication.getName()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Desativar categoria (ADMIN)")
    @SecurityRequirement(name = "bearerAuth")
    public void deactivate(@PathVariable(name = "id") UUID id, Authentication authentication) {
        categories.deactivate(id, authentication.getName());
    }
}