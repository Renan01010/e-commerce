package com.techstore.cart.adapter.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {
    @Bean
    OpenAPI cartOpenApi() {
        return new OpenAPI()
                .info(new Info().title("API do Serviço de Carrinho - TechStore")
                        .description("Contrato REST do carrinho autenticado.")
                        .version("1.0.0"))
                .servers(List.of(new Server().url("http://localhost:8080/api").description("Gateway local")))
                .components(new Components().addSecuritySchemes("BearerAuth",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("BearerAuth"));
    }

    @Bean
    OpenApiCustomizer exposeGatewayRelativePaths() {
        return openApi -> {
            Paths paths = openApi.getPaths();
            if (paths == null) return;
            List<Map.Entry<String, io.swagger.v3.oas.models.PathItem>> entries = new ArrayList<>(paths.entrySet());
            paths.clear();
            for (Map.Entry<String, io.swagger.v3.oas.models.PathItem> entry : entries) {
                String path = entry.getKey();
                String publicPath = path.startsWith("/api/") ? path.substring("/api".length()) : path;
                paths.addPathItem(publicPath, entry.getValue());
            }
        };
    }
}