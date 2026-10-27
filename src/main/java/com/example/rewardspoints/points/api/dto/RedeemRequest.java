package com.example.rewardspoints.points.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * userId is explicit until authentication (M2) lets it come from the logged-in user.
 */
public record RedeemRequest(
        @NotNull UUID userId,
        @Positive @Max(100_000_000L) long points,
        @NotBlank @Size(max = 100) String referenceId
) {
}
