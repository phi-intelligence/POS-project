CREATE TABLE IF NOT EXISTS identity.login_audit_logs (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT REFERENCES identity.users(id),
    username    VARCHAR(100),
    success     BOOLEAN NOT NULL,
    ip_address  VARCHAR(100),
    user_agent  VARCHAR(255),
    reason      TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_login_audit_logs_username_created_at
    ON identity.login_audit_logs(username, created_at);