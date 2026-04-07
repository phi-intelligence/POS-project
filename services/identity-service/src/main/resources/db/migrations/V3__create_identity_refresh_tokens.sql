CREATE TABLE IF NOT EXISTS identity.refresh_tokens (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL REFERENCES identity.users(id),
    token       VARCHAR(255) NOT NULL,
    status      VARCHAR(20) NOT NULL,
    expires_at  TIMESTAMPTZ NOT NULL,
    ip_address  VARCHAR(100),
    user_agent  VARCHAR(255),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_refresh_tokens_token
    ON identity.refresh_tokens(token);