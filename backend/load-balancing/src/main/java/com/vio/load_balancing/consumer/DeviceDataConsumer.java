package com.vio.load_balancing.consumer;

import com.vio.load_balancing.event.DeviceDataMessage;
import com.vio.load_balancing.service.LoadBalancingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DeviceDataConsumer {

    private final LoadBalancingService loadBalancingService;

    @RabbitListener(queues = "${app.rabbitmq.queue.device-data}")
    public void consumeDeviceData(DeviceDataMessage message) {
        log.info("Received device data: deviceId={}, timestamp={}, value={}", message.getDeviceId(), message.getTimestamp(), message.getMeasurementValue());

        try {
            // Route to appropriate replica
            loadBalancingService.routeMessage(message);

        } catch (Exception e) {
            log.error("Failed to process device data for device {}: {}", message.getDeviceId(), e.getMessage(), e);
            throw e;
        }
    }
}