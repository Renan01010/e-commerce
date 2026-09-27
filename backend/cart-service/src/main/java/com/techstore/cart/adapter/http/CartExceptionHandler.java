package com.techstore.cart.adapter.http;

import com.techstore.cart.adapter.http.CartApiModels.ErrorResponse;
import com.techstore.cart.application.exception.CartItemNotFoundException;
import com.techstore.cart.application.exception.InvalidCartOwnerException;
import com.techstore.cart.application.exception.ProductCatalogUnavailableException;
import com.techstore.cart.application.exception.ProductNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class CartExceptionHandler {
    @ExceptionHandler({MethodArgumentNotValidException.class, MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class, IllegalArgumentException.class})
    ResponseEntity<ErrorResponse> badRequest(Exception exception, HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST, "Invalid request", request);
    }

    @ExceptionHandler({CartItemNotFoundException.class, ProductNotFoundException.class})
    ResponseEntity<ErrorResponse> notFound(RuntimeException exception, HttpServletRequest request) {
        return response(HttpStatus.NOT_FOUND, "Cart item or product not found", request);
    }

    @ExceptionHandler(ProductCatalogUnavailableException.class)
    ResponseEntity<ErrorResponse> catalogUnavailable(ProductCatalogUnavailableException exception,
                                                       HttpServletRequest request) {
        return response(HttpStatus.SERVICE_UNAVAILABLE, "Product Service unavailable", request);
    }

    @ExceptionHandler(InvalidCartOwnerException.class)
    ResponseEntity<ErrorResponse> invalidOwner(InvalidCartOwnerException exception, HttpServletRequest request) {
        return response(HttpStatus.UNAUTHORIZED, "Authentication required", request);
    }

    private ResponseEntity<ErrorResponse> response(HttpStatus status, String message, HttpServletRequest request) {
        Object requestCorrelationId = request.getAttribute(CartCorrelationIdFilter.REQUEST_ATTRIBUTE);
        String correlationId = requestCorrelationId instanceof String value
            ? value : request.getHeader(CartAuthenticationErrorWriter.CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isBlank()) correlationId = UUID.randomUUID().toString();
        return ResponseEntity.status(status)
                .header(CartAuthenticationErrorWriter.CORRELATION_ID_HEADER, correlationId)
                .body(new ErrorResponse(status.value(), message, null, correlationId,
                        LocalDateTime.now(ZoneOffset.UTC)));
    }
}