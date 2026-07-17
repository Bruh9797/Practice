package com.axel20378.heat_exchanger_selector.catalog.exception;

import com.axel20378.heat_exchanger_selector.security.exception.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.axel20378.heat_exchanger_selector.catalog")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CatalogExceptionHandler {
    @ExceptionHandler(CatalogNotFoundException.class)
    public ResponseEntity<ApiError> notFound(CatalogNotFoundException exception, HttpServletRequest request) {
        return response(HttpStatus.NOT_FOUND, "CATALOG_NOT_FOUND", exception.getMessage(), request);
    }

    @ExceptionHandler(CatalogBadRequestException.class)
    public ResponseEntity<ApiError> badRequest(CatalogBadRequestException exception, HttpServletRequest request) {
        return ResponseEntity.badRequest().body(ApiError.of(HttpStatus.BAD_REQUEST, "CATALOG_INVALID_REQUEST",
                exception.getMessage(), request.getRequestURI(), exception.getDetails()));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> constraintViolation(ConstraintViolationException exception,
                                                        HttpServletRequest request) {
        return ResponseEntity.badRequest().body(ApiError.of(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED",
                "Ошибка валидации данных", request.getRequestURI(),
                exception.getConstraintViolations().stream().map(value -> value.getPropertyPath() + ": "
                        + value.getMessage()).toList()));
    }

    @ExceptionHandler({CatalogConflictException.class, ObjectOptimisticLockingFailureException.class})
    public ResponseEntity<ApiError> conflict(RuntimeException exception, HttpServletRequest request) {
        String message = exception instanceof CatalogConflictException
                ? exception.getMessage()
                : "Запись уже изменена другим пользователем; обновите данные";
        return response(HttpStatus.CONFLICT, "CATALOG_CONFLICT", message, request);
    }

    private ResponseEntity<ApiError> response(HttpStatus status, String code, String message,
                                              HttpServletRequest request) {
        return ResponseEntity.status(status).body(ApiError.of(status, code, message, request.getRequestURI()));
    }
}
