package com.vio.api_gateway.controller;

import com.vio.api_gateway.service.ProxyService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Slf4j
public class GatewayController {

    private final ProxyService proxyService;

    // ROOT ENDPOINT
    @GetMapping("/")
    public ResponseEntity<Map<String, String>> root() {
        Map<String, String> response = new HashMap<>();
        response.put("service", "API Gateway");
        response.put("status", "running");
        response.put("message", "Energy Management System API Gateway");
        return ResponseEntity.ok(response);
    }

    // PUBLIC ENDPOINTS

    @PostMapping("/api/auth/login")
    public ResponseEntity<String> login(HttpServletRequest request) {
        log.info("Gateway: Forwarding login request");
        return proxyService.forwardRequest(request, "http://authorization-service:8083");
    }

    @PostMapping("/api/auth/register")
    public ResponseEntity<String> register(HttpServletRequest request) {
        log.info("Gateway: Forwarding register request");
        return proxyService.forwardRequest(request, "http://authorization-service:8083");
    }

    // AUTHENTICATED ENDPOINTS

    @PostMapping("/api/auth/logout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> logout(HttpServletRequest request) {
        log.info("Gateway: Forwarding logout request");
        return proxyService.forwardRequest(request, "http://authorization-service:8083");
    }

    @GetMapping("/api/auth/validate")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> validateToken(HttpServletRequest request) {
        log.info("Gateway: Forwarding token validation request");
        return proxyService.forwardRequest(request, "http://authorization-service:8083");
    }

    @GetMapping("/api/auth/user")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> getUserFromToken(HttpServletRequest request) {
        log.info("Gateway: Forwarding get user from token request");
        return proxyService.forwardRequest(request, "http://authorization-service:8083");
    }

    // USER ENDPOINTS (CLIENT & ADMIN)

    @GetMapping("/api/users/id={userId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> getUserById(HttpServletRequest request, @PathVariable Long userId) {
        String role = (String) request.getAttribute("role");
        Long authenticatedUserId = (Long) request.getAttribute("userId");

        // Clients can only view their own profile, Admins can view any
        if ("CLIENT".equals(role) && !userId.equals(authenticatedUserId)) {
            log.warn("Client {} attempted to access user {}", authenticatedUserId, userId);
            return ResponseEntity.status(403).body("{\"error\": \"Forbidden\", \"message\": \"You can only access your own profile\"}");
        }

        log.info("Gateway: Forwarding get user request for userId: {}", userId);
        return proxyService.forwardRequest(request, "http://user-service:8081");
    }

    @PatchMapping("/api/users/id={userId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> updateUser(HttpServletRequest request, @PathVariable Long userId) {
        String role = (String) request.getAttribute("role");
        Long authenticatedUserId = (Long) request.getAttribute("userId");

        // Clients can only update their own profile, Admins can update any
        if ("CLIENT".equals(role) && !userId.equals(authenticatedUserId)) {
            log.warn("Client {} attempted to update user {}", authenticatedUserId, userId);
            return ResponseEntity.status(403).body("{\"error\": \"Forbidden\", \"message\": \"You can only update your own profile\"}");
        }

        log.info("Gateway: Forwarding update user request for userId: {}", userId);
        return proxyService.forwardRequest(request, "http://user-service:8081");
    }

    // ADMIN-ONLY USER ENDPOINTS

    @GetMapping("/api/admin/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> getAllUsers(HttpServletRequest request) {
        log.info("Gateway: Forwarding get all users request (admin)");
        return proxyService.forwardRequest(request, "http://user-service:8081");
    }

    @GetMapping("/api/admin/users/id={userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> getAdminUserById(HttpServletRequest request, @PathVariable Long userId) {
        log.info("Gateway: Forwarding get admin user request for userId: {}", userId);
        return proxyService.forwardRequest(request, "http://user-service:8081");
    }

    @PostMapping("/api/admin/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> createUser(HttpServletRequest request) {
        log.info("Gateway: Forwarding create user request (admin)");
        return proxyService.forwardRequest(request, "http://user-service:8081");
    }

    @PatchMapping("/api/admin/users/id={userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> adminUpdateUser(HttpServletRequest request, @PathVariable Long userId) {
        log.info("Gateway: Forwarding admin update user request for userId: {}", userId);
        return proxyService.forwardRequest(request, "http://user-service:8081");
    }

    @DeleteMapping("/api/admin/users/id={userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> deleteUser(HttpServletRequest request, @PathVariable Long userId) {
        log.info("Gateway: Forwarding delete user request for userId: {}", userId);
        return proxyService.forwardRequest(request, "http://user-service:8081");
    }

    // DEVICE ENDPOINTS

    @GetMapping("/api/devices")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> getAllDevices(HttpServletRequest request) {
        log.info("Gateway: Forwarding get all devices request (admin)");
        return proxyService.forwardRequest(request, "http://device-service:8082");
    }

    @GetMapping("/api/devices/id={deviceId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> getDeviceById(HttpServletRequest request, @PathVariable Long deviceId) {
        log.info("Gateway: Forwarding get device request for deviceId: {}", deviceId);
        return proxyService.forwardRequest(request, "http://device-service:8082");
    }

    @GetMapping("/api/devices/user/{userId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> getDevicesByUser(HttpServletRequest request, @PathVariable Long userId) {
        String role = (String) request.getAttribute("role");
        Long authenticatedUserId = (Long) request.getAttribute("userId");

        // Clients can only view their own devices, Admins can view any
        if ("CLIENT".equals(role) && !userId.equals(authenticatedUserId)) {
            log.warn("Client {} attempted to access devices of user {}", authenticatedUserId, userId);
            return ResponseEntity.status(403).body("{\"error\": \"Forbidden\", \"message\": \"You can only access your own devices\"}");
        }

        log.info("Gateway: Forwarding get devices by user request for userId: {}", userId);
        return proxyService.forwardRequest(request, "http://device-service:8082");
    }

    @PostMapping("/api/devices")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> createDevice(HttpServletRequest request) {
        log.info("Gateway: Forwarding create device request (admin)");
        return proxyService.forwardRequest(request, "http://device-service:8082");
    }

    @PatchMapping("/api/devices/id={deviceId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> updateDevice(HttpServletRequest request, @PathVariable Long deviceId) {
        log.info("Gateway: Forwarding update device request for deviceId: {}", deviceId);
        return proxyService.forwardRequest(request, "http://device-service:8082");
    }

    @PatchMapping("/api/devices/id={deviceId}/assign/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> assignDevice(HttpServletRequest request,
                                               @PathVariable Long deviceId,
                                               @PathVariable Long userId) {
        log.info("Gateway: Forwarding assign device {} to user {}", deviceId, userId);
        return proxyService.forwardRequest(request, "http://device-service:8082");
    }

    @DeleteMapping("/api/devices/id={deviceId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> deleteDevice(HttpServletRequest request, @PathVariable Long deviceId) {
        log.info("Gateway: Forwarding delete device request for deviceId: {}", deviceId);
        return proxyService.forwardRequest(request, "http://device-service:8082");
    }
}