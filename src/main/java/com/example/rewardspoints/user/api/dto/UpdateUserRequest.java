package com.example.rewardspoints.user.api.dto;

import com.example.rewardspoints.user.domain.UserStatus;
import jakarta.validation.constraints.Size;

/**
 * Partial update: a null field means "leave unchanged".
 */
public record UpdateUserRequest(
        @Size(max = 100) String displayName,
        UserStatus status
) {
}
