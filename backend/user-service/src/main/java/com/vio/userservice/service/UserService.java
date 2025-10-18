package com.vio.userservice.service;

import com.vio.userservice.dto.UserDTORequest;
import com.vio.userservice.dto.UserDTOResponse;
import com.vio.userservice.handler.UserEmailAlreadyExistsException;
import com.vio.userservice.handler.UserNotFoundException;
import com.vio.userservice.repository.UserRepository;
import com.vio.userservice.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
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
        log.info("Creating new user with email: {}", request.email());

        if (repository.existsByEmail(request.email())) {
            throw new UserEmailAlreadyExistsException(request.email());
        }

        User user = User.builder()
                .firstName(request.firstName())
                .lastName(request.lastName())
                .email(request.email())
                .address(request.address())
                .build();

        User savedUser = repository.save(user);
        log.info("User created successfully with id: {}", savedUser.getUserId());
        return mapToResponse(savedUser);
    }

    public UserDTOResponse updateById(Long userId, Map<String, Object> updates) {
        log.info("Partial update for user: {}", userId);

        User user = repository
                .findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));

        if (updates.containsKey("firstName")) {
            user.setFirstName((String) updates.get("firstName"));
        }
        if (updates.containsKey("lastName")) {
            user.setLastName((String) updates.get("lastName"));
        }
        if (updates.containsKey("email")) {
            String newEmail = (String) updates.get("email");
            if (!user.getEmail().equals(newEmail) && repository.existsByEmail(newEmail)) {
                throw new UserEmailAlreadyExistsException("Email already exists: " + newEmail);
            }
            user.setEmail(newEmail);
        }
        if (updates.containsKey("address")) {
            user.setAddress((String) updates.get("address"));
        }

        user.setUpdatedAt(LocalDateTime.now());
        User updatedUser = repository.save(user);

        log.info("User profile partially updated: {}", userId);
        return mapToResponse(updatedUser);
    }


    public void deleteById(Long userId) {
        log.info("Deleting user with id: {}", userId);

        User user = repository
                .findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        repository.delete(user);
        log.info("User deleted successfully with id: {}", userId);

        syncCredentialDelete(userId);
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
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getAddress(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
