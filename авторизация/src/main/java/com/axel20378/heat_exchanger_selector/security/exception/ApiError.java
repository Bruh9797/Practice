package com.axel20378.heat_exchanger_selector.security.exception;

import java.time.Instant;
import java.util.List;

public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        List<String> details
) {
    public static ApiError of(int status, String error, String message) {
        return new ApiError(Instant.now(), status, error, message, List.of());
    }

    public static ApiError of(int status, String error, String message, List<String> details) {
        return new ApiError(Instant.now(), status, error, message, details);
    }
}
