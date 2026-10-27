package com.example.rewardspoints.points.exceptions;

import com.example.rewardspoints.shared.error.ErrorCategory;
import com.example.rewardspoints.shared.error.ErrorCode;
import lombok.RequiredArgsConstructor;

// points/domain/PointsErrorCode.java
@RequiredArgsConstructor
public enum PointsErrorCode implements ErrorCode {
    INSUFFICIENT_BALANCE(ErrorCategory.BUSINESS_RULE),
    ACCOUNT_NOT_FOUND(ErrorCategory.NOT_FOUND),
    DUPLICATE_REFERENCE(ErrorCategory.CONFLICT),
    INVALID_AMOUNT(ErrorCategory.VALIDATION);

    private final ErrorCategory category;

    @Override
    public String code() {
        return "POINTS_" + name();
    }

    @Override
    public ErrorCategory category() {
        return category;
    }
}