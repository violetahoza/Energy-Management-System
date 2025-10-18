package com.vio.userservice.service;

import com.vio.userservice.dto.UserDTORequest;
import com.vio.userservice.dto.UserDTOResponse;
import com.vio.userservice.handler.UserEmailAlreadyExistsException;
import com.vio.userservice.handler.UserNotFoundException;
import com.vio.userservice.handler.UsernameAlreadyExistsException;
import com.vio.userservice.model.UserRole;
import com.vio.userservice.repository.UserRepository;
import com.vio.userservice.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    private final UserRepository repository;
    private final RestTemplate restTemplate = new RestTemplate();

    private static final String AUTH_SERVICE_URL = "http://authorization-service:8083/api/auth";

    public List<UserDTOResponse> getAllUsers() {
        log.info("Fetching all users");
        return repository
                .findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public UserDTOResponse findById(Long userId) {
        log.info("Fetching user with id: {}", userId);
        User user = repository
                .findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        return mapToResponse(user);
    }

    public UserDTOResponse createUser(UserDTORequest request) {
        log.info("Creating new user with username: {}", request.username());

        if (repository.existsByUsername(request.username())) {
            throw new UsernameAlreadyExistsException(request.username());
        }

        log.info("Creating new user with email: {}", request.email());

        if (repository.existsByEmail(request.email())) {
            throw new UserEmailAlreadyExistsException(request.email());
        }

        User user = User.builder()
                .username(request.username())
                .firstName(request.firstName())
                .lastName(request.lastName())
                .email(request.email())
                .address(request.address())
                .role(UserRole.valueOf(request.role()))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        User savedUser = repository.save(user);
        log.info("User created successfully with id: {}", savedUser.getUserId());
        return mapToResponse(savedUser);
    }

    public UserDTOResponse updateById(Long userId, UserDTORequest request) {
        log.info("Updating user with id: {}", userId);

        User user =
                repository
                .findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        if (!user.getUsername().equals(request.username()) &&
                repository.existsByUsername(request.username())) {
            throw new UsernameAlreadyExistsException(request.username());
        }

        if (!user.getEmail().equals(request.email()) &&
                repository.existsByEmail(request.email())) {
            throw new UserEmailAlreadyExistsException(request.email());
        }

        boolean usernameChanged = !user.getUsername().equals(request.username());
        boolean roleChanged = !user.getRole().toString().equals(request.role());

        user.setUsername(request.username());
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setEmail(request.email());
        user.setAddress(request.address());
        user.setRole(UserRole.valueOf(request.role()));
        user.setUpdatedAt(LocalDateTime.now());

        User updatedUser = repository.save(user);

        // Synchronize changes with Authorization Service
        if (usernameChanged || roleChanged) {
            syncCredentialUpdate(userId, request.username(), request.role());
        }

        log.info("User updated successfully with id: {}", updatedUser.getUserId());
        return mapToResponse(updatedUser);
    }

    public void deleteById(Long userId) {
        log.info("Deleting user with id: {}", userId);

        User user = repository
                .findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        // Delete from Authorization Service first
        syncCredentialDelete(userId);

        repository.delete(user);
        log.info("User deleted successfully with id: {}", userId);
    }

    private void syncCredentialUpdate(Long userId, String username, String role) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> updateRequest = new HashMap<>();
            updateRequest.put("userId", userId);
            updateRequest.put("username", username);
            updateRequest.put("role", role);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(updateRequest, headers);

            restTemplate.exchange(
                    AUTH_SERVICE_URL + "/sync/update/" + userId,
                    HttpMethod.PUT,
                    entity,
                    Void.class
            );

            log.info("Successfully synced credential update for user: {}", userId);
        } catch (Exception e) {
            log.error("Failed to sync credential update: {}", e.getMessage());
        }
    }

    private void syncCredentialDelete(Long userId) {
        try {
            restTemplate.delete(AUTH_SERVICE_URL + "/sync/delete/" + userId);
            log.info("Successfully synced credential deletion for user: {}", userId);
        } catch (Exception e) {
            log.error("Failed to sync credential deletion: {}", e.getMessage());
        }
    }

    private UserDTOResponse mapToResponse(User user) {
        return new UserDTOResponse(
                user.getUserId(),
                user.getUsername(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getAddress(),
                user.getRole().toString(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
