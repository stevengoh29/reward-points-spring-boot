package com.example.rewardspoints.user.api.dto;

import com.example.rewardspoints.user.domain.User;
import com.example.rewardspoints.user.domain.UserStatus;

import java.time.Instant;
import java.util.UUID;

public record QueryUserResponse(
        UUID id,
        String email,
        String displayName,
        UserStatus status,
        Instant createdAt,
        Instant updatedAt
) {
    public static QueryUserResponse from(User user) {
        return new QueryUserResponse(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getStatus(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
