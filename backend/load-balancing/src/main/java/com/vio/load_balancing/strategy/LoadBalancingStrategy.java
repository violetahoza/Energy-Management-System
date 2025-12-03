package com.vio.load_balancing.strategy;

import com.vio.load_balancing.event.DeviceDataMessage;

public interface LoadBalancingStrategy {
    int selectReplica(DeviceDataMessage message, int totalReplicas);
}