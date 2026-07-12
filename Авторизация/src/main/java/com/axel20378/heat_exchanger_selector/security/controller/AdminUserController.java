package com.axel20378.heat_exchanger_selector.security.controller;

import com.axel20378.heat_exchanger_selector.security.User;
import com.axel20378.heat_exchanger_selector.security.UserPrincipal;
import com.axel20378.heat_exchanger_selector.security.UserRepository;
import com.axel20378.heat_exchanger_selector.security.dto.ChangeRoleRequest;
import com.axel20378.heat_exchanger_selector.security.dto.UserResponse;
import com.axel20378.heat_exchanger_selector.security.exception.SelfModificationForbiddenException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Административные функции управления пользователями.
 * Доступ разрешен только роли ADMIN — дополнительно защищено на уровне метода (@PreAuthorize)
 * поверх правила SecurityConfig ("/api/admin/**" -> hasRole("ADMIN")), по принципу defense-in-depth.
 */
@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final UserRepository userRepository;

    public AdminUserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping
    public List<UserResponse> listUsers() {
        return userRepository.findAll().stream()
                .map(UserResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public UserResponse getUser(@PathVariable Long id) {
        return UserResponse.from(findUserOrThrow(id));
    }

    @PatchMapping("/{id}/role")
    public UserResponse changeRole(@PathVariable Long id,
                                    @Valid @RequestBody ChangeRoleRequest request,
                                    @AuthenticationPrincipal UserPrincipal principal) {
        if (principal.getUser().getId().equals(id)) {
            throw new SelfModificationForbiddenException("Нельзя изменить собственную роль");
        }
        User user = findUserOrThrow(id);
        user.setRole(request.role());
        userRepository.save(user);
        return UserResponse.from(user);
    }

    @PatchMapping("/{id}/enabled")
    public UserResponse setEnabled(@PathVariable Long id,
                                    @RequestParam boolean enabled,
                                    @AuthenticationPrincipal UserPrincipal principal) {
        if (principal.getUser().getId().equals(id) && !enabled) {
            throw new SelfModificationForbiddenException("Нельзя заблокировать самого себя");
        }
        User user = findUserOrThrow(id);
        user.setEnabled(enabled);
        userRepository.save(user);
        return UserResponse.from(user);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id,
                                            @AuthenticationPrincipal UserPrincipal principal) {
        if (principal.getUser().getId().equals(id)) {
            throw new SelfModificationForbiddenException("Нельзя удалить самого себя");
        }
        User user = findUserOrThrow(id);
        userRepository.delete(user);
        return ResponseEntity.noContent().build();
    }

    private User findUserOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь с id=" + id + " не найден"));
    }
}
