# Payment Service (`payment-service`)

## Overview
- Handles all payment channels (digital, POS, cash) and officer cash sessions.
- Allocates payments to obligations and drives receipt generation via Obligation Service.

## Responsibilities
- Record and manage payment transactions.
- Allocate payments to one or more obligations.
- Manage officer cash sessions and cash handovers for reconciliation.

## Database
- Owned schema: `payment`.
- Tables:
  - `payment.payments` – payment transactions and metadata.
  - `payment.payment_allocations` – allocation of payments to obligations.
  - `payment.officer_cash_sessions` – officer cash collection sessions.
  - `payment.cash_handovers` – handovers from officers to treasury/bank.
- Relationships:
  - FKs into `payer.payers`, `administrative.administrative_units`,
    `identity.users`, `obligation.obligations`.

## APIs (High-Level)
- Payments:
  - `POST /payments` – initiate/record payment.
  - `GET /payments/{id}` – view payment.
  - `GET /payments` – list payments with filters.
- Cash sessions:
  - `POST /cash-sessions` – open session.
  - `GET /cash-sessions/{id}` – view.
  - `PUT /cash-sessions/{id}` – close/verify.
- Cash handovers:
  - `POST /cash-handovers` – record handover.
  - `GET /cash-handovers` – list.

## Events
- Publish:
  - `payment.initiated`, `payment.confirmed`, `payment.failed`.
  - `cash.session.opened`, `cash.session.closed`.
- Downstream consumers:
  - Obligation (for receipts), Reporting, Notification.

## Dependencies
- Infrastructure: PostgreSQL (`payment` schema), Kafka, payment gateways, POS acquirer APIs.
- Internal: Coordinates with Obligation, Device, Identity, and Reporting services.

## Implementation Notes
- Configure `spring.jpa.properties.hibernate.default_schema=payment`.
- Allocation logic must be deterministic and auditable.