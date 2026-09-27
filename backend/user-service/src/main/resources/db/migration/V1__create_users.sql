CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(254) NOT NULL,
    password_hash VARCHAR(60) NOT NULL,
    role VARCHAR(16) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT chk_users_email_normalized CHECK (
        length(email) BETWEEN 3 AND 254
        AND email = lower(btrim(email))
        AND position('@' IN email) > 1
    ),
    CONSTRAINT chk_users_password_hash_not_empty CHECK (length(password_hash) > 0),
    CONSTRAINT chk_users_role CHECK (role IN ('USER', 'ADMIN')),
    CONSTRAINT chk_users_updated_at CHECK (updated_at >= created_at)
);

CREATE INDEX idx_users_active_role ON users(role) WHERE active = TRUE;