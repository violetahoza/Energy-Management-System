package com.vio.device_service.producer;

import com.vio.device_service.config.RabbitMQConfig;
import com.vio.device_service.event.DeviceSyncEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeviceEventPublisher {
    private final RabbitTemplate rabbitTemplate;

    public void publishDeviceSyncEvent(Long deviceId, Long userId, Double maxConsumption, String action) {
        DeviceSyncEvent message = DeviceSyncEvent.builder()
                .deviceId(deviceId)
                .userId(action.equals("DELETE") ? null : userId)
                .maxConsumption(maxConsumption)
                .action(action)
                .build();

        log.info("✅ Publishing device sync event: action={}, deviceId={}, userId={}, maxConsumption={}", action, deviceId, userId, maxConsumption);

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.DEVICE_SYNC_EXCHANGE,
                RabbitMQConfig.DEVICE_SYNC_ROUTING_KEY,
                message
        );
    }
}