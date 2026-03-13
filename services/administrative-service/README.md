# Administrative Service (`administrative-service`)

## Overview
- Manages the government administrative hierarchy and tax/penalty rules.
- Used by admins to configure units (country/region/district/zone) and rules.
- Provides rule information for obligation calculations.

## Responsibilities
- Maintain hierarchical `administrative_units` (country → region → district → zone, etc.).
- Define and version `rule_versions` for taxes, fees, fines, and penalties.
- Provide rule data or a rule-evaluation API to the Obligation Service.

## Database
- Owned schema: `administrative`.
- Tables:
  - `administrative.administrative_units` – hierarchy of regions/districts/zones.
  - `administrative.rule_versions` – versioned tax/fee/fine rules per asset/admin unit.
- Relationships:
  - `administrative.rule_versions.admin_unit_id` → `administrative.administrative_units.id`.

## APIs (High-Level)
- Administrative units:
  - `POST /admin-units` – create unit.
  - `GET /admin-units/{id}` – get unit detail.
  - `PUT /admin-units/{id}` – update.
  - `DELETE /admin-units/{id}` – delete/deactivate.
- Rules:
  - `POST /rules` – create new rule version.
  - `GET /rules` – list rules (filter by admin unit, asset type, obligation type).
  - `GET /rules/{id}` – rule detail.
  - `PUT /rules/{id}` – update rule definition.
  - `DELETE /rules/{id}` – retire rule version.
- Optional:
  - `POST /rules/evaluate` – evaluate payable amount for a given asset/admin unit (or this can be internal to Obligation Service).

## Events
- Publish:
  - `rule.updated` – when a rule version changes or a new version becomes active.

## Dependencies
- Infrastructure: PostgreSQL (`administrative` schema).
- Internal: Obligation and Payer services depend on its data; no strong upstream dependencies.

## Implementation Notes
- Configure `spring.jpa.properties.hibernate.default_schema=administrative`.
- Implement clear versioning semantics (`effective_from`, `effective_to`, `version_number`).