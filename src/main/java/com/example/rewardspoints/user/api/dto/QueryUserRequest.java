package com.example.rewardspoints.user.api.dto;

import com.example.rewardspoints.user.domain.UserStatus;

public record QueryUserRequest(
        String email,
        String displayName,
        UserStatus status
) {

}
