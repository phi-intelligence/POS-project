## POS Schema Separation – Single DB, One Schema per Service

This document applies the **Schema Separation Plan (Single DB, One Schema per Service)** and maps the existing logical database design in `docs/data/POS-Database-Schema.md` into **PostgreSQL schemas per microservice**.

- Physical deployment: **one PostgreSQL database**, multiple schemas.
- Logical deployment: **one microservice per schema**, as defined in the stretcher and PRD.
- Source of field-level truth: column definitions are copied/adapted from `Plan/POS-Database-Schema.md`.

> For complete logical descriptions, see: [POS-Database-Schema.md](../data/POS-Database-Schema.md).  
> For product-level requirements, see: [POS-Platform-PRD.md](../architecture/POS-Platform-PRD.md).

---

## 1. Schema-to-Service Mapping

| Schema name      | Microservice                  | Tables                                                                                                                             | Implemented by module                        |
| ---------------- | ----------------------------- | ---------------------------------------------------------------------------------------------------------------------------------- | -------------------------------------------- |
| `identity`       | Authentication & User Service | `users`, `roles`, `role_permissions`                                                                                               | `services/identity-service`                  |
| `administrative` | Administrative Service        | `administrative_units`, `rule_versions`                                                                                            | `services/administrative-service`            |
| `payer`          | Payer Service                 | `payers`, `assets`                                                                                                                 | `services/payer-service`                     |
| `obligation`     | Obligation Service            | `obligations`, `obligation_events`, `disputes`, `obligation_adjustment_requests`, `obligation_adjustments`, `approvals`, `receipts` | `services/obligation-service`                |
| `payment`        | Payment Service               | `payments`, `payment_allocations`, `officer_cash_sessions`, `cash_handovers`                                                       | `services/payment-service`                   |
| `device`         | Device Service                | `pos_terminals`, `pos_terminal_assignments`, `pos_transactions`                                                                    | `services/device-service`                    |
| `enforcement`    | Enforcement Service           | `enforcement_actions`, `evidence_files`                                                                                            | `services/enforcement-service`               |
| `reporting`      | Reporting Service             | `audit_logs`, `tax_registry_snapshot`, `fine_registry_snapshot`                                                                    | `services/reporting-service`                 |

**Note**: The Notification Service (implemented in `services/notification-service`) persists its data primarily in **Redis** and messaging infrastructure (Kafka / RabbitMQ). It does **not** own relational tables in PostgreSQL and therefore has no schema here.

---

## 2. Cross-Schema Dependencies

With **one database and multiple schemas**, there are two patterns for cross-service relationships:

- **Option A – Cross-schema foreign keys**  
  Use `schema_name.table_name` in FK definitions. PostgreSQL allows FKs across schemas.
  - **Pros**: Centralized referential integrity; easier reporting queries.
  - **Cons**: Schema (and deployment) coupling; care needed with migration order.

- **Option B – Logical references only (no FKs)**  
  Use ID fields without FK constraints (e.g. `payer_id BIGINT` referring logically to `payer.payers(id)`).
  - **Pros**: Stronger service isolation; easier future move to separate databases.
  - **Cons**: Integrity enforced in application layer and by events.

For the **single-database phase**, this design **recommends Option A** (cross-schema FKs) to keep strong integrity, with a note that FKs can be removed later if you move to **separate databases per service**.

### 2.1 Key Cross-Schema References

- **identity**  
  - `identity.users.admin_unit_id` → `administrative.administrative_units.id`  
  - `identity.users.role_id` → `identity.roles.id`

- **payer**  
  - `payer.payers.primary_admin_unit_id` → `administrative.administrative_units.id`  
  - `payer.assets.admin_unit_id` → `administrative.administrative_units.id`

- **obligation**  
  - `obligation.obligations.asset_id` → `payer.assets.id`  
  - `obligation.obligations.payer_id` → `payer.payers.id`  
  - `obligation.obligations.admin_unit_id` → `administrative.administrative_units.id`  
  - `obligation.obligations.rule_version_id` → `administrative.rule_versions.id`  
  - `obligation.obligation_events.changed_by_user_id` → `identity.users.id`  
  - `obligation.disputes.payer_id` → `payer.payers.id`  
  - `obligation.obligation_adjustment_requests.requested_by_user_id` → `identity.users.id`  
  - `obligation.obligation_adjustment_requests.approved_by_user_id` → `identity.users.id`  
  - `obligation.obligation_adjustments.created_by_user_id` → `identity.users.id`  
  - `obligation.obligation_adjustments.approved_by_user_id` → `identity.users.id`  
  - `obligation.approvals.requested_by` → `identity.users.id`  
  - `obligation.approvals.approved_by` → `identity.users.id`  
  - `obligation.receipts.payment_id` → `payment.payments.id`

