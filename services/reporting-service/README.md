# Reporting Service (`reporting-service`)

## Overview
- Provides audit logs and registry snapshots for tax and fines.
- Exposes reporting and export APIs for compliance and analytics.

## Responsibilities
- Maintain `audit_logs` for all critical system operations.
- Maintain `tax_registry_snapshot` and `fine_registry_snapshot`.
- Provide dashboards and standard reports via APIs.

## Database
- Owned schema: `reporting`.
- Tables:
  - `reporting.audit_logs` – generic audit log entries.
  - `reporting.tax_registry_snapshot` – per-asset tax registry view.
  - `reporting.fine_registry_snapshot` – per-asset fine registry view.
- Relationships:
  - FKs into `identity.users`, `payer`, `administrative`.

## APIs (High-Level)
- Audit:
  - `GET /audit-logs` – list/search logs.
  - `GET /audit-logs/{id}` – view detail.
- Reports:
  - `GET /reports/tax-registry`.
  - `GET /reports/fine-registry`.
  - Export (CSV/PDF) endpoints as needed.

## Events
- Consume:
  - `payer.created`, `asset.registered`, `obligation.created`, `obligation.overdue`,
    `payment.confirmed`, `enforcement.action.recorded`, etc., to update snapshots.

## Dependencies
- Infrastructure: PostgreSQL (`reporting` schema), Kafka.
- Internal: Reads aggregated state from all domains via events.

## Implementation Notes
- Configure `spring.jpa.properties.hibernate.default_schema=reporting`.
- Snapshot refresh strategy can be event-driven or scheduled batch jobs.