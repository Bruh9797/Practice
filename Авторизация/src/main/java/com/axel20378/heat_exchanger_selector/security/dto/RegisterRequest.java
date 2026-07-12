package com.axel20378.heat_exchanger_selector.security.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(

        @NotBlank(message = "Логин обязателен")
        @Size(min = 3, max = 64, message = "Логин должен быть от 3 до 64 символов")
        String username,

        @NotBlank(message = "Пароль обязателен")
        @Size(min = 8, max = 128, message = "Пароль должен быть не короче 8 символов")
        String password,

        @NotBlank(message = "Email обязателен")
        @Email(message = "Некорректный email")
        @Size(max = 128, message = "Email должен быть не длиннее 128 символов")
        String email
) {
    public RegisterRequest {
        username = username == null ? null : username.trim();
        email = email == null ? null : email.trim();
    }
}
