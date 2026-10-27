package com.example.rewardspoints.points.exceptions;

import com.example.rewardspoints.shared.error.BusinessException;

public class InvalidAmountException extends BusinessException {
    public InvalidAmountException(String message) {
        super(PointsErrorCode.INVALID_AMOUNT, message);
    }
}
