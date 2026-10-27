package com.example.rewardspoints.user.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * New users always start ACTIVE; there is deliberately no status field.
 */
public record CreateUserRequest(
        @NotBlank @Email @Size(max = 320) String email,
        @Size(max = 100) String displayName
) {
}
