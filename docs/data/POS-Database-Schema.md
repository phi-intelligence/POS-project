## Database Schema – Ghana Government Tax, Fees, Fines & POS Platform

This document describes the **logical** relational database schema for the national / municipal revenue and POS platform.  
Each section defines **one table**, with **field name**, **data type** (PostgreSQL‑style), and a short **description**.

> Physical schema separation by microservice (single PostgreSQL DB, one schema per service) is documented in [POS-Schema-Separation.md](POS-Schema-Separation.md).  
> This file remains the canonical **logical** view of tables and columns across the platform.

> Note: All timestamps use `TIMESTAMP WITH TIME ZONE` (`timestamptz`) unless otherwise stated.

---

## 1. Administrative & Access Control

### 1.1 `administrative_units`

| Field            | Data Type        | Description                                                                                 |
|------------------|------------------|---------------------------------------------------------------------------------------------|
| `id`             | BIGINT (PK)      | Unique identifier for the administrative unit.                                              |
| `name`           | VARCHAR(255)     | Human‑readable name (e.g. Ghana, Greater Accra, Tema, Community 1).                        |
| `type`           | VARCHAR(50)      | Type of unit: `COUNTRY`, `REGION`, `METRO`, `DISTRICT`, `MUNICIPAL`, `ZONE`.               |
| `parent_id`      | BIGINT (FK)      | Parent administrative unit in the hierarchy; `NULL` for top‑level country.                 |
| `code`           | VARCHAR(50)      | Short code used in reporting or integration (e.g. TMA‑C1).                                 |
| `is_active`      | BOOLEAN          | Whether this unit is currently active.                                                      |
| `created_at`     | timestamptz      | When the unit record was created.                                                          |

---

### 1.2 `roles`

| Field        | Data Type    | Description                                             |
|--------------|--------------|---------------------------------------------------------|
| `id`         | BIGINT (PK)  | Unique identifier for the role.                        |
| `name`       | VARCHAR(100) | Role name (e.g. ADMIN, FIELD_AGENT, AUDITOR).         |
| `description`| TEXT         | Human‑readable explanation of the role.                |

---

### 1.3 `role_permissions`

| Field           | Data Type    | Description                                                        |
|-----------------|--------------|--------------------------------------------------------------------|
| `id`            | BIGINT (PK)  | Unique identifier for the permission mapping.                      |
| `role_id`       | BIGINT (FK)  | FK to `roles.id`.                                                  |
| `permission_key`| VARCHAR(100) | Machine‑readable permission key (e.g. `OBLIGATION.WRITE_OFF`).     |

---

### 1.4 `users`

| Field          | Data Type        | Description                                                                 |
|----------------|------------------|-----------------------------------------------------------------------------|
| `id`           | BIGINT (PK)      | Unique identifier for the user (officer, admin, auditor, etc.).            |
| `username`     | VARCHAR(100)     | Login username; must be unique.                                            |
| `password_hash`| TEXT             | Password hash (never store plaintext passwords).                           |
| `role_id`      | BIGINT (FK)      | FK to `roles.id`; defines role and permissions.                            |
| `admin_unit_id`| BIGINT (FK)      | FK to `administrative_units.id`; scopes user to an area.                   |
| `status`       | VARCHAR(20)      | `ACTIVE` or `SUSPENDED`.                                                   |
| `created_at`   | timestamptz      | When the user account was created.                                         |

---

## 2. Payers & Assets

### 2.1 `payers`

| Field                 | Data Type        | Description                                                              |
|-----------------------|------------------|--------------------------------------------------------------------------|
| `id`                  | BIGINT (PK)      | Unique identifier for the payer.                                         |
| `payer_type`          | VARCHAR(20)      | `INDIVIDUAL` or `BUSINESS`.                                             |
| `full_name`           | VARCHAR(255)     | Full legal name of the individual or contact person.                     |
| `business_name`       | VARCHAR(255)     | Registered business name; `NULL` for individuals.                        |
| `ghana_card_no`       | VARCHAR(50)      | Ghana Card number (indexed); may be `NULL` for some businesses.          |
| `tin_number`          | VARCHAR(50)      | Tax Identification Number (indexed).                                     |
| `phone`               | VARCHAR(50)      | Primary phone number (indexed for quick lookup).                         |
| `email`               | VARCHAR(255)     | Email address for electronic communication.                              |
| `primary_admin_unit_id`| BIGINT (FK)     | FK to `administrative_units.id`; main area linked to the payer.          |
| `status`              | VARCHAR(20)      | `ACTIVE` or `INACTIVE`.                                                  |
| `created_at`          | timestamptz      | When the payer record was created.                                       |

