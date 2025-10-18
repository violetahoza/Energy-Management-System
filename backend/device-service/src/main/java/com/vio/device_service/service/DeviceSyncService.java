package com.vio.device_service.service;

import com.vio.device_service.model.Device;
import com.vio.device_service.repository.DeviceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeviceSyncService {
    private final DeviceRepository deviceRepository;

    @Transactional
    public void unassignDevicesForDeletedUser(Long userId) {
        log.info("Unassigning all devices for deleted user: {}", userId);

        List<Device> userDevices = deviceRepository.findByUserId(userId);

        if (userDevices.isEmpty()) {
            log.info("No devices found for user: {}", userId);
            return;
        }

        // Set userId to null - devices persist but are unassigned
        userDevices.forEach(device -> {
            log.info("Unassigning device {} from deleted user {}", device.getDeviceId(), userId);
            device.setUserId(null);
            device.setUpdatedAt(LocalDateTime.now());
        });

        deviceRepository.saveAll(userDevices);
        log.info("Successfully unassigned {} devices from deleted user {}", userDevices.size(), userId);
    }
}