package com.axel20378.heat_exchanger_selector.security.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(

        @NotBlank(message = "Логин обязателен")
        String username,

        @NotBlank(message = "Пароль обязателен")
        String password
) {
}
