package com.techstore.product.adapter.http;

import static com.techstore.product.adapter.http.ApiModels.ErrorResponse;

import com.techstore.product.application.service.ConflictException;
import com.techstore.product.application.service.NotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.stream.Collectors;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(NotFoundException.class)
    ResponseEntity<ErrorResponse> notFound(NotFoundException exception, HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, exception.getMessage(), null, request);
    }

    @ExceptionHandler({ConflictException.class, DataIntegrityViolationException.class})
    ResponseEntity<ErrorResponse> conflict(Exception exception, HttpServletRequest request) {
        String message = exception instanceof ConflictException ? exception.getMessage() : "A conflicting catalog record already exists";
        return error(HttpStatus.CONFLICT, message, null, request);
    }

    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentTypeMismatchException.class, HttpMessageNotReadableException.class})
    ResponseEntity<ErrorResponse> badRequest(Exception exception, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "Invalid request", exception.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> invalidBody(MethodArgumentNotValidException exception, HttpServletRequest request) {
        String details = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return error(HttpStatus.BAD_REQUEST, "Request validation failed", details, request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ErrorResponse> invalidParameters(ConstraintViolationException exception, HttpServletRequest request) {
        String details = exception.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .collect(Collectors.joining("; "));
        return error(HttpStatus.BAD_REQUEST, "Request validation failed", details, request);
    }

    private ResponseEntity<ErrorResponse> error(HttpStatus status, String message, String details, HttpServletRequest request) {
        String correlationId = MDC.get("correlationId");
        if (correlationId == null) correlationId = request.getHeader("X-Correlation-ID");
        return ResponseEntity.status(status).body(new ErrorResponse(status.value(), message, details,
            correlationId, LocalDateTime.now(ZoneOffset.UTC)));
    }
}