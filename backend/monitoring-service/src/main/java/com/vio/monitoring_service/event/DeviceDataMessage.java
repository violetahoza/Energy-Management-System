package com.vio.monitoring_service.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DeviceDataMessage {
    @JsonProperty("timestamp")
    private LocalDateTime timestamp;

    @JsonProperty("device_id")
    private Long deviceId;

    @JsonProperty("measurement_value")
    private Double measurementValue;
}
