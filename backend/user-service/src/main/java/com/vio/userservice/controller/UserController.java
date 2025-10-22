package com.vio.userservice.controller;

import com.vio.userservice.dto.UserProfileRequest;
import com.vio.userservice.dto.UserRequest;
import com.vio.userservice.dto.UserResponse;
import com.vio.userservice.dto.UserUpdateRequest;
import com.vio.userservice.model.User;
import com.vio.userservice.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {
    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('CLIENT') and principal == #userId.toString())")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long userId) {
        return ResponseEntity.ok(userService.getUserById(userId));
    }


    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserRequest request) {
        UserResponse user = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }

    @PatchMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('CLIENT') and principal == #userId.toString())")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable Long userId,
            @Valid @RequestBody UserUpdateRequest request) {
        UserResponse user = userService.updateUser(userId, request);
        return ResponseEntity.ok(user);
    }

    @DeleteMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable Long userId) {
        userService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/internal/profile")
    public ResponseEntity<Map<String, Object>> createUserProfile(
            @Valid @RequestBody UserProfileRequest request) {
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
    public ResponseEntity<Void> deleteUserProfile(@PathVariable Long userId) {
        userService.deleteUserProfile(userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/internal/validate/{userId}")
    public ResponseEntity<Void> validateUserExists(@PathVariable Long userId) {
        log.info("Internal validation request for userId: {}", userId);
        userService.getUserById(userId); // this will throw UserNotFoundException if not found
        return ResponseEntity.ok().build();
    }
}