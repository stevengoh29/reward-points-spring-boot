-- V4__ledger_unique_reference.sql
-- One EARN and one REDEEM per (account, reference), so a client retry can never apply twice.
-- Partial on purpose: REVERSAL and ADJUSTMENT legitimately reuse the original reference.
CREATE UNIQUE INDEX ux_ledger_entries_account_type_reference
    ON ledger_entries (account_id, type, reference_id)
    WHERE type IN ('EARN', 'REDEEM');
