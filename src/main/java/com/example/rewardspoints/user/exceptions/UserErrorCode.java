package com.example.rewardspoints.user.exceptions;

import com.example.rewardspoints.shared.error.ErrorCategory;
import com.example.rewardspoints.shared.error.ErrorCode;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum UserErrorCode implements ErrorCode {
    EMAIL_ALREADY_EXISTS(ErrorCategory.CONFLICT);

    private final ErrorCategory category;

    @Override
    public String code() {
        return "USER_" + name();
    }

    @Override
    public ErrorCategory category() {
        return category;
    }
}
