package com.vio.device_service.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeviceSyncEvent implements Serializable {
    private Long deviceId;
    private Double maxConsumption;
    private String eventType; // "CREATED" or "DELETED"

    public static DeviceSyncEvent created(Long deviceId, Double maxConsumption) {
        return new DeviceSyncEvent(deviceId, maxConsumption, "CREATED");
    }

    public static DeviceSyncEvent deleted(Long deviceId) {
        return new DeviceSyncEvent(deviceId, null, "DELETED");
    }
}