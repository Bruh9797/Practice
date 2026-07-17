package com.axel20378.heat_exchanger_selector.security.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(

        @NotBlank(message = "Логин обязателен")
        @Size(min = 3, max = 64, message = "Логин должен быть от 3 до 64 символов")
        String username,

        @NotBlank(message = "Пароль обязателен")
        String password
) {
    public LoginRequest {
        username = username == null ? null : username.trim();
    }
}
