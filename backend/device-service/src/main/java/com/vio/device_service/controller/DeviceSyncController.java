package com.vio.device_service.controller;

import com.vio.device_service.dto.ErrorResponse;
import com.vio.device_service.service.DeviceSyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/devices/sync")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Internal - Device Sync", description = "Internal endpoints for synchronizing device data across services. These endpoints are intended for inter-service communication only.")
public class DeviceSyncController {
    private final DeviceSyncService syncService;

    @PostMapping("/unassign-user/{userId}")
    @Operation(summary = "Unassign all devices from a deleted user",
            description = "Internal endpoint that removes user assignments from all devices when a user is deleted from the system. " +
                    "The devices remain in the database but their userId is set to null, making them available for reassignment. " +
                    "This endpoint is called by the User Service after a user deletion."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Devices successfully unassigned from the deleted user"),
            @ApiResponse(responseCode = "400", description = "Invalid user ID provided", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Access denied - insufficient permissions to access this internal endpoint", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error occurred while unassigning devices", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> unassignUserDevices(@PathVariable Long userId) {
        log.info("Received sync request to unassign devices for deleted user: {}", userId);
        syncService.unassignDevicesForDeletedUser(userId);
        return ResponseEntity.ok().build();
    }
}