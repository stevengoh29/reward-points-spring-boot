package com.example.rewardspoints.points.api.dto;

import java.util.UUID;

public record RedeemResponse(
        UUID entryId,
        long pointsRedeemed,
        long balance
) {
}
