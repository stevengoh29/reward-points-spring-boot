package com.example.rewardspoints.points.domain;

import com.example.rewardspoints.points.exceptions.InsufficientBalanceException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PointAccountTest {

    private final UUID userId = UUID.randomUUID();

    @Test
    void openAccount_startsAtZero() {
        PointAccount account = PointAccount.openAccount(userId);

        assertThat(account.getUserId()).isEqualTo(userId);
        assertThat(account.getBalance()).isZero();
    }

    @Test
    void openAccount_requiresUserId() {
        assertThatThrownBy(() -> PointAccount.openAccount(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void add_increasesBalance() {
        PointAccount account = PointAccount.openAccount(userId);

        account.add(30);
        account.add(12);

        assertThat(account.getBalance()).isEqualTo(42);
    }

    @Test
    void deduct_decreasesBalance_andAllowsSpendingEverything() {
        PointAccount account = PointAccount.openAccount(userId);
        account.add(100);

        account.deduct(40);
        account.deduct(60);

        assertThat(account.getBalance()).isZero();
    }

    @Test
    void deduct_moreThanBalance_throws_andLeavesBalanceUntouched() {
        PointAccount account = PointAccount.openAccount(userId);
        account.add(10);

        assertThatThrownBy(() -> account.deduct(11))
                .isInstanceOf(InsufficientBalanceException.class)
                .hasMessageContaining("balance 10")
                .hasMessageContaining("requested 11");
        assertThat(account.getBalance()).isEqualTo(10);
    }

    @Test
    void nonPositiveAmounts_areRejected() {
        PointAccount account = PointAccount.openAccount(userId);
        account.add(10);

        assertThatThrownBy(() -> account.add(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> account.add(-5)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> account.deduct(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> account.deduct(-5)).isInstanceOf(IllegalArgumentException.class);
        assertThat(account.getBalance()).isEqualTo(10);
    }

    @Test
    void add_overflow_isRejected() {
        PointAccount account = PointAccount.openAccount(userId);
        account.add(Long.MAX_VALUE);

        assertThatThrownBy(() -> account.add(1)).isInstanceOf(ArithmeticException.class);
        assertThat(account.getBalance()).isEqualTo(Long.MAX_VALUE);
    }
}
