package com.example.rewardspoints.points.domain;

import com.example.rewardspoints.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.util.Objects;
import java.util.UUID;

/**
 * One points movement. Append-only: corrections are new REVERSAL entries, never edits.
 */
@Getter
@Entity
@Immutable
@Table(name = "ledger_entries")
// Only let the own class to use new constructor.
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LedgerEntry extends BaseEntity {

    @Column(name = "account_id", nullable = false, updatable = false)
    private UUID pointAccountId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20, updatable = false)
    private LedgerType type;

    /** Signed: positive adds points, negative removes them. Never 0. */
    @Column(name = "amount", nullable = false, updatable = false)
    private long amount;

    @Column(name = "reference_id", nullable = false, length = 100, updatable = false)
    private String referenceId;

    @Column(name = "campaign_id", updatable = false)
    private UUID campaignId;

    @Column(name = "description", length = 255, updatable = false)
    private String description;

    public static LedgerEntry create(UUID pointAccountId, LedgerType type, long amount, String referenceId, UUID campaignId, String description) {
        Objects.requireNonNull(pointAccountId, "pointAccountId");
        Objects.requireNonNull(type, "type");
        if (referenceId == null || referenceId.isBlank()) {
            throw new IllegalArgumentException("referenceId is required");
        }
        if (referenceId.length() > 100) {
            throw new IllegalArgumentException("referenceId must be at most 100 characters");
        }
        if (description != null && description.length() > 255) {
            throw new IllegalArgumentException("description must be at most 255 characters");
        }
        if (amount == 0) {
            throw new IllegalArgumentException("amount must not be 0");
        }
        if (type.isCredit() && amount < 0) {
            throw new IllegalArgumentException(type + " amount must be positive");
        }
        if (type.isDebit() && amount > 0) {
            throw new IllegalArgumentException(type + " amount must be negative");
        }
        if (type == LedgerType.ADJUSTMENT && (description == null || description.isBlank())) {
            throw new IllegalArgumentException("ADJUSTMENT requires a description");
        }

        LedgerEntry newLedger = new LedgerEntry();
        newLedger.pointAccountId = pointAccountId;
        newLedger.type = type;
        newLedger.amount = amount;
        newLedger.referenceId = referenceId;
        newLedger.campaignId = campaignId;
        newLedger.description = description;
        return newLedger;
    }
}
