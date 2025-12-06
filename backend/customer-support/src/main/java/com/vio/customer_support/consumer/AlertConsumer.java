package com.vio.customer_support.consumer;

import com.vio.customer_support.config.RabbitMQConfig;
import com.vio.customer_support.event.OverconsumptionAlert;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlertConsumer {
    private final SimpMessagingTemplate messagingTemplate;

    @RabbitListener(queues = RabbitMQConfig.OVERCONSUMPTION_QUEUE)
    public void handleOverconsumptionAlert(OverconsumptionAlert alert) {
        log.info("========== ALERT RECEIVED ==========");
        log.info("Received overconsumption alert for device {} and user {}", alert.getDeviceId(), alert.getUserId());
        log.info("Alert details: {}", alert);

        try {
            messagingTemplate.convertAndSendToUser(
                    alert.getUserId().toString(),
                    "/queue/alerts",
                    alert
            );

            log.info("✅ Sent alert to  user {}", alert.getUserId());
            log.info("========== ALERT SENT SUCCESSFULLY ==========");
        } catch (Exception e) {
            log.error("❌ Failed to send alert via WebSocket", e);
        }
    }
}