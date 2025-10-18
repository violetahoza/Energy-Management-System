package com.vio.authorization_service.controller;

import com.vio.authorization_service.dto.CredentialRequest;
import com.vio.authorization_service.dto.CredentialResponse;
import com.vio.authorization_service.service.CredentialService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Internal controller for credential management
 * Used by user-service for admin operations
 */
@RestController
@RequestMapping("/api/auth/internal/credentials")
@RequiredArgsConstructor
@Slf4j
public class CredentialController {
    private final CredentialService credentialService;

    @GetMapping("/user/{userId}")
    public ResponseEntity<CredentialResponse> getCredentialByUserId(@PathVariable Long userId) {
        log.info("Fetching credentials for userId: {}", userId);
        CredentialResponse credential = credentialService.getCredentialByUserId(userId);
        return ResponseEntity.ok(credential);
    }

    @PostMapping
    public ResponseEntity<CredentialResponse> createCredential(
            @Valid @RequestBody CredentialRequest request) {
        log.info("Creating credentials for userId: {}", request.userId());
        CredentialResponse credential = credentialService.createCredential(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(credential);
    }

    @PatchMapping("/user/{userId}")
    public ResponseEntity<CredentialResponse> updateCredential(
            @PathVariable Long userId,
            @Valid @RequestBody CredentialRequest request) {
        log.info("Updating credentials for userId: {}", userId);
        CredentialResponse credential = credentialService.updateCredential(userId, request);
        return ResponseEntity.ok(credential);
    }

    @DeleteMapping("/user/{userId}")
    public ResponseEntity<Void> deleteCredential(@PathVariable Long userId) {
        log.info("Deleting credentials for userId: {}", userId);
        credentialService.deleteCredential(userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/username/{username}/exists")
    public ResponseEntity<Boolean> usernameExists(@PathVariable String username) {
        log.info("Checking if username exists: {}", username);
        boolean exists = credentialService.usernameExists(username);
        return ResponseEntity.ok(exists);
    }
}
