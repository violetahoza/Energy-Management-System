package com.vio.device_service.service;

import com.vio.device_service.dto.*;
import com.vio.device_service.handler.*;
import com.vio.device_service.model.Device;
import com.vio.device_service.repository.DeviceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeviceService {
    private final DeviceRepository deviceRepository;
    private final RestTemplate restTemplate;

    private static final String USER_SERVICE_URL = "http://user-service:8081/api/users";

    public List<DeviceResponse> getAllDevices() {
        log.info("Fetching all devices");
        try {
            return deviceRepository.findAll()
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

        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new DeviceNotFoundException(deviceId));
        return mapToResponse(device);
    }

    public List<DeviceResponse> findByUserId(Long userId) {
        log.info("Fetching devices for user with id: {}", userId);
        validateUserId(userId);
        validateUserIsClient(userId);

        try {
            return deviceRepository.findByUserId(userId)
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
            validateUserIsClient(request.userId());
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

            Device savedDevice = deviceRepository.save(device);
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
            Device device = deviceRepository.findById(deviceId)
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
                validateUserIsClient(request.userId());
                device.setUserId(request.userId());
                updated = true;
            }

            if (updated) {
                device.setUpdatedAt(LocalDateTime.now());
                Device updatedDevice = deviceRepository.save(device);
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
        validateUserIsClient(userId);

        try {
            Device device = deviceRepository.findById(deviceId).orElseThrow(() -> new DeviceNotFoundException(deviceId));

            device.setUserId(userId);
            device.setUpdatedAt(LocalDateTime.now());

            Device updatedDevice = deviceRepository.save(device);
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
            Device device = deviceRepository.findById(deviceId).orElseThrow(() -> new DeviceNotFoundException(deviceId));

            device.setUserId(null);
            device.setUpdatedAt(LocalDateTime.now());

            Device updatedDevice = deviceRepository.save(device);
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
            Device device = deviceRepository.findById(deviceId).orElseThrow(() -> new DeviceNotFoundException(deviceId));
            deviceRepository.delete(device);
            log.info("Device deleted successfully with id: {}", deviceId);
        } catch (DeviceNotFoundException | IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error deleting device {}: {}", deviceId, e.getMessage());
            throw new RuntimeException("Failed to delete device", e);
        }
    }

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
        if (request.description() == null || request.description().isEmpty()) {
            throw new IllegalArgumentException("Device description cannot be empty");
        }
    }

    private void validateUserIsClient(Long userId) {
        log.debug("Validating user {} has CLIENT role", userId);

        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(USER_SERVICE_URL  + "/internal/role/" + userId, Map.class);

            if (response.getBody() == null) {
                throw new UserServiceException("Empty response when fetching user role");
            }

            String role = (String) response.getBody().get("role");

            if (role == null || !role.equals("CLIENT")) {
                log.error("User {} has role {} which is not CLIENT", userId, role);
                throw new UserServiceException("Devices can only be assigned to users with CLIENT role");
            }

            log.debug("User {} validation successful: has CLIENT role", userId);
        } catch (HttpClientErrorException.NotFound e) {
            log.error("User not found with id: {}", userId);
            throw new UserNotFoundException("User with id " + userId + " does not exist");
        } catch (UserServiceException | UserNotFoundException  e) {
            throw e;
        } catch (Exception e) {
            log.error("Error validating user role for user {}: {}", userId, e.getMessage());
            throw new UserServiceException("Failed to validate user role: " + e.getMessage(), e);
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