package com.techstore.cart.adapter.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
public class CartAuthenticationErrorWriter implements AuthenticationEntryPoint {
    public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";

    private final ObjectMapper objectMapper;

    public CartAuthenticationErrorWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException exception) throws IOException {
        Object requestCorrelationId = request.getAttribute(CartCorrelationIdFilter.REQUEST_ATTRIBUTE);
        String correlationId = requestCorrelationId instanceof String value
            ? value : request.getHeader(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isBlank()) correlationId = UUID.randomUUID().toString();
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader(CORRELATION_ID_HEADER, correlationId);
        objectMapper.writeValue(response.getOutputStream(), new CartApiModels.ErrorResponse(
                HttpServletResponse.SC_UNAUTHORIZED, "Authentication required", null, correlationId,
                LocalDateTime.now(ZoneOffset.UTC)));
    }
}