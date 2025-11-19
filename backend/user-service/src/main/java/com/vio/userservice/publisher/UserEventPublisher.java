package com.vio.userservice.publisher;

import com.vio.userservice.event.UserSyncEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Value("${app.rabbitmq.exchange.sync:sync.exchange}")
    private String syncExchange;

    @Value("${app.rabbitmq.routing-key.user-sync:user.sync}")
    private String userSyncRoutingKey;

    public void publishUserCreated(Long userId, String username, String password, String role) {
        try {
            UserSyncEvent event = UserSyncEvent.created(userId, username, password, role);
            rabbitTemplate.convertAndSend(syncExchange, userSyncRoutingKey, event);
            log.info("Published USER_CREATED event for userId: {}, username: {}", userId, username);
        } catch (Exception e) {
            log.error("Failed to publish USER_CREATED event for userId: {}", userId, e);
        }
    }

    public void publishUserUpdated(Long userId, String username, String password, String role) {
        try {
            UserSyncEvent event = UserSyncEvent.updated(userId, username, password, role);
            rabbitTemplate.convertAndSend(syncExchange, userSyncRoutingKey, event);
            log.info("Published USER_UPDATED event for userId: {}", userId);
        } catch (Exception e) {
            log.error("Failed to publish USER_UPDATED event for userId: {}", userId, e);
        }
    }

    public void publishUserDeleted(Long userId) {
        try {
            UserSyncEvent event = UserSyncEvent.deleted(userId);
            rabbitTemplate.convertAndSend(syncExchange, userSyncRoutingKey, event);
            log.info("Published USER_DELETED event for userId: {}", userId);
        } catch (Exception e) {
            log.error("Failed to publish USER_DELETED event for userId: {}", userId, e);
        }
    }
}