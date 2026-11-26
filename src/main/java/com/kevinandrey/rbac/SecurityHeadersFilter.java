package com.kevinandrey.rbac;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/** Adds browser hardening headers to every response and keeps protected pages out of caches. */
public final class SecurityHeadersFilter extends HttpFilter {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        response.setHeader("Content-Security-Policy", "default-src 'self'; frame-ancestors 'none'; form-action 'self'");
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");
        response.setHeader("Referrer-Policy", "no-referrer");
        response.setHeader("Cache-Control", "no-store");
        chain.doFilter(request, response);
    }
}
