package com.example.rewardspoints.user.api.dto;

import com.example.rewardspoints.user.domain.User;
import com.example.rewardspoints.user.domain.UserStatus;

import java.time.Instant;
import java.util.UUID;

public record UpdateUserResponse(
        UUID id,
        String email,
        String displayName,
        UserStatus status,
        Instant createdAt,
        Instant updatedAt
) {
    public static UpdateUserResponse from(User user) {
        return new UpdateUserResponse(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getStatus(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
