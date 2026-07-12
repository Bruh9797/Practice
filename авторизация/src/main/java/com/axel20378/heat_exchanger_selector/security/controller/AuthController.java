package com.axel20378.heat_exchanger_selector.security.controller;

import com.axel20378.heat_exchanger_selector.security.Role;
import com.axel20378.heat_exchanger_selector.security.User;
import com.axel20378.heat_exchanger_selector.security.UserPrincipal;
import com.axel20378.heat_exchanger_selector.security.UserRepository;
import com.axel20378.heat_exchanger_selector.security.dto.LoginRequest;
import com.axel20378.heat_exchanger_selector.security.dto.RegisterRequest;
import com.axel20378.heat_exchanger_selector.security.dto.UserResponse;
import com.axel20378.heat_exchanger_selector.security.exception.DuplicateUsernameException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

/**
 * Регистрация, вход и выход пользователей.
 * Сценарий "Пользователь": регистрация -> вход -> поиск теплообменных аппаратов -> выход.
 * Сценарий "Администратор": вход -> управление пользователями/каталогом/журналами -> выход.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

    public AuthController(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new DuplicateUsernameException(request.username());
        }

        User user = User.builder()
                .username(request.username())
                .passwordHash(passwordEncoder.encode(request.password()))
                .email(request.email())
                .role(Role.USER) // при самостоятельной регистрации всегда выдаётся роль USER
                .enabled(true)
                .createdAt(Instant.now())
                .build();

        userRepository.save(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(user));
    }

    @PostMapping("/login")
    public ResponseEntity<UserResponse> login(@Valid @RequestBody LoginRequest request,
                                               HttpServletRequest httpRequest,
                                               HttpServletResponse httpResponse) {
        Authentication authRequest =
                UsernamePasswordAuthenticationToken.unauthenticated(request.username(), request.password());
        Authentication authResult = authenticationManager.authenticate(authRequest);

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authResult);
        SecurityContextHolder.setContext(context);
        // Сохраняем контекст безопасности в сессии, чтобы последующие запросы
        // с тем же session cookie считались авторизованными.
        securityContextRepository.saveContext(context, httpRequest, httpResponse);

        UserPrincipal principal = (UserPrincipal) authResult.getPrincipal();
        return ResponseEntity.ok(UserResponse.from(principal.getUser()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(UserResponse.from(principal.getUser()));
    }
}
