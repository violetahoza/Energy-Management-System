package com.vio.monitoring_service.consumer;

import com.vio.monitoring_service.config.RabbitMQConfig;
import com.vio.monitoring_service.event.DeviceSyncEvent;
import com.vio.monitoring_service.model.MonitoredDevice;
import com.vio.monitoring_service.repository.DeviceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;


@Component
@RequiredArgsConstructor
@Slf4j
public class DeviceSyncConsumer {
    private final DeviceRepository deviceRepository;

    @RabbitListener(queues = RabbitMQConfig.DEVICE_SYNC_EXCHANGE)
    @Transactional
    public void syncDevice(DeviceSyncEvent message) {
        log.info("Processing device sync: deviceId={}, action={}", message.getDeviceId(), message.getAction());

        try {
            switch (message.getAction()) {
                case "CREATED":
                    handleDeviceCreated(message);
                    break;

                case "UPDATED":
                    handleDeviceUpdated(message);
                    break;

                case "DELETED":
                    handleDeviceDeleted(message);
                    break;

                default:
                    log.warn("Unknown sync action: {}", message.getAction());
            }
        } catch (Exception e) {
            log.error("Error syncing device {}: {}", message.getDeviceId(), e.getMessage(), e);
            throw new RuntimeException("Failed to sync device", e);
        }
    }

    private void handleDeviceCreated(DeviceSyncEvent message) {
        // Check if device already exists (idempotency)
        if (deviceRepository.existsById(message.getDeviceId())) {
            log.info("Device {} already exists, updating instead", message.getDeviceId());
            handleDeviceUpdated(message);
            return;
        }

        MonitoredDevice device = MonitoredDevice.builder()
                .deviceId(message.getDeviceId())
                .build();

        deviceRepository.save(device);
        log.info("Device {} created in monitoring service", message.getDeviceId());
    }

    private void handleDeviceUpdated(DeviceSyncEvent message) {
        MonitoredDevice device = deviceRepository.findById(message.getDeviceId())
                .orElseGet(() -> {
                    log.warn("Device {} not found during update, creating new entry", message.getDeviceId());
                    return MonitoredDevice.builder()
                            .deviceId(message.getDeviceId())
                            .build();
                });

        deviceRepository.save(device);
        log.info("Device {} updated in monitoring service", message.getDeviceId());
    }

    private void handleDeviceDeleted(DeviceSyncEvent message) {
        if (!deviceRepository.existsById(message.getDeviceId())) {
            log.warn("Device {} not found during deletion, ignoring", message.getDeviceId());
            return;
        }

        deviceRepository.deleteById(message.getDeviceId());
        log.info("Device {} deleted from monitoring service", message.getDeviceId());
    }
}