package com.example.rewardspoints.points.domain;

public enum LedgerType {
    EARN, REDEEM, EXPIRE, REVERSAL, ADJUSTMENT;

    /** Always adds points. REVERSAL and ADJUSTMENT can go either way. */
    public boolean isCredit() {
        return this == EARN;
    }

    /** Always removes points. */
    public boolean isDebit() {
        return this == REDEEM || this == EXPIRE;
    }
}
