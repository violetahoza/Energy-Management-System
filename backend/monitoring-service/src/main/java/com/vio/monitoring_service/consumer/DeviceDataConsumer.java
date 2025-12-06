package com.vio.monitoring_service.consumer;

import com.vio.monitoring_service.event.DeviceDataMessage;
import com.vio.monitoring_service.model.Measurement;
import com.vio.monitoring_service.model.MonitoredDevice;
import com.vio.monitoring_service.producer.AlertPublisher;
import com.vio.monitoring_service.repository.MeasurementRepository;
import com.vio.monitoring_service.repository.MonitoredDeviceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
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
    private final AlertPublisher alertPublisher;

    @Value("${app.replica.id:1}")
    private int replicaId;

    @RabbitListener(queues = "#{ingestQueue.name}", containerFactory = "dataListenerContainerFactory")    @Transactional
    public void handleDeviceData(DeviceDataMessage event) {
        log.info("[Replica {}] Received device data: deviceId={}, timestamp={}, value={}", replicaId, event.getDeviceId(), event.getTimestamp(), event.getMeasurementValue());

        try {
            if (!monitoredDeviceRepository.existsById(event.getDeviceId())) {
                log.warn("[Replica {}] Device {} is not in monitored devices list. Synchronization may be pending.", replicaId, event.getDeviceId());
                return;
            }

            // check overconsumption and send alert if necessary
            MonitoredDevice device = monitoredDeviceRepository.findById(event.getDeviceId()).orElse(null);
            if (device != null && device.getMaxConsumption() != null) {
                Double currentValue = event.getMeasurementValue();
                Double maxConsumption = device.getMaxConsumption();

                if (currentValue > maxConsumption) {
                    log.warn("[Replica {}] Overconsumption detected for device {}: current reading {} > max {}", replicaId, event.getDeviceId(), currentValue, maxConsumption);
                    alertPublisher.publishOverconsumptionAlert(
                            event.getDeviceId(),
                            device.getUserId(),
                            currentValue,
                            maxConsumption
                    );
                }
            }

            LocalDate date = event.getTimestamp().toLocalDate();
            Integer hour = event.getTimestamp().getHour();

            Optional<Measurement> existingMeasurement = measurementRepository.findByDeviceIdAndDateAndHour(event.getDeviceId(), date, hour);

            Measurement measurement;
            if (existingMeasurement.isPresent()) {
                // update existing measurement
                measurement = existingMeasurement.get();
                measurement.setHourlyConsumption(measurement.getHourlyConsumption() + event.getMeasurementValue());
                measurement.setMeasurementCount(measurement.getMeasurementCount() + 1);
                log.debug("[Replica {}] Updated measurement for device {} on {} hour {}: total={} kWh, count={}", replicaId, event.getDeviceId(), date, hour, measurement.getHourlyConsumption(), measurement.getMeasurementCount());
            } else {
                // create new measurement
                measurement = Measurement.builder()
                        .deviceId(event.getDeviceId())
                        .date(date)
                        .hour(hour)
                        .hourlyConsumption(event.getMeasurementValue())
                        .measurementCount(1)
                        .build();
                log.debug("[Replica {}] Created new measurement for device {} on {} hour {}: {} kWh", replicaId, event.getDeviceId(), date, hour, event.getMeasurementValue());
            }

            measurementRepository.save(measurement);
            log.info("[Replica {}] Successfully processed device data for device {} - Date: {}, Hour: {}, Total: {} kWh", replicaId, event.getDeviceId(), date, hour, measurement.getHourlyConsumption());
        } catch (Exception e) {
            log.error("[Replica {}] Error processing device data for deviceId {}: {}", replicaId, event.getDeviceId(), e.getMessage(), e);
            throw e;
        }
    }
}