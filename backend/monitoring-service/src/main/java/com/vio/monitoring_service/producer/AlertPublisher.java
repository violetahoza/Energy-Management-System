package com.vio.monitoring_service.producer;

import com.vio.monitoring_service.dto.OverconsumptionAlertDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlertPublisher {
    private final RabbitTemplate rabbitTemplate;

    public void publishOverconsumptionAlert(Long deviceId, Long userId, Double current, Double max) {
        Double exceeded = current - max;

        OverconsumptionAlertDTO alert = OverconsumptionAlertDTO.builder()
                .deviceId(deviceId)
                .userId(userId)
                .currentConsumption(current)
                .maxConsumption(max)
                .exceededBy(exceeded)
                .timestamp(LocalDateTime.now())
                .message(String.format("Device '%d' exceeded maximum consumption limit by %.2f kWh (Current: %.2f kWh, Max: %.2f kWh)", deviceId, exceeded, current, max))
                .build();

        rabbitTemplate.convertAndSend("overconsumption.exchange", "overconsumption.alert", alert);
        log.info("Published overconsumption alert for device {} to user {}", deviceId, userId);
    }
}