- **payment**  
  - `payment.payments.payer_id` → `payer.payers.id`  
  - `payment.payments.admin_unit_id` → `administrative.administrative_units.id`  
  - `payment.payments.initiated_by_user_id` → `identity.users.id`  
  - `payment.payments.cash_session_id` → `payment.officer_cash_sessions.id`  
  - `payment.payment_allocations.obligation_id` → `obligation.obligations.id`  
  - `payment.officer_cash_sessions.officer_id` → `identity.users.id`  
  - `payment.officer_cash_sessions.admin_unit_id` → `administrative.administrative_units.id`  
  - `payment.cash_handovers.cash_session_id` → `payment.officer_cash_sessions.id`  
  - `payment.cash_handovers.received_by_user_id` → `identity.users.id`

- **device**  
  - `device.pos_terminals.admin_unit_id` → `administrative.administrative_units.id`  
  - `device.pos_terminal_assignments.terminal_id` → `device.pos_terminals.id`  
  - `device.pos_terminal_assignments.assigned_to_user_id` → `identity.users.id`

- **enforcement**  
  - `enforcement.enforcement_actions.agent_id` → `identity.users.id`  
  - `enforcement.enforcement_actions.asset_id` → `payer.assets.id`  
  - `enforcement.enforcement_actions.obligation_id` → `obligation.obligations.id`  
  - `enforcement.enforcement_actions.admin_unit_id` → `administrative.administrative_units.id`  
  - `enforcement.evidence_files.enforcement_action_id` → `enforcement.enforcement_actions.id`

- **reporting**  
  - `reporting.audit_logs.user_id` → `identity.users.id`  
  - `reporting.audit_logs.admin_unit_id` → `administrative.administrative_units.id`  
  - `reporting.tax_registry_snapshot.asset_id` → `payer.assets.id`  
  - `reporting.tax_registry_snapshot.payer_id` → `payer.payers.id`  
  - `reporting.tax_registry_snapshot.admin_unit_id` → `administrative.administrative_units.id`  
  - `reporting.fine_registry_snapshot.asset_id` → `payer.assets.id`  
  - `reporting.fine_registry_snapshot.payer_id` → `payer.payers.id`  
  - `reporting.fine_registry_snapshot.admin_unit_id` → `administrative.administrative_units.id`

---

## 3. Schemas and Tables

Below, each schema lists its tables and **full field definitions**, adapted from [POS-Database-Schema.md](../data/POS-Database-Schema.md).  
In SQL migrations, always **schema-qualify** tables, e.g.:

```sql
CREATE SCHEMA IF NOT EXISTS identity;
CREATE TABLE identity.users (...);
```

### 3.1 `identity` Schema – Authentication & User Service

#### 3.1.1 `identity.roles`

| Field        | Data Type    | Description                                             |
|-------------|--------------|---------------------------------------------------------|
| `id`        | BIGINT (PK)  | Unique identifier for the role.                        |
| `name`      | VARCHAR(100) | Role name (e.g. ADMIN, FIELD_AGENT, AUDITOR).         |
| `description` | TEXT       | Human‑readable explanation of the role.                |

#### 3.1.2 `identity.role_permissions`

| Field            | Data Type    | Description                                                        |
|------------------|--------------|--------------------------------------------------------------------|
| `id`             | BIGINT (PK)  | Unique identifier for the permission mapping.                      |
| `role_id`        | BIGINT (FK)  | FK to `identity.roles.id`.                                        |
| `permission_key` | VARCHAR(100) | Machine‑readable permission key (e.g. `OBLIGATION.WRITE_OFF`).     |

#### 3.1.3 `identity.users`

| Field           | Data Type        | Description                                                                 |
|-----------------|------------------|-----------------------------------------------------------------------------|
| `id`            | BIGINT (PK)      | Unique identifier for the user (officer, admin, auditor, etc.).            |
| `username`      | VARCHAR(100)     | Login username; must be unique.                                            |
| `password_hash` | TEXT             | Password hash (never store plaintext passwords).                           |
| `role_id`       | BIGINT (FK)      | FK to `identity.roles.id`; defines role and permissions.                   |
| `admin_unit_id` | BIGINT (FK)      | FK to `administrative.administrative_units.id`; scopes user to an area.    |
| `status`        | VARCHAR(20)      | `ACTIVE` or `SUSPENDED`.                                                   |
| `created_at`    | timestamptz      | When the user account was created.                                         |

---

### 3.2 `administrative` Schema – Administrative Service

#### 3.2.1 `administrative.administrative_units`

