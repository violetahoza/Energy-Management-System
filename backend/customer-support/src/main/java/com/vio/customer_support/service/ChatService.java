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

    private final Map<String, List<ChatMessage>> chatSessions = new ConcurrentHashMap<>();

    public void processUserMessage(String userId, String content, String username) {
        log.info("Processing message from user {}: {}", userId, content);

        ChatMessage userMessage = ChatMessage.builder()
                .content(content)
                .sender(userId)
                .senderName(username)
                .type(ChatMessage.MessageType.USER_MESSAGE)
                .timestamp(System.currentTimeMillis())
                .build();

        // Store message
        chatSessions.computeIfAbsent(userId, k -> new ArrayList<>()).add(userMessage);

        // Send to admin panel - BROADCAST to all admins
        messagingTemplate.convertAndSend("/topic/admin-chat", userMessage);
        log.info("✓ Sent user message to admin panel");

        // Try rule-based response first
        String ruleResponse = ruleBasedService.getResponse(content);

        if (ruleResponse != null) {
            log.info("✓ Found rule-based response for user {}", userId);
            sendSystemResponse(userId, ruleResponse, ChatMessage.MessageType.RULE_RESPONSE);
        } else {
            log.info("No rule match, trying AI response for user {}", userId);
            // Fall back to AI
            geminiService.getAIResponse(content).thenAccept(aiResponse -> {
                log.info("✓ Got AI response for user {}", userId);
                sendSystemResponse(userId, aiResponse, ChatMessage.MessageType.AI_RESPONSE);
            }).exceptionally(ex -> {
                log.error("Error getting AI response: ", ex);
                sendSystemResponse(userId,
                        "I apologize, but I'm having trouble processing your request. Please try again or wait for an administrator.",
                        ChatMessage.MessageType.SYSTEM_MESSAGE);
                return null;
            });
        }
    }

    public void processAdminMessage(String userId, String content) {
        log.info("Processing admin message to user {}: {}", userId, content);

        ChatMessage adminMessage = ChatMessage.builder()
                .content(content)
                .sender("ADMIN")
                .senderName("Administrator")
                .type(ChatMessage.MessageType.ADMIN_MESSAGE)
                .timestamp(System.currentTimeMillis())
                .build();

        chatSessions.computeIfAbsent(userId, k -> new ArrayList<>()).add(adminMessage);

        // Send to specific user
        messagingTemplate.convertAndSendToUser(userId, "/queue/messages", adminMessage);
        log.info("✓ Sent admin message to user {}", userId);

        ChatMessage adminBroadcast = ChatMessage.builder()
                .content(content)
                .sender(userId)
                .senderName("Administrator")
                .type(ChatMessage.MessageType.ADMIN_MESSAGE)
                .timestamp(System.currentTimeMillis())
                .recipientUserId(userId)
                .build();
        messagingTemplate.convertAndSend("/topic/admin-chat", adminBroadcast);
    }

    private void sendSystemResponse(String userId, String content, ChatMessage.MessageType type) {
        ChatMessage response = ChatMessage.builder()
                .content(content)
                .sender("SYSTEM")
                .senderName("Support Bot")
                .type(type)
                .timestamp(System.currentTimeMillis())
                .recipientUserId(userId)
                .build();

        chatSessions.computeIfAbsent(userId, k -> new ArrayList<>()).add(response);
        messagingTemplate.convertAndSendToUser(userId, "/queue/messages", response);
        log.info("✓ Sent {} to user {}", type, userId);

        ChatMessage adminBroadcast = ChatMessage.builder()
                .content(content)
                .sender(userId)
                .senderName("Support Bot")
                .type(type)
                .timestamp(System.currentTimeMillis())
                .recipientUserId(userId)
                .build();
        messagingTemplate.convertAndSend("/topic/admin-chat", adminBroadcast);
        log.info("✓ Broadcasted {} to admin panel for user {}", type, userId);
    }

    public List<ChatMessage> getChatHistory(String userId) {
        return chatSessions.getOrDefault(userId, new ArrayList<>());
    }
}