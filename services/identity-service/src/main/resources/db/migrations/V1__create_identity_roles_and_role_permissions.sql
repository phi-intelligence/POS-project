CREATE SCHEMA IF NOT EXISTS identity;

CREATE TABLE IF NOT EXISTS identity.roles (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE,
    description TEXT
);

CREATE TABLE IF NOT EXISTS identity.role_permissions (
    id             BIGSERIAL PRIMARY KEY,
    role_id        BIGINT NOT NULL REFERENCES identity.roles(id),
    permission_key VARCHAR(100) NOT NULL
);