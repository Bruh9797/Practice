package com.axel20378.heat_exchanger_selector.security.exception;

import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;

public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String code,
        String message,
        List<String> details,
        String path
) {
    public static ApiError of(HttpStatus status, String code, String message, String path) {
        return of(status, code, message, path, List.of());
    }

    public static ApiError of(HttpStatus status,
                              String code,
                              String message,
                              String path,
                              List<String> details) {
        return new ApiError(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                code,
                message,
                List.copyOf(details),
                path
        );
    }
}
