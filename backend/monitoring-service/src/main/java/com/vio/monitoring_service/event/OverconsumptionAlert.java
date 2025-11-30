package com.vio.monitoring_service.event;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OverconsumptionAlert {
    private Long deviceId;
    private Long userId;
    private Double currentConsumption;
    private Double maxConsumption;
    private Double exceededBy;
    private LocalDateTime timestamp;
    private String message;
}