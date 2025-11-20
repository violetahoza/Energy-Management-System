package com.vio.authorization_service.consumer;

import com.vio.authorization_service.config.RabbitMQConfig;
import com.vio.authorization_service.event.UserSyncEvent;
import com.vio.authorization_service.model.Credential;
import com.vio.authorization_service.model.SyncUser;
import com.vio.authorization_service.repository.CredentialRepository;
import com.vio.authorization_service.repository.SyncUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserSyncConsumer {

    private final CredentialRepository credentialRepository;
    private final SyncUserRepository syncUserRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @RabbitListener(queues = RabbitMQConfig.USER_SYNC_QUEUE_AUTH)
    @Transactional
    public void handleUserSyncEvent(UserSyncEvent event) {
        log.info("Received user sync event: {} for userId: {}", event.getEventType(), event.getUserId());

        try {
            switch (event.getEventType()) {
                case "CREATED":
                    handleUserCreated(event);
                    break;
                case "UPDATED":
                    handleUserUpdated(event);
                    break;
                case "DELETED":
                    handleUserDeleted(event);
                    break;
                default:
                    log.warn("Unknown event type: {}", event.getEventType());
            }
        } catch (Exception e) {
            log.error("Error processing user sync event: {}", e.getMessage(), e);
            throw e;
        }
    }

    private void handleUserCreated(UserSyncEvent event) {
        log.info("Creating credentials for userId: {}", event.getUserId());

        if (credentialRepository.findByUserId(event.getUserId()).isPresent()) {
            log.warn("Credentials already exist for userId: {}", event.getUserId());
            return;
        }

        if (credentialRepository.existsByUsername(event.getUsername())) {
            log.warn("Username already exists: {}", event.getUsername());
            throw new RuntimeException("Username already exists: " + event.getUsername());
        }

        Credential credential = Credential.builder()
                .userId(event.getUserId())
                .username(event.getUsername())
                .password(passwordEncoder.encode(event.getPassword()))
                .role(event.getRole())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        credentialRepository.save(credential);

        SyncUser syncUser = SyncUser.builder()
                .userId(event.getUserId())
                .username(event.getUsername())
                .role(event.getRole())
                .build();

        syncUserRepository.save(syncUser);

        log.info("Credentials and sync user created successfully for userId: {}", event.getUserId());
    }

    private void handleUserUpdated(UserSyncEvent event) {
        log.info("Updating credentials for userId: {}", event.getUserId());

        Credential credential = credentialRepository.findByUserId(event.getUserId())
                .orElseGet(() -> {
                    log.warn("Credential not found for userId: {}, creating new one", event.getUserId());

                    if (event.getUsername() != null && credentialRepository.existsByUsername(event.getUsername())) {
                        throw new RuntimeException("Username already exists: " + event.getUsername());
                    }

                    Credential newCredential = Credential.builder()
                            .userId(event.getUserId())
                            .username(event.getUsername())
                            .password(event.getPassword() != null ? passwordEncoder.encode(event.getPassword()) : passwordEncoder.encode("parola123"))
                            .role(event.getRole() != null ? event.getRole() : "CLIENT")
                            .createdAt(LocalDateTime.now())
                            .updatedAt(LocalDateTime.now())
                            .build();

                    return credentialRepository.save(newCredential);
                });

        SyncUser syncUser = syncUserRepository.findById(event.getUserId())
                .orElseGet(() -> {
                    log.warn("SyncUser not found for userId: {}, creating new one", event.getUserId());

                    SyncUser newSyncUser = SyncUser.builder()
                            .userId(event.getUserId())
                            .username(event.getUsername() != null ? event.getUsername() : credential.getUsername())
                            .role(event.getRole() != null ? event.getRole() : credential.getRole())
                            .build();

                    return syncUserRepository.save(newSyncUser);
                });

        boolean updated = false;

        // Update username
        if (event.getUsername() != null && !credential.getUsername().equals(event.getUsername())) {

            if (credentialRepository.existsByUsername(event.getUsername())) {
                credentialRepository.findByUsername(event.getUsername())
                        .ifPresent(existingCred -> {
                            if (!existingCred.getUserId().equals(event.getUserId())) {
                                throw new RuntimeException("Username already exists: " + event.getUsername());
                            }
                        });
            }

            credential.setUsername(event.getUsername());
            syncUser.setUsername(event.getUsername());
            updated = true;
            log.info("Updated username for userId: {} to: {}", event.getUserId(), event.getUsername());
        }

        if (event.getPassword() != null && !event.getPassword().isEmpty()) {
            credential.setPassword(passwordEncoder.encode(event.getPassword()));
            updated = true;
            log.info("Updated password for userId: {}", event.getUserId());
        }

        if (event.getRole() != null &&
                !credential.getRole().equals(event.getRole())) {
            credential.setRole(event.getRole());
            syncUser.setRole(event.getRole());
            updated = true;
            log.info("Updated role for userId: {} to: {}", event.getUserId(), event.getRole());
        }

        if (updated) {
            credential.setUpdatedAt(LocalDateTime.now());
            credentialRepository.save(credential);
            syncUserRepository.save(syncUser);
            log.info("Credentials and sync user updated successfully for userId: {}", event.getUserId());
        } else {
            log.info("No updates needed for userId: {}", event.getUserId());
        }
    }

    private void handleUserDeleted(UserSyncEvent event) {
        log.info("Deleting credentials for userId: {}", event.getUserId());

        credentialRepository.findByUserId(event.getUserId())
                .ifPresentOrElse(
                        credential -> {
                            credentialRepository.delete(credential);
                            log.info("Credential deleted for userId: {}", event.getUserId());
                        },
                        () -> log.warn("Credential not found for deletion, userId: {}", event.getUserId())
                );

        syncUserRepository.findById(event.getUserId())
                .ifPresentOrElse(
                        syncUser -> {
                            syncUserRepository.delete(syncUser);
                            log.info("SyncUser deleted for userId: {}", event.getUserId());
                        },
                        () -> log.warn("SyncUser not found for deletion, userId: {}", event.getUserId())
                );

        log.info("Credentials and sync user deletion process completed for userId: {}", event.getUserId());
    }
}