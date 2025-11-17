package com.vio.authorization_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

// Service to manage blacklisted tokens (for logout functionality)
@Service
@Slf4j
@RequiredArgsConstructor
public class TokenBlackListService {
    private final ConcurrentHashMap<String, Date> blacklistedTokens = new ConcurrentHashMap<>(); // store tokens with their expiration dates

    public void blacklistToken(String token, Date expirationDate) {
        blacklistedTokens.put(token, expirationDate);
        log.info("Token blacklisted until: {}", expirationDate);
        cleanupExpiredTokens();
    }

    public boolean isTokenBlacklisted(String token) {
        if (blacklistedTokens.containsKey(token)) {
            Date expirationDate = blacklistedTokens.get(token);
            if (expirationDate.before(new Date())) {
                blacklistedTokens.remove(token);
                return false;
            }
            return true;
        }
        return false;
    }

    private void cleanupExpiredTokens() {
        Date now = new Date();
        blacklistedTokens.entrySet().removeIf(entry -> entry.getValue().before(now));
    }
}
