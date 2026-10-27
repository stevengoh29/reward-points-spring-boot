package com.example.rewardspoints.shared.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;

import java.time.Clock;
import java.time.Instant;

/**
 * Base for business entities that can be soft-deleted (User, Campaign).
 * Each concrete entity must also declare, with its own table name:
 *
 *   @SQLDelete(sql = "UPDATE users SET deleted_at = now() WHERE id = ? AND version = ?")
 *   @SQLRestriction("deleted_at IS NULL")
 */
@Getter
@MappedSuperclass
public abstract class SoftDeletableEntity extends AuditableEntity {

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public void markDeleted(Clock clock) {
        if (deletedAt == null) {
            deletedAt = Instant.now(clock);
        }
    }

    public void restore() {
        deletedAt = null;
    }
}