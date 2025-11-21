package com.vio.monitoring_service.controller;

import com.vio.monitoring_service.dto.DailyConsumptionResponse;
import com.vio.monitoring_service.service.MonitoringService;
import io.swagger.v3.oas.annotations.Operation;
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
    public ResponseEntity<DailyConsumptionResponse> getDailyConsumption(@PathVariable Long deviceId, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        log.info("GET /api/monitoring/devices/{}/consumption/daily?date={}", deviceId, date);
        DailyConsumptionResponse response = service.getDailyConsumption(deviceId, date);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/devices/{deviceId}/consumption/test")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Test endpoint - Get today's consumption", description = "Quick test endpoint to see consumption data for today. Admin only.")
    public ResponseEntity<DailyConsumptionResponse> getTodayConsumption(@PathVariable Long deviceId) {
        LocalDate today = LocalDate.now();
        log.info("GET /api/monitoring/devices/{}/consumption/test (today: {})", deviceId, today);
        DailyConsumptionResponse response = service.getDailyConsumption(deviceId, today);
        return ResponseEntity.ok(response);
    }
}