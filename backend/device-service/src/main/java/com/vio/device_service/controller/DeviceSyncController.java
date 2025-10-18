package com.vio.device_service.controller;

import com.vio.device_service.service.DeviceSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/devices/sync")
@RequiredArgsConstructor
@Slf4j
public class DeviceSyncController {
    private final DeviceSyncService syncService;

    @PostMapping("/unassign-user/{userId}")
    public ResponseEntity<Void> unassignUserDevices(@PathVariable Long userId) {
        log.info("Received sync request to unassign devices for deleted user: {}", userId);
        syncService.unassignDevicesForDeletedUser(userId);
        return ResponseEntity.ok().build();
    }
}