package com.vio.device_service.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSyncEvent {
    private Long userId;
    private String username;
    private String role;
    private String eventType; // CREATED, UPDATED, DELETED
}