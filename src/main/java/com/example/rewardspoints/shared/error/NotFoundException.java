package com.example.rewardspoints.shared.error;

public class NotFoundException extends BusinessException {
    public NotFoundException(String message) {
        super(CommonErrorCode.RESOURCE_NOT_FOUND, message);
    }
}
