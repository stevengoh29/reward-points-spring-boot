package com.example.rewardspoints.points.domain;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LedgerEntryTest {

    private final UUID accountId = UUID.randomUUID();

    @Test
    void create_earn_keepsAllFields() {
        UUID campaignId = UUID.randomUUID();

        LedgerEntry entry = LedgerEntry.create(accountId, LedgerType.EARN, 25, "order-1", campaignId, "note");

        assertThat(entry.getPointAccountId()).isEqualTo(accountId);
        assertThat(entry.getType()).isEqualTo(LedgerType.EARN);
        assertThat(entry.getAmount()).isEqualTo(25);
        assertThat(entry.getReferenceId()).isEqualTo("order-1");
        assertThat(entry.getCampaignId()).isEqualTo(campaignId);
        assertThat(entry.getDescription()).isEqualTo("note");
    }

    @Test
    void create_zeroAmount_isRejected() {
        assertThatThrownBy(() -> LedgerEntry.create(accountId, LedgerType.EARN, 0, "r", null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void create_signMustMatchType() {
        assertThatThrownBy(() -> LedgerEntry.create(accountId, LedgerType.EARN, -1, "r", null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> LedgerEntry.create(accountId, LedgerType.REDEEM, 1, "r", null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> LedgerEntry.create(accountId, LedgerType.EXPIRE, 1, "r", null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void create_redeemIsNegative_reversalAndAdjustmentMayBeEitherSign() {
        assertThat(LedgerEntry.create(accountId, LedgerType.REDEEM, -5, "r", null, null).getAmount()).isEqualTo(-5);
        assertThat(LedgerEntry.create(accountId, LedgerType.REVERSAL, -5, "r", null, null).getAmount()).isEqualTo(-5);
        assertThat(LedgerEntry.create(accountId, LedgerType.REVERSAL, 5, "r", null, null).getAmount()).isEqualTo(5);
        assertThat(LedgerEntry.create(accountId, LedgerType.ADJUSTMENT, 5, "r", null, "why").getAmount()).isEqualTo(5);
        assertThat(LedgerEntry.create(accountId, LedgerType.ADJUSTMENT, -5, "r", null, "why").getAmount()).isEqualTo(-5);
    }

    @Test
    void create_adjustmentWithoutDescription_isRejected() {
        assertThatThrownBy(() -> LedgerEntry.create(accountId, LedgerType.ADJUSTMENT, 5, "r", null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> LedgerEntry.create(accountId, LedgerType.ADJUSTMENT, 5, "r", null, "  "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void create_referenceId_isRequiredAndBounded() {
        assertThatThrownBy(() -> LedgerEntry.create(accountId, LedgerType.EARN, 5, null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> LedgerEntry.create(accountId, LedgerType.EARN, 5, " ", null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> LedgerEntry.create(accountId, LedgerType.EARN, 5, "x".repeat(101), null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void create_requiresAccountAndType() {
        assertThatThrownBy(() -> LedgerEntry.create(null, LedgerType.EARN, 5, "r", null, null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> LedgerEntry.create(accountId, null, 5, "r", null, null))
                .isInstanceOf(NullPointerException.class);
    }
}
