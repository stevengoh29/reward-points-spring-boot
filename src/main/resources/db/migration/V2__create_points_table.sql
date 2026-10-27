-- V2__create_point_accounts.sql
CREATE TABLE point_accounts
(
    id         UUID PRIMARY KEY,
    user_id    UUID        NOT NULL UNIQUE,
    balance    BIGINT      NOT NULL CHECK (balance >= 0),
    created_at TIMESTAMPTZ NOT NULL,
    created_by UUID,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by UUID,
    version    BIGINT      NOT NULL
);