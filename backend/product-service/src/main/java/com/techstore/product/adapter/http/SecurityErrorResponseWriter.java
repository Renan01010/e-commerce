package com.techstore.product.adapter.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Component
public class SecurityErrorResponseWriter {
    private final ObjectMapper objectMapper;

    public SecurityErrorResponseWriter(ObjectMapper objectMapper) { this.objectMapper = objectMapper; }

    public void write(HttpServletRequest request, HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        Object attribute = request.getAttribute(CorrelationIdFilter.HEADER);
        String correlationId = attribute instanceof String value ? value : request.getHeader(CorrelationIdFilter.HEADER);
        if (correlationId == null || correlationId.isBlank()) correlationId = UUID.randomUUID().toString();
        response.setHeader(CorrelationIdFilter.HEADER, correlationId);
        objectMapper.writeValue(response.getOutputStream(), new ApiModels.ErrorResponse(status, message,
            null, correlationId, LocalDateTime.now(ZoneOffset.UTC)));
    }
}