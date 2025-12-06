package com.vio.authorization_service.service;

import com.vio.authorization_service.dto.*;
import com.vio.authorization_service.handler.*;
import com.vio.authorization_service.model.Credential;
import com.vio.authorization_service.repository.CredentialRepository;
import com.vio.authorization_service.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    private final CredentialRepository credentialRepository;
    private final JwtUtil jwtUtil;
    private final TokenBlackListService tokenBlacklistService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthResponse login(LoginRequest request) {
        log.info("User login attempt: {}", request.username());

        Credential credential = credentialRepository.findByUsername(request.username())
                .orElseThrow(() -> new InvalidCredentialsException(
                        "Invalid username and/or password"));

        if (!passwordEncoder.matches(request.password(), credential.getPassword())) {
            throw new InvalidCredentialsException("Invalid username and/or password");
        }

        String token = jwtUtil.generateToken(
                credential.getUserId(),
                credential.getUsername(),
                credential.getRole()
        );

        log.info("✅ User logged in successfully: {}", request.username());

        return new AuthResponse(
                token,
                credential.getUserId(),
                credential.getUsername(),
                credential.getRole(),
                "Login successful"
        );
    }

    public void logout(String token) {
        log.info("Processing logout request");

        if (!jwtUtil.validateToken(token)) {
            throw new InvalidTokenException("Invalid or expired token");
        }

        Date expirationDate = jwtUtil.extractExpiration(token);
        tokenBlacklistService.blacklistToken(token, expirationDate);

        log.info("✅ User logged out successfully");
    }

    public AuthResponse validateAuthorizationHeader(String authorizationHeader) {
        log.info("Validating authorization header");

        if (authorizationHeader == null || authorizationHeader.trim().isEmpty()) {
            log.warn("❌ Missing Authorization header");
            throw new InvalidTokenException("Missing Authorization header");
        }

        if (!authorizationHeader.startsWith("Bearer ")) {
            log.warn("❌ Invalid Authorization header format");
            throw new InvalidTokenException("Invalid Authorization header format");
        }

        String jwtToken = authorizationHeader.substring(7);

        if (jwtToken.trim().isEmpty()) {
            log.warn("❌ Empty JWT token");
            throw new InvalidTokenException("Empty token");
        }

        if (tokenBlacklistService.isTokenBlacklisted(jwtToken)) {
            log.warn("❌ Token is blacklisted");
            throw new TokenBlacklistedException();
        }

        if (!jwtUtil.validateToken(jwtToken)) {
            log.warn("❌ Token validation failed");
            throw new InvalidTokenException("Invalid or expired token");
        }

        try {
            String username = jwtUtil.extractUsername(jwtToken);
            Long userId = jwtUtil.extractUserId(jwtToken);
            String role = jwtUtil.extractRole(jwtToken);
            log.info("✅ Token validated successfully for user: {} (role: {})", username, role);
            return new AuthResponse(jwtToken, userId, username, role, "Token is valid");
        } catch (Exception e) {
            log.error("❌ Error extracting user info from token: {}", e.getMessage());
            throw new InvalidTokenException("Failed to extract user information from token");
        }
    }

    public AuthResponse getUserFromToken(String token) {
        if (tokenBlacklistService.isTokenBlacklisted(token)) {
            throw new TokenBlacklistedException();
        }

        if (!jwtUtil.validateToken(token)) {
            throw new InvalidTokenException("Invalid or expired token");
        }

        String username = jwtUtil.extractUsername(token);
        Long userId = jwtUtil.extractUserId(token);
        String role = jwtUtil.extractRole(token);

        return new AuthResponse(token, userId, username, role, "Token is valid");
    }
}