-- User accounts for JWT auth

CREATE TABLE app_user (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(120) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_app_user_email ON app_user(email);

-- Backfill for environments that already applied an older V3 without user_id
ALTER TABLE booking_hold
    ADD COLUMN IF NOT EXISTS user_id UUID NULL;
