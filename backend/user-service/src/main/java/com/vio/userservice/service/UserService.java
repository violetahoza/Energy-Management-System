package com.vio.userservice.service;

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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    private final UserRepository userRepository;
    private final UserEventPublisher userEventPublisher;

    public List<UserResponse> getAllUsers() {
        log.info("Fetching all users");
        try {
            List<User> users = userRepository.findAll();
            return users.stream()
                    .map(this::buildUserResponse)
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

        // Publish event to create credentials in Auth Service
        userEventPublisher.publishUserCreated(
                savedUser.getUserId(),
                request.username(),
                request.password(),
                request.role()
        );

        log.info("User created successfully: {}", request.username());
        return buildUserResponse(savedUser);
    }

    @Transactional
    public UserResponse updateUser(Long userId, UserRequest request) {
        log.info("Updating user with id: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        boolean profileUpdated = false;
        boolean credentialsUpdated = false;

        // Update profile fields
        if (hasProfileUpdates(request)) {
            updateUserProfile(user, request);
            profileUpdated = true;
        }

        // Update credentials via RabbitMQ
        if (hasCredentialUpdates(request)) {
            userEventPublisher.publishUserUpdated(
                    userId,
                    request.username(),
                    request.password(),
                    request.role()
            );
            credentialsUpdated = true;
        }

        if (!profileUpdated && !credentialsUpdated) {
            throw new InvalidUpdateException("No fields to update");
        }

        log.info("User updated successfully: {}", userId);
        return buildUserResponse(user);
    }

    @Transactional
    public User createUserProfile(UserProfileRequest request) {
        log.info("Creating user profile only: {}", request.email());

        if (userRepository.existsByEmail(request.email())) {
            throw new UserEmailAlreadyExistsException(request.email());
        }

        User user = User.builder()
                .firstName(request.firstName())
                .lastName(request.lastName())
                .email(request.email())
                .address(request.address())
                .build();

        User savedUser = userRepository.save(user);
        log.info("User profile created with id: {}", savedUser.getUserId());

        return savedUser;
    }

    @Transactional
    public void deleteUserProfile(Long userId) {
        log.info("Deleting user profile: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        userRepository.delete(user);
        log.info("User profile deleted: {}", userId);
    }

    @Transactional
    public void deleteUser(Long userId) {
        log.info("Deleting user: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        // Delete user profile
        userRepository.delete(user);
        log.info("User profile deleted: {}", userId);

        // Publish event to delete credentials in Auth Service
        userEventPublisher.publishUserDeleted(userId);

        log.info("User deletion event published for userId: {}", userId);
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
        if (!request.role().equalsIgnoreCase("CLIENT") && !request.role().equalsIgnoreCase("ADMIN")) {
            throw new InvalidUserCreationException("Role must be either CLIENT or ADMIN");
        }
        if (request.email() == null || request.email().isEmpty()) {
            throw new InvalidUserCreationException("Email is required");
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
        return new UserResponse(
                user.getUserId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getAddress(),
                null, // username will be fetched separately if needed
                null, // role will be fetched separately if needed
                user.getCreatedAt(),
                user.getUpdatedAt(),
                null,
                null
        );
    }
}