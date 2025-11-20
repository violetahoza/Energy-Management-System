package com.vio.device_service.consumer;

import com.vio.device_service.config.RabbitMQConfig;
import com.vio.device_service.event.UserSyncEvent;
import com.vio.device_service.model.Device;
import com.vio.device_service.model.SyncUser;
import com.vio.device_service.repository.DeviceRepository;
import com.vio.device_service.repository.SyncUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserSyncConsumer {
    private final SyncUserRepository syncUserRepository;
    private final DeviceRepository deviceRepository;

    @RabbitListener(queues = RabbitMQConfig.USER_SYNC_QUEUE_DEVICE)
    @Transactional
    public void handleUserSyncEvent(UserSyncEvent event) {
        log.info("Received user sync event: {} for userId: {}",
                event.getEventType(), event.getUserId());

        try {
            switch (event.getEventType()) {
                case "CREATED":
                    handleUserCreated(event);
                    break;
                case "UPDATED":
                    handleUserUpdated(event);
                    break;
                case "DELETED":
                    handleUserDeleted(event.getUserId());
                    break;
                default:
                    log.warn("Unknown event type: {}", event.getEventType());
            }
        } catch (DataIntegrityViolationException e) {
            // Idempotency: if user already exists, just log and continue
            log.warn("Data integrity violation for userId {}: {}",
                    event.getUserId(), e.getMessage());
        } catch (Exception e) {
            log.error("=== Error processing user sync event for userId: {} ===",
                    event.getUserId(), e);
            // Re-throw to trigger retry mechanism
            throw new RuntimeException("Failed to process user sync event", e);
        }
    }

    private void handleUserCreated(UserSyncEvent event) {
        log.info("Handling user created event for userId: {}", event.getUserId());

        // Check if user already exists (idempotency)
        if (syncUserRepository.existsById(event.getUserId())) {
            log.warn("User {} already exists in sync_users, skipping creation", event.getUserId());
            return;
        }

        SyncUser syncUser = SyncUser.builder()
                .userId(event.getUserId())
                .username(event.getUsername())
                .role(event.getRole())
                .build();

        syncUserRepository.save(syncUser);
        log.info("Successfully synced new user: {} with role: {} and username: {}",
                event.getUserId(), event.getRole(), event.getUsername());
    }

    private void handleUserUpdated(UserSyncEvent event) {
        log.info("Handling user updated event for userId: {}", event.getUserId());

        syncUserRepository.findById(event.getUserId()).ifPresentOrElse(
                syncUser -> {
                    boolean updated = false;

                    if (event.getUsername() != null && !event.getUsername().equals(syncUser.getUsername())) {
                        syncUser.setUsername(event.getUsername());
                        updated = true;
                    }

                    if (event.getRole() != null && !event.getRole().equals(syncUser.getRole())) {
                        syncUser.setRole(event.getRole());
                        updated = true;
                    }

                    if (updated) {
                        syncUserRepository.save(syncUser);
                        log.info("Successfully updated sync user: {} - username: {}, role: {}",
                                event.getUserId(), syncUser.getUsername(), syncUser.getRole());
                    } else {
                        log.info("No changes detected for sync user: {}", event.getUserId());
                    }
                },
                () -> {
                    // User doesn't exist locally, create it
                    log.warn("User {} not found in sync_users during update, creating new entry",
                            event.getUserId());
                    handleUserCreated(event);
                }
        );
    }

    private void handleUserDeleted(Long userId) {
        log.info("Handling user deleted event for userId: {}", userId);

        // First, unassign all devices from this user
        List<Device> userDevices = deviceRepository.findByUserId(userId);

        if (!userDevices.isEmpty()) {
            log.info("Found {} devices assigned to user {}, unassigning them",
                    userDevices.size(), userId);

            userDevices.forEach(device -> {
                log.info("Unassigning device {} (name: {}) from user {}",
                        device.getDeviceId(), device.getName(), userId);
                device.setUserId(null);
                device.setUpdatedAt(LocalDateTime.now());
            });

            deviceRepository.saveAll(userDevices);
            log.info("Successfully unassigned {} devices from deleted user {}",
                    userDevices.size(), userId);
        } else {
            log.info("No devices assigned to user {}", userId);
        }

        // Then delete the sync user record
        if (syncUserRepository.existsById(userId)) {
            syncUserRepository.deleteById(userId);
            log.info("Successfully deleted sync user: {}", userId);
        } else {
            log.warn("Sync user {} not found during deletion", userId);
        }
    }
}