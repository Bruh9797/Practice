package com.axel20378.heat_exchanger_selector.security.dto;

import org.springframework.security.web.csrf.CsrfToken;

public record CsrfTokenResponse(
        String headerName,
        String parameterName,
        String token
) {
    public static CsrfTokenResponse from(CsrfToken token) {
        return new CsrfTokenResponse(token.getHeaderName(), token.getParameterName(), token.getToken());
    }
}
