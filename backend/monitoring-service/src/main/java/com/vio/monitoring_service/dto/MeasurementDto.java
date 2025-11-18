package com.vio.monitoring_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MeasurementDto {
    @JsonProperty("timestamp")
    private String timestamp;

    @JsonProperty("device_id")
    private String deviceId;

    @JsonProperty("measured_value")
    private double measuredValue;
}