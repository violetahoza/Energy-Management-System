package com.vio.monitoring_service.event;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DeviceSyncEvent {
    private Long deviceId;
    private String action; // "CREATED", "UPDATED", "DELETED"
}
