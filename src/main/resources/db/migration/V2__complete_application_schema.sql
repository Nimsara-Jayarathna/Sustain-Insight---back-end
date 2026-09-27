-- Complete the schema required by the current Sustain Insight domain model.
-- This migration is intentionally additive so it can upgrade a database that
-- already contains the historical V1 baseline as well as a fresh V1 database.

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS is_email_verified boolean NOT NULL DEFAULT false;

ALTER TABLE articles
    ADD COLUMN IF NOT EXISTS insight_count bigint NOT NULL DEFAULT 0;

CREATE TABLE IF NOT EXISTS raw_articles (
    id bigserial PRIMARY KEY,
    api_source varchar(255) NOT NULL,
    title text,
    description text,
    content text,
    url text,
    image_url text,
    source_name text,
    published_at timestamptz,
    fetched_at timestamptz NOT NULL DEFAULT now(),
    raw_json jsonb,
    processed boolean NOT NULL DEFAULT false
);

CREATE TABLE IF NOT EXISTS insights (
    user_id bigint NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    article_id bigint NOT NULL REFERENCES articles(id) ON DELETE CASCADE,
    created_at timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, article_id)
);

CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id bigserial PRIMARY KEY,
    token varchar(255) NOT NULL UNIQUE,
    user_id bigint NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at timestamp NOT NULL,
    expires_at timestamp NOT NULL
);

CREATE TABLE IF NOT EXISTS refresh_tokens (
    id bigserial PRIMARY KEY,
    user_id bigint NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token varchar(255) NOT NULL UNIQUE,
    expiry_date timestamptz NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS email_verification_tokens (
    id bigserial PRIMARY KEY,
    token varchar(255) NOT NULL UNIQUE,
    user_id bigint NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    expires_at timestamptz NOT NULL
);

CREATE TABLE IF NOT EXISTS email_change_otp (
    id bigserial PRIMARY KEY,
    user_id bigint NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    otp_hash varchar(255) NOT NULL,
    type varchar(20) NOT NULL,
    new_email varchar(255),
    expires_at timestamptz NOT NULL,
    used boolean NOT NULL DEFAULT false,
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS user_sessions (
    id uuid PRIMARY KEY,
    user_id bigint NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    refresh_token_id bigint UNIQUE REFERENCES refresh_tokens(id) ON DELETE SET NULL,
    device_info varchar(255),
    ip_address varchar(255),
    user_agent varchar(255),
    location varchar(255),
    created_at timestamptz NOT NULL DEFAULT now(),
    last_active_at timestamptz NOT NULL DEFAULT now(),
    expires_at timestamptz,
    is_active boolean NOT NULL DEFAULT true
);

-- Query-path indexes. Unique constraints above already create indexes for tokens.
CREATE INDEX IF NOT EXISTS idx_raw_articles_processed
    ON raw_articles(processed);
CREATE INDEX IF NOT EXISTS idx_raw_articles_published_at
    ON raw_articles(published_at DESC);
CREATE INDEX IF NOT EXISTS idx_articles_published_at
    ON articles(published_at DESC);
CREATE INDEX IF NOT EXISTS idx_password_reset_tokens_expires_at
    ON password_reset_tokens(expires_at);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user_id
    ON refresh_tokens(user_id);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_expiry_date
    ON refresh_tokens(expiry_date);
CREATE INDEX IF NOT EXISTS idx_email_verification_tokens_user_id
    ON email_verification_tokens(user_id);
CREATE INDEX IF NOT EXISTS idx_email_change_otp_user_id
    ON email_change_otp(user_id);
CREATE INDEX IF NOT EXISTS idx_email_change_otp_expires_at
    ON email_change_otp(expires_at);
CREATE INDEX IF NOT EXISTS idx_user_sessions_user_id
    ON user_sessions(user_id);
CREATE INDEX IF NOT EXISTS idx_user_sessions_active
    ON user_sessions(user_id, is_active);
CREATE INDEX IF NOT EXISTS idx_user_sessions_expires_at
    ON user_sessions(expires_at);
