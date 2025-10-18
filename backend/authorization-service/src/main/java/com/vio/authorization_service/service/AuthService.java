package com.vio.authorization_service.service;

import com.vio.authorization_service.dto.AuthResponse;
import com.vio.authorization_service.dto.LoginRequest;
import com.vio.authorization_service.dto.RegisterRequest;
import com.vio.authorization_service.model.Credential;
import com.vio.authorization_service.repository.CredentialRepository;
import com.vio.authorization_service.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

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

        // Check if username already exists
        Optional<Credential> existingCredential = credentialRepository.findByUsername(request.username());
        if (existingCredential.isPresent()) {
            throw new RuntimeException("Username already exists");
        }

        // Create user in User Service first
        Long userId = createUserInUserService(request);

        // Create credentials
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

        // Generate JWT token
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
    }

    public AuthResponse login(LoginRequest request) {
        log.info("User login attempt: {}", request.username());

        Credential credential = credentialRepository.findByUsername(request.username())
                .orElseThrow(() -> new RuntimeException("Invalid username"));

        if (!passwordEncoder.matches(request.password(), credential.getPassword())) {
            throw new RuntimeException("Invalid password");
        }

        // Generate JWT token
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
            throw new RuntimeException("Invalid token");
        }

        // Add token to blacklist
        Date expirationDate = jwtUtil.extractExpiration(token);
        tokenBlacklistService.blacklistToken(token, expirationDate);

        log.info("User logged out successfully");
    }

    public boolean validateToken(String token) {
        // Check if token is blacklisted
        if (tokenBlacklistService.isTokenBlacklisted(token)) {
            log.warn("Token is blacklisted");
            return false;
        }

        return jwtUtil.validateToken(token);
    }

    public AuthResponse getUserFromToken(String token) {
        if (!jwtUtil.validateToken(token)) {
            throw new RuntimeException("Invalid or expired token");
        }

        String username = jwtUtil.extractUsername(token);
        Long userId = jwtUtil.extractUserId(token);
        String role = jwtUtil.extractRole(token);

        return new AuthResponse(
                token,
                userId,
                username,
                role,
                "Token is valid"
        );
    }

    private Long createUserInUserService(RegisterRequest request) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> userRequest = new HashMap<>();

            // Parse fullName into firstName and lastName
            String[] nameParts = request.fullName() != null ?
                    request.fullName().split(" ", 2) : new String[]{"", ""};
            userRequest.put("firstName", nameParts.length > 0 ? nameParts[0] : "");
            userRequest.put("lastName", nameParts.length > 1 ? nameParts[1] : "");
            userRequest.put("email", request.email());
            userRequest.put("address", request.address());

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(userRequest, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    USER_SERVICE_URL,
                    entity,
                    Map.class
            );

            Map<String, Object> responseBody = response.getBody();
            if (responseBody != null && responseBody.containsKey("userId")) {
                return ((Number) responseBody.get("userId")).longValue();
            }

            throw new RuntimeException("Failed to create user in User Service");
        } catch (Exception e) {
            log.error("Error creating user in User Service: {}", e.getMessage());
            throw new RuntimeException("Failed to create user: " + e.getMessage());
        }
    }
}