---

### 2.2 `assets`

| Field           | Data Type        | Description                                                                 |
|-----------------|------------------|-----------------------------------------------------------------------------|
| `id`            | BIGINT (PK)      | Unique identifier for the asset.                                            |
| `asset_type`    | VARCHAR(30)      | `PROPERTY`, `VEHICLE`, `STALL`, `LICENSE`, `TERMINAL`, etc.                |
| `owner_payer_id`| BIGINT (FK)      | FK to `payers.id`; the payer legally responsible for this asset.           |
| `admin_unit_id` | BIGINT (FK)      | FK to `administrative_units.id`; where the asset is physically located.    |
| `unique_identifier`| VARCHAR(255)  | Real‑world identifier (plot code, license number, stall ID, etc.).        |
| `status`        | VARCHAR(30)      | Operational status of the asset (e.g. ACTIVE, INACTIVE).                   |
| `created_at`    | timestamptz      | When the asset was registered.                                             |

---

## 3. Rule Engine

### 3.1 `rule_versions`

| Field            | Data Type        | Description                                                                 |
|------------------|------------------|-----------------------------------------------------------------------------|
| `id`             | BIGINT (PK)      | Unique identifier for the rule version.                                     |
| `rule_name`      | VARCHAR(255)     | Human‑readable name for the rule (e.g. "Property Rate – Residential").      |
| `admin_unit_id`  | BIGINT (FK)      | FK to `administrative_units.id`; `NULL` if rule is national.               |
| `asset_type`     | VARCHAR(30)      | Asset type this rule applies to (e.g. PROPERTY, VEHICLE).                   |
| `obligation_type`| VARCHAR(20)      | `TAX`, `FEE`, or `FINE`.                                                   |
| `calculation_type`| VARCHAR(20)     | `FIXED`, `PERCENTAGE`, or `FORMULA`.                                       |
| `base_value`     | NUMERIC(18,2)    | Base amount or base rate used for calculation.                              |
| `effective_from` | DATE             | Date from which this rule version is valid.                                 |
| `effective_to`   | DATE             | Date until which this rule version is valid; `NULL` if still active.       |
| `version_number` | INTEGER          | Sequential version number for the rule.                                     |
| `created_at`     | timestamptz      | When this rule version was created.                                         |

---

## 4. Obligations & Events

### 4.1 `obligations`

| Field           | Data Type        | Description                                                                 |
|-----------------|------------------|-----------------------------------------------------------------------------|
| `id`            | BIGINT (PK)      | Unique identifier for the obligation (debt).                                |
| `asset_id`      | BIGINT (FK)      | FK to `assets.id`; asset the obligation is tied to.                         |
| `payer_id`      | BIGINT (FK)      | FK to `payers.id`; who owes the obligation.                                 |
| `admin_unit_id` | BIGINT (FK)      | FK to `administrative_units.id`; area responsible for the revenue.         |
| `obligation_type`| VARCHAR(20)     | `TAX`, `FEE`, or `FINE`.                                                   |
| `rule_version_id`| BIGINT (FK)     | FK to `rule_versions.id`; rule used to calculate this obligation.          |
| `principal_amount`| NUMERIC(18,2)  | Base amount before penalties.                                              |
| `penalty_amount` | NUMERIC(18,2)   | Penalties or surcharges applied.                                           |
| `total_amount`  | NUMERIC(18,2)    | Total amount due (principal + penalty).                                    |
| `currency`      | VARCHAR(10)      | ISO currency code (e.g. GHS).                                              |
| `due_date`      | DATE             | Date the obligation is due.                                                |
| `status`        | VARCHAR(30)      | `OPEN`, `OVERDUE`, `PAID`, `PARTIALLY_PAID`, `IN_DISPUTE`, `WAIVED`, `CANCELLED`. |
| `created_at`    | timestamptz      | When the obligation was created.                                           |
| `updated_at`    | timestamptz      | Last time the obligation record was updated.                               |

