package com.example.rewardspoints.points.application;

import com.example.rewardspoints.points.api.dto.BalanceResponse;
import com.example.rewardspoints.points.api.dto.EarnRequest;
import com.example.rewardspoints.points.api.dto.EarnResponse;
import com.example.rewardspoints.points.api.dto.LedgerEntryResponse;
import com.example.rewardspoints.points.api.dto.RedeemRequest;
import com.example.rewardspoints.points.api.dto.RedeemResponse;
import com.example.rewardspoints.points.domain.LedgerEntry;
import com.example.rewardspoints.points.domain.LedgerType;
import com.example.rewardspoints.points.domain.PointAccount;
import com.example.rewardspoints.points.exceptions.InsufficientBalanceException;
import com.example.rewardspoints.points.exceptions.InvalidAmountException;
import com.example.rewardspoints.points.infrastructure.LedgerEntryRepository;
import com.example.rewardspoints.points.infrastructure.PointAccountRepository;
import com.example.rewardspoints.shared.pagination.PageResponse;
import com.example.rewardspoints.user.application.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Earn, redeem, balance and history.
 *
 * <p>Every write is one transaction: ledger entry and balance change commit together or not at all.
 * Concurrent writers to the same account are serialised by the {@code @Version} on {@link PointAccount}
 * (optimistic locking): the loser's commit fails and the whole transaction is retried against fresh data.
 * The retry loop sits outside the transaction on purpose, so each attempt gets its own.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PointsService {
    /** Base earn rate: 1 point per this many currency units spent. */
    static final long UNITS_PER_POINT = 100;
    static final int MAX_ATTEMPTS = 10;

    private final PointAccountRepository accountRepository;
    private final LedgerEntryRepository ledgerRepository;
    private final UserService userService;
    private final TransactionTemplate transactionTemplate;

    public EarnResponse earn(EarnRequest request) {
        userService.getUserByUuid(request.userId());
        long points = request.purchaseAmount() / UNITS_PER_POINT;
        if (points <= 0) {
            throw new InvalidAmountException(
                    "Purchase amount must be at least " + UNITS_PER_POINT + " to earn points");
        }
        return withRetry(() -> transactionTemplate.execute(status -> {
            PointAccount account = accountRepository.findByUserId(request.userId())
                    .orElseGet(() -> PointAccount.openAccount(request.userId()));
            account.add(points);
            PointAccount saved = accountRepository.saveAndFlush(account);
            LedgerEntry entry = ledgerRepository.save(
                    LedgerEntry.create(saved.getId(), LedgerType.EARN, points, request.referenceId(), null, null));
            return new EarnResponse(entry.getId(), points, saved.getBalance());
        }));
    }

    public RedeemResponse redeem(RedeemRequest request) {
        userService.getUserByUuid(request.userId());
        return withRetry(() -> transactionTemplate.execute(status -> {
            PointAccount account = accountRepository.findByUserId(request.userId())
                    // no account yet means a balance of 0
                    .orElseThrow(() -> new InsufficientBalanceException(0, request.points()));
            account.deduct(request.points());
            PointAccount saved = accountRepository.saveAndFlush(account);
            LedgerEntry entry = ledgerRepository.save(
                    LedgerEntry.create(saved.getId(), LedgerType.REDEEM, -request.points(), request.referenceId(), null, null));
            return new RedeemResponse(entry.getId(), request.points(), saved.getBalance());
        }));
    }

    public BalanceResponse getBalance(UUID userId) {
        userService.getUserByUuid(userId);
        long balance = accountRepository.findByUserId(userId).map(PointAccount::getBalance).orElse(0L);
        return new BalanceResponse(userId, balance);
    }

    public PageResponse<LedgerEntryResponse> getHistory(UUID userId, Pageable pageable) {
        userService.getUserByUuid(userId);
        return accountRepository.findByUserId(userId)
                .map(account -> PageResponse.from(
                        ledgerRepository.findByPointAccountId(account.getId(), pageable), LedgerEntryResponse::from))
                .orElseGet(() -> PageResponse.from(
                        org.springframework.data.domain.Page.empty(pageable), LedgerEntryResponse::from));
    }

    /**
     * Re-runs the unit of work when it lost a race: a stale {@code @Version} on the account, or a second
     * transaction opening the same user's first account (unique user_id). Business errors are not retried.
     */
    private <T> T withRetry(Supplier<T> unitOfWork) {
        for (int attempt = 1; ; attempt++) {
            try {
                return unitOfWork.get();
            } catch (OptimisticLockingFailureException | DataIntegrityViolationException e) {
                if (attempt >= MAX_ATTEMPTS) {
                    log.warn("Giving up after {} attempts: {}", attempt, e.toString());
                    throw e;
                }
            }
        }
    }
}
