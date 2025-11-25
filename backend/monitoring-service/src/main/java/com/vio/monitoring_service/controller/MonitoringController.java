package com.vio.monitoring_service.controller;

import com.vio.monitoring_service.dto.DailyConsumptionResponse;
import com.vio.monitoring_service.dto.ErrorResponse;
import com.vio.monitoring_service.service.MonitoringService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/monitoring")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Device Data Monitoring", description = "Monitoring microservice APIs for device energy consumption")
public class MonitoringController {

    private final MonitoringService service;

    @GetMapping("/devices/{deviceId}/consumption/daily")
    @PreAuthorize("hasRole('ADMIN') or @deviceSecurityService.isDeviceOwnedByUser(#deviceId, principal)")
    @Operation(summary = "Get daily energy consumption", description = "Retrieve hourly energy consumption data for a specific device on a specific date. Returns 24 hours of data (0-23) with consumption in kWh for each hour.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved daily consumption data", content = @Content(mediaType = "application/json", schema = @Schema(implementation = DailyConsumptionResponse.class))),
            @ApiResponse(responseCode = "404", description = "Device not found in the monitoring system", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden: User does not have access to this device", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "400", description = "Bad Request (e.g., Invalid date format)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<DailyConsumptionResponse> getDailyConsumption(@PathVariable Long deviceId, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        log.info("GET /api/monitoring/devices/{}/consumption/daily?date={}", deviceId, date);
        DailyConsumptionResponse response = service.getDailyConsumption(deviceId, date);
        return ResponseEntity.ok(response);
    }
}