---

### 4.2 `obligation_events`

| Field             | Data Type        | Description                                                                 |
|-------------------|------------------|-----------------------------------------------------------------------------|
| `id`              | BIGINT (PK)      | Unique identifier for the obligation event.                                 |
| `obligation_id`   | BIGINT (FK)      | FK to `obligations.id`; obligation whose lifecycle changed.                 |
| `event_type`      | VARCHAR(30)      | `CREATED`, `MARKED_OVERDUE`, `PENALTY_APPLIED`, `PAID`, `PARTIALLY_PAID`, `CANCELLED`, `ADJUSTED`, `DISPUTED`. |
| `previous_status` | VARCHAR(30)      | Status before this event.                                                   |
| `new_status`      | VARCHAR(30)      | Status after this event.                                                    |
| `changed_by_user_id`| BIGINT (FK)    | FK to `users.id`; user who triggered the change.                            |
| `created_at`      | timestamptz      | When the event was recorded.                                                |

---

## 5. Payments, POS & Receipts

### 5.1 `payments`

| Field             | Data Type        | Description                                                                 |
|-------------------|------------------|-----------------------------------------------------------------------------|
| `id`              | BIGINT (PK)      | Unique identifier for the payment.                                          |
| `payer_id`        | BIGINT (FK)      | FK to `payers.id`; who made the payment.                                    |
| `admin_unit_id`   | BIGINT (FK)      | FK to `administrative_units.id`; revenue‑owning area.                       |
| `channel`         | VARCHAR(20)      | `MOMO`, `POS`, `PORTAL`, `BANK`, or `CASH`.                                 |
| `initiated_by_user_id`| BIGINT (FK)  | FK to `users.id`; officer/user who initiated payment; `NULL` for self‑service. |
| `initiated_via`   | VARCHAR(30)      | `SELF_SERVICE` or `OFFICER_DEVICE`.                                         |
| `provider_name`   | VARCHAR(100)     | Name of payment provider (telco, bank, POS acquirer).                       |
| `provider_reference`| VARCHAR(255)   | Unique provider reference for digital/POS payments.                         |
| `amount`          | NUMERIC(18,2)    | Total payment amount.                                                       |
| `currency`        | VARCHAR(10)      | ISO currency code (e.g. GHS).                                               |
| `status`          | VARCHAR(20)      | `PENDING`, `CONFIRMED`, `FAILED`, `REVERSED`.                               |
| `custody_status`  | VARCHAR(30)      | For cash: `WITH_OFFICER`, `SUBMITTED_TO_TREASURY`, `VERIFIED`; `NULL` for digital. |
| `cash_session_id` | BIGINT (FK)      | FK to `officer_cash_sessions.id`; only for cash payments.                   |
| `initiated_at`    | timestamptz      | When the payment was initiated.                                             |
| `confirmed_at`    | timestamptz      | When the payment was confirmed by provider or treasury.                     |

---

### 5.2 `payment_allocations`

| Field           | Data Type        | Description                                                                 |
|-----------------|------------------|-----------------------------------------------------------------------------|
| `id`            | BIGINT (PK)      | Unique identifier for the allocation record.                                |
| `payment_id`    | BIGINT (FK)      | FK to `payments.id`; the payment being allocated.                           |
| `obligation_id` | BIGINT (FK)      | FK to `obligations.id`; obligation receiving part of the payment.           |
| `allocated_amount`| NUMERIC(18,2)  | Amount of the payment allocated to this obligation.                         |
| `created_at`    | timestamptz      | When the allocation was created.                                            |

---

### 5.3 `pos_terminals`

