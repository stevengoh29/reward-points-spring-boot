/**
 * Shared error model and HTTP error mapping.
 *
 * <p>Kept free of any dependency on feature modules (user, points, auth, ...) so this package can later be
 * extracted into a shared library unchanged. Feature modules plug in by defining their own
 * {@link com.example.rewardspoints.shared.error.ErrorCode} enum and throwing a
 * {@link com.example.rewardspoints.shared.error.BusinessException} subclass; nothing here needs editing for that.
 */
package com.example.rewardspoints.shared.error;
