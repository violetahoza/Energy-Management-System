package com.vio.monitoring_service.service;

import com.vio.monitoring_service.dto.DailyConsumptionResponse;
import com.vio.monitoring_service.dto.HourlyConsumptionResponse;
import com.vio.monitoring_service.model.Measurement;
import com.vio.monitoring_service.repository.MeasurementRepository;
import com.vio.monitoring_service.repository.MonitoredDeviceRepository;
import com.vio.monitoring_service.handler.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MonitoringService {

    private final MeasurementRepository measurementRepository;
    private final MonitoredDeviceRepository monitoredDeviceRepository;

    public DailyConsumptionResponse getDailyConsumption(Long deviceId, LocalDate date) {
        log.info("Fetching daily consumption for device {} on {}", deviceId, date);

        if (!monitoredDeviceRepository.existsById(deviceId)) {
            throw new ResourceNotFoundException("Device " + deviceId + " not found in monitoring system");
        }

        // Fetch measurements for the day
        List<Measurement> measurements = measurementRepository.findByDeviceIdAndDateOrderByHourAsc(deviceId, date);

        // Convert to hourly responses
        List<HourlyConsumptionResponse> hourlyData = measurements.stream()
                .map(m -> HourlyConsumptionResponse.builder()
                        .hour(m.getHour())
                        .consumption(m.getHourlyConsumption())
                        .measurementCount(m.getMeasurementCount())
                        .build())
                .collect(Collectors.toList());

        // Calculate total daily consumption
        Double totalDailyConsumption = measurements.stream()
                .mapToDouble(Measurement::getHourlyConsumption)
                .sum();

        // Fill in missing hours with zero consumption (for complete 24h display)
        hourlyData = fillMissingHours(hourlyData);

        log.info("Retrieved {} hourly measurements for device {} on {}, total: {} kWh",
                hourlyData.size(), deviceId, date, totalDailyConsumption);

        return DailyConsumptionResponse.builder()
                .deviceId(deviceId)
                .date(date)
                .hourlyData(hourlyData)
                .totalDailyConsumption(totalDailyConsumption)
                .build();
    }

    private List<HourlyConsumptionResponse> fillMissingHours(List<HourlyConsumptionResponse> hourlyData) {
        List<HourlyConsumptionResponse> completeData = new ArrayList<>();

        for (int hour = 0; hour < 24; hour++) {
            final int currentHour = hour;
            HourlyConsumptionResponse existingData = hourlyData.stream()
                    .filter(h -> h.getHour() == currentHour)
                    .findFirst()
                    .orElse(null);

            if (existingData != null) {
                completeData.add(existingData);
            } else {
                completeData.add(HourlyConsumptionResponse.builder()
                        .hour(currentHour)
                        .consumption(0.0)
                        .measurementCount(0)
                        .build());
            }
        }

        return completeData;
    }
}