package com.vio.api_gateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vio.api_gateway.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        final String requestPath = request.getRequestURI();

        log.debug("Processing request to: {} with method: {}", requestPath, request.getMethod());

        // Allow public endpoints (login, register)
        if (isPublicEndpoint(requestPath)) {
            log.debug("Public endpoint, skipping authentication: {}", requestPath);
            filterChain.doFilter(request, response);
            return;
        }

        // Check for Authorization header
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Missing or invalid Authorization header for: {}", requestPath);
            sendUnauthorizedResponse(response, "Missing or invalid Authorization header");
            return;
        }

        try {
            final String jwt = authHeader.substring(7);

            // Validate token
            if (!jwtUtil.validateToken(jwt)) {
                log.warn("Invalid or expired token for: {}", requestPath);
                sendUnauthorizedResponse(response, "Invalid or expired token");
                return;
            }

            // Extract user information
            String username = jwtUtil.extractUsername(jwt);
            Long userId = jwtUtil.extractUserId(jwt);
            String role = jwtUtil.extractRole(jwt);

            log.debug("Authenticated user: {}, role: {}, userId: {}", username, role, userId);

            // Set authentication in security context
            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                    username,
                    null,
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role))
            );
            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authToken);

            // Add user info to request attributes for controllers
            request.setAttribute("userId", userId);
            request.setAttribute("username", username);
            request.setAttribute("role", role);

            filterChain.doFilter(request, response);

        } catch (Exception e) {
            log.error("Error processing authentication: {}", e.getMessage());
            sendUnauthorizedResponse(response, "Authentication failed: " + e.getMessage());
        }
    }

    private boolean isPublicEndpoint(String path) {
        return path.startsWith("/api/auth/login") ||
                path.startsWith("/api/auth/register") ||
                path.equals("/") ||
                path.startsWith("/error");
    }

    private void sendUnauthorizedResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", System.currentTimeMillis());
        errorResponse.put("status", 401);
        errorResponse.put("error", "Unauthorized");
        errorResponse.put("message", message);

        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}