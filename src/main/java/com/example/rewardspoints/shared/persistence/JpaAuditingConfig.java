package com.example.rewardspoints.shared.persistence;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Populates @CreatedDate / @LastModifiedDate on {@link BaseEntity} and {@link AuditableEntity}.
 * No AuditorAware yet, so created_by / updated_by stay null until authentication exists.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
