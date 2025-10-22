package com.vio.userservice.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * JWT Authentication Filter that extracts user information from headers
 * The headers are set by Traefik's ForwardAuth middleware after token validation
 */
@Component
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        try {
            // Extract user information from headers set by Traefik
            String userId = request.getHeader("X-User-Id");
            String username = request.getHeader("X-Username");
            String role = request.getHeader("X-User-Role");

            log.debug("Request URI: {}", request.getRequestURI());
            log.debug("Headers - X-User-Id: {}, X-Username: {}, X-User-Role: {}", userId, username, role);

            if (userId != null && username != null && role != null) {
                log.info("Authenticating user: {} (ID: {}, Role: {})", username, userId, role);

                // Create authentication token with role
                SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + role);
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userId,  // Set userId as principal for easy access in security checks
                                null,
                                Collections.singletonList(authority)
                        );

                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.info("Security context set successfully for user: {} with role: ROLE_{}", username, role);
            } else {
                log.debug("No complete user headers found in request. Missing: {}{}{}",
                        userId == null ? "X-User-Id " : "",
                        username == null ? "X-Username " : "",
                        role == null ? "X-User-Role" : "");
            }
        } catch (Exception e) {
            log.error("Error in JWT authentication filter: {}", e.getMessage(), e);
            // Don't block the request, let it continue without authentication
        }

        filterChain.doFilter(request, response);
    }
}