package com.example.rewardspoints.points.domain;

import com.example.rewardspoints.points.exceptions.InsufficientBalanceException;
import com.example.rewardspoints.shared.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;
import java.util.UUID;

@Getter
@Entity
@Table(name = "point_accounts")
// Only let the own class to use new constructor.
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PointAccount extends AuditableEntity {

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    /** Cached sum of the account's ledger entries. */
    @Column(name = "balance", nullable = false)
    private long balance;

    public static PointAccount openAccount(UUID userId) {
        PointAccount pointAccount = new PointAccount();
        pointAccount.userId = Objects.requireNonNull(userId, "userId");
        pointAccount.balance = 0L;
        return pointAccount;
    }

    public void add(long amount) {
        if (amount <= 0) throw new IllegalArgumentException("Amount must be positive");
        this.balance = Math.addExact(this.balance, amount);
    }

    public void deduct(long amount) {
        if (amount <= 0) throw new IllegalArgumentException("Amount must be positive");
        if (this.balance < amount) throw new InsufficientBalanceException(this.balance, amount);

        this.balance -= amount;
    }
}
