package com.axel20378.heat_exchanger_selector.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/**
 * Сверяет сохраненную в сессии учетную запись с БД перед каждым защищенным API-запросом.
 * Благодаря этому блокировка, удаление и смена роли действуют без повторного входа.
 */
public class CurrentUserRefreshFilter extends OncePerRequestFilter {

    private static final Set<String> PUBLIC_AUTH_ENDPOINTS = Set.of(
            "/api/auth/csrf",
            "/api/auth/register",
            "/api/auth/login",
            "/api/auth/logout"
    );

    private final UserRepository userRepository;
    private final SecurityContextRepository securityContextRepository;
    private final JsonAuthenticationEntryPoint authenticationEntryPoint;

    public CurrentUserRefreshFilter(UserRepository userRepository,
                                    SecurityContextRepository securityContextRepository,
                                    JsonAuthenticationEntryPoint authenticationEntryPoint) {
        this.userRepository = userRepository;
        this.securityContextRepository = securityContextRepository;
        this.authenticationEntryPoint = authenticationEntryPoint;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !path.startsWith("/api/") || PUBLIC_AUTH_ENDPOINTS.contains(path);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Authentication current = SecurityContextHolder.getContext().getAuthentication();
        if (current == null || !current.isAuthenticated() || !(current.getPrincipal() instanceof UserPrincipal principal)) {
            filterChain.doFilter(request, response);
            return;
        }

        User freshUser = userRepository.findById(principal.getUser().getId()).orElse(null);
        if (freshUser == null || !freshUser.isEnabled()) {
            SecurityContextHolder.clearContext();
            HttpSession session = request.getSession(false);
            if (session != null) {
                session.invalidate();
            }
            authenticationEntryPoint.commence(
                    request,
                    response,
                    new InsufficientAuthenticationException("Учетная запись недоступна")
            );
            return;
        }

        UserPrincipal refreshedPrincipal = new UserPrincipal(freshUser);
        UsernamePasswordAuthenticationToken refreshed = UsernamePasswordAuthenticationToken.authenticated(
                refreshedPrincipal,
                null,
                refreshedPrincipal.getAuthorities()
        );
        refreshed.setDetails(current.getDetails());

        SecurityContext context = SecurityContextHolder.getContext();
        context.setAuthentication(refreshed);
        securityContextRepository.saveContext(context, request, response);
        filterChain.doFilter(request, response);
    }
}
