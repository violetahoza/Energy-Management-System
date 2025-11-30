package com.vio.customer_support.controller;

import com.vio.customer_support.dto.ChatMessage;
import com.vio.customer_support.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final ChatService chatService;

    @MessageMapping("/chat.sendMessage")
    public void sendMessage(@Payload Map<String, String> payload, Principal principal) {
        String userId = principal.getName();
        String content = payload.get("content");
        String username = payload.getOrDefault("username", "User");

        log.info("Received message from user {}: {}", userId, content);
        chatService.processUserMessage(userId, content, username);
    }

    @MessageMapping("/chat.adminResponse")
    public void sendAdminResponse(@Payload Map<String, String> payload) {
        String userId = payload.get("userId");
        String content = payload.get("content");

        log.info("Admin sending message to user {}", userId);
        chatService.processAdminMessage(userId, content);
    }

    @GetMapping("/api/chat/history")
    @ResponseBody
    public List<ChatMessage> getChatHistory(@AuthenticationPrincipal Principal principal) {
        return chatService.getChatHistory(principal.getName());
    }
}