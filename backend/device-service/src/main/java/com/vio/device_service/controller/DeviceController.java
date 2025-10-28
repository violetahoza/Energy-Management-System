package com.vio.device_service.controller;

import com.vio.device_service.dto.DeviceRequest;
import com.vio.device_service.dto.DeviceResponse;
import com.vio.device_service.service.DeviceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/devices")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Device Management", description = "CRUD operations for devices")
@SecurityRequirement(name = "bearer-jwt")
public class DeviceController {
    private final DeviceService service;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Get all devices",
            description = "Retrieve all devices in the system. Admin role required."
    )
    public ResponseEntity<List<DeviceResponse>> getAllDevices() {
        log.info("Admin fetching all devices");
        List<DeviceResponse> devices = service.getAllDevices();
        return ResponseEntity.ok(devices);
    }

    @GetMapping("/{deviceId}")
    @PreAuthorize("hasRole('ADMIN') or @deviceSecurityService.isDeviceOwnedByUser(#deviceId, principal)")
    @Operation(
            summary = "Get device by ID",
            description = "Retrieve a specific device. Admins can access any device, clients can only access their own."
    )
    public ResponseEntity<DeviceResponse> findById(@PathVariable Long deviceId) {
        log.info("Fetching device by id: {}", deviceId);
        DeviceResponse device = service.findById(deviceId);
        return ResponseEntity.ok(device);
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN') or principal == #userId.toString()")
    @Operation(
            summary = "Get devices by user ID",
            description = "Retrieve all devices assigned to a specific user"
    )
    public ResponseEntity<List<DeviceResponse>> findByUserId(@PathVariable Long userId) {
        log.info("Fetching devices for user: {}", userId);
        List<DeviceResponse> devices = service.findByUserId(userId);
        return ResponseEntity.ok(devices);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Create device",
            description = "Create a new device. Admin role required."
    )
    public ResponseEntity<DeviceResponse> createDevice(@RequestBody @Valid DeviceRequest request) {
        log.info("Admin creating new device: {}", request.name());
        DeviceResponse device = service.createDevice(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(device);
    }

    @PatchMapping("/{deviceId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Update device",
            description = "Update an existing device. Admin role required."
    )
    public ResponseEntity<DeviceResponse> updateById(@PathVariable Long deviceId, @RequestBody @Valid DeviceRequest request) {
        log.info("Admin updating device: {}", deviceId);
        DeviceResponse device = service.updateById(deviceId, request);
        return ResponseEntity.ok(device);
    }

    @PatchMapping("/{deviceId}/assign/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Assign device to user",
            description = "Assign a device to a specific user. Admin role required."
    )
    public ResponseEntity<DeviceResponse> assignDeviceToUser(@PathVariable Long deviceId, @PathVariable Long userId) {
        log.info("Admin assigning device {} to user {}", deviceId, userId);
        DeviceResponse device = service.assignDeviceToUser(deviceId, userId);
        return ResponseEntity.ok(device);
    }

    @PatchMapping("/{deviceId}/unassign")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Unassign device",
            description = "Remove device assignment from user. Admin role required."
    )
    public ResponseEntity<DeviceResponse> unassignDevice(@PathVariable Long deviceId) {
        log.info("Admin unassigning device: {}", deviceId);
        DeviceResponse device = service.unassignDevice(deviceId);
        return ResponseEntity.ok(device);
    }

    @DeleteMapping("/{deviceId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Delete device",
            description = "Delete a device from the system. Admin role required."
    )
    public ResponseEntity<Void> deleteById(@PathVariable Long deviceId) {
        log.info("Admin deleting device: {}", deviceId);
        service.deleteById(deviceId);
        return ResponseEntity.noContent().build();
    }
}