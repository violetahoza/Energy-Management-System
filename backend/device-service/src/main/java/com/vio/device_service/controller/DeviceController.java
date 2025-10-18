package com.vio.device_service.controller;

import com.vio.device_service.dto.DeviceDTORequest;
import com.vio.device_service.dto.DeviceDTOResponse;
import com.vio.device_service.dto.DeviceUpdateRequest;
import com.vio.device_service.service.DeviceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/devices")
@RequiredArgsConstructor
public class DeviceController {
    private final DeviceService service;

    @GetMapping
    public ResponseEntity<List<DeviceDTOResponse>> getAllDevices() {
        List<DeviceDTOResponse> devices = service.getAllDevices();
        return ResponseEntity.ok(devices);
    }

    @GetMapping("/id={deviceId}")
    public ResponseEntity<DeviceDTOResponse> findById(@PathVariable Long deviceId) {
        DeviceDTOResponse device = service.findById(deviceId);
        return ResponseEntity.ok(device);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<DeviceDTOResponse>> findByUserId(@PathVariable Long userId) {
        List<DeviceDTOResponse> devices = service.findByUserId(userId);
        return ResponseEntity.ok(devices);
    }

    @PostMapping
    public ResponseEntity<DeviceDTOResponse> createDevice(@RequestBody @Valid DeviceDTORequest request) {
        DeviceDTOResponse device = service.createDevice(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(device);
    }

    @PatchMapping("/id={deviceId}")
    public ResponseEntity<DeviceDTOResponse> updateById(
            @PathVariable Long deviceId,
            @RequestBody @Valid DeviceUpdateRequest request) {
        DeviceDTOResponse device = service.updateById(deviceId, request);
        return ResponseEntity.ok(device);
    }

    @PatchMapping("/id={deviceId}/assign/{userId}")
    public ResponseEntity<DeviceDTOResponse> assignDeviceToUser(
            @PathVariable Long deviceId,
            @PathVariable Long userId) {
        DeviceDTOResponse device = service.assignDeviceToUser(deviceId, userId);
        return ResponseEntity.ok(device);
    }

    @DeleteMapping("/id={deviceId}")
    public ResponseEntity<Void> deleteById(@PathVariable Long deviceId) {
        service.deleteById(deviceId);
        return ResponseEntity.noContent().build();
    }
}