package com.vio.device_service.service;

import com.vio.device_service.dto.DeviceDTORequest;
import com.vio.device_service.dto.DeviceDTOResponse;
import com.vio.device_service.handler.DeviceNotFoundException;
import com.vio.device_service.model.Device;
import com.vio.device_service.repository.DeviceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeviceService {
    private final DeviceRepository repository;

    public List<DeviceDTOResponse> getAllDevices() {
        log.info("Fetching all devices");
        return repository
                .findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public DeviceDTOResponse findById(Long deviceId) {
        log.info("Fetching device with id: {}", deviceId);
        Device device = repository
                .findById(deviceId)
                .orElseThrow(() -> new DeviceNotFoundException(deviceId));
        return mapToResponse(device);
    }

    public List<DeviceDTOResponse> findByUserId(Long userId) {
        log.info("Fetching devices for user with id: {}", userId);
        return repository
                .findByUserId(userId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public DeviceDTOResponse createDevice(DeviceDTORequest request) {
        log.info("Creating new device with name: {}", request.name());

        Device device = Device.builder()
                .name(request.name())
                .description(request.description())
                .location(request.location())
                .maxConsumption(request.maximumConsumption())
                .userId(request.userId())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Device savedDevice = repository.save(device);
        log.info("Device created successfully with id: {}", savedDevice.getDeviceId());
        return mapToResponse(savedDevice);
    }

    public DeviceDTOResponse updateById(Long deviceId, DeviceDTORequest request) {
        log.info("Updating device with id: {}", deviceId);

        Device device = repository
                .findById(deviceId)
                .orElseThrow(() -> new DeviceNotFoundException(deviceId));

        device.setName(request.name());
        device.setDescription(request.description());
        device.setLocation(request.location());
        device.setMaxConsumption(request.maximumConsumption());
        device.setUserId(request.userId());
        device.setUpdatedAt(LocalDateTime.now());

        Device updatedDevice = repository.save(device);
        log.info("Device updated successfully with id: {}", updatedDevice.getDeviceId());
        return mapToResponse(updatedDevice);
    }

    public void deleteById(Long deviceId) {
        log.info("Deleting device with id: {}", deviceId);

        Device device = repository
                .findById(deviceId)
                .orElseThrow(() -> new DeviceNotFoundException(deviceId));

        repository.delete(device);
        log.info("Device deleted successfully with id: {}", deviceId);
    }

    public DeviceDTOResponse assignDeviceToUser(Long deviceId, Long userId) {
        log.info("Assigning device {} to user {}", deviceId, userId);

        Device device = repository
                .findById(deviceId)
                .orElseThrow(() -> new DeviceNotFoundException(deviceId));

        device.setUserId(userId);
        device.setUpdatedAt(LocalDateTime.now());

        Device updatedDevice = repository.save(device);
        log.info("Device assigned successfully");
        return mapToResponse(updatedDevice);
    }

    private DeviceDTOResponse mapToResponse(Device device) {
        return new DeviceDTOResponse(
                device.getDeviceId(),
                device.getName(),
                device.getDescription(),
                device.getLocation(),
                device.getMaxConsumption(),
                device.getUserId(),
                device.getCreatedAt(),
                device.getUpdatedAt(),
                device.isActive()
        );
    }
}