package com.vio.authorization_service.controller;

import com.vio.authorization_service.dto.AuthResponse;
import com.vio.authorization_service.dto.LoginRequest;
import com.vio.authorization_service.dto.RegisterRequest;
import com.vio.authorization_service.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Register endpoint called for user: {}", request.username());
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login endpoint called for user: {}", request.username());
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(@RequestHeader("Authorization") String token) {
        log.info("Logout endpoint called");
        String jwtToken = token.startsWith("Bearer ") ? token.substring(7) : token;
        authService.logout(jwtToken);
        return ResponseEntity.ok(Map.of("message", "Logout successful"));
    }

    @GetMapping("/validate")
    public ResponseEntity<Boolean> validateToken(@RequestHeader("Authorization") String token) {
        String jwtToken = token.startsWith("Bearer ") ? token.substring(7) : token;
        boolean isValid = authService.validateToken(jwtToken);
        return ResponseEntity.ok(isValid);
    }

    @GetMapping("/user")
    public ResponseEntity<AuthResponse> getUserFromToken(@RequestHeader("Authorization") String token) {
        String jwtToken = token.startsWith("Bearer ") ? token.substring(7) : token;
        AuthResponse response = authService.getUserFromToken(jwtToken);
        return ResponseEntity.ok(response);
    }
}