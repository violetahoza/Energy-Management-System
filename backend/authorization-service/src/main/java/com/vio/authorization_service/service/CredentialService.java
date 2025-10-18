package com.vio.authorization_service.service;

import com.vio.authorization_service.dto.CredentialRequest;
import com.vio.authorization_service.dto.CredentialResponse;
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
        Credential credential = credentialRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Credentials not found for userId: " + userId));
        return mapToResponse(credential);
    }

    @Transactional
    public CredentialResponse createCredential(CredentialRequest request) {
        log.info("Creating credentials for userId: {}", request.userId());

        if (credentialRepository.existsByUsername(request.username())) {
            throw new RuntimeException("Username already exists: " + request.username());
        }

        if (credentialRepository.findByUserId(request.userId()).isPresent()) {
            throw new RuntimeException("Credentials already exist for userId: " + request.userId());
        }

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
    }

    @Transactional
    public CredentialResponse updateCredential(Long userId, CredentialRequest request) {
        log.info("Updating credentials for userId: {}", userId);

        Credential credential = credentialRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Credentials not found for userId: " + userId));

        boolean updated = false;

        if (request.username() != null && !credential.getUsername().equals(request.username())) {
            if (credentialRepository.existsByUsername(request.username())) {
                throw new RuntimeException("Username already exists: " + request.username());
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
            credential.setUpdatedAt(LocalDateTime.now());
            Credential saved = credentialRepository.save(credential);
            log.info("Credentials updated successfully for userId: {}", userId);
            return mapToResponse(saved);
        }

        return mapToResponse(credential);
    }

    @Transactional
    public void deleteCredential(Long userId) {
        log.info("Deleting credentials for userId: {}", userId);
        Credential credential = credentialRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Credentials not found for userId: " + userId));
        credentialRepository.delete(credential);
        log.info("Credentials deleted successfully for userId: {}", userId);
    }

    public boolean usernameExists(String username) {
        return credentialRepository.existsByUsername(username);
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