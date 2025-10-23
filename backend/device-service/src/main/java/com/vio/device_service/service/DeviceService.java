package com.vio.device_service.service;

import com.vio.device_service.dto.*;
import com.vio.device_service.handler.*;
import com.vio.device_service.model.Device;
import com.vio.device_service.repository.DeviceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.*;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeviceService {
    private final DeviceRepository repository;
    private final RestTemplate restTemplate;

    private static final String USER_SERVICE_URL = "http://user-service:8081/api/users/internal/validate";

    public List<DeviceResponse> getAllDevices() {
        log.info("Fetching all devices");
        try {
            return repository.findAll()
                    .stream()
                    .map(this::mapToResponse)
                    .toList();
        } catch (Exception e) {
            log.error("Error fetching all devices: {}", e.getMessage());
            throw new RuntimeException("Failed to retrieve devices", e);
        }
    }

    public DeviceResponse findById(Long deviceId) {
        log.info("Fetching device with id: {}", deviceId);
        validateDeviceId(deviceId);

        Device device = repository.findById(deviceId)
                .orElseThrow(() -> new DeviceNotFoundException(deviceId));
        return mapToResponse(device);
    }

    public List<DeviceResponse> findByUserId(Long userId) {
        log.info("Fetching devices for user with id: {}", userId);
        validateUserId(userId);
        validateUserExists(userId);

        try {
            return repository.findByUserId(userId)
                    .stream()
                    .map(this::mapToResponse)
                    .toList();
        } catch (Exception e) {
            log.error("Error fetching devices for user {}: {}", userId, e.getMessage());
            throw new RuntimeException("Failed to retrieve devices for user", e);
        }
    }

    @Transactional
    public DeviceResponse createDevice(DeviceRequest request) {
        log.info("Creating new device with name: {}", request.name());

        validateDeviceCreationRequest(request);

        if (request.userId() != null) {
            validateUserId(request.userId());
            validateUserExists(request.userId());
        }

        try {
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
        } catch (UserServiceException | IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error creating device: {}", e.getMessage());
            throw new RuntimeException("Failed to create device", e);
        }
    }

    @Transactional
    public DeviceResponse updateById(Long deviceId, DeviceRequest request) {
        log.info("Updating device with id: {}", deviceId);
        validateDeviceId(deviceId);

        try {
            Device device = repository.findById(deviceId)
                    .orElseThrow(() -> new DeviceNotFoundException(deviceId));

            boolean updated = false;

            if (request.name() != null && !request.name().isEmpty() &&
                    !request.name().equals(device.getName())) {
                device.setName(request.name());
                updated = true;
            }

            if (request.description() != null && !request.description().equals(device.getDescription())) {
                device.setDescription(request.description());
                updated = true;
            }

            if (request.location() != null && !request.location().isEmpty() &&
                    !request.location().equals(device.getLocation())) {
                device.setLocation(request.location());
                updated = true;
            }

            if (request.maximumConsumption() != null) {
                if (request.maximumConsumption() <= 0) {
                    throw new IllegalArgumentException("Maximum consumption must be positive");
                }
                if (!request.maximumConsumption().equals(device.getMaxConsumption())) {
                    device.setMaxConsumption(request.maximumConsumption());
                    updated = true;
                }
            }

            if (request.userId() != null && !request.userId().equals(device.getUserId())) {
                validateUserId(request.userId());
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
        } catch (DeviceNotFoundException | UserServiceException | IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error updating device {}: {}", deviceId, e.getMessage());
            throw new RuntimeException("Failed to update device", e);
        }
    }

    @Transactional
    public DeviceResponse assignDeviceToUser(Long deviceId, Long userId) {
        log.info("Assigning device {} to user {}", deviceId, userId);
        validateDeviceId(deviceId);
        validateUserId(userId);
        validateUserExists(userId);

        try {
            Device device = repository.findById(deviceId).orElseThrow(() -> new DeviceNotFoundException(deviceId));

            device.setUserId(userId);
            device.setUpdatedAt(LocalDateTime.now());

            Device updatedDevice = repository.save(device);
            log.info("Device assigned successfully");
            return mapToResponse(updatedDevice);
        } catch (DeviceNotFoundException | UserServiceException | IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error assigning device {} to user {}: {}", deviceId, userId, e.getMessage());
            throw new RuntimeException("Failed to assign device to user", e);
        }
    }

    @Transactional
    public DeviceResponse unassignDevice(Long deviceId) {
        log.info("Unassigning device {}", deviceId);
        validateDeviceId(deviceId);

        try {
            Device device = repository.findById(deviceId).orElseThrow(() -> new DeviceNotFoundException(deviceId));

            device.setUserId(null);
            device.setUpdatedAt(LocalDateTime.now());

            Device updatedDevice = repository.save(device);
            log.info("Device unassigned successfully");
            return mapToResponse(updatedDevice);
        } catch (DeviceNotFoundException | IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error unassigning device {}: {}", deviceId, e.getMessage());
            throw new RuntimeException("Failed to unassign device", e);
        }
    }

    @Transactional
    public void deleteById(Long deviceId) {
        log.info("Deleting device with id: {}", deviceId);
        validateDeviceId(deviceId);

        try {
            Device device = repository.findById(deviceId).orElseThrow(() -> new DeviceNotFoundException(deviceId));
            repository.delete(device);
            log.info("Device deleted successfully with id: {}", deviceId);
        } catch (DeviceNotFoundException | IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error deleting device {}: {}", deviceId, e.getMessage());
            throw new RuntimeException("Failed to delete device", e);
        }
    }

    // Validation methods
    private void validateDeviceId(Long deviceId) {
        if (deviceId == null || deviceId <= 0) {
            throw new IllegalArgumentException("Device ID must be a positive number");
        }
    }

    private void validateUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("User ID must be a positive number");
        }
    }

    private void validateDeviceCreationRequest(DeviceRequest request) {
        if (request.name() == null || request.name().isEmpty()) {
            throw new IllegalArgumentException("Device name is required");
        }
        if (request.location() == null || request.location().isEmpty()) {
            throw new IllegalArgumentException("Device location is required");
        }
        if (request.maximumConsumption() == null || request.maximumConsumption() <= 0) {
            throw new IllegalArgumentException("Valid maximum consumption is required");
        }
    }

    private void validateUserExists(Long userId) {
        log.debug("Validating user exists with id: {}", userId);

        try {
            restTemplate.getForEntity(USER_SERVICE_URL + "/" + userId, Void.class);
            log.debug("User validation successful for id: {}", userId);
        } catch (HttpClientErrorException.NotFound e) {
            log.error("User not found with id: {}", userId);
            throw new UserServiceException("User with id " + userId + " does not exist");
        } catch (HttpClientErrorException e) {
            log.error("HTTP error while validating user {}: {} - {}", userId, e.getStatusCode(), e.getMessage());
            throw new UserServiceException("Error validating user: " + e.getMessage(), e);
        } catch (ResourceAccessException e) {
            log.error("User Service is unavailable: {}", e.getMessage());
            throw new UserServiceException("User Service is currently unavailable. Please try again later.", e);
        } catch (Exception e) {
            log.error("Unexpected error while validating user {}: {}", userId, e.getMessage());
            throw new UserServiceException("Failed to validate user existence", e);
        }
    }

    private DeviceResponse mapToResponse(Device device) {
        return new DeviceResponse(
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