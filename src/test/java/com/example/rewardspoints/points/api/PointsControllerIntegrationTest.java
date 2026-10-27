package com.example.rewardspoints.points.api;

import com.example.rewardspoints.points.domain.PointAccount;
import com.example.rewardspoints.points.infrastructure.LedgerEntryRepository;
import com.example.rewardspoints.points.infrastructure.PointAccountRepository;
import com.example.rewardspoints.user.domain.User;
import com.example.rewardspoints.user.infrastructure.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Earn / redeem / balance / history through the real stack on PostgreSQL (Testcontainers).
 */
@SpringBootTest
@Testcontainers
@AutoConfigureMockMvc
class PointsControllerIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    MockMvc mockMvc;

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
    void earn_awardsOnePointPerHundredUnits_roundingDown() throws Exception {
        UUID user = newUser("alice@example.com");

        earn(user, 2599, "order-1")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pointsEarned").value(25))
                .andExpect(jsonPath("$.balance").value(25))
                .andExpect(jsonPath("$.entryId").isNotEmpty());

        earn(user, 100, "order-2")
                .andExpect(jsonPath("$.pointsEarned").value(1))
                .andExpect(jsonPath("$.balance").value(26));
    }

    @Test
    void earn_belowOneHundredUnits_returns400AndWritesNothing() throws Exception {
        UUID user = newUser("alice@example.com");

        earn(user, 99, "order-1")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("POINTS_INVALID_AMOUNT"));

        assertThat(accountRepository.count()).isZero();
        assertThat(ledgerRepository.count()).isZero();
    }

    @Test
    void earn_invalidInput_returns400() throws Exception {
        UUID user = newUser("alice@example.com");

        postJson("/points/earn", "{}").andExpect(status().isBadRequest());
        postJson("/points/earn", body(user, "purchaseAmount", 0, "order-1")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[0].field").value("purchaseAmount"));
        postJson("/points/earn", body(user, "purchaseAmount", -500, "order-1")).andExpect(status().isBadRequest());
        postJson("/points/earn", body(user, "purchaseAmount", 1_000_000_001L, "order-1")).andExpect(status().isBadRequest());
        postJson("/points/earn", body(user, "purchaseAmount", 500, "")).andExpect(status().isBadRequest());
        postJson("/points/earn", body(user, "purchaseAmount", 500, "x".repeat(101))).andExpect(status().isBadRequest());
        postJson("/points/earn", "{\"userId\": \"" + user + "\", \"purchaseAmount\": 500}").andExpect(status().isBadRequest());
        postJson("/points/earn", "{\"userId\": \"" + user + "\", \"purchaseAmount\": 1.5, \"referenceId\": \"r\"}")
                .andExpect(status().isBadRequest());

        assertThat(ledgerRepository.count()).isZero();
    }

    @Test
    void earn_unknownUser_returns404() throws Exception {
        earn(UUID.randomUUID(), 500, "order-1")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COMMON_RESOURCE_NOT_FOUND"));
        assertThat(accountRepository.count()).isZero();
    }

    @Test
    void earn_forSameUserTwice_reusesTheSingleAccount() throws Exception {
        UUID user = newUser("alice@example.com");

        earn(user, 1000, "order-1");
        earn(user, 1000, "order-2");

        assertThat(accountRepository.count()).isEqualTo(1);
        assertThat(ledgerRepository.count()).isEqualTo(2);
    }

    @Test
    void redeem_deductsPoints_andRecordsNegativeEntry() throws Exception {
        UUID user = newUser("alice@example.com");
        earn(user, 10_000, "order-1");

        redeem(user, 30, "reward-1")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pointsRedeemed").value(30))
                .andExpect(jsonPath("$.balance").value(70));

        balance(user).andExpect(jsonPath("$.balance").value(70));
        history(user, "")
                .andExpect(jsonPath("$.content[0].type").value("REDEEM"))
                .andExpect(jsonPath("$.content[0].amount").value(-30));
    }

    @Test
    void redeem_exactBalance_leavesZero() throws Exception {
        UUID user = newUser("alice@example.com");
        earn(user, 5_000, "order-1");

        redeem(user, 50, "reward-1").andExpect(status().isOk()).andExpect(jsonPath("$.balance").value(0));
    }

    @Test
    void redeem_moreThanBalance_returns422_andChangesNothing() throws Exception {
        UUID user = newUser("alice@example.com");
        earn(user, 1_000, "order-1");

        redeem(user, 11, "reward-1")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("POINTS_INSUFFICIENT_BALANCE"));

        balance(user).andExpect(jsonPath("$.balance").value(10));
        assertThat(ledgerRepository.count()).isEqualTo(1);
    }

    @Test
    void redeem_withoutAnAccount_returns422() throws Exception {
        UUID user = newUser("alice@example.com");

        redeem(user, 1, "reward-1")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("POINTS_INSUFFICIENT_BALANCE"));
        assertThat(accountRepository.count()).isZero();
    }

    @Test
    void redeem_invalidInput_returns400() throws Exception {
        UUID user = newUser("alice@example.com");
        earn(user, 10_000, "order-1");

        postJson("/points/redeem", body(user, "points", 0, "r")).andExpect(status().isBadRequest());
        postJson("/points/redeem", body(user, "points", -5, "r")).andExpect(status().isBadRequest());
        postJson("/points/redeem", body(user, "points", 100_000_001L, "r")).andExpect(status().isBadRequest());
        postJson("/points/redeem", body(user, "points", 5, " ")).andExpect(status().isBadRequest());

        balance(user).andExpect(jsonPath("$.balance").value(100));
    }

    @Test
    void redeem_unknownUser_returns404() throws Exception {
        redeem(UUID.randomUUID(), 1, "reward-1").andExpect(status().isNotFound());
    }

    @Test
    void balance_withoutAccount_isZero_andUnknownUserIs404() throws Exception {
        UUID user = newUser("alice@example.com");

        balance(user).andExpect(status().isOk()).andExpect(jsonPath("$.balance").value(0));
        mockMvc.perform(get("/points/balance").param("userId", UUID.randomUUID().toString()))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/points/balance")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/points/balance").param("userId", "nope")).andExpect(status().isBadRequest());
    }

    @Test
    void history_isNewestFirst_andPaginated() throws Exception {
        UUID user = newUser("alice@example.com");
        earn(user, 100, "first");
        earn(user, 200, "second");
        earn(user, 300, "third");
        redeem(user, 1, "fourth");

        history(user, "&size=3")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(3))
                .andExpect(jsonPath("$.content[0].referenceId").value("fourth"))
                .andExpect(jsonPath("$.content[1].referenceId").value("third"))
                .andExpect(jsonPath("$.content[2].referenceId").value("second"))
                .andExpect(jsonPath("$.totalElements").value(4))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.last").value(false));

        history(user, "&size=3&page=1")
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].referenceId").value("first"))
                .andExpect(jsonPath("$.last").value(true));
    }

    @Test
    void history_withoutAccount_isEmpty_andUnknownUserIs404() throws Exception {
        UUID user = newUser("alice@example.com");

        history(user, "").andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.content.length()").value(0));
        mockMvc.perform(get("/points/history").param("userId", UUID.randomUUID().toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    void users_neverSeeEachOthersPointsOrHistory() throws Exception {
        UUID alice = newUser("alice@example.com");
        UUID bob = newUser("bob@example.com");
        earn(alice, 10_000, "alice-order");
        earn(bob, 500, "bob-order");
        redeem(alice, 40, "alice-reward");

        balance(alice).andExpect(jsonPath("$.balance").value(60));
        balance(bob).andExpect(jsonPath("$.balance").value(5));
        history(bob, "")
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].referenceId").value("bob-order"));
    }

    @Test
    void balance_alwaysEqualsSumOfLedgerEntries() throws Exception {
        UUID user = newUser("alice@example.com");
        earn(user, 12_345, "o1");
        redeem(user, 20, "r1");
        earn(user, 999, "o2");
        redeem(user, 5000, "too-much");
        redeem(user, 33, "r2");

        PointAccount account = accountRepository.findByUserId(user).orElseThrow();
        assertThat(account.getBalance()).isEqualTo(ledgerRepository.sumAmountByAccountId(account.getId()));
        assertThat(account.getBalance()).isEqualTo(123 - 20 + 9 - 33);
    }

    @Test
    void database_rejectsNegativeBalance_andZeroAmountLedgerEntries() {
        UUID user = newUserDirect("alice@example.com");
        PointAccount account = accountRepository.saveAndFlush(PointAccount.openAccount(user));

        assertThatThrownBy(() -> jdbc.update("UPDATE point_accounts SET balance = -1 WHERE id = ?", account.getId()))
                .hasMessageContaining("point_accounts_balance_check");
        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO ledger_entries (id, account_id, type, amount, reference_id, created_at) "
                        + "VALUES (gen_random_uuid(), ?, 'EARN', 0, 'r', now())", account.getId()))
                .hasMessageContaining("amount");
    }

    @Test
    void sqlInjectionStrings_areTreatedAsPlainData() throws Exception {
        UUID user = newUser("alice@example.com");
        String evil = "'; DROP TABLE ledger_entries; --";

        earn(user, 500, evil).andExpect(status().isOk());

        history(user, "")
                .andExpect(jsonPath("$.content[0].referenceId").value(evil));
        assertThat(ledgerRepository.count()).isEqualTo(1);
    }

    private UUID newUser(String email) {
        return newUserDirect(email);
    }

    private UUID newUserDirect(String email) {
        return userRepository.save(User.create(email, "Test")).getId();
    }

    private ResultActions earn(UUID user, long purchaseAmount, String referenceId) throws Exception {
        return postJson("/points/earn", body(user, "purchaseAmount", purchaseAmount, referenceId));
    }

    private ResultActions redeem(UUID user, long points, String referenceId) throws Exception {
        return postJson("/points/redeem", body(user, "points", points, referenceId));
    }

    private ResultActions balance(UUID user) throws Exception {
        return mockMvc.perform(get("/points/balance").param("userId", user.toString()));
    }

    private ResultActions history(UUID user, String extraQuery) throws Exception {
        return mockMvc.perform(get("/points/history?userId=" + user + extraQuery));
    }

    private ResultActions postJson(String url, String json) throws Exception {
        return mockMvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private static String body(UUID user, String amountField, long amount, String referenceId) {
        return "{\"userId\": \"%s\", \"%s\": %d, \"referenceId\": \"%s\"}"
                .formatted(user, amountField, amount, referenceId);
    }
}
