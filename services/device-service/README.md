# Device Service (`device-service`)

## Overview
- Manages physical POS terminals and their assignments to officers.
- Records POS transaction metadata for reconciliation with Payment Service.

## Responsibilities
- Register and manage POS terminals (status, model, serial number).
- Assign POS terminals to officers/cashiers over time.
- Record POS transactions and map them to Payments for reconciliation.

## Database
- Owned schema: `device`.
- Tables:
  - `device.pos_terminals` – POS terminal inventory.
  - `device.pos_terminal_assignments` – assignment history to users.
  - `device.pos_transactions` – POS transaction metadata.
- Relationships:
  - `device.pos_terminals.admin_unit_id` → `administrative.administrative_units.id`.
  - `device.pos_terminal_assignments.assigned_to_user_id` → `identity.users.id`.

## APIs (High-Level)
- Terminals:
  - `POST/GET/PUT/DELETE /pos-terminals`.
- Assignments:
  - `POST/GET/PUT /pos-assignments` – assign/unassign.
- POS transactions:
  - `GET /pos-transactions` – list/filter for reconciliation views.

## Events
- Optionally publish POS transaction events to Reporting.

## Dependencies
- Infrastructure: PostgreSQL (`device` schema), possibly external POS network integration.
- Internal: Works closely with Payment Service and Reporting.

## Implementation Notes
- Configure `spring.jpa.properties.hibernate.default_schema=device`.
- Ensure assignment history is complete for audit trails.