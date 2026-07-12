package com.axel20378.heat_exchanger_selector.security.dto;

import com.axel20378.heat_exchanger_selector.security.Role;
import com.axel20378.heat_exchanger_selector.security.User;

import java.time.Instant;

public record UserResponse(
        Long id,
        String username,
        String email,
        Role role,
        boolean enabled,
        Instant createdAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                user.isEnabled(),
                user.getCreatedAt()
        );
    }
}
