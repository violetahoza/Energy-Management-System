package com.vio.monitoring_service.producer;

import com.vio.monitoring_service.config.RabbitMQConfig;
import com.vio.monitoring_service.event.OverconsumptionAlert;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlertPublisher {
    @Qualifier("alertRabbitTemplate")
    private final RabbitTemplate rabbitTemplate;

    public void publishOverconsumptionAlert(Long deviceId, Long userId, Double current, Double max) {
        Double exceeded = current - max;

        OverconsumptionAlert alert = OverconsumptionAlert.builder()
                .deviceId(deviceId)
                .userId(userId)
                .currentConsumption(current)
                .maxConsumption(max)
                .exceededBy(exceeded)
                .timestamp(LocalDateTime.now())
                .message(String.format("Device '%d' exceeded maximum consumption limit by %.2f kWh (Current: %.2f kWh, Max: %.2f kWh)", deviceId, exceeded, current, max))
                .build();

        rabbitTemplate.convertAndSend(RabbitMQConfig.OVERCONSUMPTION_EXCHANGE, RabbitMQConfig.OVERCONSUMPTION_ROUTING_KEY, alert);
        log.info("Published overconsumption alert for device {} to user {} via sync broker", deviceId, userId);
    }
}