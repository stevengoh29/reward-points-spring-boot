package com.example.rewardspoints.shared.error;

public interface ErrorCode {
    String code();              // stable string the frontend relies on, e.g. "POINTS_INSUFFICIENT_BALANCE"

    ErrorCategory category();
}