package com.vio.device_service.publisher;

import com.vio.device_service.event.DeviceSyncEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeviceEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Value("${app.rabbitmq.exchange.sync:sync.exchange}")
    private String syncExchange;

    @Value("${app.rabbitmq.routing-key.device-sync:device.sync}")
    private String deviceSyncRoutingKey;

    public void publishDeviceCreated(Long deviceId, Double maxConsumption) {
        try {
            DeviceSyncEvent event = DeviceSyncEvent.created(deviceId,  maxConsumption);
            rabbitTemplate.convertAndSend(syncExchange, deviceSyncRoutingKey, event);
            log.info("Published DEVICE_CREATED event for deviceId: {}", deviceId);
        } catch (Exception e) {
            log.error("Failed to publish DEVICE_CREATED event for deviceId: {}", deviceId, e);
        }
    }

    public void publishDeviceDeleted(Long deviceId) {
        try {
            DeviceSyncEvent event = DeviceSyncEvent.deleted(deviceId);
            rabbitTemplate.convertAndSend(syncExchange, deviceSyncRoutingKey, event);
            log.info("Published DEVICE_DELETED event for deviceId: {}", deviceId);
        } catch (Exception e) {
            log.error("Failed to publish DEVICE_DELETED event for deviceId: {}", deviceId, e);
        }
    }
}