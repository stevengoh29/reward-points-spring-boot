package com.example.rewardspoints.user.exceptions;

import com.example.rewardspoints.shared.error.BusinessException;

public class EmailAlreadyExistsException extends BusinessException {
    public EmailAlreadyExistsException(String email) {
        super(UserErrorCode.EMAIL_ALREADY_EXISTS, "Email already registered: " + email);
    }
}
