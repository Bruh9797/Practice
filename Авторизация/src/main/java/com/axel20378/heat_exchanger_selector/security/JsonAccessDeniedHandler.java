package com.axel20378.heat_exchanger_selector.security;

import com.axel20378.heat_exchanger_selector.security.exception.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.csrf.InvalidCsrfTokenException;
import org.springframework.security.web.csrf.MissingCsrfTokenException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Возвращает 403 в формате JSON, когда авторизованный пользователь
 * пытается выполнить административное действие без прав ADMIN.
 */
@Component
public class JsonAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public JsonAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                        AccessDeniedException accessDeniedException) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        boolean missingCsrf = accessDeniedException instanceof MissingCsrfTokenException;
        boolean invalidCsrf = accessDeniedException instanceof InvalidCsrfTokenException;
        String code = missingCsrf ? "CSRF_MISSING" : invalidCsrf ? "CSRF_INVALID" : "ACCESS_DENIED";
        String message = (missingCsrf || invalidCsrf)
                ? "Отсутствует или недействителен CSRF-токен"
                : "Недостаточно прав для выполнения действия";
        ApiError body = ApiError.of(HttpStatus.FORBIDDEN, code, message, request.getRequestURI());
        objectMapper.writeValue(response.getWriter(), body);
    }
}
