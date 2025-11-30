package com.vio.customer_support.service;

import com.vio.customer_support.dto.ChatMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private final SimpMessagingTemplate messagingTemplate;
    private final RuleBasedResponseService ruleBasedService;
    private final GeminiService geminiService;

    // Store active chat sessions: userId -> List of messages
    private final Map<String, List<ChatMessage>> chatSessions = new ConcurrentHashMap<>();

    public void processUserMessage(String userId, String content, String username) {
        log.info("Processing message from user {}: {}", userId, content);

        // Create user message
        ChatMessage userMessage = ChatMessage.builder()
                .content(content)
                .sender(userId)
                .senderName(username)
                .type(ChatMessage.MessageType.USER_MESSAGE)
                .timestamp(System.currentTimeMillis())
                .build();

        // Store message
        chatSessions.computeIfAbsent(userId, k -> new ArrayList<>()).add(userMessage);

        // Send to admin
        messagingTemplate.convertAndSend("/topic/admin-chat", userMessage);

        // Try rule-based response first
        String ruleResponse = ruleBasedService.getResponse(content);

        if (ruleResponse != null) {
            sendSystemResponse(userId, ruleResponse, ChatMessage.MessageType.RULE_RESPONSE);
        } else {
            // Fall back to AI
            geminiService.getAIResponse(content).thenAccept(aiResponse -> {
                sendSystemResponse(userId, aiResponse, ChatMessage.MessageType.AI_RESPONSE);
            });
        }
    }

    public void processAdminMessage(String userId, String content) {
        ChatMessage adminMessage = ChatMessage.builder()
                .content(content)
                .sender("ADMIN")
                .senderName("Administrator")
                .type(ChatMessage.MessageType.ADMIN_MESSAGE)
                .timestamp(System.currentTimeMillis())
                .build();

        chatSessions.computeIfAbsent(userId, k -> new ArrayList<>()).add(adminMessage);
        messagingTemplate.convertAndSendToUser(userId, "/queue/messages", adminMessage);
    }

    private void sendSystemResponse(String userId, String content, ChatMessage.MessageType type) {
        ChatMessage response = ChatMessage.builder()
                .content(content)
                .sender("SYSTEM")
                .senderName("Support Bot")
                .type(type)
                .timestamp(System.currentTimeMillis())
                .build();

        chatSessions.computeIfAbsent(userId, k -> new ArrayList<>()).add(response);
        messagingTemplate.convertAndSendToUser(userId, "/queue/messages", response);
    }

    public List<ChatMessage> getChatHistory(String userId) {
        return chatSessions.getOrDefault(userId, new ArrayList<>());
    }
}