| Field        | Data Type        | Description                                                                                 |
|--------------|------------------|---------------------------------------------------------------------------------------------|
| `id`         | BIGINT (PK)      | Unique identifier for the administrative unit.                                              |
| `name`       | VARCHAR(255)     | Human‑readable name (e.g. Ghana, Greater Accra, Tema, Community 1).                        |
| `type`       | VARCHAR(50)      | Type of unit: `COUNTRY`, `REGION`, `METRO`, `DISTRICT`, `MUNICIPAL`, `ZONE`.               |
| `parent_id`  | BIGINT (FK)      | Parent administrative unit in the hierarchy; `NULL` for top‑level country.                 |
| `code`       | VARCHAR(50)      | Short code used in reporting or integration (e.g. TMA‑C1).                                 |
| `is_active`  | BOOLEAN          | Whether this unit is currently active.                                                      |
| `created_at` | timestamptz      | When the unit record was created.                                                          |

#### 3.2.2 `administrative.rule_versions`

| Field            | Data Type        | Description                                                                 |
|------------------|------------------|-----------------------------------------------------------------------------|
| `id`             | BIGINT (PK)      | Unique identifier for the rule version.                                     |
| `rule_name`      | VARCHAR(255)     | Human‑readable name for the rule (e.g. \"Property Rate – Residential\").     |
| `admin_unit_id`  | BIGINT (FK)      | FK to `administrative.administrative_units.id`; `NULL` if rule is national. |
| `asset_type`     | VARCHAR(30)      | Asset type this rule applies to (e.g. PROPERTY, VEHICLE).                   |
| `obligation_type`| VARCHAR(20)      | `TAX`, `FEE`, or `FINE`.                                                   |
| `calculation_type`| VARCHAR(20)     | `FIXED`, `PERCENTAGE`, or `FORMULA`.                                       |
| `base_value`     | NUMERIC(18,2)    | Base amount or base rate used for calculation.                              |
| `effective_from` | DATE             | Date from which this rule version is valid.                                 |
| `effective_to`   | DATE             | Date until which this rule version is valid; `NULL` if still active.       |
| `version_number` | INTEGER          | Sequential version number for the rule.                                     |
| `created_at`     | timestamptz      | When this rule version was created.                                         |

---

### 3.3 `payer` Schema – Payer Service

#### 3.3.1 `payer.payers`

| Field                  | Data Type        | Description                                                              |
|------------------------|------------------|--------------------------------------------------------------------------|
| `id`                   | BIGINT (PK)      | Unique identifier for the payer.                                         |
| `payer_type`           | VARCHAR(20)      | `INDIVIDUAL` or `BUSINESS`.                                             |
| `full_name`            | VARCHAR(255)     | Full legal name of the individual or contact person.                     |
| `business_name`        | VARCHAR(255)     | Registered business name; `NULL` for individuals.                        |
| `ghana_card_no`        | VARCHAR(50)      | Ghana Card number (indexed); may be `NULL` for some businesses.          |
| `tin_number`           | VARCHAR(50)      | Tax Identification Number (indexed).                                     |
| `phone`                | VARCHAR(50)      | Primary phone number (indexed for quick lookup).                         |
| `email`                | VARCHAR(255)     | Email address for electronic communication.                              |
| `primary_admin_unit_id`| BIGINT (FK)      | FK to `administrative.administrative_units.id`; main area for the payer. |
| `status`               | VARCHAR(20)      | `ACTIVE` or `INACTIVE`.                                                  |
| `created_at`           | timestamptz      | When the payer record was created.                                       |

#### 3.3.2 `payer.assets`

| Field             | Data Type        | Description                                                                 |
|-------------------|------------------|-----------------------------------------------------------------------------|
| `id`              | BIGINT (PK)      | Unique identifier for the asset.                                            |
| `asset_type`      | VARCHAR(30)      | `PROPERTY`, `VEHICLE`, `STALL`, `LICENSE`, `TERMINAL`, etc.                |
| `owner_payer_id`  | BIGINT (FK)      | FK to `payer.payers.id`; payer legally responsible for this asset.         |
| `admin_unit_id`   | BIGINT (FK)      | FK to `administrative.administrative_units.id`; physical location.         |
| `unique_identifier`| VARCHAR(255)    | Real‑world identifier (plot code, license number, stall ID, etc.).        |
| `status`          | VARCHAR(30)      | Operational status of the asset (e.g. ACTIVE, INACTIVE).                   |
| `created_at`      | timestamptz      | When the asset was registered.                                             |

---

### 3.4 `obligation` Schema – Obligation Service

#### 3.4.1 `obligation.obligations`

| Field            | Data Type        | Description                                                                 |
|------------------|------------------|-----------------------------------------------------------------------------|
| `id`             | BIGINT (PK)      | Unique identifier for the obligation (debt).                                |
| `asset_id`       | BIGINT (FK)      | FK to `payer.assets.id`; asset the obligation is tied to.                   |
| `payer_id`       | BIGINT (FK)      | FK to `payer.payers.id`; who owes the obligation.                           |
| `admin_unit_id`  | BIGINT (FK)      | FK to `administrative.administrative_units.id`; revenue‑owning area.       |
| `obligation_type`| VARCHAR(20)      | `TAX`, `FEE`, or `FINE`.                                                   |
| `rule_version_id`| BIGINT (FK)      | FK to `administrative.rule_versions.id`; rule used to calculate.           |
| `principal_amount`| NUMERIC(18,2)   | Base amount before penalties.                                              |
| `penalty_amount` | NUMERIC(18,2)    | Penalties or surcharges applied.                                           |
| `total_amount`   | NUMERIC(18,2)    | Total amount due (principal + penalty).                                    |
| `currency`       | VARCHAR(10)      | ISO currency code (e.g. GHS).                                              |
| `due_date`       | DATE             | Date the obligation is due.                                                |
| `status`         | VARCHAR(30)      | `OPEN`, `OVERDUE`, `PAID`, `PARTIALLY_PAID`, `IN_DISPUTE`, `WAIVED`, `CANCELLED`. |
| `created_at`     | timestamptz      | When the obligation was created.                                           |
| `updated_at`     | timestamptz      | Last time the obligation record was updated.                               |

#### 3.4.2 `obligation.obligation_events`

| Field              | Data Type        | Description                                                                 |
|--------------------|------------------|-----------------------------------------------------------------------------|
| `id`               | BIGINT (PK)      | Unique identifier for the obligation event.                                 |
| `obligation_id`    | BIGINT (FK)      | FK to `obligation.obligations.id`; obligation whose lifecycle changed.     |
| `event_type`       | VARCHAR(30)      | `CREATED`, `MARKED_OVERDUE`, `PENALTY_APPLIED`, `PAID`, `PARTIALLY_PAID`, `CANCELLED`, `ADJUSTED`, `DISPUTED`. |
| `previous_status`  | VARCHAR(30)      | Status before this event.                                                   |
| `new_status`       | VARCHAR(30)      | Status after this event.                                                    |
| `changed_by_user_id`| BIGINT (FK)     | FK to `identity.users.id`; user who triggered the change.                   |
| `created_at`       | timestamptz      | When the event was recorded.                                                |

#### 3.4.3 `obligation.disputes`

| Field              | Data Type        | Description                                                                 |
|--------------------|------------------|-----------------------------------------------------------------------------|
| `id`               | BIGINT (PK)      | Unique identifier for the dispute.                                          |
| `obligation_id`    | BIGINT (FK)      | FK to `obligation.obligations.id`; obligation being disputed.              |
| `payer_id`         | BIGINT (FK)      | FK to `payer.payers.id`; who raised the dispute.                            |
| `reason`           | TEXT             | Reason for the dispute.                                                     |
| `status`           | VARCHAR(20)      | `OPEN`, `UNDER_REVIEW`, `APPROVED`, `REJECTED`.                             |
| `submitted_at`     | timestamptz      | When the dispute was submitted.                                             |
| `resolved_at`      | timestamptz      | When the dispute was resolved; `NULL` if not resolved.                      |
| `resolved_by_user_id`| BIGINT (FK)    | FK to `identity.users.id`; who resolved the dispute.                        |

#### 3.4.4 `obligation.obligation_adjustment_requests`

| Field                  | Data Type        | Description                                                                 |
|------------------------|------------------|-----------------------------------------------------------------------------|
| `id`                   | BIGINT (PK)      | Unique identifier for the adjustment request.                               |
| `obligation_id`        | BIGINT (FK)      | FK to `obligation.obligations.id`; obligation being adjusted.              |
| `requested_by_user_id` | BIGINT (FK)      | FK to `identity.users.id`; officer who requested the adjustment.           |
| `requested_action`     | VARCHAR(20)      | `CANCEL`, `REDUCE`, or `WRITE_OFF`.                                         |
| `proposed_amount`      | NUMERIC(18,2)    | New amount or reduction amount; `NULL` if not applicable.                   |
| `reason`               | TEXT             | Justification for the change.                                               |
| `status`               | VARCHAR(20)      | `PENDING`, `APPROVED`, `REJECTED`.                                          |
| `created_at`           | timestamptz      | When the request was created.                                               |
| `approved_by_user_id`  | BIGINT (FK)      | FK to `identity.users.id`; supervisor who approved/rejected.               |
| `approved_at`          | timestamptz      | When the request was approved/rejected; `NULL` if pending.                  |

#### 3.4.5 `obligation.obligation_adjustments`

| Field               | Data Type        | Description                                                                 |
|---------------------|------------------|-----------------------------------------------------------------------------|
| `id`                | BIGINT (PK)      | Unique identifier for the final adjustment record.                          |
| `obligation_id`     | BIGINT (FK)      | FK to `obligation.obligations.id`; obligation being adjusted.              |
| `adjustment_type`   | VARCHAR(20)      | `REDUCTION` or `WRITE_OFF`.                                                 |
| `amount`            | NUMERIC(18,2)    | Adjustment amount applied.                                                  |
| `created_by_user_id`| BIGINT (FK)      | FK to `identity.users.id`; who created the adjustment record.              |
| `approved_by_user_id`| BIGINT (FK)     | FK to `identity.users.id`; who approved the adjustment.                     |
| `created_at`        | timestamptz      | When the adjustment was created.                                            |

#### 3.4.6 `obligation.approvals`

| Field         | Data Type        | Description                                                                 |
|--------------|------------------|-----------------------------------------------------------------------------|
| `id`         | BIGINT (PK)      | Unique identifier for the approval record.                                  |
| `entity_type`| VARCHAR(50)      | Type of entity requiring approval (e.g. `OBLIGATION_ADJUSTMENT_REQUEST`).   |
| `entity_id`  | BIGINT           | ID of the entity requiring approval.                                        |
| `requested_by`| BIGINT (FK)     | FK to `identity.users.id`; who requested the action.                        |
| `approved_by` | BIGINT (FK)     | FK to `identity.users.id`; who approved/rejected; `NULL` if pending.       |
| `status`     | VARCHAR(20)      | `PENDING`, `APPROVED`, `REJECTED`.                                          |
| `requested_at`| timestamptz     | When approval was requested.                                                |
| `approved_at` | timestamptz     | When approval decision was made; `NULL` if pending.                         |

#### 3.4.7 `obligation.receipts`

| Field              | Data Type        | Description                                                                 |
|--------------------|------------------|-----------------------------------------------------------------------------|
| `id`               | BIGINT (PK)      | Unique identifier for the receipt.                                          |
| `payment_id`       | BIGINT (FK)      | FK to `payment.payments.id`; payment this receipt validates.               |
| `receipt_number`   | VARCHAR(100)     | Unique receipt number visible to citizens and auditors.                     |
| `qr_code_payload`  | TEXT             | Encoded data used in QR; used to verify receipt online.                     |
| `issued_channel`   | VARCHAR(30)      | `MOBILE_APP`, `PORTAL`, `POS`, `BACK_OFFICE`.                               |
| `issued_at`        | timestamptz      | When the receipt was issued.                                                |
| `voided`           | BOOLEAN          | Whether the receipt has been voided.                                        |
| `voided_at`        | timestamptz      | When the receipt was voided; `NULL` if not voided.                          |
| `voided_by_user_id`| BIGINT (FK)      | FK to `identity.users.id`; who voided the receipt; `NULL` if never voided. |

---

### 3.5 `payment` Schema – Payment Service

#### 3.5.1 `payment.payments`

| Field                | Data Type        | Description                                                                 |
|----------------------|------------------|-----------------------------------------------------------------------------|
| `id`                 | BIGINT (PK)      | Unique identifier for the payment.                                          |
| `payer_id`           | BIGINT (FK)      | FK to `payer.payers.id`; who made the payment.                              |
| `admin_unit_id`      | BIGINT (FK)      | FK to `administrative.administrative_units.id`; revenue‑owning area.       |
| `channel`            | VARCHAR(20)      | `MOMO`, `POS`, `PORTAL`, `BANK`, or `CASH`.                                 |
| `initiated_by_user_id`| BIGINT (FK)     | FK to `identity.users.id`; officer/user who initiated payment; `NULL` for self‑service. |
| `initiated_via`      | VARCHAR(30)      | `SELF_SERVICE` or `OFFICER_DEVICE`.                                         |
| `provider_name`      | VARCHAR(100)     | Name of payment provider (telco, bank, POS acquirer).                       |
| `provider_reference` | VARCHAR(255)     | Unique provider reference for digital/POS payments.                         |
| `amount`             | NUMERIC(18,2)    | Total payment amount.                                                       |
| `currency`           | VARCHAR(10)      | ISO currency code (e.g. GHS).                                               |
| `status`             | VARCHAR(20)      | `PENDING`, `CONFIRMED`, `FAILED`, `REVERSED`.                               |
| `custody_status`     | VARCHAR(30)      | For cash: `WITH_OFFICER`, `SUBMITTED_TO_TREASURY`, `VERIFIED`; `NULL` for digital. |
| `cash_session_id`    | BIGINT (FK)      | FK to `payment.officer_cash_sessions.id`; only for cash payments.           |
| `initiated_at`       | timestamptz      | When the payment was initiated.                                             |
| `confirmed_at`       | timestamptz      | When the payment was confirmed by provider or treasury.                     |

#### 3.5.2 `payment.payment_allocations`

| Field            | Data Type        | Description                                                                 |
|------------------|------------------|-----------------------------------------------------------------------------|
| `id`             | BIGINT (PK)      | Unique identifier for the allocation record.                                |
| `payment_id`     | BIGINT (FK)      | FK to `payment.payments.id`; the payment being allocated.                   |
| `obligation_id`  | BIGINT (FK)      | FK to `obligation.obligations.id`; obligation receiving part of the payment.|
| `allocated_amount`| NUMERIC(18,2)   | Amount of the payment allocated to this obligation.                         |
| `created_at`     | timestamptz      | When the allocation was created.                                            |

#### 3.5.3 `payment.officer_cash_sessions`

| Field               | Data Type        | Description                                                                 |
|---------------------|------------------|-----------------------------------------------------------------------------|
| `id`                | BIGINT (PK)      | Unique identifier for the cash session.                                     |
| `officer_id`        | BIGINT (FK)      | FK to `identity.users.id`; officer collecting cash.                         |
| `admin_unit_id`     | BIGINT (FK)      | FK to `administrative.administrative_units.id`; assembly/zone of officer.  |
| `opened_at`         | timestamptz      | When the cash session was opened.                                           |
| `closed_at`         | timestamptz      | When the session was closed; `NULL` if still open.                          |
| `expected_cash_total`| NUMERIC(18,2)   | System‑calculated total from recorded cash payments.                        |
| `declared_cash_total`| NUMERIC(18,2)   | Amount the officer declares physically.                                     |
| `variance_amount`   | NUMERIC(18,2)    | Difference between expected and declared totals.                             |
| `status`            | VARCHAR(20)      | `OPEN`, `CLOSED`, or `VERIFIED`.                                            |

#### 3.5.4 `payment.cash_handovers`

| Field               | Data Type        | Description                                                                 |
|---------------------|------------------|-----------------------------------------------------------------------------|
| `id`                | BIGINT (PK)      | Unique identifier for the cash handover.                                    |
| `cash_session_id`   | BIGINT (FK)      | FK to `payment.officer_cash_sessions.id`; session being handed over.        |
| `received_by_user_id`| BIGINT (FK)     | FK to `identity.users.id`; treasury/staff who received the cash.           |
| `received_amount`   | NUMERIC(18,2)    | Amount of cash received.                                                    |
| `received_at`       | timestamptz      | When the cash was received.                                                 |
| `verification_status`| VARCHAR(20)     | `PENDING`, `VERIFIED`, or `DISPUTED`.                                       |

---

### 3.6 `device` Schema – Device Service

#### 3.6.1 `device.pos_terminals`

| Field          | Data Type        | Description                                                                 |
|----------------|------------------|-----------------------------------------------------------------------------|
| `id`           | BIGINT (PK)      | Unique identifier for the POS terminal.                                     |
| `terminal_code`| VARCHAR(100)     | Unique code/ID used by provider and system for this terminal.               |
| `admin_unit_id`| BIGINT (FK)      | FK to `administrative.administrative_units.id`; assembly/zone owning device.|
| `serial_number`| VARCHAR(100)     | Manufacturer or provider serial number.                                     |
| `model`        | VARCHAR(100)     | Device model or type.                                                       |
| `status`       | VARCHAR(30)      | `ACTIVE`, `INACTIVE`, `LOST`, `RETIRED`.                                    |
| `created_at`   | timestamptz      | When the terminal was registered.                                           |

#### 3.6.2 `device.pos_terminal_assignments`

| Field                | Data Type        | Description                                                                 |
|----------------------|------------------|-----------------------------------------------------------------------------|
| `id`                 | BIGINT (PK)      | Unique identifier for the assignment record.                                |
| `terminal_id`        | BIGINT (FK)      | FK to `device.pos_terminals.id`; terminal being assigned.                   |
| `assigned_to_user_id`| BIGINT (FK)      | FK to `identity.users.id`; officer or cashier responsible during this period.|
| `assigned_from`      | timestamptz      | Start time of the assignment.                                               |
| `assigned_to`        | timestamptz      | End time of the assignment; `NULL` if currently active.                     |
| `status`             | VARCHAR(30)      | `ACTIVE` or `ENDED`.                                                        |

#### 3.6.3 `device.pos_transactions`

| Field               | Data Type        | Description                                                                 |
|---------------------|------------------|-----------------------------------------------------------------------------|
| `id`                | BIGINT (PK)      | Unique identifier for the POS transaction record.                           |
| `terminal_id`       | BIGINT (FK)      | FK to `device.pos_terminals.id`; terminal used.                             |
| `provider_reference`| VARCHAR(255)     | Unique provider transaction reference (mapped to `payment.payments.provider_reference`). |
| `card_scheme`       | VARCHAR(50)      | Card scheme or wallet type (e.g. VISA, MTN_MOMO); nullable.                 |
| `amount`            | NUMERIC(18,2)    | Amount processed by POS.                                                    |
| `currency`          | VARCHAR(10)      | Currency used (e.g. GHS).                                                   |
| `status`            | VARCHAR(20)      | `APPROVED`, `DECLINED`, `REVERSED`.                                         |
| `transaction_time`  | timestamptz      | Time of the transaction on POS.                                             |
| `raw_payload`       | JSONB            | Raw response payload for audit and reconciliation.                           |

---

### 3.7 `enforcement` Schema – Enforcement Service

#### 3.7.1 `enforcement.enforcement_actions`

| Field          | Data Type        | Description                                                                 |
|----------------|------------------|-----------------------------------------------------------------------------|
| `id`           | BIGINT (PK)      | Unique identifier for the enforcement action.                               |
| `agent_id`     | BIGINT (FK)      | FK to `identity.users.id`; enforcement agent performing the action.        |
| `asset_id`     | BIGINT (FK)      | FK to `payer.assets.id`; asset targeted by enforcement.                     |
| `obligation_id`| BIGINT (FK)      | FK to `obligation.obligations.id`; related obligation; `NULL` for general inspections. |
| `admin_unit_id`| BIGINT (FK)      | FK to `administrative.administrative_units.id`; where the action occurred.  |
| `gps_lat`      | NUMERIC(10,7)    | Latitude captured at time of enforcement.                                   |
| `gps_lng`      | NUMERIC(10,7)    | Longitude captured at time of enforcement.                                  |
| `action_type`  | VARCHAR(30)      | `INSPECTION`, `NOTICE_ISSUED`, `WARNING`, `SEIZURE`.                        |
| `notes`        | TEXT             | Free‑text notes describing the action.                                      |
| `created_at`   | timestamptz      | When the action was recorded.                                               |

#### 3.7.2 `enforcement.evidence_files`

| Field                 | Data Type        | Description                                                                 |
|-----------------------|------------------|-----------------------------------------------------------------------------|
| `id`                  | BIGINT (PK)      | Unique identifier for the evidence file record.                             |
| `enforcement_action_id`| BIGINT (FK)     | FK to `enforcement.enforcement_actions.id`; which action this evidence belongs to. |
| `file_url`            | TEXT             | Storage URL or path to the evidence file.                                   |
| `file_hash`           | VARCHAR(255)     | Hash of the file for tamper detection.                                      |
| `created_at`          | timestamptz      | When the evidence record was created.                                       |

---

### 3.8 `reporting` Schema – Reporting Service

#### 3.8.1 `reporting.audit_logs`

| Field         | Data Type        | Description                                                                 |
|---------------|------------------|-----------------------------------------------------------------------------|
| `id`          | BIGINT (PK)      | Unique identifier for the audit log entry.                                  |
| `user_id`     | BIGINT (FK)      | FK to `identity.users.id`; who performed the action.                        |
| `admin_unit_id`| BIGINT (FK)     | FK to `administrative.administrative_units.id`; area context of the action. |
| `action_type` | VARCHAR(50)      | Action performed (e.g. `CREATE`, `UPDATE`, `LOGIN`).                         |
| `entity_type` | VARCHAR(50)      | Entity affected (e.g. `OBLIGATION`, `PAYMENT`).                              |
| `entity_id`   | BIGINT           | ID of the entity affected.                                                  |
| `old_data`    | JSONB            | Previous state snapshot; may be `NULL` for creations.                        |
| `new_data`    | JSONB            | New state snapshot; may be `NULL` for deletions.                             |
| `created_at`  | timestamptz      | When the action occurred.                                                   |

#### 3.8.2 `reporting.tax_registry_snapshot`

| Field                     | Data Type        | Description                                                                 |
|---------------------------|------------------|-----------------------------------------------------------------------------|
| `id`                      | BIGINT (PK)      | Unique identifier for the snapshot row.                                     |
| `asset_id`                | BIGINT (FK)      | FK to `payer.assets.id`; the asset this row summarizes.                     |
| `payer_id`                | BIGINT (FK)      | FK to `payer.payers.id`; payer responsible.                                 |
| `admin_unit_id`           | BIGINT (FK)      | FK to `administrative.administrative_units.id`; area of the asset.          |
| `last_payment_date`       | DATE             | Date of the most recent payment.                                            |
| `last_payment_amount`     | NUMERIC(18,2)    | Amount of the most recent payment.                                          |
| `last_payment_channel`    | VARCHAR(20)      | Channel of the most recent payment (e.g. MOMO).                             |
| `next_due_date`           | DATE             | Next upcoming due date for tax obligations.                                 |
| `next_due_amount`         | NUMERIC(18,2)    | Amount due by the next due date.                                            |
| `total_outstanding`       | NUMERIC(18,2)    | Sum of all unpaid obligations.                                              |
| `total_overdue`           | NUMERIC(18,2)    | Sum of all overdue obligations.                                             |
| `open_obligations_count`  | INTEGER          | Count of open (non‑paid) obligations.                                       |
| `overdue_obligations_count`| INTEGER         | Count of overdue obligations.                                               |
| `compliance_status`       | VARCHAR(20)      | `COMPLIANT`, `DUE_SOON`, `OVERDUE`, `IN_DISPUTE`.                           |
| `snapshot_at`             | timestamptz      | Timestamp when this snapshot row was last refreshed.                        |

#### 3.8.3 `reporting.fine_registry_snapshot`

| Field                     | Data Type        | Description                                                                 |
|---------------------------|------------------|-----------------------------------------------------------------------------|
| `id`                      | BIGINT (PK)      | Unique identifier for the snapshot row.                                     |
| `asset_id`                | BIGINT (FK)      | FK to `payer.assets.id`; asset this row summarizes.                         |
| `payer_id`                | BIGINT (FK)      | FK to `payer.payers.id`; payer responsible.                                 |
| `admin_unit_id`           | BIGINT (FK)      | FK to `administrative.administrative_units.id`; area of the asset.          |
| `last_payment_date`       | DATE             | Date of most recent fine payment.                                           |
| `total_outstanding`       | NUMERIC(18,2)    | Sum of unpaid fines.                                                        |
| `total_overdue`           | NUMERIC(18,2)    | Sum of overdue fines.                                                       |
| `open_obligations_count`  | INTEGER          | Count of open fine obligations.                                             |
| `overdue_obligations_count`| INTEGER         | Count of overdue fine obligations.                                          |
| `compliance_status`       | VARCHAR(20)      | `COMPLIANT`, `DUE_SOON`, `OVERDUE`, `IN_DISPUTE`.                           |
| `latest_evidence_url`     | TEXT             | URL of the most recent enforcement evidence (if any).                       |
| `latest_evidence_description`| TEXT          | Short description of the most recent evidence or notice.                    |
| `snapshot_at`             | timestamptz      | Timestamp when this snapshot row was last refreshed.                        |

---

## 4. Migration & Implementation Guidance

### 4.1 Creation Order

When initializing a fresh database:

1. Create schemas: `identity`, `administrative`, `payer`, `obligation`, `payment`, `device`, `enforcement`, `reporting`.
2. Create tables in schemas that have **no external dependencies** first: `identity`, `administrative`.
3. Then create `payer` tables (which depend on `administrative`).
4. Then create `obligation`, `payment`, `device`, `enforcement`, and `reporting`, in an order that respects FKs defined above.

Each migration script **must** schema-qualify table and FK names, e.g.:

```sql
CREATE TABLE obligation.obligations (
  id BIGSERIAL PRIMARY KEY,
  payer_id BIGINT NOT NULL REFERENCES payer.payers(id),
  ...
);
```

### 4.2 Service Configuration (Spring Boot / JPA)

- Set `spring.jpa.properties.hibernate.default_schema` (or equivalent) in each service to its own schema (`identity`, `payer`, etc.).
- Use `@Table(schema = "obligation", name = "obligations")` in entity mappings to ensure each service writes only to its own schema.
- If a service must read from another schema (e.g. Obligation Service reading payer name), either:
  - Call the other service via **HTTP/API** (preferred), or
  - Use a read-only DB user with limited cross-schema SELECT permissions, or
  - Maintain a local, event-sourced copy of the needed data in the service’s own schema.

### 4.3 Future Transition to Separate Databases

If you later move to **one database per service**:

- Drop cross-schema foreign keys and rely on ID references only.
- Use the same table structures within each service-specific database (schemas can then be `public`).
- Keep Kafka events and service APIs as the integration contract between services.

---

## 5. Summary

- **Eight PostgreSQL schemas** aligned to microservices: `identity`, `administrative`, `payer`, `obligation`, `payment`, `device`, `enforcement`, `reporting`.
- Each schema owns its tables; cross-service relationships are implemented as **cross-schema foreign keys** in the single-DB phase.
- Migrations, ORM mappings, and service configs must always use **schema-qualified** names to preserve boundaries and support future evolution to full DB-per-service.

