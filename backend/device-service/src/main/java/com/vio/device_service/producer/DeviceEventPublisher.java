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

    public void publishDeviceCreated(Long deviceId) {
        DeviceSyncEvent message = DeviceSyncEvent.builder()
                .deviceId(deviceId)
                .action("CREATED")
                .build();

        log.info("Publishing device created event: deviceId={}", deviceId);

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.DEVICE_SYNC_EXCHANGE,
                RabbitMQConfig.DEVICE_SYNC_ROUTING_KEY,
                message
        );
    }

    public void publishDeviceDeleted(Long deviceId) {
        DeviceSyncEvent message = DeviceSyncEvent.builder()
                .deviceId(deviceId)
                .action("DELETED")
                .build();

        log.info("Publishing device deleted event: deviceId={}", deviceId);

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.DEVICE_SYNC_EXCHANGE,
                RabbitMQConfig.DEVICE_SYNC_ROUTING_KEY,
                message
        );
    }
}