| Field          | Data Type        | Description                                                                 |
|----------------|------------------|-----------------------------------------------------------------------------|
| `id`           | BIGINT (PK)      | Unique identifier for the POS terminal.                                     |
| `terminal_code`| VARCHAR(100)     | Unique code/ID used by provider and system for this terminal.               |
| `admin_unit_id`| BIGINT (FK)      | FK to `administrative_units.id`; assembly/zone owning the device.          |
| `serial_number`| VARCHAR(100)     | Manufacturer or provider serial number.                                     |
| `model`        | VARCHAR(100)     | Device model or type.                                                       |
| `status`       | VARCHAR(30)      | `ACTIVE`, `INACTIVE`, `LOST`, `RETIRED`.                                    |
| `created_at`   | timestamptz      | When the terminal was registered.                                           |

---

### 5.4 `pos_terminal_assignments`

| Field               | Data Type        | Description                                                                 |
|---------------------|------------------|-----------------------------------------------------------------------------|
| `id`                | BIGINT (PK)      | Unique identifier for the assignment record.                                |
| `terminal_id`       | BIGINT (FK)      | FK to `pos_terminals.id`; terminal being assigned.                          |
| `assigned_to_user_id`| BIGINT (FK)     | FK to `users.id`; officer or cashier responsible during this period.        |
| `assigned_from`     | timestamptz      | Start time of the assignment.                                               |
| `assigned_to`       | timestamptz      | End time of the assignment; `NULL` if currently active.                     |
| `status`            | VARCHAR(30)      | `ACTIVE` or `ENDED`.                                                        |

---

### 5.5 `pos_transactions`

| Field             | Data Type        | Description                                                                 |
|-------------------|------------------|-----------------------------------------------------------------------------|
| `id`              | BIGINT (PK)      | Unique identifier for the POS transaction record.                           |
| `terminal_id`     | BIGINT (FK)      | FK to `pos_terminals.id`; terminal used.                                    |
| `provider_reference`| VARCHAR(255)   | Unique provider transaction reference (mapped to `payments.provider_reference`). |
| `card_scheme`     | VARCHAR(50)      | Card scheme or wallet type (e.g. VISA, MTN_MOMO); nullable.                 |
| `amount`          | NUMERIC(18,2)    | Amount processed by POS.                                                    |
| `currency`        | VARCHAR(10)      | Currency used (e.g. GHS).                                                   |
| `status`          | VARCHAR(20)      | `APPROVED`, `DECLINED`, `REVERSED`.                                         |
| `transaction_time`| timestamptz      | Time of the transaction on POS.                                             |
| `raw_payload`     | JSONB            | Raw response payload for audit and reconciliation.                           |

---

### 5.6 `receipts`

| Field            | Data Type        | Description                                                                 |
|------------------|------------------|-----------------------------------------------------------------------------|
| `id`             | BIGINT (PK)      | Unique identifier for the receipt.                                          |
| `payment_id`     | BIGINT (FK)      | FK to `payments.id`; payment this receipt validates.                        |
| `receipt_number` | VARCHAR(100)     | Unique receipt number visible to citizens and auditors.                     |
| `qr_code_payload`| TEXT             | Encoded data used in QR; used to verify receipt online.                     |
| `issued_channel` | VARCHAR(30)      | `MOBILE_APP`, `PORTAL`, `POS`, `BACK_OFFICE`.                               |
| `issued_at`      | timestamptz      | When the receipt was issued.                                                |
| `voided`         | BOOLEAN          | Whether the receipt has been voided.                                        |
| `voided_at`      | timestamptz      | When the receipt was voided; `NULL` if not voided.                          |
| `voided_by_user_id`| BIGINT (FK)    | FK to `users.id`; who voided the receipt; `NULL` if never voided.          |

---

## 6. Cash Management

### 6.1 `officer_cash_sessions`

