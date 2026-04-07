CREATE TABLE IF NOT EXISTS identity.users (
    id             BIGSERIAL PRIMARY KEY,
    username       VARCHAR(100) NOT NULL UNIQUE,
    password_hash  TEXT NOT NULL,
    role_id        BIGINT NOT NULL REFERENCES identity.roles(id),
    -- Cross-schema reference to administrative service removed for now to avoid dependency
    -- on the administrative.administrative_units table during identity schema creation.
    admin_unit_id  BIGINT,
    status         VARCHAR(20) NOT NULL,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);