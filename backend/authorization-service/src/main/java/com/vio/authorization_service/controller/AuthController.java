package com.vio.authorization_service.controller;

import com.vio.authorization_service.dto.*;
import com.vio.authorization_service.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication", description = "User authentication and authorization endpoints for login, registration, and token management")
public class AuthController {
    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "User login", description = "Authenticate user with username and password. Returns a JWT token that should be included in the Authorization header (as 'Bearer {token}') for subsequent requests to protected endpoints.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully authenticated", content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request format", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Invalid credentials", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error during login", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login endpoint called for user: {}", request.username());
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    @Operation(summary = "User logout", description = "Invalidate the current JWT token by adding it to the blacklist. The token will no longer be accepted for authentication after logout.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully logged out"),
            @ApiResponse(responseCode = "401", description = "Invalid or expired token", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error during logout", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Map<String, String>> logout(@RequestHeader("Authorization") String token) {
        log.info("Logout endpoint called");
        String jwtToken = token.startsWith("Bearer ") ? token.substring(7) : token;
        authService.logout(jwtToken);
        return ResponseEntity.ok(Map.of("message", "Logout successful"));
    }

    @GetMapping("/validate")
    @Operation(summary = "Validate JWT token", description = "Validate JWT token and return user information. This endpoint is used by Traefik ForwardAuth middleware to authenticate requests.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Token is valid - returns user information in response body and headers (X-User-Id, X-Username, X-User-Role)"),
            @ApiResponse(responseCode = "401", description = "Token is invalid, expired, blacklisted, or missing")
    })
    public ResponseEntity<?> validateToken(@RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        log.info("ForwardAuth validation request received");

        try {
            AuthResponse userInfo = authService.validateAuthorizationHeader(authorizationHeader);

            log.info("Request authorized for user: {} (role: {})", userInfo.username(), userInfo.role());

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
            log.error("❌ Token validation failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", e.getMessage()));
        }
    }


    @GetMapping("/user")
    @Operation(summary = "Get current user information", description = "Extract and return user information from the provided JWT token.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User information retrieved successfully", content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "401", description = "Invalid, expired, or blacklisted token", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<AuthResponse> getUserFromToken(@RequestHeader("Authorization") String token) {
        log.info("Get user from token request");
        String jwtToken = token.startsWith("Bearer ") ? token.substring(7) : token;
        AuthResponse response = authService.getUserFromToken(jwtToken);
        return ResponseEntity.ok(response);
    }
}