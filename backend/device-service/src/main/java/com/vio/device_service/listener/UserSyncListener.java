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

    @RabbitListener(queues = "sync.queue")
    @Transactional
    public void handleSyncEvent(Map<String, Object> message) {
        log.info("DEVICE SERVICE RECEIVED MESSAGE: {}", message);

        try {
            String eventType = (String) message.get("eventType");
            if (eventType == null) {
                log.warn("No eventType in message");
                return;
            }
            log.info("Event type: {}", eventType);

            boolean hasUserId = message.containsKey("userId");
            boolean hasUsername = message.containsKey("username");

            log.info("hasUserId: {}, hasUsername: {}", hasUserId, hasUsername);

            if (hasUserId && hasUsername) {
                log.info("Processing USER event...");
                Long userId = getLongValue(message.get("userId"));

                if ("CREATED".equals(eventType)) {
                    handleUserCreated(userId, message);
                }
            } else {
                log.info("Not a USER event, ignoring");
            }

        } catch (Exception e) {
            log.error("Error processing sync event: {}", message, e);
        }
    }

    private void handleUserCreated(Long userId, Map<String, Object> message) {
        try {
            String username = (String) message.get("username");
            String role = (String) message.get("role");

            log.info("CREATING SYNCED USER - userId: {}, username: {}, role: {}",
                    userId, username, role);

            if (syncedUserRepository.existsById(userId)) {
                log.warn("User already exists: {}", userId);
                return;
            }

            SyncedUser syncedUser = new SyncedUser();
            syncedUser.setUserId(userId);
            syncedUser.setUsername(username);
            syncedUser.setRole(role != null ? role : "CLIENT");

            syncedUserRepository.save(syncedUser);

            log.info("SUCCESS! User saved to Device Service DB: userId={}, username={}, role={}",
                    userId, username, role);

        } catch (Exception e) {
            log.error("FAILED to sync user: userId={}", userId, e);
            throw e;
        }
    }

    private Long getLongValue(Object value) {
        if (value instanceof Integer) {
            return ((Integer) value).longValue();
        } else if (value instanceof Long) {
            return (Long) value;
        } else if (value instanceof String) {
            return Long.parseLong((String) value);
        }
        throw new IllegalArgumentException("Cannot convert to Long: " + value);
    }
}