package com.vio.userservice.service;

import com.vio.userservice.dto.UserDTORequest;
import com.vio.userservice.dto.UserDTOResponse;
import com.vio.userservice.handler.UserEmailAlreadyExistsException;
import com.vio.userservice.handler.InvalidUpdateException;
import com.vio.userservice.handler.UserNotFoundException;
import com.vio.userservice.repository.UserRepository;
import com.vio.userservice.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    private final UserRepository repository;
    //private final RestTemplate restTemplate = new RestTemplate();

    //private static final String AUTH_SERVICE_URL = "http://authorization-service:8083/api/auth";

//    public List<UserDTOResponse> getAllUsers() {
//        log.info("Fetching all users");
//        return repository
//                .findAll()
//                .stream()
//                .map(this::mapToResponse)
//                .toList();
//    }

    public UserDTOResponse findById(Long userId) {
        log.info("Fetching user with id: {}", userId);

        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("User ID must be a positive number");
        }

        User user = repository
                .findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        log.debug("User found: {}", user.getEmail());
        return mapToResponse(user);
    }

    public UserDTOResponse createUser(UserDTORequest request) {
        log.info("Creating new user with email: {}", request.email());

        if (repository.existsByEmail(request.email())) {
            log.warn("Attempt to create user with existing email: {}", request.email());
            throw new UserEmailAlreadyExistsException(request.email());
        }

        try {
            User user = User.builder()
                    .firstName(request.firstName())
                    .lastName(request.lastName())
                    .email(request.email())
                    .address(request.address())
                    .build();

            User savedUser = repository.save(user);
            log.info("User created successfully with id: {}", savedUser.getUserId());

            return mapToResponse(savedUser);
        } catch (Exception e) {
            log.error("Unexpected error creating user: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create user: " + e.getMessage(), e);
        }
    }

    public UserDTOResponse updateById(Long userId, Map<String, Object> updates) {
        log.info("Update for user: {} with {} fields", userId, updates.size());

        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("User ID must be a positive number");
        }

        if (updates == null || updates.isEmpty()) {
            throw new InvalidUpdateException("No fields provided for update");
        }

        User user = repository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        boolean hasUpdates = false;

        if (updates.containsKey("firstName")) {
            String firstName = (String) updates.get("firstName");
            if (firstName == null || firstName.trim().isEmpty()) {
                throw new IllegalArgumentException("First name cannot be empty");
            }
            user.setFirstName(firstName);
            hasUpdates = true;
            log.debug("Updating firstName for user: {}", userId);
        }

        if (updates.containsKey("lastName")) {
            String lastName = (String) updates.get("lastName");
            if (lastName == null || lastName.trim().isEmpty()) {
                throw new IllegalArgumentException("Last name cannot be empty");
            }
            user.setLastName(lastName);
            hasUpdates = true;
            log.debug("Updating lastName for user: {}", userId);
        }

        if (updates.containsKey("email")) {
            String newEmail = (String) updates.get("email");
            if (newEmail == null || newEmail.trim().isEmpty()) {
                throw new IllegalArgumentException("Email cannot be empty");
            }

            if (!user.getEmail().equals(newEmail)) {
                if (repository.existsByEmail(newEmail)) {
                    log.warn("Attempt to update to existing email: {}", newEmail);
                    throw new UserEmailAlreadyExistsException(newEmail);
                }
                user.setEmail(newEmail);
                hasUpdates = true;
                log.debug("Updating email for user: {}", userId);
            }
        }

        if (updates.containsKey("address")) {
            String address = (String) updates.get("address");
            user.setAddress(address);
            hasUpdates = true;
            log.debug("Updating address for user: {}", userId);
        }

        if (!hasUpdates) {
            throw new InvalidUpdateException("No valid fields provided for update");
        }

        user.setUpdatedAt(LocalDateTime.now());

        try {
            User updatedUser = repository.save(user);
            log.info("User profile partially updated: {}", userId);
            return mapToResponse(updatedUser);
        } catch (Exception e) {
            log.error("Error updating user {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Failed to update user: " + e.getMessage(), e);
        }
    }

//    public void deleteById(Long userId) {
//        log.info("Deleting user with id: {}", userId);
//
//        User user = repository
//                .findById(userId)
//                .orElseThrow(() -> new UserNotFoundException(userId));
//
//        repository.delete(user);
//        log.info("User deleted successfully with id: {}", userId);
//
//        syncCredentialDelete(userId);
//    }

//    private void syncCredentialDelete(Long userId) {
//        try {
//            restTemplate.delete(AUTH_SERVICE_URL + "/sync/delete/" + userId);
//            log.info("Successfully synced credential deletion for user: {}", userId);
//        } catch (Exception e) {
//            log.error("Failed to sync credential deletion: {}", e.getMessage());
//        }
//    }

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
