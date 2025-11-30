package com.vio.customer_support.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class GeminiService {

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.url}")
    private String apiUrl;

    private final WebClient webClient;

    public GeminiService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    public CompletableFuture<String> getAIResponse(String userMessage) {
        String systemPrompt = "You are a helpful customer support assistant for an Energy Management System. " +
                "The system allows users to monitor energy consumption of smart devices. " +
                "Provide helpful, concise responses to user queries. Keep responses under 150 words.";

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of(
                                "parts", List.of(
                                        Map.of("text", systemPrompt + "\n\nUser: " + userMessage)
                                )
                        )
                )
        );

        return webClient.post()
                .uri(apiUrl + "?key=" + apiKey)
                .header("Content-Type", "application/json")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .map(this::extractTextFromResponse)
                .onErrorResume(error -> {
                    log.error("Gemini API error: {}", error.getMessage());
                    return Mono.just("I apologize, but I'm having trouble processing your request right now. " +
                            "An administrator will assist you shortly.");
                })
                .toFuture();
    }

    @SuppressWarnings("unchecked")
    private String extractTextFromResponse(Map<String, Object> response) {
        try {
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
            if (candidates != null && !candidates.isEmpty()) {
                Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
                List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
                if (parts != null && !parts.isEmpty()) {
                    return (String) parts.get(0).get("text");
                }
            }
        } catch (Exception e) {
            log.error("Error parsing Gemini response", e);
        }
        return "I apologize, but I couldn't generate a proper response. Please try again or contact an administrator.";
    }
}