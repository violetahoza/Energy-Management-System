package com.vio.load_balancing.strategy;

import com.vio.load_balancing.event.DeviceDataMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class RoundRobinStrategy implements LoadBalancingStrategy {
    @Value("${app.load-balancing.devices-per-replica:3}")
    private int devicesPerReplica;

    // track which replica each device is assigned to
    private final Map<Long, Integer> deviceToReplicaMap = new ConcurrentHashMap<>();

    @Override
    public int selectReplica(DeviceDataMessage message, int totalReplicas) {
        Long deviceId = message.getDeviceId();
        Integer assignedReplica = deviceToReplicaMap.get(deviceId);

        if (assignedReplica != null) {
            log.debug("Device {} already assigned to replica {} (batched round-robin)", deviceId, assignedReplica);
            return assignedReplica;
        }

        // calculate replica based on device ID
        int replica = (int) (((deviceId - 1) / devicesPerReplica) % totalReplicas) + 1;

        deviceToReplicaMap.put(deviceId, replica);
        log.info("Device {} assigned to replica {} using batched round-robin (batch size: {})", deviceId, replica, devicesPerReplica);

        return replica;
    }
}