package com.axel20378.heat_exchanger_selector.security.exception;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(EntityNotFoundException ex, HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", ex.getMessage(), request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> handleNoResource(NoResourceFoundException ex, HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "Ресурс не найден", request);
    }

    @ExceptionHandler({DuplicateUsernameException.class, DuplicateUserException.class})
    public ResponseEntity<ApiError> handleDuplicateUser(RuntimeException ex, HttpServletRequest request) {
        return error(HttpStatus.CONFLICT, "USER_ALREADY_EXISTS", ex.getMessage(), request);
    }

    @ExceptionHandler(SelfModificationForbiddenException.class)
    public ResponseEntity<ApiError> handleSelfModification(SelfModificationForbiddenException ex,
                                                            HttpServletRequest request) {
        return error(HttpStatus.CONFLICT, "SELF_MODIFICATION_FORBIDDEN", ex.getMessage(), request);
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ApiError> handleOptimisticLock(ObjectOptimisticLockingFailureException ex,
                                                          HttpServletRequest request) {
        return error(
                HttpStatus.CONFLICT,
                "OPTIMISTIC_LOCK_CONFLICT",
                "Запись уже изменена другим пользователем. Обновите данные и повторите действие",
                request
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataConflict(DataIntegrityViolationException ex,
                                                        HttpServletRequest request) {
        return error(HttpStatus.CONFLICT, "DATA_CONFLICT", "Данные конфликтуют с существующей записью", request);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiError> handleBadCredentials(BadCredentialsException ex, HttpServletRequest request) {
        return error(HttpStatus.UNAUTHORIZED, "AUTH_INVALID_CREDENTIALS", "Неверный логин или пароль", request);
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ApiError> handleDisabled(DisabledException ex, HttpServletRequest request) {
        return error(HttpStatus.FORBIDDEN, "AUTH_ACCOUNT_DISABLED", "Учетная запись заблокирована", request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex,
                                                      HttpServletRequest request) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .toList();
        return ResponseEntity.badRequest().body(ApiError.of(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_FAILED",
                "Ошибка валидации данных",
                request.getRequestURI(),
                details
        ));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException ex,
                                                               HttpServletRequest request) {
        List<String> details = ex.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .toList();
        return ResponseEntity.badRequest().body(ApiError.of(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_FAILED",
                "Ошибка валидации данных",
                request.getRequestURI(),
                details
        ));
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class,
            HandlerMethodValidationException.class
    })
    public ResponseEntity<ApiError> handleMalformedRequest(Exception ex, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "REQUEST_MALFORMED", "Некорректный формат запроса", request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex,
                                                            HttpServletRequest request) {
        return error(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED", "HTTP-метод не поддерживается", request);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiError> handleMediaType(HttpMediaTypeNotSupportedException ex,
                                                    HttpServletRequest request) {
        return error(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "MEDIA_TYPE_UNSUPPORTED", "Тип содержимого не поддерживается", request);
    }

    private ResponseEntity<ApiError> error(HttpStatus status,
                                           String code,
                                           String message,
                                           HttpServletRequest request) {
        return ResponseEntity.status(status)
                .body(ApiError.of(status, code, message, request.getRequestURI()));
    }
}
