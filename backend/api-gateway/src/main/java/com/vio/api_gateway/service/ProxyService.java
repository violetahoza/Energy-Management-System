package com.vio.api_gateway.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProxyService {

    private final RestTemplate restTemplate;

    public ResponseEntity<String> forwardRequest(HttpServletRequest request, String targetServiceUrl) {
        try {
            String requestUri = request.getRequestURI();
            String queryString = request.getQueryString();

            // Build target URL
            String targetUrl = targetServiceUrl + requestUri;
            if (queryString != null && !queryString.isEmpty()) {
                targetUrl += "?" + queryString;
            }

            log.debug("Forwarding {} request to: {}", request.getMethod(), targetUrl);

            // Copy headers
            HttpHeaders headers = new HttpHeaders();
            Enumeration<String> headerNames = request.getHeaderNames();
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                // Skip hop-by-hop headers
                if (!isHopByHopHeader(headerName)) {
                    headers.put(headerName, Collections.list(request.getHeaders(headerName)));
                }
            }

            // Read request body
            String body = null;
            if (request.getContentLength() > 0) {
                body = StreamUtils.copyToString(request.getInputStream(), java.nio.charset.StandardCharsets.UTF_8);
            }

            // Create request entity
            HttpEntity<String> requestEntity = new HttpEntity<>(body, headers);

            // Forward request to target service
            ResponseEntity<String> response = restTemplate.exchange(
                    targetUrl,
                    HttpMethod.valueOf(request.getMethod()),
                    requestEntity,
                    String.class
            );

            log.debug("Received response with status: {}", response.getStatusCode());
            return response;

        } catch (HttpClientErrorException e) {
            log.error("HTTP client error: {} - {}", e.getStatusCode(), e.getMessage());
            return ResponseEntity
                    .status(e.getStatusCode())
                    .body(e.getResponseBodyAsString());
        } catch (IOException e) {
            log.error("Error reading request body: {}", e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\": \"Failed to read request body\"}");
        } catch (Exception e) {
            log.error("Error forwarding request: {}", e.getMessage(), e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\": \"Gateway error: " + e.getMessage() + "\"}");
        }
    }

    private boolean isHopByHopHeader(String headerName) {
        String lowerCaseHeaderName = headerName.toLowerCase();
        return lowerCaseHeaderName.equals("connection") ||
                lowerCaseHeaderName.equals("keep-alive") ||
                lowerCaseHeaderName.equals("proxy-authenticate") ||
                lowerCaseHeaderName.equals("proxy-authorization") ||
                lowerCaseHeaderName.equals("te") ||
                lowerCaseHeaderName.equals("trailers") ||
                lowerCaseHeaderName.equals("transfer-encoding") ||
                lowerCaseHeaderName.equals("upgrade");
    }
}