package com.vio.customer_support.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Map;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class GeminiService {
    private final WebClient webClient;

    @Value("${gemini.api.key:}")
    private String apiKey;

    @Value("${gemini.api.url}")
    private String apiUrl;

    public GeminiService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    public CompletableFuture<String> getAIResponse(String userMessage) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            log.warn("Gemini API key not configured");
            return CompletableFuture.completedFuture("I'm sorry, AI support is currently unavailable. An administrator will assist you shortly.");
        }

        String prompt = buildCustomerSupportPrompt(userMessage);

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(
                                Map.of("text", prompt)
                        ))
                ),
                "generationConfig", Map.of(
                        "temperature", 0.7,
                        "maxOutputTokens", 500
                )
        );

        String finalUrl = apiUrl + "?key=" + apiKey;

        return webClient.post()
                .uri(finalUrl)
                .header("Content-Type", "application/json")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .map(this::extractTextFromResponse)
                .doOnError(error -> {
                    log.error("Error calling Gemini API: {}", error.getMessage());
                    if (error instanceof WebClientResponseException) {
                        WebClientResponseException we = (WebClientResponseException) error;
                        log.error("Google API Response Status: {} Body: {}", we.getStatusCode(), we.getResponseBodyAsString());
                    }
                })
                .onErrorReturn("I apologize, but I'm having trouble processing your request. An administrator will assist you shortly.")
                .toFuture();
    }

    private String buildCustomerSupportPrompt(String userMessage) {
        return String.format("""
            You are a helpful customer support assistant for an Energy Management System.
            The system allows users to monitor their energy consumption through smart devices.
            
            User question: %s
            
            Provide a helpful, concise response (max 350 words). Be friendly and professional.
            If the question is outside your domain, politely say an administrator will help them.
            """, userMessage);
    }

    private String extractTextFromResponse(Map response) {
        try {
            List<Map> candidates = (List<Map>) response.get("candidates");
            if (candidates != null && !candidates.isEmpty()) {
                Map content = (Map) candidates.get(0).get("content");
                List<Map> parts = (List<Map>) content.get("parts");
                if (parts != null && !parts.isEmpty()) {
                    return (String) parts.get(0).get("text");
                }
            }
        } catch (Exception e) {
            log.error("Error parsing Gemini response", e);
        }
        return "I'm sorry, I couldn't process that request. An administrator will help you shortly.";
    }
}