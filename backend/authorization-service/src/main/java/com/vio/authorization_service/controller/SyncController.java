package com.vio.authorization_service.controller;

import com.vio.authorization_service.repository.CredentialRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth/sync")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Internal - Auth Sync")
public class SyncController {
    private final CredentialRepository credentialRepository;

    @DeleteMapping("/delete/{userId}")
    @Operation(summary = "Sync credential deletion (internal)")
    public ResponseEntity<Void> syncDelete(@PathVariable Long userId) {
        log.info("Syncing credential deletion for user: {}", userId);

        credentialRepository.findByUserId(userId).ifPresent(credential -> {
            credentialRepository.delete(credential);
            log.info("Credential deleted successfully for user: {}", userId);
        });

        return ResponseEntity.ok().build();
    }
}