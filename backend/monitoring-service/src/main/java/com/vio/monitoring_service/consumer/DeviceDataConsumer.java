package com.vio.monitoring_service.consumer;

import com.vio.monitoring_service.config.RabbitMQConfig;
import com.vio.monitoring_service.event.DeviceDataMessage;
import com.vio.monitoring_service.model.Measurement;
import com.vio.monitoring_service.model.MonitoredDevice;
import com.vio.monitoring_service.repository.MeasurementRepository;
import com.vio.monitoring_service.repository.MonitoredDeviceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeviceDataConsumer {

    private final MeasurementRepository measurementRepository;
    private final MonitoredDeviceRepository monitoredDeviceRepository;

    @RabbitListener(queues = RabbitMQConfig.DEVICE_DATA_QUEUE, containerFactory = "dataListenerContainerFactory")
    @Transactional
    public void handleDeviceData(DeviceDataMessage event) {
        log.info("Received device data: deviceId={}, timestamp={}, value={}",
                event.getDeviceId(), event.getTimestamp(), event.getMeasurementValue());

        try {
            // Validate device exists in monitored devices
            if (!monitoredDeviceRepository.existsById(event.getDeviceId())) {
                log.warn("Device {} is not in monitored devices list. Synchronization may be pending.", event.getDeviceId());
                return;
            }

            // Extract date and hour from timestamp
            LocalDate date = event.getTimestamp().toLocalDate();
            Integer hour = event.getTimestamp().getHour();

            // Find or create measurement record for this device, date, and hour
            Optional<Measurement> existingMeasurement = measurementRepository.findByDeviceIdAndDateAndHour(event.getDeviceId(), date, hour);

            Measurement measurement;
            if (existingMeasurement.isPresent()) {
                // Update existing measurement (aggregate)
                measurement = existingMeasurement.get();
                measurement.setHourlyConsumption(
                        measurement.getHourlyConsumption() + event.getMeasurementValue()
                );
                measurement.setMeasurementCount(measurement.getMeasurementCount() + 1);
                log.debug("Updated measurement for device {} on {} hour {}: total={} kWh, count={}",
                        event.getDeviceId(), date, hour,
                        measurement.getHourlyConsumption(),
                        measurement.getMeasurementCount());
            } else {
                // Create new measurement
                measurement = Measurement.builder()
                        .deviceId(event.getDeviceId())
                        .date(date)
                        .hour(hour)
                        .hourlyConsumption(event.getMeasurementValue())
                        .measurementCount(1)
                        .build();
                log.debug("Created new measurement for device {} on {} hour {}: {} kWh",
                        event.getDeviceId(), date, hour, event.getMeasurementValue());
            }

            measurementRepository.save(measurement);
            log.info("Successfully processed device data for device {} - Date: {}, Hour: {}, Total: {} kWh",
                    event.getDeviceId(), date, hour, measurement.getHourlyConsumption());

        } catch (Exception e) {
            log.error("Error processing device data for deviceId {}: {}",
                    event.getDeviceId(), e.getMessage(), e);
            throw e;
        }
    }
}