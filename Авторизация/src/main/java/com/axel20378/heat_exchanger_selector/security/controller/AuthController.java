package com.axel20378.heat_exchanger_selector.security.controller;

import com.axel20378.heat_exchanger_selector.security.Role;
import com.axel20378.heat_exchanger_selector.security.User;
import com.axel20378.heat_exchanger_selector.security.UserPrincipal;
import com.axel20378.heat_exchanger_selector.security.UserRepository;
import com.axel20378.heat_exchanger_selector.security.dto.CsrfTokenResponse;
import com.axel20378.heat_exchanger_selector.security.dto.LoginRequest;
import com.axel20378.heat_exchanger_selector.security.dto.RegisterRequest;
import com.axel20378.heat_exchanger_selector.security.dto.UserResponse;
import com.axel20378.heat_exchanger_selector.security.exception.DuplicateUserException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Locale;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final SessionAuthenticationStrategy sessionAuthenticationStrategy;
    private final CsrfTokenRepository csrfTokenRepository;

    public AuthController(UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          AuthenticationManager authenticationManager,
                          SecurityContextRepository securityContextRepository,
                          SessionAuthenticationStrategy sessionAuthenticationStrategy,
                          CsrfTokenRepository csrfTokenRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
        this.sessionAuthenticationStrategy = sessionAuthenticationStrategy;
        this.csrfTokenRepository = csrfTokenRepository;
    }

    @GetMapping("/csrf")
    public ResponseEntity<CsrfTokenResponse> csrf(CsrfToken token) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(CsrfTokenResponse.from(token));
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        String username = request.username().trim();
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByUsername(username)) {
            throw DuplicateUserException.username(username);
        }
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw DuplicateUserException.email(email);
        }

        User user = User.builder()
                .username(username)
                .passwordHash(passwordEncoder.encode(request.password()))
                .email(email)
                .role(Role.USER)
                .enabled(true)
                .createdAt(Instant.now())
                .build();

        try {
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException conflict) {
            // Повторная проверка превращает и конкурентную регистрацию в тот же
            // стабильный 409-контракт, что и обычное дублирование.
            if (userRepository.existsByUsername(username)) {
                throw DuplicateUserException.username(username);
            }
            if (userRepository.existsByEmailIgnoreCase(email)) {
                throw DuplicateUserException.email(email);
            }
            throw conflict;
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(user));
    }

    @PostMapping("/login")
    public ResponseEntity<UserResponse> login(@Valid @RequestBody LoginRequest request,
                                               HttpServletRequest httpRequest,
                                               HttpServletResponse httpResponse) {
        Authentication authRequest = UsernamePasswordAuthenticationToken.unauthenticated(
                request.username().trim(),
                request.password()
        );
        Authentication authResult = authenticationManager.authenticate(authRequest);

        // JSON-login выполняется вне UsernamePasswordAuthenticationFilter, поэтому
        // стратегию защиты от фиксации сессии необходимо вызвать явно.
        sessionAuthenticationStrategy.onAuthentication(authResult, httpRequest, httpResponse);

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authResult);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, httpRequest, httpResponse);

        // Токен, использованный для входа, больше не действует. SPA получает новый
        // отдельным GET /api/auth/csrf сразу после успешной авторизации.
        csrfTokenRepository.saveToken(null, httpRequest, httpResponse);

        UserPrincipal principal = (UserPrincipal) authResult.getPrincipal();
        return ResponseEntity.ok(UserResponse.from(principal.getUser()));
    }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal UserPrincipal principal) {
        return UserResponse.from(principal.getUser());
    }
}
