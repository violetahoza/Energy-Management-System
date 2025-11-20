package com.vio.userservice.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserSyncEvent implements Serializable {
    private Long userId;
    private String eventType; // "CREATED", "UPDATED", "DELETED"
    private String username;
    private String password;
    private String role;

    public static UserSyncEvent created(Long userId, String username, String password, String role) {
        return new UserSyncEvent(userId, "CREATED", username, password, role);
    }

    public static UserSyncEvent updated(Long userId, String username, String password, String role) {
        return new UserSyncEvent(userId, "UPDATED", username, password, role);
    }

    public static UserSyncEvent deleted(Long userId) {
        return new UserSyncEvent(userId, "DELETED", null, null, null);
    }
}