package com.vio.userservice;

import jakarta.validation.constraints.NotEmpty;

public record UserDTORequest(
        @NotEmpty(message = "Name must not be empty")
        String name
) {
}
