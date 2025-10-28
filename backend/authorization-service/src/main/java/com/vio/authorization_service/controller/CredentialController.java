package com.vio.authorization_service.controller;

import com.vio.authorization_service.dto.*;
import com.vio.authorization_service.service.CredentialService;
import io.swagger.v3.oas.annotations.Operation;
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
@Tag(name = "Internal - Credentials", description = "Internal API for credential management (service-to-service)")
public class CredentialController {
    private final CredentialService credentialService;

    @GetMapping("/user/{userId}")
    @Operation(
            summary = "Get credentials by user ID",
            description = "Retrieve user credentials for internal service communication"
    )
    public ResponseEntity<CredentialResponse> getCredentialByUserId(@PathVariable Long userId) {
        log.info("Fetching credentials for userId: {}", userId);
        CredentialResponse credential = credentialService.getCredentialByUserId(userId);
        return ResponseEntity.ok(credential);
    }

    @PostMapping
    @Operation(summary = "Create new user credentials for internal service communication")
    public ResponseEntity<CredentialResponse> createCredential(@Valid @RequestBody CredentialRequest request) {
        log.info("Creating credentials for userId: {}", request.userId());
        CredentialResponse credential = credentialService.createCredential(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(credential);
    }

    @PatchMapping("/user/{userId}")
    @Operation(summary = "Update user credentials for internal service communication")
    public ResponseEntity<CredentialResponse> updateCredential(@PathVariable Long userId, @Valid @RequestBody CredentialUpdateRequest request) {
        log.info("Updating credentials for userId: {}", userId);
        CredentialResponse credential = credentialService.updateCredential(userId, request);
        return ResponseEntity.ok(credential);
    }

    @DeleteMapping("/user/{userId}")
    @Operation(summary = "Delete user credentials for internal service communication")
    public ResponseEntity<Void> deleteCredential(@PathVariable Long userId) {
        log.info("Deleting credentials for userId: {}", userId);
        credentialService.deleteCredential(userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/username/{username}/exists")
    @Operation(summary = "Check if a username already exists in the system")
    public ResponseEntity<Boolean> usernameExists(@PathVariable String username) {
        log.info("Checking if username exists: {}", username);
        boolean exists = credentialService.usernameExists(username);
        return ResponseEntity.ok(exists);
    }
}
