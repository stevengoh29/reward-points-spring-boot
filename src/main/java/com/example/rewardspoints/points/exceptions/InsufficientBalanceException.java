package com.example.rewardspoints.points.exceptions;

import com.example.rewardspoints.shared.error.BusinessException;
import lombok.Getter;

@Getter
public class InsufficientBalanceException extends BusinessException {
    private final long balance;
    private final long requested;

    public InsufficientBalanceException(long balance, long requested) {
        super(PointsErrorCode.INSUFFICIENT_BALANCE,
                "Insufficient points: balance " + balance + ", requested " + requested);
        this.balance = balance;
        this.requested = requested;
    }
}
