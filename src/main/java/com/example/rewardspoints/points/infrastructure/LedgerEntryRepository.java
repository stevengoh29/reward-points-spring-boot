package com.example.rewardspoints.points.infrastructure;

import com.example.rewardspoints.points.domain.LedgerEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, UUID> {

    Page<LedgerEntry> findByPointAccountId(UUID pointAccountId, Pageable pageable);

    /** What the account's balance must equal; used to verify the cached balance. */
    @Query("select coalesce(sum(e.amount), 0) from LedgerEntry e where e.pointAccountId = :accountId")
    long sumAmountByAccountId(UUID accountId);
}
