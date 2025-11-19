package com.vio.device_service.listener;

import com.vio.device_service.model.SyncedUser;
import com.vio.device_service.repository.SyncedUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserSyncListener {

    private final SyncedUserRepository syncedUserRepository;

    @RabbitListener(queues = "${app.rabbitmq.queue.sync:sync.queue}")
    @Transactional
    public void handleUserSyncEvent(Map<String, Object> message) {
        try {
            String eventType = (String) message.get("eventType");

            if (eventType == null) {
                log.warn("Received sync event without eventType");
                return;
            }

            // Only process USER events (ignore DEVICE events from this same queue)
            if (!message.containsKey("userId") || !message.containsKey("username")) {
                log.debug("Ignoring non-user event");
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
            // Don't rethrow - we don't want to block the queue
        }
    }

    private void handleUserCreated(Long userId, Map<String, Object> message) {
        try {
            String username = (String) message.get("username");

            if (syncedUserRepository.existsById(userId)) {
                log.warn("User already exists in synced_users: userId={}", userId);
                return;
            }

            SyncedUser syncedUser = new SyncedUser();
            syncedUser.setUserId(userId);
            syncedUser.setUsername(username);

            syncedUserRepository.save(syncedUser);
            log.info("User synced to Device Service: userId={}, username={}", userId, username);
        } catch (Exception e) {
            log.error("Failed to sync user creation: userId={}", userId, e);
        }
    }

    private void handleUserUpdated(Long userId, Map<String, Object> message) {
        try {
            String username = (String) message.get("username");

            SyncedUser syncedUser = syncedUserRepository.findById(userId).orElse(null);

            if (syncedUser == null) {
                log.warn("User not found in synced_users during update: userId={}", userId);
                // Create it if it doesn't exist
                syncedUser = new SyncedUser();
                syncedUser.setUserId(userId);
            }

            syncedUser.setUsername(username);
            syncedUserRepository.save(syncedUser);

            log.info("User updated in Device Service: userId={}, username={}", userId, username);
        } catch (Exception e) {
            log.error("Failed to sync user update: userId={}", userId, e);
        }
    }

    private void handleUserDeleted(Long userId) {
        try {
            if (syncedUserRepository.existsById(userId)) {
                syncedUserRepository.deleteById(userId);
                log.info("User deleted from Device Service synced_users: userId={}", userId);
            } else {
                log.warn("User not found in synced_users during deletion: userId={}", userId);
            }
        } catch (Exception e) {
            log.error("Failed to sync user deletion: userId={}", userId, e);
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