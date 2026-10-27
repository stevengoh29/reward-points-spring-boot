package com.example.rewardspoints.shared.error;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@NoArgsConstructor
public enum ErrorCategory {
    VALIDATION,
    NOT_FOUND,
    CONFLICT,
    BUSINESS_RULE,
    UNAUTHORIZED,
    FORBIDDEN,
    RATE_LIMITED,
    INTERNAL
}
