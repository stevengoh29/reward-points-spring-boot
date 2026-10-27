package com.example.rewardspoints.points.api.dto;

import java.util.UUID;

public record EarnResponse(
        UUID entryId,
        long pointsEarned,
        long balance
) {
}
