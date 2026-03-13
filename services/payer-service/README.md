# Payer Service (`payer-service`)

## Overview
- Registers and manages payers (citizens and businesses) and their assets.
- Forms the tax base for obligations and enforcement.

## Responsibilities
- Create and maintain payer profiles (individuals and businesses).
- Register and maintain taxable assets linked to payers and administrative units.
- Provide search by identifiers (Ghana Card, TIN, phone, business name, etc.).

## Database
- Owned schema: `payer`.
- Tables:
  - `payer.payers` – payer master data (type, names, identifiers, contacts, primary admin unit).
  - `payer.assets` – assets (properties, vehicles, stalls, licenses, terminals) owned by payers.
- Relationships:
  - `payer.payers.primary_admin_unit_id` → `administrative.administrative_units.id`.
  - `payer.assets.admin_unit_id` → `administrative.administrative_units.id`.

## APIs (High-Level)
- Payers:
  - `POST /payers` – register payer.
  - `GET /payers/{id}` – fetch payer by ID.
  - `PUT /payers/{id}` – update payer.
  - `DELETE /payers/{id}` – deactivate payer.
  - `GET /payers` – search (by name, Ghana Card, TIN, phone, etc.).
- Assets:
  - `POST /assets` – register asset linked to a payer.
  - `GET /assets/{id}` – fetch asset.
  - `PUT /assets/{id}` – update asset.
  - `DELETE /assets/{id}` – deactivate asset.
  - `GET /assets` – search (by identifier, type, admin unit).

## Events
- Publish:
  - `payer.created`.
  - `asset.registered`.

## Dependencies
- Infrastructure: PostgreSQL (`payer` schema).
- Internal:
  - Depends on `administrative` data via FKs.
  - Provides data to Obligation, Enforcement, Reporting services.

## Implementation Notes
- Configure `spring.jpa.properties.hibernate.default_schema=payer`.
- Index key search fields (Ghana Card, TIN, phone, unique asset identifiers).