package com.vio.device_service.event;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DeviceSyncEvent {
    private Long deviceId;
    private Long userId;
    private Double maxConsumption;
    private String action; // CREATED, DELETED, UPDATED
}
