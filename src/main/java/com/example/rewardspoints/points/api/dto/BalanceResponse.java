package com.example.rewardspoints.points.api.dto;

import java.util.UUID;

public record BalanceResponse(
        UUID userId,
        long balance
) {
}
