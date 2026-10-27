package com.example.rewardspoints.points.application;

import com.example.rewardspoints.points.api.dto.EarnRequest;
import com.example.rewardspoints.points.api.dto.RedeemRequest;
import com.example.rewardspoints.points.domain.LedgerType;
import com.example.rewardspoints.points.domain.PointAccount;
import com.example.rewardspoints.points.exceptions.InsufficientBalanceException;
import com.example.rewardspoints.points.infrastructure.LedgerEntryRepository;
import com.example.rewardspoints.points.infrastructure.PointAccountRepository;
import com.example.rewardspoints.user.domain.User;
import com.example.rewardspoints.user.infrastructure.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The M1 acceptance test: concurrent writers can never spend the same points or corrupt the balance.
 * Calls the service directly (no MockMvc) so threads really race on the database.
 */
@SpringBootTest
@Testcontainers
class PointsConcurrencyTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    PointsService pointsService;

    @Autowired
    UserRepository userRepository;

    @Autowired
    PointAccountRepository accountRepository;

    @Autowired
    LedgerEntryRepository ledgerRepository;

    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    void cleanDatabase() {
        jdbc.execute("TRUNCATE ledger_entries, point_accounts, users");
    }

    @Test
    void fiftyParallelRedeems_againstBalanceForOne_exactlyOneSucceeds() throws Exception {
        UUID user = userRepository.save(User.create("alice@example.com", "Alice")).getId();
        pointsService.earn(new EarnRequest(user, 10_000, "seed")); // 100 points

        List<Callable<Object>> tasks = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            String ref = "redeem-" + i;
            tasks.add(() -> pointsService.redeem(new RedeemRequest(user, 100, ref)));
        }
        List<Object> outcomes = runInParallel(tasks);

        long successes = outcomes.stream().filter(o -> !(o instanceof Throwable)).count();
        long insufficient = outcomes.stream().filter(o -> o instanceof InsufficientBalanceException).count();
        assertThat(successes).as("successful redeems").isEqualTo(1);
        assertThat(insufficient).as("rejected for insufficient balance").isEqualTo(49);

        PointAccount account = accountRepository.findByUserId(user).orElseThrow();
        assertThat(account.getBalance()).isZero();
        assertThat(ledgerRepository.sumAmountByAccountId(account.getId())).isZero();
        assertThat(ledgerRepository.findAll().stream().filter(e -> e.getType() == LedgerType.REDEEM)).hasSize(1);
    }

    @Test
    void parallelEarns_onBrandNewUser_createOneAccount_andLoseNoPoints() throws Exception {
        UUID user = userRepository.save(User.create("bob@example.com", "Bob")).getId();

        List<Callable<Object>> tasks = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            String ref = "earn-" + i;
            tasks.add(() -> pointsService.earn(new EarnRequest(user, 1_000, ref))); // 10 points each
        }
        List<Object> outcomes = runInParallel(tasks);

        assertThat(outcomes).as("no earn should fail").noneMatch(o -> o instanceof Throwable);
        assertThat(accountRepository.count()).isEqualTo(1);
        PointAccount account = accountRepository.findByUserId(user).orElseThrow();
        assertThat(account.getBalance()).isEqualTo(100);
        assertThat(ledgerRepository.count()).isEqualTo(10);
        assertThat(ledgerRepository.sumAmountByAccountId(account.getId())).isEqualTo(100);
    }

    @Test
    void mixedParallelEarnsAndRedeems_keepBalanceEqualToLedgerSum_andNeverNegative() throws Exception {
        UUID user = userRepository.save(User.create("carol@example.com", "Carol")).getId();
        pointsService.earn(new EarnRequest(user, 5_000, "seed")); // 50 points

        List<Callable<Object>> tasks = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            String ref = "mixed-" + i;
            tasks.add(() -> pointsService.earn(new EarnRequest(user, 1_000, "earn-" + ref)));
            tasks.add(() -> pointsService.redeem(new RedeemRequest(user, 10, "redeem-" + ref)));
        }
        List<Object> outcomes = runInParallel(tasks);

        // 50 start + 40 earned covers all 40 redeemed, so nothing may fail
        assertThat(outcomes).noneMatch(o -> o instanceof Throwable);
        PointAccount account = accountRepository.findByUserId(user).orElseThrow();
        assertThat(account.getBalance()).isEqualTo(50);
        assertThat(ledgerRepository.sumAmountByAccountId(account.getId())).isEqualTo(account.getBalance());
    }

    /** Releases all tasks at the same instant, and returns each result or the exception it threw. */
    private List<Object> runInParallel(List<Callable<Object>> tasks) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(tasks.size());
        try {
            CountDownLatch ready = new CountDownLatch(tasks.size());
            CountDownLatch go = new CountDownLatch(1);
            List<Future<Object>> futures = new ArrayList<>();
            for (Callable<Object> task : tasks) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    go.await();
                    try {
                        return task.call();
                    } catch (Exception e) {
                        return e;
                    }
                }));
            }
            ready.await();
            go.countDown();

            List<Object> results = new ArrayList<>();
            for (Future<Object> future : futures) {
                results.add(future.get());
            }
            return results;
        } finally {
            executor.shutdownNow();
        }
    }
}
