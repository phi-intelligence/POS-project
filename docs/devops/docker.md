### POS Backend Docker & docker-compose Guide

#### Prerequisites

- **Docker** and **Docker Compose** installed on your machine.
- From a terminal, set your working directory to the project root, e.g.:
  - `e:\Phi-Intelligence\POS\POS-project`

#### What gets started

The provided configuration will start:

- PostgreSQL (`db`) with database `pos` and user `pos` / password `pos`.
- Redis (`redis`) for notification and caching.
- All backend microservices:
  - `identity-service` (port 8081)
  - `administrative-service` (port 8082)
  - `payer-service` (port 8083)
  - `obligation-service` (port 8084)
  - `payment-service` (port 8085)
  - `device-service` (port 8086)
  - `enforcement-service` (port 8087)
  - `reporting-service` (port 8088)
  - `notification-service` (port 8089)

#### Build and run the full stack

From the project root:

```bash
docker compose up --build
```

This will:

- Build Docker images for each service using the Dockerfiles under `services/<service-name>`.
- Start PostgreSQL, Redis, and all services on a shared Docker network.

To run in the background:

```bash
docker compose up --build -d
```

To stop everything:

```bash
docker compose down
```

#### Running only infrastructure (Postgres + Redis)

If you want to run only the database and Redis and start services manually, you can use:

```bash
docker compose up -d db redis
```

#### Environment variables and overrides

- Database connection details are configured via:
  - `DB_HOST` (defaults to `db` in `docker-compose.yml`)
  - `DB_PORT` (defaults to `5432`)
  - `DB_NAME` (defaults to `pos`)
  - `DB_USERNAME` (defaults to `pos`)
  - `DB_PASSWORD` (defaults to `pos`)
- Redis connection for `notification-service` is configured via:
  - `REDIS_HOST` (defaults to `redis`)
  - `REDIS_PORT` (defaults to `6379`)
- `identity-service` JWT secret can be overridden via:
  - `JWT_SECRET` (defaults to `dev-identity-secret` from `docker-compose.yml`)

You can override these values at runtime by passing `-e KEY=value` or by editing `docker-compose.yml` to suit your environment.

#### Notes

- Each service runs with the `docker` Spring profile (`SPRING_PROFILES_ACTIVE=docker`), which is defined in `application-docker.yaml` per service.
- PostgreSQL schemas and tables are managed by Flyway and JPA according to the schema separation described in `docs/data/POS-Schema-Separation.md`.

