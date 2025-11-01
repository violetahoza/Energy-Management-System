package com.vio.authorization_service.controller;

import com.vio.authorization_service.dto.*;
import com.vio.authorization_service.service.CredentialService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth/internal/credentials")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Internal - Credentials", description = "Internal API for credential management between microservices. These endpoints are not intended for direct client access.")
public class CredentialController {
    private final CredentialService credentialService;

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get credentials by user ID", description = "Retrieve user credentials for internal service communication. Used by User Service to fetch authentication details for a specific user.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Credentials retrieved successfully", content = @Content(schema = @Schema(implementation = CredentialResponse.class))),
            @ApiResponse(responseCode = "404", description = "Credentials not found for the specified user ID", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<CredentialResponse> getCredentialByUserId(@PathVariable Long userId) {
        log.info("Fetching credentials for userId: {}", userId);
        CredentialResponse credential = credentialService.getCredentialByUserId(userId);
        return ResponseEntity.ok(credential);
    }

    @PostMapping
    @Operation(summary = "Create new user credentials", description = "Create authentication credentials for a user. This endpoint is called internally by User Service when a new user needs authentication credentials.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Credentials created successfully", content = @Content(schema = @Schema(implementation = CredentialResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request data or validation failed", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Username already exists or credentials already exist for this user", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error during credential creation", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<CredentialResponse> createCredential(@Valid @RequestBody CredentialRequest request) {
        log.info("Creating credentials for userId: {}", request.userId());
        CredentialResponse credential = credentialService.createCredential(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(credential);
    }

    @PatchMapping("/user/{userId}")
    @Operation(summary = "Update user credentials", description = "Update existing user credentials (username, password, or role). All fields are optional - only provided fields will be updated.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Credentials updated successfully", content = @Content(schema = @Schema(implementation = CredentialResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request data", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Credentials not found for the specified user ID", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Username already exists", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error during update", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<CredentialResponse> updateCredential(@PathVariable Long userId, @Valid @RequestBody CredentialUpdateRequest request) {
        log.info("Updating credentials for userId: {}", userId);
        CredentialResponse credential = credentialService.updateCredential(userId, request);
        return ResponseEntity.ok(credential);
    }

    @DeleteMapping("/user/{userId}")
    @Operation(summary = "Delete user credentials", description = "Delete authentication credentials for a user. This is called when a user account is deleted to ensure all authentication data is removed from the system.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Credentials deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Credentials not found for the specified user ID", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error during deletion", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> deleteCredential(@PathVariable Long userId) {
        log.info("Deleting credentials for userId: {}", userId);
        credentialService.deleteCredential(userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/username/{username}/exists")
    @Operation(summary = "Check username availability", description = "Check if a username already exists in the system. Used for username validation during registration or profile updates to ensure uniqueness.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Check completed - returns true if username exists, false otherwise"),
            @ApiResponse(responseCode = "500", description = "Internal server error during username check", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Boolean> usernameExists(@PathVariable String username) {
        log.info("Checking if username exists: {}", username);
        boolean exists = credentialService.usernameExists(username);
        return ResponseEntity.ok(exists);
    }
}
