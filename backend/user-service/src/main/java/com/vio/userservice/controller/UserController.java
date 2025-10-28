package com.vio.userservice.controller;

import com.vio.userservice.dto.*;
import com.vio.userservice.model.User;
import com.vio.userservice.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.*;

import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "User Management", description = "CRUD operations for user accounts")
@SecurityRequirement(name = "bearer-jwt")
public class UserController {
    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all users", description = "Retrieve all users (Admin only)")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('CLIENT') and principal == #userId.toString())")
    @Operation(summary = "Get user by ID", description = "Retrieve user by ID (Admin or own profile)")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long userId) {
        return ResponseEntity.ok(userService.getUserById(userId));
    }


    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create user", description = "Create new user (Admin only)")
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserRequest request) {
        UserResponse user = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }

    @PatchMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('CLIENT') and principal == #userId.toString())")
    @Operation(summary = "Update user", description = "Update user (Admin or own profile)")
    public ResponseEntity<UserResponse> updateUser(@PathVariable Long userId, @Valid @RequestBody UserUpdateRequest request) {
        UserResponse user = userService.updateUser(userId, request);
        return ResponseEntity.ok(user);
    }

    @DeleteMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete user", description = "Delete user (Admin only)")
    public ResponseEntity<Void> deleteUser(@PathVariable Long userId) {
        userService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/internal/profile")
    @Operation(summary = "Create user profile (internal)", description = "Internal service endpoint")
    public ResponseEntity<Map<String, Object>> createUserProfile(@Valid @RequestBody UserProfileRequest request) {
        User user = userService.createUserProfile(request);

        Map<String, Object> response = new HashMap<>();
        response.put("userId", user.getUserId());
        response.put("firstName", user.getFirstName());
        response.put("lastName", user.getLastName());
        response.put("email", user.getEmail());
        response.put("address", user.getAddress());
        response.put("createdAt", user.getCreatedAt());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/internal/profile/{userId}")
    @Operation(summary = "Delete user profile (internal)", description = "Internal service endpoint")
    public ResponseEntity<Void> deleteUserProfile(@PathVariable Long userId) {
        userService.deleteUserProfile(userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/internal/validate/{userId}")
    @Operation(summary = "Validate user exists (internal)", description = "Internal service endpoint")
    public ResponseEntity<Void> validateUserExists(@PathVariable Long userId) {
        log.info("Internal validation request for userId: {}", userId);
        userService.getUserById(userId);
        return ResponseEntity.ok().build();
    }
}