| Field              | Data Type        | Description                                                                 |
|--------------------|------------------|-----------------------------------------------------------------------------|
| `id`               | BIGINT (PK)      | Unique identifier for the cash session.                                     |
| `officer_id`       | BIGINT (FK)      | FK to `users.id`; officer collecting cash.                                  |
| `admin_unit_id`    | BIGINT (FK)      | FK to `administrative_units.id`; assembly/zone of the officer.             |
| `opened_at`        | timestamptz      | When the cash session was opened.                                           |
| `closed_at`        | timestamptz      | When the session was closed; `NULL` if still open.                          |
| `expected_cash_total`| NUMERIC(18,2)  | System‑calculated total from recorded cash payments.                        |
| `declared_cash_total`| NUMERIC(18,2)  | Amount the officer declares physically.                                     |
| `variance_amount`  | NUMERIC(18,2)    | Difference between expected and declared totals.                             |
| `status`           | VARCHAR(20)      | `OPEN`, `CLOSED`, or `VERIFIED`.                                            |

---

### 6.2 `cash_handovers`

| Field              | Data Type        | Description                                                                 |
|--------------------|------------------|-----------------------------------------------------------------------------|
| `id`               | BIGINT (PK)      | Unique identifier for the cash handover.                                    |
| `cash_session_id`  | BIGINT (FK)      | FK to `officer_cash_sessions.id`; session being handed over.                |
| `received_by_user_id`| BIGINT (FK)    | FK to `users.id`; treasury/staff who received the cash.                     |
| `received_amount`  | NUMERIC(18,2)    | Amount of cash received.                                                    |
| `received_at`      | timestamptz      | When the cash was received.                                                 |
| `verification_status`| VARCHAR(20)    | `PENDING`, `VERIFIED`, or `DISPUTED`.                                       |

---

## 7. Corrections, Disputes & Enforcement

### 7.1 `obligation_adjustment_requests`

| Field                 | Data Type        | Description                                                                 |
|-----------------------|------------------|-----------------------------------------------------------------------------|
| `id`                  | BIGINT (PK)      | Unique identifier for the adjustment request.                               |
| `obligation_id`       | BIGINT (FK)      | FK to `obligations.id`; obligation being adjusted.                          |
| `requested_by_user_id`| BIGINT (FK)      | FK to `users.id`; officer who requested the adjustment.                     |
| `requested_action`    | VARCHAR(20)      | `CANCEL`, `REDUCE`, or `WRITE_OFF`.                                         |
| `proposed_amount`     | NUMERIC(18,2)    | New amount or reduction amount; `NULL` if not applicable.                   |
| `reason`              | TEXT             | Justification for the change.                                               |
| `status`              | VARCHAR(20)      | `PENDING`, `APPROVED`, `REJECTED`.                                          |
| `created_at`          | timestamptz      | When the request was created.                                               |
| `approved_by_user_id` | BIGINT (FK)      | FK to `users.id`; supervisor who approved/rejected.                         |
| `approved_at`         | timestamptz      | When the request was approved/rejected; `NULL` if pending.                  |

---

### 7.2 `obligation_adjustments`

| Field              | Data Type        | Description                                                                 |
|--------------------|------------------|-----------------------------------------------------------------------------|
| `id`               | BIGINT (PK)      | Unique identifier for the final adjustment record.                          |
| `obligation_id`    | BIGINT (FK)      | FK to `obligations.id`; obligation being adjusted.                          |
| `adjustment_type`  | VARCHAR(20)      | `REDUCTION` or `WRITE_OFF`.                                                 |
| `amount`           | NUMERIC(18,2)    | Adjustment amount applied.                                                  |
| `created_by_user_id`| BIGINT (FK)     | FK to `users.id`; who created the adjustment record.                        |
| `approved_by_user_id`| BIGINT (FK)    | FK to `users.id`; who approved the adjustment.                              |
| `created_at`       | timestamptz      | When the adjustment was created.                                            |

---

### 7.3 `disputes`

