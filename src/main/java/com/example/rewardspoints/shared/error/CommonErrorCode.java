package com.example.rewardspoints.shared.error;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum CommonErrorCode implements ErrorCode {
    VALIDATION_FAILED(ErrorCategory.VALIDATION),

    MALFORMED_REQUEST(ErrorCategory.VALIDATION),

    RESOURCE_NOT_FOUND(ErrorCategory.NOT_FOUND),

    DUPLICATE_REQUEST(ErrorCategory.CONFLICT),

    CONCURRENT_MODIFICATION(ErrorCategory.CONFLICT),

    RATE_LIMIT_EXCEEDED(ErrorCategory.RATE_LIMITED),

    INTERNAL_ERROR(ErrorCategory.INTERNAL);

    private final ErrorCategory category;

    @Override
    public String code() {
        return "COMMON_" + name();
    }

    @Override
    public ErrorCategory category() {
        return category;
    }
}
