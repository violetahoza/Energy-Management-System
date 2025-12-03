package com.vio.load_balancing.strategy;

import com.vio.load_balancing.event.DeviceDataMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

@Component
@Slf4j
public class RoundRobinStrategy implements LoadBalancingStrategy {

    private final AtomicInteger counter = new AtomicInteger(0);

    @Override
    public int selectReplica(DeviceDataMessage message, int totalReplicas) {
        // Get next replica in round-robin fashion (1 to N)
        int replica = (counter.getAndIncrement() % totalReplicas) + 1;

        log.debug("Device {} assigned to replica {} using round-robin",
                message.getDeviceId(), replica);

        return replica;
    }
}