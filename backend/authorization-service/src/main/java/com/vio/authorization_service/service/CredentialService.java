package com.vio.authorization_service.service;

import com.vio.authorization_service.dto.*;
import com.vio.authorization_service.handler.*;
import com.vio.authorization_service.model.Credential;
import com.vio.authorization_service.repository.CredentialRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class CredentialService {
    private final CredentialRepository credentialRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public CredentialResponse getCredentialByUserId(Long userId) {
        log.info("Fetching credentials for userId: {}", userId);
        Credential credential = credentialRepository.findByUserId(userId).orElseThrow(() -> new CredentialNotFoundException(userId));
        return mapToResponse(credential);
    }

    @Transactional
    public CredentialResponse createCredential(CredentialRequest request) {
        log.info("Creating credentials for userId: {}", request.userId());

        validateCredentialCreationRequest(request);

        if (credentialRepository.existsByUsername(request.username())) {
            throw new UsernameAlreadyExistsException(request.username());
        }

        if (credentialRepository.findByUserId(request.userId()).isPresent()) {
            throw new CredentialAlreadyExistsException("Credentials already exist for userId: " + request.userId());
        }

        try {
            Credential credential = Credential.builder()
                    .userId(request.userId())
                    .username(request.username())
                    .password(passwordEncoder.encode(request.password()))
                    .role(request.role().toUpperCase())
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            Credential saved = credentialRepository.save(credential);
            log.info("Credentials created successfully for userId: {}", request.userId());

            return mapToResponse(saved);
        } catch (Exception e) {
            log.error("Error creating credentials for userId: {}", request.userId(), e);
            throw new RuntimeException("Failed to create credentials: " + e.getMessage(), e);
        }
    }

    @Transactional
    public CredentialResponse updateCredential(Long userId, CredentialRequest request) {
        log.info("Updating credentials for userId: {}", userId);

        Credential credential = credentialRepository.findByUserId(userId).orElseThrow(() -> new CredentialNotFoundException(userId));

        boolean updated = false;

        if (request.username() != null && !credential.getUsername().equals(request.username())) {
            if (credentialRepository.existsByUsername(request.username())) {
                throw new UsernameAlreadyExistsException(request.username());
            }
            credential.setUsername(request.username());
            updated = true;
        }

        if (request.password() != null && !request.password().isEmpty()) {
            credential.setPassword(passwordEncoder.encode(request.password()));
            updated = true;
        }

        if (request.role() != null && !credential.getRole().equals(request.role().toUpperCase())) {
            credential.setRole(request.role().toUpperCase());
            updated = true;
        }

        if (updated) {
            try {
                credential.setUpdatedAt(LocalDateTime.now());
                Credential saved = credentialRepository.save(credential);
                log.info("Credentials updated successfully for userId: {}", userId);
                return mapToResponse(saved);
            } catch (Exception e) {
                log.error("Error updating credentials for userId: {}", userId, e);
                throw new RuntimeException("Failed to update credentials: " + e.getMessage(), e);
            }
        }

        return mapToResponse(credential);
    }

    @Transactional
    public void deleteCredential(Long userId) {
        log.info("Deleting credentials for userId: {}", userId);

        Credential credential = credentialRepository.findByUserId(userId).orElseThrow(() -> new CredentialNotFoundException(userId));

        try {
            credentialRepository.delete(credential);
            log.info("Credentials deleted successfully for userId: {}", userId);
        } catch (Exception e) {
            log.error("Error deleting credentials for userId: {}", userId, e);
            throw new RuntimeException("Failed to delete credentials: " + e.getMessage(), e);
        }
    }

    public boolean usernameExists(String username) {
        try {
            return credentialRepository.existsByUsername(username);
        } catch (Exception e) {
            log.error("Error checking username existence: {}", username, e);
            throw new RuntimeException("Failed to check username: " + e.getMessage(), e);
        }
    }

    private void validateCredentialCreationRequest(CredentialRequest request) {
        if (request.userId() == null) {
            throw new InvalidCredentialRequestException("User ID is required");
        }
        if (request.username() == null || request.username().isEmpty()) {
            throw new InvalidCredentialRequestException("Username is required");
        }
        if (request.password() == null || request.password().isEmpty()) {
            throw new InvalidCredentialRequestException("Password is required");
        }
        if (request.role() == null || request.role().isEmpty()) {
            throw new InvalidCredentialRequestException("Role is required");
        }
        validateRole(request.role());
    }

    private void validateRole(String role) {
        if (!role.equalsIgnoreCase("CLIENT") && !role.equalsIgnoreCase("ADMIN")) {
            throw new InvalidCredentialRequestException("Role must be either CLIENT or ADMIN");
        }
    }

    private CredentialResponse mapToResponse(Credential credential) {
        return new CredentialResponse(
                credential.getId(),
                credential.getUserId(),
                credential.getUsername(),
                credential.getRole(),
                credential.getCreatedAt(),
                credential.getUpdatedAt()
        );
    }
}