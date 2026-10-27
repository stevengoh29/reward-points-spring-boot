-- src/main/resources/db/migration/V1__create_users.sql
CREATE TABLE users
(
    id           UUID PRIMARY KEY,
    email        VARCHAR(320) NOT NULL,
    display_name VARCHAR(100),
    status       VARCHAR(20)  NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL,
    created_by   UUID,
    updated_at   TIMESTAMPTZ  NOT NULL,
    updated_by   UUID,
    version      BIGINT       NOT NULL,
    deleted_at   TIMESTAMPTZ
);

-- unique only among non-deleted users, so a deleted email can register again
CREATE UNIQUE INDEX ux_users_email_active ON users (email) WHERE deleted_at IS NULL;