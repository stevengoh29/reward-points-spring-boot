package com.example.rewardspoints.shared.error;

/**
 * One field-level problem, serialized under the {@code errors} property of a validation response.
 */
public record ValidationError(String field, String message) {
}
