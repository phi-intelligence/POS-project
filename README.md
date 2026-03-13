## POS Backend (`POS-project`)

Backend services for the POS platform, implemented as separate Spring Boot + Gradle projects (one per domain) under the `services/` directory. A root `docker-compose.yml` starts PostgreSQL, Redis, and all backend services for local development.

### Services

- `services/identity-service` – auth & users (`identity` schema)
- `services/administrative-service` – admin units & rules (`administrative` schema)
- `services/payer-service` – payers & assets (`payer` schema)
- `services/obligation-service` – obligations, disputes, receipts (`obligation` schema)
- `services/payment-service` – payments, allocations, cash sessions (`payment` schema)
- `services/device-service` – POS terminals & assignments (`device` schema)
- `services/enforcement-service` – enforcement actions & evidence (`enforcement` schema)
- `services/reporting-service` – audit logs & registries (`reporting` schema)
- `services/notification-service` – notifications (Redis, no own DB schema)

Infra and data model are documented in:

- `docs/devops/docker.md`
- `docs/data/POS-Schema-Separation.md`

---

### Prerequisites

- JDK 21 (toolchain configured in each service’s `build.gradle`)
- Docker and Docker Compose
- Git

All Gradle commands below use the service-specific wrapper (`./gradlew` on macOS/Linux, `gradlew.bat` on Windows) inside each service directory.

---

### 1. Run the full stack with Docker

From the project root (`POS-project`):

```bash
docker compose up --build
```

This starts:

- PostgreSQL 16 (`db`) on `localhost:5432` (DB `pos`, user `pos`, password `pos`)
- Redis 7 (`redis`) on `localhost:6379`
- All backend services on the following ports:
  - `identity-service` – `8081`
  - `administrative-service` – `8082`
  - `payer-service` – `8083`
  - `obligation-service` – `8084`
  - `payment-service` – `8085`
  - `device-service` – `8086`
  - `enforcement-service` – `8087`
  - `reporting-service` – `8088`
  - `notification-service` – `8089`

Configuration inside containers uses the `docker` Spring profile (`SPRING_PROFILES_ACTIVE=docker`) and environment variables wired in `docker-compose.yml` (see `docs/devops/docker.md` for details).

To stop everything:

```bash
docker compose down
```

To bring up only infrastructure (Postgres + Redis):

```bash
docker compose up -d db redis
```

---

### 2. Run a single service locally (without Docker)

You can also run individual services on your host machine against a local or containerized Postgres/Redis.

Example (identity service) from the project root:

```bash
cd services/identity-service
./gradlew bootRun
```

Repeat with the appropriate directory for other services:

- `services/administrative-service`
- `services/payer-service`
- `services/obligation-service`
- `services/payment-service`
- `services/device-service`
- `services/enforcement-service`
- `services/reporting-service`
- `services/notification-service`

Each service’s default port is configured in its `src/main/resources/application.yaml` (8081–8089).

If you run services on your host while using the Dockerised Postgres/Redis, point them to:

- `jdbc:postgresql://localhost:5432/pos`
- Redis host `localhost`, port `6379`

---

### 3. Database schemas per service

Each service owns a schema in the single `pos` PostgreSQL database:

- `identity-service` → `identity`
- `administrative-service` → `administrative`
- `payer-service` → `payer`
- `obligation-service` → `obligation`
- `payment-service` → `payment`
- `device-service` → `device`
- `enforcement-service` → `enforcement`
- `reporting-service` → `reporting`

Schema separation and table structures are described in `docs/data/POS-Schema-Separation.md` and `docs/data/POS-Database-Schema.md`. Flyway migrations live under each service’s `src/main/resources/db/migration` (when present).

---

### 4. Common Gradle commands (per service)

From a given service directory (for example `services/identity-service`):

```bash
./gradlew clean build      # build the service
./gradlew test             # run unit tests
```

---

### 5. Where to go next

- **Docker & local stack**: `docs/devops/docker.md`
- **DB schema and separation**: `docs/data/POS-Schema-Separation.md`, `docs/data/POS-Database-Schema.md`
- **Per-service domain docs**: `services/*-service/README.md`
