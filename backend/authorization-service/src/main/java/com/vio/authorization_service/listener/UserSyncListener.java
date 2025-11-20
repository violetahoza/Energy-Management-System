package com.vio.authorization_service.listener;

import com.vio.authorization_service.model.Credential;
import com.vio.authorization_service.model.SyncedUser;
import com.vio.authorization_service.repository.CredentialRepository;
import com.vio.authorization_service.repository.SyncedUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserSyncListener {

    private final SyncedUserRepository syncedUserRepository;
    private final CredentialRepository credentialRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @RabbitListener(queues = "${app.rabbitmq.queue.sync:sync.queue}")
    @Transactional
    public void handleUserSyncEvent(Map<String, Object> message) {
        try {
            String eventType = (String) message.get("eventType");

            if (eventType == null) {
                log.warn("Received sync event without eventType");
                return;
            }

            // Only process USER events
            if (!message.containsKey("userId")) {
                log.debug("Ignoring event without userId");
                return;
            }

            Long userId = getLongValue(message.get("userId"));

            switch (eventType) {
                case "CREATED":
                    handleUserCreated(userId, message);
                    break;
                case "UPDATED":
                    handleUserUpdated(userId, message);
                    break;
                case "DELETED":
                    handleUserDeleted(userId);
                    break;
                default:
                    log.warn("Unknown event type: {}", eventType);
            }

        } catch (Exception e) {
            log.error("Error processing sync event: {}", message, e);
            throw e;
        }
    }

    private void handleUserCreated(Long userId, Map<String, Object> message) {
        try {
            String username = (String) message.get("username");
            String password = (String) message.get("password");
            String role = (String) message.get("role");

            if (username == null || password == null || role == null) {
                log.error("Missing required fields for user creation: userId={}", userId);
                return;
            }

            // 1. Create Credential
            if (credentialRepository.findByUserId(userId).isPresent()) {
                log.warn("Credentials already exist for userId: {}", userId);
            } else {
                Credential credential = Credential.builder()
                        .userId(userId)
                        .username(username)
                        .password(passwordEncoder.encode(password))
                        .role(role.toUpperCase())
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build();
                credentialRepository.save(credential);
                log.info("Credentials created for userId: {}", userId);
            }

            // 2. Create SyncedUser
            if (syncedUserRepository.existsById(userId)) {
                log.warn("User already exists in synced_users: userId={}", userId);
            } else {
                SyncedUser syncedUser = new SyncedUser();
                syncedUser.setUserId(userId);
                syncedUser.setUsername(username);
                syncedUser.setRole(role.toUpperCase());
                syncedUserRepository.save(syncedUser);
                log.info("User synced to synced_users: userId={}, username={}", userId, username);
            }

        } catch (Exception e) {
            log.error("Failed to handle user creation: userId={}", userId, e);
            throw e;
        }
    }

    private void handleUserUpdated(Long userId, Map<String, Object> message) {
        try {
            String username = (String) message.get("username");
            String password = (String) message.get("password");
            String role = (String) message.get("role");

            boolean credentialUpdated = false;
            boolean syncedUserUpdated = false;

            // 1. Update Credential
            Credential credential = credentialRepository.findByUserId(userId).orElse(null);
            if (credential != null) {
                if (username != null && !credential.getUsername().equals(username)) {
                    credential.setUsername(username);
                    credentialUpdated = true;
                }
                if (password != null && !password.isEmpty()) {
                    credential.setPassword(passwordEncoder.encode(password));
                    credentialUpdated = true;
                }
                if (role != null && !credential.getRole().equals(role.toUpperCase())) {
                    credential.setRole(role.toUpperCase());
                    credentialUpdated = true;
                }

                if (credentialUpdated) {
                    credential.setUpdatedAt(LocalDateTime.now());
                    credentialRepository.save(credential);
                    log.info("Credentials updated for userId: {}", userId);
                }
            } else {
                log.warn("Credentials not found for update: userId={}", userId);
            }

            // 2. Update SyncedUser
            SyncedUser syncedUser = syncedUserRepository.findById(userId).orElse(null);
            if (syncedUser != null) {
                if (username != null) {
                    syncedUser.setUsername(username);
                    syncedUserUpdated = true;
                }
                if (role != null) {
                    syncedUser.setRole(role.toUpperCase());
                    syncedUserUpdated = true;
                }

                if (syncedUserUpdated) {
                    syncedUserRepository.save(syncedUser);
                    log.info("SyncedUser updated for userId: {}", userId);
                }
            } else {
                log.warn("SyncedUser not found for update: userId={}", userId);
            }
        } catch (Exception e) {
            log.error("Failed to handle user update: userId={}", userId, e);
            throw e;
        }
    }

    private void handleUserDeleted(Long userId) {
        try {
            // 1. Delete Credential
            credentialRepository.findByUserId(userId).ifPresent(credential -> {
                credentialRepository.delete(credential);
                log.info("Credentials deleted for userId: {}", userId);
            });

            // 2. Delete SyncedUser
            if (syncedUserRepository.existsById(userId)) {
                syncedUserRepository.deleteById(userId);
                log.info("User deleted from synced_users: userId={}", userId);
            } else {
                log.warn("User not found in synced_users during deletion: userId={}", userId);
            }
        } catch (Exception e) {
            log.error("Failed to handle user deletion: userId={}", userId, e);
            throw e;
        }
    }

    private Long getLongValue(Object value) {
        if (value instanceof Integer) {
            return ((Integer) value).longValue();
        } else if (value instanceof Long) {
            return (Long) value;
        }
        throw new IllegalArgumentException("Cannot convert to Long: " + value);
    }
}