package com.vio.authorization_service.dto;

public record UpdateCredentialsRequest(
        String username,
        String password
) {
}
