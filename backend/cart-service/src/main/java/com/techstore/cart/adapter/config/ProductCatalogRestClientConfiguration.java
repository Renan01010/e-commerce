package com.techstore.cart.adapter.config;

import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class ProductCatalogRestClientConfiguration {
    @Bean
    RestClient productCatalogRestClient(
            RestClient.Builder builder,
            @Value("${techstore.product-service.url}") String productServiceUrl,
            @Value("${techstore.product-service.connect-timeout}") Duration connectTimeout,
            @Value("${techstore.product-service.read-timeout}") Duration readTimeout) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(connectTimeout)
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(readTimeout);

        return builder
                .baseUrl(productServiceUrl)
                .requestFactory(requestFactory)
                .build();
    }
}