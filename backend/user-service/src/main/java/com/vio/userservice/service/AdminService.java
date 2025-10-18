package com.vio.userservice.service;

import com.vio.userservice.dto.AdminUserRequest;
import com.vio.userservice.dto.AdminUserResponse;
import com.vio.userservice.dto.AdminUserUpdateRequest;
import com.vio.userservice.handler.UserEmailAlreadyExistsException;
import com.vio.userservice.handler.UserNotFoundException;
import com.vio.userservice.model.User;
import com.vio.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
public class AdminService {
    private final UserRepository userRepository;
    private final RestTemplate restTemplate;

    private static final String AUTH_SERVICE_URL = "http://authorization-service:8083/api/auth/internal/credentials";

    public List<AdminUserResponse> getAllUsers() {
        log.info("Admin fetching all users with credentials");

        List<User> users = userRepository.findAll();

        return users.stream()
                .map(this::getUserWithCredentials)
                .collect(Collectors.toList());
    }

    public AdminUserResponse getUserById(Long userId) {
        log.info("Admin fetching user by id: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        return getUserWithCredentials(user);
    }

    @Transactional
    public AdminUserResponse createUser(AdminUserRequest request) {
        log.info("Admin creating new user: {}", request.username());

        // Check email uniqueness
        if (userRepository.existsByEmail(request.email())) {
            throw new UserEmailAlreadyExistsException(request.email());
        }

        // Check username uniqueness via authorization service
        try {
            ResponseEntity<Boolean> usernameCheck = restTemplate.getForEntity(
                    AUTH_SERVICE_URL + "/username/" + request.username() + "/exists",
                    Boolean.class
            );
            if (Boolean.TRUE.equals(usernameCheck.getBody())) {
                throw new RuntimeException("Username already exists: " + request.username());
            }
        } catch (Exception e) {
            log.error("Failed to check username uniqueness: {}", e.getMessage());
            throw new RuntimeException("Failed to check username uniqueness: " + e.getMessage());
        }

        // Create user profile
        User user = User.builder()
                .firstName(request.firstName())
                .lastName(request.lastName())
                .email(request.email())
                .address(request.address())
                .build();

        User savedUser = userRepository.save(user);
        log.info("User profile created with id: {}", savedUser.getUserId());

        // Create credentials via authorization service
        Map<String, Object> credentialData = createCredentialsInAuthService(
                savedUser.getUserId(),
                request.username(),
                request.password(),
                request.role()
        );

        log.info("User and credentials created successfully: {}", request.username());
        return buildAdminUserResponse(savedUser, credentialData);
    }

    /**
     * UPDATE user (partial update - profile and/or credentials)
     */
    @Transactional
    public AdminUserResponse updateUser(Long userId, AdminUserUpdateRequest request) {
        log.info("Admin updating user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        boolean profileUpdated = false;
        boolean credentialsUpdated = false;

        // Update profile if any profile fields provided
        if (hasProfileUpdates(request)) {
            updateUserProfile(user, request);
            profileUpdated = true;
        }

        // Update credentials if any credential fields provided
        if (hasCredentialUpdates(request)) {
            updateCredentialsInAuthService(userId, request);
            credentialsUpdated = true;
        }

        if (!profileUpdated && !credentialsUpdated) {
            throw new RuntimeException("No fields to update");
        }

        log.info("User updated successfully: {}", userId);
        return getUserWithCredentials(user);
    }

    @Transactional
    public void deleteUser(Long userId) {
        log.info("Admin deleting user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        // Delete credentials first via authorization service
        try {
            restTemplate.delete(AUTH_SERVICE_URL + "/user/" + userId);
            log.info("Credentials deleted for user: {}", userId);
        } catch (Exception e) {
            log.warn("Credentials not found or already deleted for user: {}", userId);
        }

        // Notify Device Service to unassign devices (devices persist, just unassigned)
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            restTemplate.postForEntity(
                    "http://device-service:8082/api/devices/sync/unassign-user/" + userId,
                    entity,
                    Void.class
            );
            log.info("Device unassignment triggered for user: {}", userId);
        } catch (Exception e) {
            log.warn("Failed to notify Device Service about user deletion: {}", e.getMessage());
            // Continue with user deletion even if device sync fails
        }

        // Delete user profile
        userRepository.delete(user);
        log.info("User profile deleted: {}", userId);

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

    private void updateUserProfile(User user, AdminUserUpdateRequest request) {
        if (request.firstName() != null) {
            user.setFirstName(request.firstName());
        }
        if (request.lastName() != null) {
            user.setLastName(request.lastName());
        }
        if (request.email() != null) {
            if (!user.getEmail().equals(request.email()) &&
                    userRepository.existsByEmail(request.email())) {
                throw new UserEmailAlreadyExistsException(request.email());
            }
            user.setEmail(request.email());
        }
        if (request.address() != null) {
            user.setAddress(request.address());
        }

        userRepository.save(user);
        log.info("User profile updated successfully: {}", user.getUserId());
    }

    private Map<String, Object> createCredentialsInAuthService(
            Long userId, String username, String password, String role) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> credentialRequest = new HashMap<>();
            credentialRequest.put("userId", userId);
            credentialRequest.put("username", username);
            credentialRequest.put("password", password);
            credentialRequest.put("role", role.toUpperCase());

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(credentialRequest, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    AUTH_SERVICE_URL,
                    entity,
                    Map.class
            );

            if (response.getBody() == null) {
                throw new RuntimeException("Failed to create credentials");
            }

            return response.getBody();
        } catch (Exception e) {
            log.error("Error creating credentials: {}", e.getMessage());
            throw new RuntimeException("Failed to create credentials: " + e.getMessage());
        }
    }

    private void updateCredentialsInAuthService(Long userId, AdminUserUpdateRequest request) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> updates = new HashMap<>();
            updates.put("userId", userId);
            if (request.username() != null) updates.put("username", request.username());
            if (request.password() != null) updates.put("password", request.password());
            if (request.role() != null) updates.put("role", request.role().toUpperCase());

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(updates, headers);

            restTemplate.exchange(
                    AUTH_SERVICE_URL + "/user/" + userId,
                    HttpMethod.PATCH,
                    entity,
                    Map.class
            );

            log.info("Credentials updated successfully: {}", userId);
        } catch (Exception e) {
            log.error("Failed to update credentials: {}", e.getMessage());
            throw new RuntimeException("Failed to update credentials: " + e.getMessage());
        }
    }

    private AdminUserResponse getUserWithCredentials(User user) {
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(
                    AUTH_SERVICE_URL + "/user/" + user.getUserId(),
                    Map.class
            );

            Map<String, Object> credential = response.getBody();
            return buildAdminUserResponse(user, credential);
        } catch (Exception e) {
            log.warn("Credentials not found for user: {}", user.getUserId());
            return new AdminUserResponse(
                    user.getUserId(),
                    user.getFirstName(),
                    user.getLastName(),
                    user.getEmail(),
                    user.getAddress(),
                    null,
                    null,
                    user.getCreatedAt(),
                    user.getUpdatedAt(),
                    null,
                    null
            );
        }
    }

    private AdminUserResponse buildAdminUserResponse(User user, Map<String, Object> credential) {
        return new AdminUserResponse(
                user.getUserId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getAddress(),
                credential != null ? (String) credential.get("username") : null,
                credential != null ? (String) credential.get("role") : null,
                user.getCreatedAt(),
                user.getUpdatedAt(),
                credential != null ? parseDateTime(credential.get("createdAt")) : null,
                credential != null ? parseDateTime(credential.get("updatedAt")) : null
        );
    }

    private LocalDateTime parseDateTime(Object dateTime) {
        if (dateTime == null) return null;
        if (dateTime instanceof LocalDateTime) return (LocalDateTime) dateTime;
        return LocalDateTime.parse(dateTime.toString());
    }
}