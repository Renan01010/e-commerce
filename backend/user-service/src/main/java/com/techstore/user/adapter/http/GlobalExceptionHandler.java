package com.techstore.user.adapter.http;

import com.techstore.user.application.exception.DuplicateEmailException;
import com.techstore.user.application.exception.InvalidCredentialsException;
import com.techstore.user.application.exception.LastActiveAdminException;
import com.techstore.user.application.exception.UserNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(InvalidCredentialsException.class)
    ResponseEntity<ErrorResponse> invalidCredentials(HttpServletRequest request) {
        return error(HttpStatus.UNAUTHORIZED, "Invalid email or password", null, request);
    }

    @ExceptionHandler(DuplicateEmailException.class)
    ResponseEntity<ErrorResponse> duplicateEmail(HttpServletRequest request) {
        return error(HttpStatus.CONFLICT, "Email already registered", null, request);
    }

    @ExceptionHandler(LastActiveAdminException.class)
    ResponseEntity<ErrorResponse> lastAdmin(LastActiveAdminException exception, HttpServletRequest request) {
        return error(HttpStatus.CONFLICT, exception.getMessage(), null, request);
    }

    @ExceptionHandler(UserNotFoundException.class)
    ResponseEntity<ErrorResponse> notFound(HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, "User not found", null, request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ErrorResponse> integrityConflict(HttpServletRequest request) {
        return error(HttpStatus.CONFLICT, "A conflicting user record already exists", null, request);
    }

    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class})
    ResponseEntity<ErrorResponse> badRequest(Exception exception, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "Invalid request", safeDetail(exception), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> invalidBody(MethodArgumentNotValidException exception,
                                               HttpServletRequest request) {
        String details = exception.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return error(HttpStatus.BAD_REQUEST, "Request validation failed", details, request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ErrorResponse> invalidParameters(ConstraintViolationException exception,
                                                     HttpServletRequest request) {
        String details = exception.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .collect(Collectors.joining("; "));
        return error(HttpStatus.BAD_REQUEST, "Request validation failed", details, request);
    }

    private ResponseEntity<ErrorResponse> error(HttpStatus status, String message, String details,
                                                 HttpServletRequest request) {
        return ResponseEntity.status(status).body(new ErrorResponse(status.value(), message, details,
                correlationId(request), Instant.now()));
    }

    static String correlationId(HttpServletRequest request) {
        String correlationId = request.getHeader("X-Correlation-ID");
        return correlationId == null || correlationId.isBlank() ? UUID.randomUUID().toString() : correlationId;
    }

    private static String safeDetail(Exception exception) {
        if (exception instanceof HttpMessageNotReadableException) return "Request body is malformed";
        if (exception instanceof MethodArgumentTypeMismatchException mismatch) {
            return "Invalid value for parameter " + mismatch.getName();
        }
        return exception.getMessage();
    }
}