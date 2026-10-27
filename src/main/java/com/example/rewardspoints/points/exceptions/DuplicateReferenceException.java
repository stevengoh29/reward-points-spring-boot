package com.example.rewardspoints.points.exceptions;

import com.example.rewardspoints.points.domain.LedgerType;
import com.example.rewardspoints.shared.error.BusinessException;

public class DuplicateReferenceException extends BusinessException {
    public DuplicateReferenceException(LedgerType type, String referenceId) {
        super(PointsErrorCode.DUPLICATE_REFERENCE,
                type + " already recorded for reference '" + referenceId + "'");
    }
}
