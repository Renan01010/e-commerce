package com.techstore.product.adapter.config;

import com.techstore.product.adapter.http.SecurityErrorResponseWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, SecurityErrorResponseWriter errors) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                    .requestMatchers(HttpMethod.GET, "/api/**").permitAll()
                    .requestMatchers(HttpMethod.OPTIONS, "/api/**").permitAll()
                    .requestMatchers("/api/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/actuator/health/**", "/actuator/metrics/**", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .anyRequest().authenticated())
                    .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, cause) -> errors.write(request, response, 401, "Authentication required"))
                        .accessDeniedHandler((request, response, cause) -> errors.write(request, response, 403, "Insufficient permissions")))
                .oauth2ResourceServer(resource -> resource
                    .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                    .authenticationEntryPoint((request, response, cause) -> errors.write(request, response, 401, "Authentication required"))
                    .accessDeniedHandler((request, response, cause) -> errors.write(request, response, 403, "Insufficient permissions")))
                .build();
    }

    @Bean
    JwtDecoder jwtDecoder(@Value("${techstore.jwt.secret}") String secret) {
        byte[] key = secret.getBytes(StandardCharsets.UTF_8);
        if (key.length < 32) throw new IllegalStateException("TECHSTORE_JWT_SECRET must contain at least 32 bytes");
        return NimbusJwtDecoder.withSecretKey(new SecretKeySpec(key, "HmacSHA256")).build();
    }

    private Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Object roles = jwt.getClaims().get("roles");
            Collection<GrantedAuthority> authorities = new ArrayList<>();
            if (roles instanceof Collection<?> values) {
                values.stream().filter(String.class::isInstance).map(String.class::cast)
                        .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                        .map(SimpleGrantedAuthority::new).forEach(authorities::add);
            }
            return authorities;
        });
        return converter;
    }
}