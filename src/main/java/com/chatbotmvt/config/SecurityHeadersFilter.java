package com.chatbotmvt.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SecurityHeadersFilter implements Filter {

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletResponse response = (HttpServletResponse) res;
        HttpServletRequest request = (HttpServletRequest) req;

        // Evitar que la página se embeba en iframes (clickjacking)
        response.setHeader("X-Frame-Options", "DENY");

        // Content Security Policy básica
        response.setHeader("Content-Security-Policy",
                "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:;");

        // Prevenir MIME sniffing
        response.setHeader("X-Content-Type-Options", "nosniff");

        // Política de referrer
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");

        // HSTS (solo en HTTPS/producción)
        if (request.isSecure() || request.getHeader("X-Forwarded-Proto") != null) {
            response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
        }

        // Permissions Policy
        response.setHeader("Permissions-Policy",
                "camera=(), microphone=(), geolocation=(), payment=()");

        chain.doFilter(req, res);
    }
}
