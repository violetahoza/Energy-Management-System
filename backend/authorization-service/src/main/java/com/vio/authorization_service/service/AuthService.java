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
import org.springframework.web.client.*;
import org.springframework.http.*;

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
    private final RestTemplate restTemplate = new RestTemplate();

    private static final String USER_SERVICE_URL = "http://user-service:8081/api/users";

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Registering new user: {}", request.username());

        if (credentialRepository.existsByUsername(request.username())) {
            throw new UsernameAlreadyExistsException(request.username());
        }

        Long userId = createUserProfileInUserService(request);

        try {
            Credential credential = Credential.builder()
                    .userId(userId)
                    .username(request.username())
                    .password(passwordEncoder.encode(request.password()))
                    .role(request.role() != null ? request.role().toUpperCase() : "CLIENT")
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            Credential savedCredential = credentialRepository.save(credential);
            log.info("Credential created for user: {}", savedCredential.getUsername());

            String token = jwtUtil.generateToken(
                    savedCredential.getUserId(),
                    savedCredential.getUsername(),
                    savedCredential.getRole()
            );

            return new AuthResponse(
                    token,
                    savedCredential.getUserId(),
                    savedCredential.getUsername(),
                    savedCredential.getRole(),
                    "Registration successful"
            );
        } catch (Exception e) {
            log.error("Failed to create credentials for userId: {}", userId, e);
            throw new AuthorizationException("Failed to complete registration: " + e.getMessage(), e);
        }
    }

    public AuthResponse login(LoginRequest request) {
        log.info("User login attempt: {}", request.username());

        Credential credential = credentialRepository.findByUsername(request.username())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid username"));

        if (!passwordEncoder.matches(request.password(), credential.getPassword())) {
            throw new InvalidCredentialsException("Invalid password");
        }

        String token = jwtUtil.generateToken(
                credential.getUserId(),
                credential.getUsername(),
                credential.getRole()
        );

        log.info("User logged in successfully: {}", request.username());

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

        log.info("User logged out successfully");
    }

    public AuthResponse validateAuthorizationHeader(String authorizationHeader) {
        log.info("Validating authorization header");

        // Check if Authorization header is present
        if (authorizationHeader == null || authorizationHeader.trim().isEmpty()) {
            log.warn("Missing Authorization header");
            throw new InvalidTokenException("Missing Authorization header");
        }

        // Check if token has Bearer prefix
        if (!authorizationHeader.startsWith("Bearer ")) {
            log.warn("Invalid Authorization header format");
            throw new InvalidTokenException("Invalid Authorization header format");
        }

        // Extract JWT token
        String jwtToken = authorizationHeader.substring(7);

        // Validate token is not empty after removing Bearer prefix
        if (jwtToken.trim().isEmpty()) {
            log.warn("Empty JWT token");
            throw new InvalidTokenException("Empty token");
        }

        // Check if token is blacklisted
        if (tokenBlacklistService.isTokenBlacklisted(jwtToken)) {
            log.warn("Token is blacklisted");
            throw new TokenBlacklistedException();
        }

        // Validate token structure and expiration
        if (!jwtUtil.validateToken(jwtToken)) {
            log.warn("Token validation failed");
            throw new InvalidTokenException("Invalid or expired token");
        }

        // Extract user information from token
        try {
            String username = jwtUtil.extractUsername(jwtToken);
            Long userId = jwtUtil.extractUserId(jwtToken);
            String role = jwtUtil.extractRole(jwtToken);

            log.info("Token validated successfully for user: {} (role: {})", username, role);

            return new AuthResponse(
                    jwtToken,
                    userId,
                    username,
                    role,
                    "Token is valid"
            );
        } catch (Exception e) {
            log.error("Error extracting user info from token: {}", e.getMessage());
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

    private Long createUserProfileInUserService(RegisterRequest request) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> userRequest = new HashMap<>();

            String[] nameParts = request.fullName() != null ? request.fullName().split(" ", 2) : new String[]{"", ""};
            userRequest.put("firstName", nameParts.length > 0 ? nameParts[0] : "");
            userRequest.put("lastName", nameParts.length > 1 ? nameParts[1] : "");
            userRequest.put("email", request.email());
            userRequest.put("address", request.address());

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(userRequest, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(USER_SERVICE_URL + "/internal/profile", entity, Map.class);

            Map<String, Object> responseBody = response.getBody();
            if (responseBody != null && responseBody.containsKey("userId")) {
                return ((Number) responseBody.get("userId")).longValue();
            }

            throw new ExternalServiceException("User Service", "Invalid response format");
        } catch (RestClientException e) {
            log.error("Error communicating with User Service: {}", e.getMessage(), e);
            throw new ExternalServiceException("User Service", e);
        } catch (Exception e) {
            log.error("Unexpected error creating user in User Service: {}", e.getMessage(), e);
            throw new ExternalServiceException("User Service", e);
        }

    }
}