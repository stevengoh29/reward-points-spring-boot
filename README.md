# Rewards Points

A points and rewards backend: users earn points from purchases and redeem them. Every change to a balance is a row in an append-only ledger, and the balance is derived from it.

Built with Java 21, Spring Boot 4.1, Spring Data JPA, Flyway and PostgreSQL. Requirements and the entity design live in [`.docs/`](.docs/).

## Status

| Milestone | State |
|---|---|
| M1. Core points engine (users, earn, redeem, balance, history, concurrency test) | Done |
| M2. Security (JWT, roles, `Idempotency-Key` header, rate limiting, audit log) | Not started |
| M3. Campaigns and expiry | Not started |
| M4 to M6. Kubernetes, events, load tests | Not started |

Until M2 there is no authentication. Endpoints take a `userId` explicitly; M2 replaces that with the logged-in user.

## Running it

Requires Java 21 and Docker.

```bash
./mvnw spring-boot:run     # starts PostgreSQL from compose.yaml automatically
./mvnw test                # integration tests start their own PostgreSQL via Testcontainers
```

The schema is created by Flyway migrations in `src/main/resources/db/migration`; Hibernate never changes it.

## API

Errors are always RFC 9457 Problem Details with a stable `code` field (see [Errors](#errors)). Lists are paginated with `page`, `size` and `sort`.

### Users

| Method | Path | Notes |
|---|---|---|
| POST | `/users` | `email` (required, valid, max 320), `displayName` (max 100). New users are always `ACTIVE`. Duplicate email gives 409. |
| GET | `/users` | Filter by `email`, `displayName`, `status`. |
| GET | `/users/{id}` | |
| PATCH | `/users/{id}` | Partial update: `displayName`, `status`. |
| DELETE | `/users/{id}` | Soft delete. The email can be registered again afterwards. |

### Points

| Method | Path | Notes |
|---|---|---|
| POST | `/points/earn` | `userId`, `purchaseAmount`, `referenceId`. Awards 1 point per 100 units, rounded down. Under 100 gives 400. Same `referenceId` again gives 409 `POINTS_DUPLICATE_REFERENCE`. |
| POST | `/points/redeem` | `userId`, `points`, `referenceId`. Not enough balance gives 422. Same `referenceId` again gives 409 `POINTS_DUPLICATE_REFERENCE`. |
| GET | `/points/balance?userId=` | 0 if the user has never earned. |
| GET | `/points/history?userId=` | Ledger entries, newest first. |

## Design decisions

| Decision | Why | Trade-off |
|---|---|---|
| Append-only ledger (`@Immutable`, no updates or deletes) | Full history; mistakes are fixed with new entries | More rows; the balance is cached on `PointAccount` |
| Cached balance on `PointAccount` | Fast balance reads | Must always equal the ledger sum, which a test checks |
| Optimistic locking (`@Version`) with retry for earn and redeem | No lock held while waiting; fast when conflicts are rare | Under heavy contention on one account a request can exhaust its retries and return 409 |
| Retry loop outside the transaction | Each attempt gets a fresh transaction and fresh data | Slightly more code than `@Retryable` |
| One EARN and one REDEEM per `(account, referenceId)`, enforced by a partial unique index | A client retry can never apply twice, even under parallel requests | The caller must use a unique reference per operation; a retry gets 409 rather than the original response |
| Database `CHECK`s (balance >= 0, amount <> 0) | Last line of defence if application code is wrong | Violations surface as 500s if the app ever skips its own checks |
| Modules reference each other by ID only | Modules stay independent | No foreign keys between modules, so integrity is enforced in code |
| Soft delete with a partial unique index on email | A deleted user's email can register again | Queries must respect `deleted_at` (done via `@SQLRestriction`) |

### Concurrency

Two requests racing on one account both read the same `version`; only one commit succeeds. The loser's transaction is retried against the new balance, so it either succeeds on top of the winner or is rejected for insufficient balance. `PointsConcurrencyTest` runs 50 parallel redeems against a balance that covers one and asserts exactly one succeeds.

### Errors

`shared/error` maps every exception to a Problem Details response with a `code`:

- `BusinessException` subclasses use their own code and a status from their category.
- Bean Validation failures give 400 `COMMON_VALIDATION_FAILED` with an `errors` list of `{field, message}`.
- Unreadable bodies and bad path or query values give 400 `COMMON_MALFORMED_REQUEST`.
- Unexpected exceptions give 500 `COMMON_INTERNAL_ERROR` with a generic message; details are only logged.

## Project layout

```
src/main/java/com/example/rewardspoints
  user/      users: api, application, domain, infrastructure, exceptions
  points/    point accounts and the ledger, same layout
  shared/    error handling, pagination, JPA base classes
```

`shared` must not import from feature modules (enforced by `SharedPackageIndependenceTest`), so it can later be lifted into its own library.

## Testing

- Unit tests for domain rules and services.
- Integration tests against a real PostgreSQL via Testcontainers, no in-memory database.
- Concurrency tests for parallel redeems, parallel first earns, and identical parallel requests (exactly one applies).

## Not built yet

Authentication and roles, the `Idempotency-Key` header and replaying the original response (a retry with the same `referenceId` is rejected with 409 instead), campaigns, points expiry, caching, events, and Kubernetes. See [`.docs/`](.docs/) for the plan.
