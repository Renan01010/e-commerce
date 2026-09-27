package com.techstore.cart.adapter.product;

import com.techstore.cart.application.exception.ProductCatalogUnavailableException;
import com.techstore.cart.application.port.out.ProductCatalogPort;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class ProductCatalogHttpAdapter implements ProductCatalogPort {
    private final RestClient productService;

    public ProductCatalogHttpAdapter(RestClient productCatalogRestClient) {
        this.productService = productCatalogRestClient;
    }

    @Override
    public Optional<ProductSummary> findActiveById(UUID productId) {
        try {
            ProductResponse product = productService.get()
                    .uri("/api/products/{id}", productId)
                    .retrieve()
                    .body(ProductResponse.class);
            if (product == null) {
                throw new ProductCatalogUnavailableException("Product Service returned an empty response");
            }
            if (!product.isActive()) return Optional.empty();
            return Optional.of(new ProductSummary(product.name(), product.price(), product.brand(), product.imageUrl()));
        } catch (HttpClientErrorException.NotFound exception) {
            return Optional.empty();
        } catch (RestClientException exception) {
            throw new ProductCatalogUnavailableException("Product Service request failed", exception);
        }
    }
}