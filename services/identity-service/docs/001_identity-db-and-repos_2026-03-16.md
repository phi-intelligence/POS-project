# Identity DB and Repositories

**Date**: 2026-03-16  
**Change ID**: 001  
**Service**: identity-service

## Summary
- Added core identity schema tables and corresponding JPA entities.
- Introduced Flyway migrations for `identity.roles`, `identity.role_permissions`, `identity.users`, `identity.refresh_tokens`, and `identity.login_audit_logs`.
- Created Spring Data repositories for roles, users, refresh tokens, and login audit logs.

## Details
- Entities added or updated under `services/identity-service/src/main/java/com/pos/identity_service/model/`:
  - `Role`, `RolePermission`, `User`, `RefreshToken`, `LoginAuditLog`.
- Flyway migrations added under `services/identity-service/src/main/resources/db/migration/`:
  - `V1__create_identity_roles_and_role_permissions.sql`
  - `V2__create_identity_users.sql`
  - `V3__create_identity_refresh_tokens.sql`
  - `V4__create_identity_login_audit_logs.sql`
- Repositories added under `services/identity-service/src/main/java/com/pos/identity_service/repository/`:
  - `RoleRepository`, `RolePermissionRepository`, `UserRepository`, `RefreshTokenRepository`, `LoginAuditLogRepository`.

## Impact
- Identity service now has a concrete database schema and persistence layer.
- Subsequent work (JWT auth, AuthService, controllers) can rely on these tables and repositories.
- Flyway migrations must run successfully before starting identity-service in any environment.

