package com.axel20378.heat_exchanger_selector.security.dto;

import com.axel20378.heat_exchanger_selector.security.Role;
import jakarta.validation.constraints.NotNull;

public record ChangeRoleRequest(
        @NotNull(message = "Роль обязательна")
        Role role
) {
}
