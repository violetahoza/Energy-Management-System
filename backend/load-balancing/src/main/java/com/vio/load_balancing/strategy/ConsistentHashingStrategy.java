package com.vio.load_balancing.strategy;

import com.vio.load_balancing.event.DeviceDataMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Component
@Slf4j
public class ConsistentHashingStrategy implements LoadBalancingStrategy {

    @Override
    public int selectReplica(DeviceDataMessage message, int totalReplicas) {
        try {
            // Use device ID as the key for hashing
            String key = String.valueOf(message.getDeviceId());

            // Calculate MD5 hash
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hashBytes = md.digest(key.getBytes(StandardCharsets.UTF_8));

            // Convert first 4 bytes to int
            int hash = 0;
            for (int i = 0; i < 4; i++) {
                hash = (hash << 8) | (hashBytes[i] & 0xFF);
            }

            // Make hash positive and map to replica (1 to N)
            int replica = (Math.abs(hash) % totalReplicas) + 1;

            log.debug("Device {} mapped to replica {} using consistent hashing", message.getDeviceId(), replica);

            return replica;

        } catch (NoSuchAlgorithmException e) {
            log.error("MD5 algorithm not found, falling back to simple modulo", e);
            return (int) ((message.getDeviceId() % totalReplicas) + 1);
        }
    }
}