| Field             | Data Type        | Description                                                                 |
|-------------------|------------------|-----------------------------------------------------------------------------|
| `id`              | BIGINT (PK)      | Unique identifier for the dispute.                                          |
| `obligation_id`   | BIGINT (FK)      | FK to `obligations.id`; obligation being disputed.                          |
| `payer_id`        | BIGINT (FK)      | FK to `payers.id`; who raised the dispute.                                  |
| `reason`          | TEXT             | Reason for the dispute.                                                     |
| `status`          | VARCHAR(20)      | `OPEN`, `UNDER_REVIEW`, `APPROVED`, `REJECTED`.                             |
| `submitted_at`    | timestamptz      | When the dispute was submitted.                                             |
| `resolved_at`     | timestamptz      | When the dispute was resolved; `NULL` if not resolved.                      |
| `resolved_by_user_id`| BIGINT (FK)   | FK to `users.id`; who resolved the dispute.                                 |

---

### 7.4 `enforcement_actions`

| Field           | Data Type        | Description                                                                 |
|-----------------|------------------|-----------------------------------------------------------------------------|
| `id`            | BIGINT (PK)      | Unique identifier for the enforcement action.                               |
| `agent_id`      | BIGINT (FK)      | FK to `users.id`; enforcement agent performing the action.                  |
| `asset_id`      | BIGINT (FK)      | FK to `assets.id`; asset targeted by enforcement.                           |
| `obligation_id` | BIGINT (FK)      | FK to `obligations.id`; related obligation; `NULL` for general inspections. |
| `admin_unit_id` | BIGINT (FK)      | FK to `administrative_units.id`; where the action occurred.                 |
| `gps_lat`       | NUMERIC(10,7)    | Latitude captured at time of enforcement.                                   |
| `gps_lng`       | NUMERIC(10,7)    | Longitude captured at time of enforcement.                                  |
| `action_type`   | VARCHAR(30)      | `INSPECTION`, `NOTICE_ISSUED`, `WARNING`, `SEIZURE`.                        |
| `notes`         | TEXT             | Free‑text notes describing the action.                                      |
| `created_at`    | timestamptz      | When the action was recorded.                                               |

---

### 7.5 `evidence_files`

| Field                | Data Type        | Description                                                                 |
|----------------------|------------------|-----------------------------------------------------------------------------|
| `id`                 | BIGINT (PK)      | Unique identifier for the evidence file record.                             |
| `enforcement_action_id`| BIGINT (FK)    | FK to `enforcement_actions.id`; which action this evidence belongs to.      |
| `file_url`           | TEXT             | Storage URL or path to the evidence file.                                   |
| `file_hash`          | VARCHAR(255)     | Hash of the file for tamper detection.                                      |
| `created_at`         | timestamptz      | When the evidence record was created.                                       |

---

## 8. Audit & Approvals

### 8.1 `audit_logs`

| Field         | Data Type        | Description                                                                 |
|---------------|------------------|-----------------------------------------------------------------------------|
| `id`          | BIGINT (PK)      | Unique identifier for the audit log entry.                                  |
| `user_id`     | BIGINT (FK)      | FK to `users.id`; who performed the action.                                 |
| `admin_unit_id`| BIGINT (FK)     | FK to `administrative_units.id`; area context of the action.                |
| `action_type` | VARCHAR(50)      | Action performed (e.g. `CREATE`, `UPDATE`, `LOGIN`).                         |
| `entity_type` | VARCHAR(50)      | Entity affected (e.g. `OBLIGATION`, `PAYMENT`).                              |
| `entity_id`   | BIGINT           | ID of the entity affected.                                                  |
| `old_data`    | JSONB            | Previous state snapshot; may be `NULL` for creations.                        |
| `new_data`    | JSONB            | New state snapshot; may be `NULL` for deletions.                             |
| `created_at`  | timestamptz      | When the action occurred.                                                   |

---

### 8.2 `approvals`

