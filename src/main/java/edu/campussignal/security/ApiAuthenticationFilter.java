package edu.campussignal.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Fails closed: only the OAuth endpoints and the owner-scoped /me API exist in multi-user mode. */
@Component
public class ApiAuthenticationFilter extends OncePerRequestFilter {
    private final AuthenticatedUserService users;
    public ApiAuthenticationFilter(AuthenticatedUserService users) { this.users = users; }
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        String path = request.getRequestURI();
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) { chain.doFilter(request, response); return; }
        if (!users.required() || !path.startsWith("/api/v1/")) { chain.doFilter(request, response); return; }
        if (path.startsWith("/api/v1/auth/")) { chain.doFilter(request, response); return; }
        if (!path.startsWith("/api/v1/me/")) { response.sendError(HttpServletResponse.SC_NOT_FOUND); return; }
        try { users.current(request); chain.doFilter(request, response); }
        catch (AuthenticationRequiredException exception) { response.sendError(HttpServletResponse.SC_UNAUTHORIZED); }
    }
}
