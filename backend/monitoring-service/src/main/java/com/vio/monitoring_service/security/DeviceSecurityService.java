package com.vio.monitoring_service.security;

import com.vio.monitoring_service.model.MonitoredDevice;
import com.vio.monitoring_service.repository.MonitoredDeviceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service("deviceSecurityService")
@RequiredArgsConstructor
@Slf4j
public class DeviceSecurityService {
    private final MonitoredDeviceRepository monitoredDeviceRepository;

    public boolean isDeviceOwnedByUser(Long deviceId, String userId) {
        try {
            Long userIdLong = Long.parseLong(userId);
            MonitoredDevice device = monitoredDeviceRepository.findById(deviceId).orElse(null);

            if (device == null) {
                log.warn("Device not found in monitoring service: {}", deviceId);
                return false;
            }

            boolean isOwner = device.getUserId() != null && device.getUserId().equals(userIdLong);
            log.debug("Device {} ownership check for user {}: {}", deviceId, userId, isOwner);
            return isOwner;
        } catch (NumberFormatException e) {
            log.error("Invalid user ID format: {}", userId);
            return false;
        }
    }
}