| Field         | Data Type        | Description                                                                 |
|---------------|------------------|-----------------------------------------------------------------------------|
| `id`          | BIGINT (PK)      | Unique identifier for the approval record.                                  |
| `entity_type` | VARCHAR(50)      | Type of entity requiring approval (e.g. `OBLIGATION_ADJUSTMENT_REQUEST`).   |
| `entity_id`   | BIGINT           | ID of the entity requiring approval.                                        |
| `requested_by`| BIGINT (FK)      | FK to `users.id`; who requested the action.                                 |
| `approved_by` | BIGINT (FK)      | FK to `users.id`; who approved/rejected; `NULL` if pending.                 |
| `status`      | VARCHAR(20)      | `PENDING`, `APPROVED`, `REJECTED`.                                          |
| `requested_at`| timestamptz      | When approval was requested.                                                |
| `approved_at` | timestamptz      | When approval decision was made; `NULL` if pending.                         |

---

## 9. Reporting Snapshots (Registers)

> These can be implemented as materialized views or periodically refreshed tables based on `obligations`, `payments`, and `payment_allocations`.

### 9.1 `tax_registry_snapshot`

| Field                    | Data Type        | Description                                                                 |
|--------------------------|------------------|-----------------------------------------------------------------------------|
| `id`                     | BIGINT (PK)      | Unique identifier for the snapshot row.                                     |
| `asset_id`               | BIGINT (FK)      | FK to `assets.id`; the asset this row summarizes.                           |
| `payer_id`               | BIGINT (FK)      | FK to `payers.id`; payer responsible.                                       |
| `admin_unit_id`          | BIGINT (FK)      | FK to `administrative_units.id`; area of the asset.                         |
| `last_payment_date`      | DATE             | Date of the most recent payment.                                            |
| `last_payment_amount`    | NUMERIC(18,2)    | Amount of the most recent payment.                                          |
| `last_payment_channel`   | VARCHAR(20)      | Channel of the most recent payment (e.g. MOMO).                             |
| `next_due_date`          | DATE             | Next upcoming due date for tax obligations.                                 |
| `next_due_amount`        | NUMERIC(18,2)    | Amount due by the next due date.                                            |
| `total_outstanding`      | NUMERIC(18,2)    | Sum of all unpaid obligations.                                              |
| `total_overdue`          | NUMERIC(18,2)    | Sum of all overdue obligations.                                             |
| `open_obligations_count` | INTEGER          | Count of open (non‑paid) obligations.                                       |
| `overdue_obligations_count`| INTEGER        | Count of overdue obligations.                                               |
| `compliance_status`      | VARCHAR(20)      | `COMPLIANT`, `DUE_SOON`, `OVERDUE`, `IN_DISPUTE`.                           |
| `snapshot_at`            | timestamptz      | Timestamp when this snapshot row was last refreshed.                        |

---

### 9.2 `fine_registry_snapshot`

| Field                    | Data Type        | Description                                                                 |
|--------------------------|------------------|-----------------------------------------------------------------------------|
| `id`                     | BIGINT (PK)      | Unique identifier for the snapshot row.                                     |
| `asset_id`               | BIGINT (FK)      | FK to `assets.id`; asset this row summarizes.                               |
| `payer_id`               | BIGINT (FK)      | FK to `payers.id`; payer responsible.                                       |
| `admin_unit_id`          | BIGINT (FK)      | FK to `administrative_units.id`; area of the asset.                         |
| `last_payment_date`      | DATE             | Date of most recent fine payment.                                           |
| `total_outstanding`      | NUMERIC(18,2)    | Sum of unpaid fines.                                                        |
| `total_overdue`          | NUMERIC(18,2)    | Sum of overdue fines.                                                       |
| `open_obligations_count` | INTEGER          | Count of open fine obligations.                                             |
| `overdue_obligations_count`| INTEGER        | Count of overdue fine obligations.                                          |
| `compliance_status`      | VARCHAR(20)      | `COMPLIANT`, `DUE_SOON`, `OVERDUE`, `IN_DISPUTE`.                           |
| `latest_evidence_url`    | TEXT             | URL of the most recent enforcement evidence (if any).                       |
| `latest_evidence_description`| TEXT         | Short description of the most recent evidence or notice.                    |
| `snapshot_at`            | timestamptz      | Timestamp when this snapshot row was last refreshed.                        |

---

This schema is designed for a **government‑grade, auditable national POS and tax platform**, with strong anti‑corruption controls and full lifecycle traceability of obligations, payments, and enforcement.

