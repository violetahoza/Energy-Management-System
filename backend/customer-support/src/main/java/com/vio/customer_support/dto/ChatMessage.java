package com.vio.customer_support.dto;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ChatMessage {
    private String content;
    private String sender; // userId or "ADMIN" or "SYSTEM"
    private String senderName;
    private MessageType type;
    private Long timestamp;

    public enum MessageType {
        USER_MESSAGE,
        ADMIN_MESSAGE,
        SYSTEM_MESSAGE,
        RULE_RESPONSE,
        AI_RESPONSE
    }
}