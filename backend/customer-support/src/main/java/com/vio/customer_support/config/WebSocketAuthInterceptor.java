package com.vio.customer_support.config;

import com.vio.customer_support.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.Collections;

@Component
@Slf4j
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor {
    private final JwtUtil jwtUtil;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            log.info("========== WebSocket CONNECT Attempt ==========");

            try {
                String authHeader = accessor.getFirstNativeHeader("Authorization");

                if (authHeader == null || authHeader.trim().isEmpty()) {
                    log.error("❌ Missing Authorization header");
                    throw new IllegalArgumentException("Missing Authorization header");
                }

                if (!authHeader.startsWith("Bearer ")) {
                    log.error("❌ Invalid Authorization header format");
                    throw new IllegalArgumentException("Invalid Authorization header format");
                }

                String token = authHeader.substring(7).trim();

                if (token.isEmpty()) {
                    log.error("❌ Empty JWT token");
                    throw new IllegalArgumentException("Empty token");
                }

                log.info("Token extracted, length: {}", token.length());

                if (!jwtUtil.validateToken(token)) {
                    log.error("❌ JWT token validation failed");
                    throw new IllegalArgumentException("Invalid or expired JWT token");
                }
                log.info("✅ Token validated successfully");

                Long userId = jwtUtil.extractUserId(token);
                String username = jwtUtil.extractUsername(token);
                String role = jwtUtil.extractRole(token);

                log.info("✅ Extracted from token - userId: {}, username: {}, role: {}", userId, username, role);

                Principal principal = new WebSocketPrincipal(userId.toString());
                accessor.setUser(principal);

                SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + role);
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userId.toString(),
                                null,
                                Collections.singletonList(authority)
                        );
                SecurityContextHolder.getContext().setAuthentication(authentication);

                log.info("✅ WebSocket authenticated successfully for user: {} (role: {})", username, role);
                log.info("========== WebSocket CONNECT Success ==========");

            } catch (IllegalArgumentException e) {
                log.error("❌ WebSocket authentication failed: {}", e.getMessage());
                log.error("========== WebSocket CONNECT Failed ==========");
                throw e;
            } catch (Exception e) {
                log.error("❌ Unexpected error during WebSocket authentication: {}", e.getMessage(), e);
                log.error("========== WebSocket CONNECT Failed ==========");
                throw new IllegalArgumentException("Authentication failed: " + e.getMessage());
            }
        }

        return message;
    }

    private static class WebSocketPrincipal implements Principal {
        private final String name;

        public WebSocketPrincipal(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }
    }
}