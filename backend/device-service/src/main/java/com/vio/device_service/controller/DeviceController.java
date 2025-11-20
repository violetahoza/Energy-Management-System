package com.vio.device_service.controller;

import com.vio.device_service.dto.DeviceRequest;
import com.vio.device_service.dto.DeviceResponse;
import com.vio.device_service.dto.ErrorResponse;
import com.vio.device_service.service.DeviceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
public class DeviceController {
    private final DeviceService service;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all devices", description = "Retrieve all the devices in the system. Admin role required.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved all devices", content = @Content(mediaType = "application/json", schema = @Schema(implementation = DeviceResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - User does not have ADMIN role", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<List<DeviceResponse>> getAllDevices() {
        log.info("Admin fetching all devices");
        List<DeviceResponse> devices = service.getAllDevices();
        return ResponseEntity.ok(devices);
    }

    @GetMapping("/{deviceId}")
    @PreAuthorize("hasRole('ADMIN') or @deviceSecurityService.isDeviceOwnedByUser(#deviceId, principal)")
    @Operation(summary = "Get device by ID", description = "Retrieve a specific device by its ID. Admins can access any device, clients can only access devices assigned to them.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved device", content = @Content(mediaType = "application/json", schema = @Schema(implementation = DeviceResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - Client attempting to access device not assigned to them", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Device not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<DeviceResponse> findById(@PathVariable Long deviceId) {
        log.info("Fetching device by id: {}", deviceId);
        DeviceResponse device = service.findById(deviceId);
        return ResponseEntity.ok(device);
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN') or principal == #userId.toString()")
    @Operation(summary = "Get devices by user ID", description = "Retrieve all devices assigned to a specific user. Administrators can view devices for any user. Clients can only view their own devices.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved devices for user", content = @Content(mediaType = "application/json", schema = @Schema(implementation = DeviceResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - Client attempting to view another user's devices", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "User not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "503", description = "Service Unavailable - Cannot communicate with User Service", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<List<DeviceResponse>> findByUserId(@PathVariable Long userId) {
        log.info("Fetching devices for user: {}", userId);
        List<DeviceResponse> devices = service.findByUserId(userId);
        return ResponseEntity.ok(devices);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create new device", description = "Create a new device. Only administrators can create devices. The device can optionally be assigned to a user during creation.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Device created successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = DeviceResponse.class))),
            @ApiResponse(responseCode = "400", description = "Bad Request - Validation failed or invalid data", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - User does not have ADMIN role", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "User not found - When assigning to non-existent user", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "503", description = "Service Unavailable - Cannot communicate with User Service", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<DeviceResponse> createDevice(@RequestBody @Valid DeviceRequest request) {
        log.info("Admin creating new device: {}", request.name());
        DeviceResponse device = service.createDevice(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(device);
    }

    @PatchMapping("/{deviceId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update device", description = "Update an existing device. Only administrators can update devices. All fields in the request body will replace existing values.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Device updated successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = DeviceResponse.class))),
            @ApiResponse(responseCode = "400", description = "Bad Request -  Invalid data", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - User does not have ADMIN role", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Device not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<DeviceResponse> updateById(@PathVariable Long deviceId, @RequestBody @Valid DeviceRequest request) {
        log.info("Admin updating device: {}", deviceId);
        DeviceResponse device = service.updateById(deviceId, request);
        return ResponseEntity.ok(device);
    }

    @PatchMapping("/{deviceId}/assign/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Assign device to user", description = "Assign a device to a specific user. Only administrators can perform device assignments. The device must exist and not be currently assigned to another user.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Device assigned to user successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = DeviceResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - User does not have ADMIN role", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Device or user not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "503", description = "Service Unavailable - Cannot communicate with User Service", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<DeviceResponse> assignDeviceToUser(@PathVariable Long deviceId, @PathVariable Long userId) {
        log.info("Admin assigning device {} to user {}", deviceId, userId);
        DeviceResponse device = service.assignDeviceToUser(deviceId, userId);
        return ResponseEntity.ok(device);
    }

    @PatchMapping("/{deviceId}/unassign")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Unassign device from user", description = "Remove the current user assignment from a device, making it unassigned. Only administrators can unassign devices.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Device unassigned successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = DeviceResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - User does not have ADMIN role", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Device not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<DeviceResponse> unassignDevice(@PathVariable Long deviceId) {
        log.info("Admin unassigning device: {}", deviceId);
        DeviceResponse device = service.unassignDevice(deviceId);
        return ResponseEntity.ok(device);
    }

    @DeleteMapping("/{deviceId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete device", description = "Permanently delete a device from the system. Only administrators can delete devices. This action cannot be undone.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Device deleted successfully - No content returned"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - User does not have ADMIN role", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Device not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> deleteById(@PathVariable Long deviceId) {
        log.info("Admin deleting device: {}", deviceId);
        service.deleteById(deviceId);
        return ResponseEntity.noContent().build();
    }
}