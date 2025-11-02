package com.vio.userservice.controller;

import com.vio.userservice.dto.*;
import com.vio.userservice.dto.ErrorResponse;
import com.vio.userservice.model.User;
import com.vio.userservice.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
public class UserController {
    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Retrieve all users (Admin only)",
            description = "@PreAuthorize(\"hasRole('ADMIN')\") checks if the authenticated user has ADMIN role. If authorized, calls userService.getAllUsers() to fetch and return the list of all users. If not authorized, Spring Security throws AccessDeniedException (403 Forbidden)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved all users", content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - User does not have ADMIN role", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('CLIENT') and principal == #userId.toString())")
    @Operation(summary = "Get user by ID", description = "Retrieve user by ID (clients can only view their own profile; admins can view anyone)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved user", content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - Client attempting to access another user's profile", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "User not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long userId) {
        return ResponseEntity.ok(userService.getUserById(userId));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create user", description = "Only admins can create new users")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "User created successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "400", description = "Bad Request - Invalid input data", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - User does not have ADMIN role", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Conflict - Email or username already exists", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "503", description = "Service Unavailable - Cannot communicate with Authorization Service", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserRequest request) {
        UserResponse user = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }

    @PatchMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('CLIENT') and principal == #userId.toString())")
    @Operation(summary = "Update user", description = "Update user (clients can only update their own profile; admins can update anyone). PATCH allows partial updates (unlike PUT which requires full object)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User updated successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "400", description = "Bad Request - Validation failed or no fields to update", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - Client attempting to update another user's profile", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "User not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Conflict - Email or username already exists", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<UserResponse> updateUser(@PathVariable Long userId, @Valid @RequestBody UserRequest request) {
        UserResponse user = userService.updateUser(userId, request);
        return ResponseEntity.ok(user);
    }

    @DeleteMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete user", description = "Only admins can delete users. Deleting a user triggers cascading deletion in other services (credentials, device assignments).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "User deleted successfully - No content returned"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - User does not have ADMIN role", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "User not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> deleteUser(@PathVariable Long userId) {
        userService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/internal/profile")
    @Operation(summary = "Create user profile (internal)", description = "Internal service endpoint to create user profile without credentials. Used by Authorization Service during user registration.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "User profile created successfully"),
            @ApiResponse(responseCode = "400", description = "Bad Request - Validation failed", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Conflict - Email already exists", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
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
    @Operation(summary = "Delete user profile (internal)", description = "Internal service endpoint to delete user profile by userId. Used by Authorization Service when deleting user credentials.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "User profile deleted successfully - No content returned"),
            @ApiResponse(responseCode = "404", description = "User not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> deleteUserProfile(@PathVariable Long userId) {
        userService.deleteUserProfile(userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/internal/validate/{userId}")
    @Operation(summary = "Validate user exists (internal)", description = "Internal service endpoint to validate if a user exists by userId. Used by other services to ensure user existence before performing operations.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User exists - Validation successful"),
            @ApiResponse(responseCode = "404", description = "User not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> validateUserExists(@PathVariable Long userId) {
        log.info("Internal validation request for userId: {}", userId);
        userService.getUserById(userId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/internal/role/{userId}")
    @Operation(summary = "Get user role (internal)", description = "Internal service endpoint to get a user's role by userId. Used by other services for role-based validation.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User exists - Returns user role"),
            @ApiResponse(responseCode = "404", description = "User not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Map<String, String>> getUserRole(@PathVariable Long userId) {
        log.info("Internal role request for userId: {}", userId);
        UserResponse user = userService.getUserById(userId);

        Map<String, String> response = new HashMap<>();
        response.put("userId", String.valueOf(userId));
        response.put("role", user.role());

        return ResponseEntity.ok(response);
    }
}