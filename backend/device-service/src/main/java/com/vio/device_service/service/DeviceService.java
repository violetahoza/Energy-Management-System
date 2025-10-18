package com.vio.device_service.service;

import com.vio.device_service.dto.DeviceDTORequest;
import com.vio.device_service.dto.DeviceDTOResponse;
import com.vio.device_service.dto.DeviceUpdateRequest;
import com.vio.device_service.handler.DeviceNotFoundException;
import com.vio.device_service.model.Device;
import com.vio.device_service.repository.DeviceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeviceService {
    private final DeviceRepository repository;
    private final RestTemplate restTemplate;

    private static final String USER_SERVICE_URL = "http://user-service:8081/api/users";

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

        // Validate user exists
        validateUserExists(userId);

        return repository
                .findByUserId(userId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    public DeviceDTOResponse createDevice(DeviceDTORequest request) {
        log.info("Creating new device with name: {}", request.name());

        // Validate user if userId is provided
        if (request.userId() != null) {
            validateUserExists(request.userId());
        }

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

    @Transactional
    public DeviceDTOResponse updateById(Long deviceId, DeviceUpdateRequest request) {
        log.info("Updating device with id: {}", deviceId);

        Device device = repository
                .findById(deviceId)
                .orElseThrow(() -> new DeviceNotFoundException(deviceId));

        boolean updated = false;

        if (request.name() != null && !request.name().equals(device.getName())) {
            device.setName(request.name());
            updated = true;
        }

        if (request.description() != null && !request.description().equals(device.getDescription())) {
            device.setDescription(request.description());
            updated = true;
        }

        if (request.location() != null && !request.location().equals(device.getLocation())) {
            device.setLocation(request.location());
            updated = true;
        }

        if (request.maximumConsumption() != null && !request.maximumConsumption().equals(device.getMaxConsumption())) {
            device.setMaxConsumption(request.maximumConsumption());
            updated = true;
        }

        if (request.userId() != null && !request.userId().equals(device.getUserId())) {
            validateUserExists(request.userId());
            device.setUserId(request.userId());
            updated = true;
        }

        if (updated) {
            device.setUpdatedAt(LocalDateTime.now());
            Device updatedDevice = repository.save(device);
            log.info("Device updated successfully with id: {}", updatedDevice.getDeviceId());
            return mapToResponse(updatedDevice);
        }

        log.info("No changes detected for device with id: {}", deviceId);
        return mapToResponse(device);
    }

    @Transactional
    public void deleteById(Long deviceId) {
        log.info("Deleting device with id: {}", deviceId);

        Device device = repository
                .findById(deviceId)
                .orElseThrow(() -> new DeviceNotFoundException(deviceId));

        repository.delete(device);
        log.info("Device deleted successfully with id: {}", deviceId);
    }

    @Transactional
    public DeviceDTOResponse assignDeviceToUser(Long deviceId, Long userId) {
        log.info("Assigning device {} to user {}", deviceId, userId);

        // Validate user exists
        validateUserExists(userId);

        Device device = repository
                .findById(deviceId)
                .orElseThrow(() -> new DeviceNotFoundException(deviceId));

        device.setUserId(userId);
        device.setUpdatedAt(LocalDateTime.now());

        Device updatedDevice = repository.save(device);
        log.info("Device assigned successfully");
        return mapToResponse(updatedDevice);
    }

    private void validateUserExists(Long userId) {
        log.info("Validating user exists with id: {}", userId);

        try {
            ResponseEntity<Void> response = restTemplate.getForEntity(
                    USER_SERVICE_URL + "/id=" + userId,
                    Void.class
            );
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("User with id " + userId + " does not exist");
            }
        } catch (Exception e) {
            log.error("Failed to validate user existence: {}", e.getMessage());
            throw new RuntimeException("User with id " + userId + " does not exist or User Service is unavailable");
        }
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
                device.getUpdatedAt()
        );
    }
}