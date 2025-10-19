package com.vio.userservice.service;

import com.vio.userservice.dto.AdminUserRequest;
import com.vio.userservice.dto.AdminUserResponse;
import com.vio.userservice.dto.AdminUserUpdateRequest;
import com.vio.userservice.handler.*;
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
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
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
    private static final String DEVICE_SERVICE_URL = "http://device-service:8082/api/devices/sync/unassign-user";

    public List<AdminUserResponse> getAllUsers() {
        log.info("Admin fetching all users with credentials");

        try {
            List<User> users = userRepository.findAll();
            return users.stream()
                    .map(this::getUserWithCredentials)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error fetching all users: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch users", e);
        }
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

        if (userRepository.existsByEmail(request.email())) {
            throw new UserEmailAlreadyExistsException(request.email());
        }

        validateUsernameUniqueness(request.username());

        User user = User.builder()
                .firstName(request.firstName())
                .lastName(request.lastName())
                .email(request.email())
                .address(request.address())
                .build();

        User savedUser = userRepository.save(user);
        log.info("User profile created with id: {}", savedUser.getUserId());

        Map<String, Object> credentialData;
        try {
            credentialData = createCredentialsInAuthService(
                    savedUser.getUserId(),
                    request.username(),
                    request.password(),
                    request.role()
            );
        } catch (Exception e) {
            log.error("Failed to create credentials, transaction will rollback: {}", e.getMessage());
            throw e;
        }


        log.info("User and credentials created successfully: {}", request.username());
        return buildAdminUserResponse(savedUser, credentialData);
    }

    @Transactional
    public AdminUserResponse updateUser(Long userId, AdminUserUpdateRequest request) {
        log.info("Admin updating user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        boolean profileUpdated = false;
        boolean credentialsUpdated = false;

        if (hasProfileUpdates(request)) {
            updateUserProfile(user, request);
            profileUpdated = true;
        }

        if (hasCredentialUpdates(request)) {
            updateCredentialsInAuthService(userId, request);
            credentialsUpdated = true;
        }

        if (!profileUpdated && !credentialsUpdated) {
            throw new InvalidUpdateException("No fields to update");
        }

        log.info("User updated successfully: {}", userId);
        return getUserWithCredentials(user);
    }

    @Transactional
    public void deleteUser(Long userId) {
        log.info("Admin deleting user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        deleteCredentialsInAuthService(userId);
        notifyDeviceServiceUserDeletion(userId);

        userRepository.delete(user);
        log.info("User profile deleted: {}", userId);

        log.info("User deleted successfully: {}", userId);
    }

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
                throw new ServiceCommunicationException("authorization-service",
                        "Empty response from credentials creation");
            }

            return response.getBody();
        } catch (HttpClientErrorException e) {
            log.error("HTTP error creating credentials: {} - {}", e.getStatusCode(), e.getMessage());
            throw new ServiceCommunicationException("authorization-service",
                    "Failed to create credentials: " + e.getStatusText(), e);
        } catch (ResourceAccessException e) {
            log.error("Network error creating credentials: {}", e.getMessage());
            throw new ServiceCommunicationException("authorization-service",
                    "Unable to reach authorization service", e);
        } catch (ServiceCommunicationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error creating credentials: {}", e.getMessage());
            throw new ServiceCommunicationException("authorization-service",
                    "Unexpected error: " + e.getMessage(), e);
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
        } catch (HttpClientErrorException e) {
            log.error("HTTP error updating credentials: {} - {}", e.getStatusCode(), e.getMessage());
            throw new ServiceCommunicationException("authorization-service",
                    "Failed to update credentials: " + e.getStatusText(), e);
        } catch (ResourceAccessException e) {
            log.error("Network error updating credentials: {}", e.getMessage());
            throw new ServiceCommunicationException("authorization-service",
                    "Unable to reach authorization service", e);
        } catch (Exception e) {
            log.error("Unexpected error updating credentials: {}", e.getMessage());
            throw new ServiceCommunicationException("authorization-service",
                    "Unexpected error: " + e.getMessage(), e);
        }
    }

    private void deleteCredentialsInAuthService(Long userId) {
        try {
            restTemplate.delete(AUTH_SERVICE_URL + "/user/" + userId);
            log.info("Credentials deleted for user: {}", userId);
        } catch (HttpClientErrorException.NotFound e) {
            log.warn("Credentials not found for user: {}", userId);
            // Continue - credentials already deleted or never existed
        } catch (HttpClientErrorException e) {
            log.error("HTTP error deleting credentials: {} - {}", e.getStatusCode(), e.getMessage());
            throw new ServiceCommunicationException("authorization-service",
                    "Failed to delete credentials: " + e.getStatusText(), e);
        } catch (ResourceAccessException e) {
            log.error("Network error deleting credentials: {}", e.getMessage());
            throw new ServiceCommunicationException("authorization-service",
                    "Unable to reach authorization service", e);
        } catch (Exception e) {
            log.error("Unexpected error deleting credentials: {}", e.getMessage());
            throw new ServiceCommunicationException("authorization-service",
                    "Unexpected error: " + e.getMessage(), e);
        }
    }

    private void notifyDeviceServiceUserDeletion(Long userId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            restTemplate.postForEntity(
                    DEVICE_SERVICE_URL + "/" + userId,
                    entity,
                    Void.class
            );
            log.info("Device unassignment triggered for user: {}", userId);
        } catch (Exception e) {
            log.warn("Failed to notify Device Service about user deletion: {}", e.getMessage());
            // Non-critical - continue with user deletion
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
        } catch (HttpClientErrorException.NotFound e) {
            log.warn("Credentials not found for user: {}", user.getUserId());
            return buildAdminUserResponse(user, null);
        } catch (Exception e) {
            log.warn("Error fetching credentials for user {}: {}", user.getUserId(), e.getMessage());
            return buildAdminUserResponse(user, null);
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
        try {
            return LocalDateTime.parse(dateTime.toString());
        } catch (Exception e) {
            log.warn("Failed to parse datetime: {}", dateTime);
            return null;
        }
    }

    private void validateUsernameUniqueness(String username) {
        try {
            ResponseEntity<Boolean> response = restTemplate.getForEntity(
                    AUTH_SERVICE_URL + "/username/" + username + "/exists",
                    Boolean.class
            );

            if (Boolean.TRUE.equals(response.getBody())) {
                throw new UsernameAlreadyExistsException(username);
            }
        } catch (HttpClientErrorException e) {
            log.error("HTTP error checking username: {} - {}", e.getStatusCode(), e.getMessage());
            throw new ServiceCommunicationException("authorization-service",
                    "Error checking username uniqueness: " + e.getStatusText(), e);
        } catch (ResourceAccessException e) {
            log.error("Network error checking username: {}", e.getMessage());
            throw new ServiceCommunicationException("authorization-service",
                    "Unable to reach authorization service", e);
        } catch (UsernameAlreadyExistsException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error checking username: {}", e.getMessage());
            throw new ServiceCommunicationException("authorization-service",
                    "Unexpected error: " + e.getMessage(), e);
        }
    }
}