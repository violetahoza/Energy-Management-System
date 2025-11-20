package com.vio.userservice.service;

import com.vio.userservice.client.AuthServiceClient;
import com.vio.userservice.dto.*;
import com.vio.userservice.handler.*;
import com.vio.userservice.model.User;
import com.vio.userservice.publisher.UserEventPublisher;
import com.vio.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    private final UserRepository userRepository;
    private final UserEventPublisher userEventPublisher;
    private final AuthServiceClient authServiceClient;

    public List<UserResponse> getAllUsers() {
        log.info("Fetching all users");
        try {
            List<User> users = userRepository.findAll();

            List<Long> userIds = users.stream()
                    .map(User::getUserId)
                    .collect(Collectors.toList());

            Map<Long, Map<String, String>> credentialsMap =
                    authServiceClient.getUserCredentialsBatch(userIds);

            return users.stream()
                    .map(user -> buildUserResponseWithCredentials(user, credentialsMap.get(user.getUserId())))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error fetching all users: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch users", e);
        }
    }

    public UserResponse getUserById(Long userId) {
        log.info("Fetching user by id: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        return buildUserResponse(user);
    }

    @Transactional
    public UserResponse createUser(UserRequest request) {
        log.info("Creating new user with username: {}", request.username());

        validateUserCreationRequest(request);

        if (userRepository.existsByEmail(request.email())) {
            throw new UserEmailAlreadyExistsException(request.email());
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

        // Publish sync event pentru Authorization Service și Device Service
        userEventPublisher.publishUserCreated(
                savedUser.getUserId(),
                request.username(),
                request.password(),
                request.role()
        );

        // Build response cu credentials
        return buildUserResponseWithCredentials(savedUser, Map.of(
                "username", request.username(),
                "role", request.role()
        ));
    }

    @Transactional
    public UserResponse updateUser(Long userId, UserRequest request) {
        log.info("Updating user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        boolean hasProfileUpdates = hasProfileUpdates(request);
        boolean hasCredentialUpdates = hasCredentialUpdates(request);

        if (!hasProfileUpdates && !hasCredentialUpdates) {
            throw new InvalidUpdateException("No fields to update");
        }

        if (hasProfileUpdates) {
            updateUserProfile(user, request);
        }

        if (hasCredentialUpdates) {
            userEventPublisher.publishUserUpdated(
                    userId,
                    request.username(),
                    request.password(),
                    request.role()
            );
        }

        return buildUserResponse(user);
    }

    @Transactional
    public void deleteUser(Long userId) {
        log.info("Deleting user: {}", userId);

        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException(userId);
        }

        userRepository.deleteById(userId);
        log.info("User deleted: {}", userId);

        userEventPublisher.publishUserDeleted(userId);
    }

    @Transactional
    public User createUserProfile(UserProfileRequest request) {
        log.info("Creating user profile for email: {}", request.email());

        if (userRepository.existsByEmail(request.email())) {
            throw new UserEmailAlreadyExistsException(request.email());
        }

        User user = User.builder()
                .firstName(request.firstName())
                .lastName(request.lastName())
                .email(request.email())
                .address(request.address())
                .build();

        return userRepository.save(user);
    }

    @Transactional
    public void deleteUserProfile(Long userId) {
        log.info("Deleting user profile: {}", userId);

        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException(userId);
        }

        userRepository.deleteById(userId);
    }

    public boolean validateUserExists(Long userId) {
        return userRepository.existsById(userId);
    }

    private void validateUserCreationRequest(UserRequest request) {
        if (request.username() == null || request.username().isEmpty()) {
            throw new InvalidUserCreationException("Username is required");
        }
        if (request.password() == null || request.password().isEmpty()) {
            throw new InvalidUserCreationException("Password is required");
        }
        if (request.role() == null || request.role().isEmpty()) {
            throw new InvalidUserCreationException("Role is required");
        }
        if (request.firstName() == null || request.firstName().isEmpty()) {
            throw new InvalidUserCreationException("First name is required");
        }
        if (request.lastName() == null || request.lastName().isEmpty()) {
            throw new InvalidUserCreationException("Last name is required");
        }
        if (request.address() == null || request.address().isEmpty()) {
            throw new InvalidUserCreationException("Address is required");
        }
    }

    private boolean hasProfileUpdates(UserRequest request) {
        return request.firstName() != null || request.lastName() != null ||
                request.email() != null || request.address() != null;
    }

    private boolean hasCredentialUpdates(UserRequest request) {
        return request.username() != null || request.password() != null || request.role() != null;
    }

    private void updateUserProfile(User user, UserRequest request) {
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

    private UserResponse buildUserResponse(User user) {
        // Fetch credentials de la Authorization Service
        Map<String, String> credentials = authServiceClient.getUserCredentials(user.getUserId());

        return buildUserResponseWithCredentials(user, credentials);
    }

    private UserResponse buildUserResponseWithCredentials(User user, Map<String, String> credentials) {
        String username = credentials != null ? credentials.get("username") : null;
        String role = credentials != null ? credentials.get("role") : null;

        return new UserResponse(
                user.getUserId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getAddress(),
                username,
                role,
                user.getCreatedAt(),
                user.getUpdatedAt(),
                null,
                null
        );
    }
}