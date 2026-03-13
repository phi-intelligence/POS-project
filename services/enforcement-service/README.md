# Enforcement Service (`enforcement-service`)

## Overview
- Tracks field inspections, notices, seizures, and associated evidence.
- Used heavily by Field Agent mobile app and back-office compliance staff.

## Responsibilities
- Record enforcement actions linked to payers, assets, and/or obligations.
- Capture GPS coordinates at time of enforcement.
- Store references to evidence files (photos, documents, etc.).

## Database
- Owned schema: `enforcement`.
- Tables:
  - `enforcement.enforcement_actions` – enforcement records (inspection, notice, seizure).
  - `enforcement.evidence_files` – evidence file metadata linked to actions.
- Relationships:
  - FKs into `identity.users`, `payer.assets`, `obligation.obligations`, `administrative.administrative_units`.

## APIs (High-Level)
- Enforcement actions:
  - `POST /enforcement-actions` – record new action.
  - `GET /enforcement-actions/{id}` – view action.
  - `PUT /enforcement-actions/{id}` – update.
  - `GET /enforcement-actions` – list with filters.
- Evidence:
  - `POST /evidence` – attach evidence to an enforcement action.
  - `GET /enforcement-actions/{id}/evidence` – list evidence.

## Events
- Publish:
  - `enforcement.action.recorded`.

## Dependencies
- Infrastructure: PostgreSQL (`enforcement` schema), file/blob storage for evidence.
- Internal: Uses payer/asset/obligation IDs; consumed by Reporting and Notification services.

## Implementation Notes
- Configure `spring.jpa.properties.hibernate.default_schema=enforcement`.
- File storage for evidence can be in object storage (S3, Azure Blob, on-prem) with URLs stored in `evidence_files`.