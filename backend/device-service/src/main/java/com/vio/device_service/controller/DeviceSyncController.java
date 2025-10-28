package com.vio.device_service.controller;

import com.vio.device_service.service.DeviceSyncService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/devices/sync")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Internal - Device Sync")
public class DeviceSyncController {
    private final DeviceSyncService syncService;

    @PostMapping("/unassign-user/{userId}")
    @Operation(summary = "Unassign all devices from deleted user (internal)")
    public ResponseEntity<Void> unassignUserDevices(@PathVariable Long userId) {
        log.info("Received sync request to unassign devices for deleted user: {}", userId);
        syncService.unassignDevicesForDeletedUser(userId);
        return ResponseEntity.ok().build();
    }
}