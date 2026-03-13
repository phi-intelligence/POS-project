# Identity Service (`identity-service`)

## Overview
- Authentication & User Service for the POS platform.
- Handles officer/admin login, JWT token issuing, and user/role management.
- Used by Admin Desktop, Field Agent app, and Citizen app (indirectly via gateway).

## Responsibilities
- Authenticate users (officers, admins, other internal users).
- Issue and validate JWT access tokens.
- Manage users: create, update, suspend/activate, delete.
- Manage roles and permissions (RBAC), including officer permissions.

## Database
- Owned schema: `identity`.
- Tables (see `docs/data/POS-Database-Schema.md`):
  - `identity.users` – user accounts (login, role, status, admin unit link).
  - `identity.roles` – role definitions (e.g. ADMIN, FIELD_AGENT).
  - `identity.role_permissions` – mapping of roles to permission keys.
- Relationships:
  - `identity.users.admin_unit_id` → `administrative.administrative_units.id`.

## APIs (High-Level)
- Auth:
  - `POST /auth/login` – authenticate user, return JWT.
  - `POST /auth/logout` – invalidate/blacklist token (implementation-dependent).
- Users:
  - `POST /users` – create user.
  - `GET /users/{id}` – fetch user.
  - `PUT /users/{id}` – update user.
  - `PATCH /users/{id}/status` – activate/suspend.
  - `DELETE /users/{id}` – soft/hard delete (per design).
- Roles & permissions:
  - `GET /roles`, `POST /roles`, etc. (for admin-only use).

## Events
- Typically none initially; later could emit `user.created`, `user.status.changed`, etc.

## Dependencies
- Infrastructure: PostgreSQL (`identity` schema), Spring Security, JWT library.
- Internal: referenced by all other services via JWT claims and user IDs.

## Implementation Notes
- Configure `spring.jpa.properties.hibernate.default_schema=identity`.
- Map entities to `identity.users`, `identity.roles`, `identity.role_permissions`.
- Keep password hashing and token handling centralized and well-tested.