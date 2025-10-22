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
    public ResponseEntity<?> validateToken(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {

        log.info("ForwardAuth validation request received");

        try {
            AuthResponse userInfo = authService.validateAuthorizationHeader(authorizationHeader);

            log.info("Request authorized for user: {} (role: {})",
                    userInfo.username(), userInfo.role());

            // Return 200 OK with user information in headers
            // These headers will be forwarded to downstream services by Traefik
            return ResponseEntity.ok()
                    .header("X-User-Id", userInfo.userId().toString())
                    .header("X-Username", userInfo.username())
                    .header("X-User-Role", userInfo.role())
                    .body(Map.of(
                            "valid", true,
                            "userId", userInfo.userId(),
                            "username", userInfo.username(),
                            "role", userInfo.role()
                    ));

        } catch (Exception e) {
            log.error("Token validation failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", e.getMessage()));
        }
    }


    @GetMapping("/user")
    public ResponseEntity<AuthResponse> getUserFromToken(@RequestHeader("Authorization") String token) {
        log.info("Get user from token request");
        String jwtToken = token.startsWith("Bearer ") ? token.substring(7) : token;
        AuthResponse response = authService.getUserFromToken(jwtToken);
        return ResponseEntity.ok(response);
    }
}