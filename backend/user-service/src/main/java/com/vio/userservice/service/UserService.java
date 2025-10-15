package com.vio.userservice.service;

import com.vio.userservice.dto.UserDTORequest;
import com.vio.userservice.dto.UserDTOResponse;
import com.vio.userservice.handler.UserEmailAlreadyExistsException;
import com.vio.userservice.handler.UserNotFoundException;
import com.vio.userservice.model.UserRole;
import com.vio.userservice.repository.UserRepository;
import com.vio.userservice.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    private final UserRepository repository;

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
                .name(request.name())
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

        if (!user.getEmail().equals(request.email()) &&
                repository.existsByEmail(request.email())) {
            throw new UserEmailAlreadyExistsException(request.email());
        }

        user.setName(request.name());
        user.setEmail(request.email());
        user.setAddress(request.address());
        user.setRole(UserRole.valueOf(request.role()));
        user.setUpdatedAt(LocalDateTime.now());

        User updatedUser = repository.save(user);
        log.info("User updated successfully with id: {}", updatedUser.getUserId());
        return mapToResponse(updatedUser);
    }

    public void deleteById(Long userId) {
        log.info("Deleting user with id: {}", userId);

        User user = repository
                .findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        repository.delete(user);
        log.info("User deleted successfully with id: {}", userId);
    }

    private UserDTOResponse mapToResponse(User user) {
        return new UserDTOResponse(
                user.getUserId(),
                user.getName(),
                user.getEmail(),
                user.getAddress(),
                user.getRole().toString(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
