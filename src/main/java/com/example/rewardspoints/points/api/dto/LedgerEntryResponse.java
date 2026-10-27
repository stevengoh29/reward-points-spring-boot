package com.example.rewardspoints.points.api.dto;

import com.example.rewardspoints.points.domain.LedgerEntry;
import com.example.rewardspoints.points.domain.LedgerType;

import java.time.Instant;
import java.util.UUID;

public record LedgerEntryResponse(
        UUID id,
        LedgerType type,
        long amount,
        String referenceId,
        UUID campaignId,
        String description,
        Instant createdAt
) {
    public static LedgerEntryResponse from(LedgerEntry entry) {
        return new LedgerEntryResponse(
                entry.getId(),
                entry.getType(),
                entry.getAmount(),
                entry.getReferenceId(),
                entry.getCampaignId(),
                entry.getDescription(),
                entry.getCreatedAt()
        );
    }
}
