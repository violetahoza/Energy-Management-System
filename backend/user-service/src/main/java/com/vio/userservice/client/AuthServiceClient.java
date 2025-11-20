package com.vio.userservice.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class AuthServiceClient {

    private final RestTemplate restTemplate;
    private final String authServiceUrl;

    public AuthServiceClient(RestTemplate restTemplate, @Value("http://authorization-service:8083") String authServiceUrl) {
        this.restTemplate = restTemplate;
        this.authServiceUrl = authServiceUrl;
    }

    public Map<String, String> getUserCredentials(Long userId) {
        try {
            String url = authServiceUrl + "/api/auth/internal/credentials/user/" + userId;
            log.debug("Fetching credentials from: {}", url);
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                Map<String, String> credentials = new HashMap<>();
                credentials.put("username", (String) body.get("username"));
                credentials.put("role", (String) body.get("role"));
                log.debug("Successfully fetched credentials for userId: {}", userId);
                return credentials;
            }

            log.warn("No credentials found for userId: {}", userId);
            return null;
        } catch (RestClientException e) {
            log.error("Error fetching credentials for userId {}: {}", userId, e.getMessage());
            return null;
        }
    }

    public Map<Long, Map<String, String>> getUserCredentialsBatch(Iterable<Long> userIds) {
        Map<Long, Map<String, String>> results = new HashMap<>();
        for (Long userId : userIds) {
            Map<String, String> credentials = getUserCredentials(userId);
            if (credentials != null) {
                results.put(userId, credentials);
            }
        }
        return results;
    }
}