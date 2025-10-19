package com.vio.authorization_service.util;

import com.vio.authorization_service.handler.*;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
@Slf4j
public class JwtUtil {

    @Value("${spring.jwt.secret}")
    private String secret;

    @Value("${spring.jwt.expiration}")
    private Long expiration;

    private Key getSigningKey() {
        try {
            byte[] keyBytes = secret.getBytes();
            return Keys.hmacShaKeyFor(keyBytes);
        } catch (Exception e) {
            log.error("Error creating signing key: {}", e.getMessage());
            throw new RuntimeException("Failed to create JWT signing key", e);
        }
    }

    public String extractUsername(String token) {
        try {
            return extractClaim(token, Claims::getSubject);
        } catch (Exception e) {
            log.error("Error extracting username from token: {}", e.getMessage());
            throw new InvalidTokenException("Invalid token format");
        }
    }

    public Long extractUserId(String token) {
        try {
            return extractClaim(token, claims -> ((Number) claims.get("userId")).longValue());
        } catch (Exception e) {
            log.error("Error extracting userId from token: {}", e.getMessage());
            throw new InvalidTokenException("Invalid token format");
        }
    }

    public String extractRole(String token) {
        try {
            return extractClaim(token, claims -> (String) claims.get("role"));
        } catch (Exception e) {
            log.error("Error extracting role from token: {}", e.getMessage());
            throw new InvalidTokenException("Invalid token format");
        }
    }

    public Date extractExpiration(String token) {
        try {
            return extractClaim(token, Claims::getExpiration);
        } catch (Exception e) {
            log.error("Error extracting expiration from token: {}", e.getMessage());
            throw new InvalidTokenException("Invalid token format");
        }
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            log.warn("JWT token has expired: {}", e.getMessage());
            throw new InvalidTokenException("Token has expired");
        } catch (JwtException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            throw new InvalidTokenException("Invalid token");
        } catch (Exception e) {
            log.error("Error parsing JWT token: {}", e.getMessage());
            throw new InvalidTokenException("Token parsing failed");
        }
    }

    private Boolean isTokenExpired(String token) {
        try {
            return extractExpiration(token).before(new Date());
        } catch (InvalidTokenException e) {
            // Token is invalid, consider it expired
            return true;
        }
    }

    public String generateToken(Long userId, String username, String role) {
        try {
            Map<String, Object> claims = new HashMap<>();
            claims.put("userId", userId);
            claims.put("role", role);
            return createToken(claims, username);
        } catch (Exception e) {
            log.error("Error generating token for user {}: {}", username, e.getMessage());
            throw new RuntimeException("Failed to generate authentication token", e);
        }
    }

    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public Boolean validateToken(String token) {
        try {
            return !isTokenExpired(token);
        } catch (InvalidTokenException e) {
            log.warn("Token validation failed: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("Unexpected error during token validation: {}", e.getMessage());
            return false;
        }
    }
}