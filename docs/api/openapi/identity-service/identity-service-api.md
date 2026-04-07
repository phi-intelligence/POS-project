## POS Identity Service API

### Overview

- **Base URL**: `http://localhost:8081`
- **Auth model**: JWT Bearer
  - Obtain tokens via `POST /auth/login`
  - Refresh access token via `POST /auth/refresh`
  - Send as header: `Authorization: Bearer <accessToken>`
- **Admin endpoints** (user/role/audit management) are restricted to `ROLE_ADMIN`.

### Public endpoints

- **GET `/health`**
  - Returns simple liveness probe.
  - **Response 200**: plain text `"OK Good"`.

### Authentication & tokens

- **POST `/auth/login`**
  - **Description**: Authenticate an ACTIVE user and issue JWT access + refresh tokens.
  - **Headers (optional)**:
    - `X-Forwarded-For`: client IP for audit logging
    - `User-Agent`: client user agent for audit logging
  - **Request body (`AuthRequest`)**:
    - `username` (string, required)
    - `password` (string, required)
  - **Responses**:
    - `200` – `AuthTokensResponse`
      - `accessToken` (string, JWT)
      - `refreshToken` (string, opaque UUID)
      - `tokenType` (string, e.g. `Bearer`)
    - `401` – invalid credentials or user not ACTIVE (empty body).
- **POST `/auth/refresh`**
  - **Description**: Exchange a valid/active refresh token for a new access token.
  - **Request body (`RefreshTokenRequest`)**:
    - `refreshToken` (string, required)
  - **Responses**:
    - `200` – `AuthTokensResponse`
    - `401` – refresh token not found / expired / revoked.
- **POST `/auth/logout`**
  - **Description**: Revoke a refresh token (logout).
  - **Request body (`RefreshTokenRequest`)**:
    - `refreshToken` (string, required)
  - **Responses**:
    - `204` – no content (even if token was already inactive).

### Break-glass admin bootstrap

- **POST `/bootstrap/admin`**
  - **Status**: Disabled by default. Enable only in controlled deployments:
    - `bootstrap.breakglass.enabled=true`
    - `bootstrap.breakglass.token` set from a secret (used as `X-Bootstrap-Token`)
  - **Purpose**: One-time creation of the first `ADMIN` user and `ADMIN` role.
  - **Headers**:
    - `X-Bootstrap-Token` (string, required) – must match configured secret.
    - `X-Forwarded-For` (string, optional) – client IP for allowlist + audit.
    - `User-Agent` (string, optional) – for audit logging.
  - **Request body (`AdminBootstrapRequest`)**:
    - `username` (string, required)
    - `password` (string, required, must satisfy password policy)
    - `adminUnitId` (int64, optional)
  - **Password policy (enforced)**:
    - At least 12 characters
    - Must contain upper, lower, digit, and symbol
    - Must not contain the username
  - **Responses**:
    - `201` – admin created.
    - `400` – validation error / password policy failure.
    - `403` – bad bootstrap token or denied by IP allowlist.
    - `409` – admin already provisioned or username already exists.

### User management (ADMIN only)

All `/users` endpoints require a valid JWT with `ROLE_ADMIN`.

- **GET `/users`**
  - List all users.
  - **Response 200**: array of `UserDto`.
- **POST `/users`**
  - Create a new user.
  - **Request body (`UserCreateRequest`)**:
    - `username` (string, required)
    - `password` (string, required, password policy enforced)
    - `roleId` (int64, required) – reference to an existing role.
    - `adminUnitId` (int64, optional)
  - **Responses**:
    - `201` – created user (`UserDto`), with `Location` header like `/users/{id}`.
    - `500` – role not found (server-side validation error).
- **GET `/users/{id}`**
  - Fetch a user by ID.
  - **Responses**:
    - `200` – `UserDto`
    - `404` – not found.
- **PUT `/users/{id}`**
  - Update user’s role and/or admin unit.
  - **Request body (`UserUpdateRequest`)**:
    - `roleId` (int64, optional)
    - `adminUnitId` (int64, optional)
  - **Responses**:
    - `200` – updated `UserDto`
    - `404` – not found
    - `500` – role not found.
- **PATCH `/users/{id}/status`**
  - Update user status (e.g. `ACTIVE`, `DISABLED`).
  - **Request body (`UserStatusUpdateRequest`)**:
    - `status` (string, required)
  - **Responses**:
    - `200` – updated `UserDto`
    - `404` – not found.
- **DELETE `/users/{id}`**
  - Delete a user.
  - **Response 204** – no content (idempotent).

`**UserDto` structure**

- `id` (int64)
- `username` (string)
- `roleName` (string, nullable)
- `adminUnitId` (int64, nullable)
- `status` (string)

### Role & permission management (ADMIN only)

All `/roles` endpoints require a valid JWT with `ROLE_ADMIN`.

- **GET `/roles`**
  - List all roles.
  - **Response 200**: array of `RoleDto`.
- **POST `/roles`**
  - Create a role.
  - **Request body (`RoleCreateRequest`)**:
    - `name` (string, required)
    - `description` (string, optional)
  - **Response 201**: `RoleDto` with `Location` header `/roles/{id}`.
- **GET `/roles/{id}`**
  - Fetch a role.
  - **Responses**:
    - `200` – `RoleDto`
    - `404` – not found.
- **PUT `/roles/{id}`**
  - Update role description.
  - **Request body (`RoleUpdateRequest`)**:
    - `description` (string, optional)
  - **Responses**:
    - `200` – updated `RoleDto`
    - `404` – not found.
- **DELETE `/roles/{id}`**
  - Delete a role.
  - **Response 204** – no content.

`**RoleDto` structure**

- `id` (int64)
- `name` (string)
- `description` (string, nullable)

#### Role permissions

- **GET `/roles/{id}/permissions`**
  - List permissions assigned to a role.
  - **Response 200**: array of `PermissionDto`.
- **POST `/roles/{id}/permissions`**
  - Assign a permission to a role.
  - **Request body (`PermissionAssignRequest`)**:
    - `permissionKey` (string, required)
  - **Response 200**: updated array of `PermissionDto`.
- **DELETE `/roles/{roleId}/permissions/{permissionId}`**
  - Remove a permission from a role.
  - **Response 204** – removed.

`**PermissionDto` structure**

- `id` (int64)
- `permissionKey` (string)

### Login audit (ADMIN only)

- **GET `/auth/login-audit`**
  - List login audit entries.
  - **Query params**:
    - `username` (string, optional) – filter by username.
  - **Response 200**: array of `LoginAuditLogDto`.

`**LoginAuditLogDto` structure**

- `id` (int64)
- `userId` (int64, nullable)
- `username` (string)
- `success` (boolean)
- `ipAddress` (string, nullable)
- `userAgent` (string, nullable)
- `reason` (string, nullable)
- `createdAt` (ISO 8601 date-time)

