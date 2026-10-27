package com.example.rewardspoints.points.infrastructure;

import com.example.rewardspoints.points.domain.PointAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PointAccountRepository extends JpaRepository<PointAccount, UUID> {

    Optional<PointAccount> findByUserId(UUID userId);
}
