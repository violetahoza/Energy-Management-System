package com.vio.monitoring_service.dto;

import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DailyConsumptionResponse {
    private Long deviceId;
    private LocalDate date;
    private List<HourlyConsumptionResponse> hourlyData;
    private Double totalDailyConsumption; // sum of all hourly consumptions
}