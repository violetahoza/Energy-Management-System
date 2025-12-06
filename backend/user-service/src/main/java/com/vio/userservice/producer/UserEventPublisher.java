package com.vio.userservice.producer;

import com.vio.userservice.config.RabbitMQConfig;
import com.vio.userservice.event.UserSyncEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserEventPublisher {
    private final RabbitTemplate rabbitTemplate;

    public void publishUserCreated(UserSyncEvent event) {
        event.setEventType("CREATED");
        publishEvent(event);
    }

    public void publishUserUpdated(UserSyncEvent event) {
        event.setEventType("UPDATED");
        publishEvent(event);
    }

    public void publishUserDeleted(Long userId) {
        UserSyncEvent event = UserSyncEvent.builder()
                .userId(userId)
                .eventType("DELETED")
                .build();
        publishEvent(event);
    }

    private void publishEvent(UserSyncEvent event) {
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.USER_SYNC_EXCHANGE,
                    RabbitMQConfig.USER_SYNC_ROUTING_KEY,
                    event
            );
            log.info("✅ Published user sync event: {} for userId: {}", event.getEventType(), event.getUserId());
        } catch (Exception e) {
            log.error("❌ Failed to publish user sync event: {}", e.getMessage(), e);
        }
    }
}