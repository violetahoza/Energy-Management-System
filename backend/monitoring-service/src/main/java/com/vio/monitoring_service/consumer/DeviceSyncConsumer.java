package com.vio.monitoring_service.consumer;

import com.vio.monitoring_service.event.DeviceSyncEvent;
import com.vio.monitoring_service.model.MonitoredDevice;
import com.vio.monitoring_service.repository.MonitoredDeviceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeviceSyncConsumer {

    private final MonitoredDeviceRepository monitoredDeviceRepository;

    @RabbitListener(queues = "device.sync.queue.monitoring", containerFactory = "syncListenerContainerFactory")
    @Transactional
    public void handleDeviceSync(DeviceSyncEvent event) {
        log.info("Received device sync event: action={}, deviceId={}, userId={}",
                event.getAction(), event.getDeviceId(), event.getUserId());

        try {
            switch (event.getAction()) {
                case "CREATED":
                    handleDeviceCreated(event.getDeviceId(), event.getUserId());
                    break;
                case "DELETED":
                    handleDeviceDeleted(event.getDeviceId());
                    break;
                case "UPDATED":
                    handleDeviceUpdated(event.getDeviceId(), event.getUserId());
                    break;
                default:
                    log.warn("Unknown action type: {}", event.getAction());
            }
        } catch (Exception e) {
            log.error("Error processing device sync event for deviceId {}: {}",
                    event.getDeviceId(), e.getMessage(), e);
            throw e;
        }
    }

    private void handleDeviceCreated(Long deviceId, Long userId) {
        if (monitoredDeviceRepository.existsById(deviceId)) {
            log.info("Device {} already exists in monitoring service, updating userId", deviceId);
            Optional<MonitoredDevice> existing = monitoredDeviceRepository.findById(deviceId);
            existing.ifPresent(device -> {
                device.setUserId(userId);
                monitoredDeviceRepository.save(device);
            });
            return;
        }

        MonitoredDevice monitoredDevice = MonitoredDevice.builder()
                .deviceId(deviceId)
                .userId(userId)
                .build();

        monitoredDeviceRepository.save(monitoredDevice);
        log.info("Successfully synchronized device {} with userId {} to monitoring service", deviceId, userId);
    }

    private void handleDeviceDeleted(Long deviceId) {
        if (!monitoredDeviceRepository.existsById(deviceId)) {
            log.info("Device {} does not exist in monitoring service, skipping deletion", deviceId);
            return;
        }

        monitoredDeviceRepository.deleteById(deviceId);
        log.info("Successfully deleted device {} from monitoring service", deviceId);
    }

    private void handleDeviceUpdated(Long deviceId, Long userId) {
        Optional<MonitoredDevice> deviceOpt = monitoredDeviceRepository.findById(deviceId);

        if (deviceOpt.isEmpty()) {
            log.warn("Device {} not found in monitoring service, creating it", deviceId);
            handleDeviceCreated(deviceId, userId);
            return;
        }

        MonitoredDevice device = deviceOpt.get();
        device.setUserId(userId);
        monitoredDeviceRepository.save(device);

        log.info("Successfully updated device {}", deviceId);
    }
}