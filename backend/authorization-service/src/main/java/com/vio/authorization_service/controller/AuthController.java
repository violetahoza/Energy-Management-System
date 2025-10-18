package com.vio.authorization_service.controller;

import com.vio.authorization_service.dto.AuthResponse;
import com.vio.authorization_service.dto.LoginRequest;
import com.vio.authorization_service.dto.RegisterRequest;
import com.vio.authorization_service.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/validate")
    public ResponseEntity<Boolean> validateToken(@RequestHeader("Authorization") String token) {
        String jwtToken = token.substring(7);
        boolean isValid = authService.validateToken(jwtToken);
        return ResponseEntity.ok(isValid);
    }

    @GetMapping("/user")
    public ResponseEntity<AuthResponse> getUserFromToken(@RequestHeader("Authorization") String token) {
        String jwtToken = token.substring(7);
        AuthResponse response = authService.getUserFromToken(jwtToken);
        return ResponseEntity.ok(response);
    }
}
