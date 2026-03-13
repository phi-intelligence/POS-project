# Obligation Service (`obligation-service`)

## Overview
- Manages tax/fee/fine obligations and compliance.
- Handles disputes, adjustments, approvals, and receipts.

## Responsibilities
- Create obligations from rules and assets/payers.
- Maintain obligation lifecycle and outstanding balances.
- Record obligation events (status changes, penalties applied).
- Handle disputes and adjustment workflows (requests + approvals).
- Generate and serve receipts after payments.

## Database
- Owned schema: `obligation`.
- Tables:
  - `obligation.obligations` – main obligation records.
  - `obligation.obligation_events` – event history per obligation.
  - `obligation.disputes` – disputes/appeals against obligations.
  - `obligation.obligation_adjustment_requests` – requests for write-offs/reductions.
  - `obligation.obligation_adjustments` – approved adjustments applied.
  - `obligation.approvals` – generic approval records.
  - `obligation.receipts` – receipts linked to payments.
- Relationships:
  - FKs into `payer.payers`, `payer.assets`, `administrative.administrative_units`,
    `administrative.rule_versions`, `identity.users`, `payment.payments`.

## APIs (High-Level)
- Obligations:
  - `POST /obligations` – create obligation.
  - `GET /obligations/{id}` – view obligation.
  - `PUT /obligations/{id}` – update core fields.
  - `DELETE /obligations/{id}` – cancel/void.
  - `GET /obligations/{id}/events` – history.
- Disputes & adjustments:
  - `POST /disputes`, `GET /disputes/{id}`, `PUT /disputes/{id}`.
  - `POST /adjustments` – create adjustment request.
  - `POST /approvals` – approve/reject requests.
- Receipts:
  - `POST /receipts` – generate (if not purely event-driven).
  - `GET /receipts/{id}`.
  - `GET /receipts/payment/{paymentId}`.
  - `GET /receipts` with filters.

## Events
- Publish:
  - `obligation.created`, `obligation.overdue`, `receipt.generated`, `dispute.created`.
- Consume:
  - `payment.confirmed` – to mark obligations as paid/partially paid and generate receipts.

## Dependencies
- Infrastructure: PostgreSQL (`obligation` schema), Kafka.
- Internal: Depends on Payer, Administrative, Identity (users), Payment.

## Implementation Notes
- Configure `spring.jpa.properties.hibernate.default_schema=obligation`.
- Ensure all status transitions and adjustments are fully audited through `obligation_events`.