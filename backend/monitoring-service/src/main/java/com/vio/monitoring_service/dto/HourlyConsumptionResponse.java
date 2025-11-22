package com.vio.monitoring_service.dto;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class HourlyConsumptionResponse {
    private Integer hour; // 0-23
    private Double consumption; // kWh for this hour
    private Integer measurementCount; // nr of 10-min readings aggregated
}