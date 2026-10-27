-- V3__create_ledger_entries_table.sql
-- Append-only: nothing in the application updates or deletes rows here.
-- No FK to point_accounts on purpose: modules share IDs, not relationships.
CREATE TABLE ledger_entries
(
    id           UUID PRIMARY KEY,
    account_id   UUID         NOT NULL,
    type         VARCHAR(20)  NOT NULL CHECK (type IN ('EARN', 'REDEEM', 'EXPIRE', 'REVERSAL', 'ADJUSTMENT')),
    amount       BIGINT       NOT NULL CHECK (amount <> 0),
    reference_id VARCHAR(100) NOT NULL,
    campaign_id  UUID,
    description  VARCHAR(255),
    created_at   TIMESTAMPTZ  NOT NULL,
    created_by   UUID,
    CONSTRAINT ck_ledger_adjustment_has_description
        CHECK (type <> 'ADJUSTMENT' OR (description IS NOT NULL AND description <> ''))
);

-- history pages: newest first for one account
CREATE INDEX ix_ledger_entries_account_created ON ledger_entries (account_id, created_at);
