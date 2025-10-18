package com.vio.authorization_service.controller;

import com.vio.authorization_service.model.Credential;
import com.vio.authorization_service.repository.CredentialRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/auth/sync")
@RequiredArgsConstructor
@Slf4j
public class SyncController {
    private final CredentialRepository credentialRepository;

    @PutMapping("/update/{userId}")
    public ResponseEntity<Void> syncUpdate(
            @PathVariable Long userId,
            @RequestBody Map<String, String> request) {
        log.info("Syncing credential update for user: {}", userId);

        credentialRepository.findByUserId(userId).ifPresent(credential -> {
            if (request.containsKey("username")) {
                credential.setUsername(request.get("username"));
            }
            if (request.containsKey("role")) {
                credential.setRole(request.get("role"));
            }
            credential.setUpdatedAt(LocalDateTime.now());
            credentialRepository.save(credential);
            log.info("Credential updated successfully for user: {}", userId);
        });

        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/delete/{userId}")
    public ResponseEntity<Void> syncDelete(@PathVariable Long userId) {
        log.info("Syncing credential deletion for user: {}", userId);

        credentialRepository.findByUserId(userId).ifPresent(credential -> {
            credentialRepository.delete(credential);
            log.info("Credential deleted successfully for user: {}", userId);
        });

        return ResponseEntity.ok().build();
    }
}