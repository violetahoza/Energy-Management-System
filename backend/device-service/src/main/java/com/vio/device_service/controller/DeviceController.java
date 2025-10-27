package com.vio.device_service.controller;

import com.vio.device_service.dto.DeviceRequest;
import com.vio.device_service.dto.DeviceResponse;
import com.vio.device_service.service.DeviceService;
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
public class DeviceController {
    private final DeviceService service;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<DeviceResponse>> getAllDevices() {
        log.info("Admin fetching all devices");
        List<DeviceResponse> devices = service.getAllDevices();
        return ResponseEntity.ok(devices);
    }

    @GetMapping("/{deviceId}")
    @PreAuthorize("hasRole('ADMIN') or @deviceSecurityService.isDeviceOwnedByUser(#deviceId, principal)")
    public ResponseEntity<DeviceResponse> findById(@PathVariable Long deviceId) {
        log.info("Fetching device by id: {}", deviceId);
        DeviceResponse device = service.findById(deviceId);
        return ResponseEntity.ok(device);
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN') or principal == #userId.toString()")
    public ResponseEntity<List<DeviceResponse>> findByUserId(@PathVariable Long userId) {
        log.info("Fetching devices for user: {}", userId);
        List<DeviceResponse> devices = service.findByUserId(userId);
        return ResponseEntity.ok(devices);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DeviceResponse> createDevice(@RequestBody @Valid DeviceRequest request) {
        log.info("Admin creating new device: {}", request.name());
        DeviceResponse device = service.createDevice(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(device);
    }

    @PatchMapping("/{deviceId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DeviceResponse> updateById(@PathVariable Long deviceId, @RequestBody @Valid DeviceRequest request) {
        log.info("Admin updating device: {}", deviceId);
        DeviceResponse device = service.updateById(deviceId, request);
        return ResponseEntity.ok(device);
    }

    @PatchMapping("/{deviceId}/assign/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DeviceResponse> assignDeviceToUser(@PathVariable Long deviceId, @PathVariable Long userId) {
        log.info("Admin assigning device {} to user {}", deviceId, userId);
        DeviceResponse device = service.assignDeviceToUser(deviceId, userId);
        return ResponseEntity.ok(device);
    }

    @PatchMapping("/{deviceId}/unassign")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DeviceResponse> unassignDevice(@PathVariable Long deviceId) {
        log.info("Admin unassigning device: {}", deviceId);
        DeviceResponse device = service.unassignDevice(deviceId);
        return ResponseEntity.ok(device);
    }

    @DeleteMapping("/{deviceId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteById(@PathVariable Long deviceId) {
        log.info("Admin deleting device: {}", deviceId);
        service.deleteById(deviceId);
        return ResponseEntity.noContent().build();
    }
}