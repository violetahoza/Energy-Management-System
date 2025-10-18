package com.vio.authorization_service.service;

import com.vio.authorization_service.dto.AdminUserRequest;
import com.vio.authorization_service.dto.AdminUserResponse;
import com.vio.authorization_service.dto.AdminUserUpdateRequest;
import com.vio.authorization_service.model.Credential;
import com.vio.authorization_service.repository.CredentialRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminUserService {
    private final CredentialRepository credentialRepository;
    private final RestTemplate restTemplate = new RestTemplate();
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private static final String USER_SERVICE_URL = "http://user-service:8081/api/users";

    /**
     * GET all users with credentials
     */
    public List<AdminUserResponse> getAllUsers() {
        log.info("Admin fetching all users with credentials");

        ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                USER_SERVICE_URL,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {}
        );

        List<Map<String, Object>> users = response.getBody();
        if (users == null || users.isEmpty()) {
            return List.of();
        }

        return users.stream()
                .map(this::combineUserAndCredential)
                .collect(Collectors.toList());
    }

    /**
     * GET user by ID with credentials
     */
    public AdminUserResponse getUserById(Long userId) {
        log.info("Admin fetching user by id: {}", userId);
        return getUserWithCredentials(userId);
    }

    /**
     * CREATE complete user (profile + credentials)
     */
    @Transactional
    public AdminUserResponse createUser(AdminUserRequest request) {
        log.info("Admin creating new user: {}", request.username());

        // Check username uniqueness
        if (credentialRepository.existsByUsername(request.username())) {
            throw new RuntimeException("Username already exists");
        }

        // Create user profile first
        Long userId = createUserProfile(request);

        // Create credentials
        Credential credential = Credential.builder()
                .userId(userId)
                .username(request.username())
                .password(passwordEncoder.encode(request.password()))
                .role(request.role().toUpperCase())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        credentialRepository.save(credential);
        log.info("User and credentials created successfully: {}", request.username());

        return getUserWithCredentials(userId);
    }

    /**
     * UPDATE user (partial update - profile and/or credentials)
     */
    @Transactional
    public AdminUserResponse updateUser(Long userId, AdminUserUpdateRequest request) {
        log.info("Admin updating user: {}", userId);

        boolean updated = false;

        // Update profile if any profile fields provided
        if (hasProfileUpdates(request)) {
            updateUserProfile(userId, request);
            updated = true;
        }

        // Update credentials if any credential fields provided
        if (hasCredentialUpdates(request)) {
            updateUserCredentials(userId, request);
            updated = true;
        }

        if (!updated) {
            throw new RuntimeException("No fields to update");
        }

        log.info("User updated successfully: {}", userId);
        return getUserWithCredentials(userId);
    }

    /**
     * DELETE user (profile + credentials)
     */
    @Transactional
    public void deleteUser(Long userId) {
        log.info("Admin deleting user: {}", userId);

        // Delete credentials first
        Credential credential = credentialRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("User credentials not found"));
        credentialRepository.delete(credential);
        log.info("Credentials deleted for user: {}", userId);

        // Delete user profile
        try {
            restTemplate.delete(USER_SERVICE_URL + "/id=" + userId);
            log.info("User profile deleted: {}", userId);
        } catch (Exception e) {
            log.error("Failed to delete user profile: {}", e.getMessage());
            throw new RuntimeException("Failed to delete user profile: " + e.getMessage());
        }

        log.info("User deleted successfully: {}", userId);
    }

    // ==================== Helper Methods ====================

    private boolean hasProfileUpdates(AdminUserUpdateRequest request) {
        return request.firstName() != null || request.lastName() != null ||
                request.email() != null || request.address() != null;
    }

    private boolean hasCredentialUpdates(AdminUserUpdateRequest request) {
        return request.username() != null || request.password() != null ||
                request.role() != null;
    }

    private Long createUserProfile(AdminUserRequest request) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> userRequest = new HashMap<>();
            userRequest.put("firstName", request.firstName());
            userRequest.put("lastName", request.lastName());
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

            throw new RuntimeException("Failed to create user profile");
        } catch (Exception e) {
            log.error("Error creating user profile: {}", e.getMessage());
            throw new RuntimeException("Failed to create user profile: " + e.getMessage());
        }
    }

    private void updateUserProfile(Long userId, AdminUserUpdateRequest request) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> updates = new HashMap<>();
            if (request.firstName() != null) updates.put("firstName", request.firstName());
            if (request.lastName() != null) updates.put("lastName", request.lastName());
            if (request.email() != null) updates.put("email", request.email());
            if (request.address() != null) updates.put("address", request.address());

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(updates, headers);

            restTemplate.exchange(
                    USER_SERVICE_URL + "/id=" + userId,
                    HttpMethod.PATCH,
                    entity,
                    Map.class
            );

            log.info("User profile updated successfully: {}", userId);
        } catch (Exception e) {
            log.error("Failed to update user profile: {}", e.getMessage());
            throw new RuntimeException("Failed to update user profile: " + e.getMessage());
        }
    }

    private void updateUserCredentials(Long userId, AdminUserUpdateRequest request) {
        Credential credential = credentialRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Credentials not found"));

        boolean updated = false;

        if (request.username() != null && !credential.getUsername().equals(request.username())) {
            if (credentialRepository.existsByUsername(request.username())) {
                throw new RuntimeException("Username already exists");
            }
            credential.setUsername(request.username());
            updated = true;
        }

        if (request.password() != null && !request.password().isEmpty()) {
            credential.setPassword(passwordEncoder.encode(request.password()));
            updated = true;
        }

        if (request.role() != null && !credential.getRole().equals(request.role().toUpperCase())) {
            credential.setRole(request.role().toUpperCase());
            updated = true;
        }

        if (updated) {
            credential.setUpdatedAt(LocalDateTime.now());
            credentialRepository.save(credential);
            log.info("Credentials updated successfully: {}", userId);
        }
    }

    private AdminUserResponse getUserWithCredentials(Long userId) {
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                USER_SERVICE_URL + "/id=" + userId,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Map<String, Object>>() {}
        );

        Map<String, Object> user = response.getBody();
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        return combineUserAndCredential(user);
    }

    private AdminUserResponse combineUserAndCredential(Map<String, Object> user) {
        Long userId = ((Number) user.get("userId")).longValue();

        Credential credential = credentialRepository.findByUserId(userId)
                .orElse(null);

        return new AdminUserResponse(
                userId,
                (String) user.get("firstName"),
                (String) user.get("lastName"),
                (String) user.get("email"),
                (String) user.get("address"),
                credential != null ? credential.getUsername() : null,
                credential != null ? credential.getRole() : null,
                parseDateTime(user.get("createdAt")),
                parseDateTime(user.get("updatedAt")),
                credential != null ? credential.getCreatedAt() : null,
                credential != null ? credential.getUpdatedAt() : null
        );
    }

    private LocalDateTime parseDateTime(Object dateTime) {
        if (dateTime == null) return null;
        if (dateTime instanceof LocalDateTime) return (LocalDateTime) dateTime;
        return LocalDateTime.parse(dateTime.toString());
    }
}