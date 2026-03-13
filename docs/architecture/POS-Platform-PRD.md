## 1. Introduction & Scope

**Product Name**: POS-based Government Revenue & Tax Collection Platform  
**Version**: v1.0 (Initial PRD)  
**Document Purpose**: This Product Requirements Document (PRD) defines the functional and non-functional requirements for a government revenue platform that supports tax/levy assessment, enforcement, payment (including POS & cash), and reporting across administrative units.

The platform consists of:
- **Admin Desktop Application** (Electron + React) for back-office staff and administrators.
- **Field Agent Mobile Application** (Flutter) for officers operating in the field.
- **Citizen Mobile Application** (Flutter) for taxpayers (citizens and businesses).
- **Backend Microservices** exposed via an **API Gateway**, with event-driven integration via **Kafka** and **RabbitMQ**.

The scope of this PRD covers:
- Identity & access control, administrative hierarchy and rules.
- Payer & asset registration, obligation management, disputes and enforcement.
- Payment processing (digital, POS, cash), cash sessions and reconciliation.
- POS device management.
- Reporting, analytics, audit, and notifications.

Out of scope for this PRD:
- Low-level implementation details (e.g., specific DB schemas, Gradle configuration).
- Internal DevOps tooling and CI/CD pipelines.

## 2. Stakeholders & Personas

### 2.1 Stakeholders

- **Revenue Authority Leadership**: Needs visibility into collections, compliance, and trends.
- **Tax Administration & Policy Unit**: Configures administrative hierarchy, tax rules, and penalties.
- **IT & Operations**: Deploys, maintains, and monitors the platform.
- **Compliance & Enforcement Unit**: Manages field inspections, violations, and enforcement outcomes.
- **Field Collection Agents**: Use mobile + POS devices to register payers, issue obligations, and collect payments in the field.
- **Citizens / Businesses (Payers)**: View and settle obligations, receive receipts and notifications.
- **Audit & Internal Control**: Requires complete, immutable audit trails of system activity.

### 2.2 Personas

- **Admin Officer (Back-office)**  
  Works on the desktop app to manage users, rules, payers, obligations, and view reports.

- **Field Agent**  
  Uses the mobile app and a POS device to perform inspections, register payers, create obligations, and collect payments (POS or cash).

- **Citizen / Business Owner (Payer)**  
  Uses the citizen mobile app to register, view obligations, pay, and retrieve receipts.

- **Finance / Compliance Analyst**  
  Uses the desktop app to generate reports, monitor collections, and track compliance metrics.

## 3. Product Overview & Objectives

### 3.1 Product Vision

Provide a unified, event-driven platform for governments to register payers, calculate and manage tax/levy obligations, support field collections via POS and cash, and deliver transparent reporting and audit trails across all administrative units.

### 3.2 Objectives

- **Increase revenue collection** by enabling convenient, multi-channel payment options (POS, digital, cash via agents).
- **Improve compliance** through clear obligations, enforcement workflows, and dispute handling.
- **Enhance transparency & trust** via strong audit logging, reporting, and citizen-facing visibility.
- **Support scalability & resilience** using a microservices architecture with Kafka-based eventing and PostgreSQL + Redis for data and caching.

## 4. User Journeys & Core Workflows

### 4.1 Payer Registration & Asset Setup

- As a Field Agent or Admin Officer, I can register a new payer (citizen or business) with identity and contact details.
- As a Field Agent or Admin Officer, I can register one or more assets (e.g., businesses, vehicles, properties) and link them to a payer.
- As a user, I can search for existing payers and view their registered assets.

### 4.2 Obligation Creation & Lifecycle

- As the system, I can generate tax/levy/fine obligations based on configured rules and registered assets.
- As an Admin Officer, I can manually create or adjust obligations when needed (subject to approval where required).
- As a user, I can view the current status of each obligation (e.g., CREATED → OPEN → OVERDUE → PARTIALLY_PAID → PAID).
- As the system, I can automatically update obligation status based on payments, penalties, and disputes outcomes.

### 4.3 Payment & Receipt Workflow

- As a Citizen, I can initiate a payment from my mobile app (digital channels) or via a POS device operated by a Field Agent.
- As a Field Agent, I can accept cash or POS payments in the field and have them recorded against specific obligations.
- As the system, I allocate each payment to one or more obligations, update balances, and generate a receipt.
- As a Citizen or Field Agent, I can retrieve and view receipt details and payment history.

### 4.4 Disputes & Appeals

- As a Payer, I can raise a dispute/appeal regarding an obligation (amount, penalties, or status).
- As an Admin Officer, I can review, update, and resolve disputes, with full history retained.
- As the system, I can adjust the obligation and its events according to approved adjustment requests, maintaining an audit trail.

### 4.5 Enforcement & Field Operations

- As a Field Agent, I can plan and record enforcement actions (inspections, notices, seizures) associated with payers and obligations.
- As a Field Agent, I can capture GPS location and photo/video evidence during enforcement actions.
- As an Admin Officer, I can review enforcement actions, evidence, and their outcomes.

### 4.6 Cash Sessions & Reconciliation

- As a Field Agent, I can open a cash collection session before starting field collections.
- As a Field Agent, I can close a cash session and record handover of collected cash to a cashier or bank.
- As a Finance/Compliance Analyst, I can see expected vs. handed-over cash per session and per officer, supporting reconciliation.

### 4.7 Notifications & Communication

- As a Payer, I receive notifications (SMS, email, push, in-app) when obligations are created, updated, overdue, or paid.
- As a Field Agent or Admin, I receive notifications related to disputes, enforcement actions, and critical system events.

## 5. Functional Requirements by Domain

### 5.1 Identity & Access (Authentication & User Service)

**Purpose**: Control who can use the system and what they can do.

- **User Authentication**
  - The system **must** allow officers and admins to log in via username/password and return a JWT access token.
  - The system **must** support secure logout that invalidates active tokens (server-side or via token blacklist/expiry).
- **User Management**
  - Admins **must** be able to create, view, update, suspend/activate, and delete users.
  - Each user **must** be assignable to one or more roles (e.g., Admin, FieldAgent, Analyst).
- **Role & Permission Management**
  - The system **must** support configurable roles with associated permissions (e.g., manage_users, view_reports, create_obligations).
  - All APIs **must** enforce role-based access control (RBAC) based on JWT claims.

### 5.2 Administrative Setup & Rule Engine (Administrative Service)

**Purpose**: Manage administrative hierarchy and rules for taxes, penalties, and obligations.

- **Administrative Units**
  - Admins **must** be able to define hierarchical administrative units (e.g., Country → State → LGA → Ward).
  - The system **must** support assigning officers to specific administrative units.
- **Rule Management**
  - Admins **must** be able to create, view, update, and delete rule definitions for tax/levy calculations and penalties.
  - The system **must** maintain rule versions and effective dates, allowing historical reconstruction of calculations.
  - Obligations **must** be calculated consistently using the current active rule version for an administrative unit.

### 5.3 Payer & Asset Registry (Payer Service)

**Purpose**: Register citizens/businesses and their taxable assets.

- **Payer Management**
  - Users (Admin/Field Agent) **must** be able to create, view, update, and (soft) delete payer records.
  - The system **must** support searching payers by name, identifier (e.g., tax ID), phone, or asset details.
- **Asset Management**
  - Users **must** be able to register assets (e.g., business premises, vehicles, properties) and link them to payers.
  - Users **must** be able to update and deactivate assets while preserving history.

### 5.4 Obligations, Compliance & Receipts (Obligation Service)

**Purpose**: Manage obligations, compliance issues, disputes, adjustments, approvals, and receipts.

- **Obligation Lifecycle**
  - The system **must** support creation, viewing, updating, and deactivation of obligations.
  - Each obligation **must** track status transitions, outstanding balance, penalties, and history of events.
- **Disputes & Appeals**
  - Payers or officers **must** be able to create disputes/appeals for obligations.
  - Admins **must** be able to update dispute status, record resolutions, and link outcomes to obligation adjustments.
- **Adjustments & Approvals**
  - The system **must** support requests for obligation adjustments (reductions, waivers, corrections).
  - Adjustments **must** go through an approval workflow with appropriate roles.
- **Receipts**
  - The system **must** generate a unique receipt for applicable payments and link it to the corresponding payment and obligations.
  - Users **must** be able to retrieve receipts by ID, payment reference, or receipt number.

### 5.5 Payments & Cash Management (Payment Service)

**Purpose**: Handle all payment transactions across channels and manage cash sessions.

- **Payment Processing**
  - The system **must** accept payment initiation from the citizen app, field agent app (POS), and potentially external channels.
  - Each payment **must** have a unique identifier, status (initiated, confirmed, failed), amount, method (card, cash, transfer), and timestamps.
- **Allocation to Obligations**
  - The system **must** allocate payments to one or more obligations based on provided references and business rules.
  - Partial and multiple-obligation allocations **must** be supported.
- **Cash Sessions & Handovers**
  - Field Agents **must** be able to open and close cash collection sessions.
  - The system **must** record expected cash totals for each session and actual handover entries, supporting reconciliation.

### 5.6 POS Device Management (Device Service)

**Purpose**: Manage physical POS devices used by field agents.

- **Device Registry**
  - Admins **must** be able to register, view, update, and retire POS devices.
  - Each device **must** have identifiers (serial number, terminal ID), status (active, inactive, lost), and assignment history.
- **Device Assignment**
  - Admins **must** be able to assign devices to field agents, with effective dates.
  - The system **must** track which device was used for each POS transaction.

### 5.7 Enforcement (Enforcement Service)

**Purpose**: Track field inspections and enforcement actions.

- **Enforcement Actions**
  - Field Agents **must** be able to create enforcement records linked to payers, assets, and/or obligations.
  - Each action **must** capture type (inspection, notice, seizure), status, date/time, and location (GPS).
- **Evidence Capture**
  - Field Agents **must** be able to attach photos, videos, or documents as evidence to enforcement actions.
  - The system **must** store references to evidence files and ensure they remain accessible and tamper-evident.

### 5.8 Reporting & Audit (Reporting Service)

**Purpose**: Provide analytical reports, dashboards, and audit trails.

- **Operational & Financial Reports**
  - Users with appropriate roles **must** be able to generate reports for:
    - Tax registry (list of obligations and statuses).
    - Fine registry (fines, penalties, and their settlement statuses).
    - Collection summaries by period, administrative unit, payer segment, and payment method.
- **Audit Logs**
  - The system **must** log key user and system actions (logins, rule changes, obligation updates, payment allocations, dispute outcomes).
  - Authorized users **must** be able to search and view audit logs by user, entity, action type, and date range.

### 5.9 Notifications (Notification Service)

**Purpose**: Deliver real-time and asynchronous notifications.

- **Event-driven Notifications**
  - The system **must** listen to key events (e.g., payer.created, obligation.created, payment.confirmed, receipt.generated, dispute.created, enforcement.action.recorded).
  - For configured events, the system **must** send notifications via one or more channels: SMS, email, push, in-app.
- **Notification History**
  - Users **must** be able to view recent notifications within the mobile apps and desktop app.
  - The system **must** track delivery status (sent, delivered, failed, read) to support retries and analytics.

## 6. Client Application Requirements

### 6.1 Admin Desktop Application

- **User & Role Administration**
  - Manage users, roles, and permissions, including search and filtering.
- **Administrative Units & Rules**
  - Configure administrative units and view their hierarchy.
  - Manage tax and penalty rules, including version history.
- **Payer & Asset Management**
  - Search, view, and maintain payers and their assets.
- **Obligation & Dispute Management**
  - View obligations by payer, status, and administrative unit.
  - Review and resolve disputes and adjustment requests.
- **Payments & Cash Oversight**
  - Monitor payments, cash sessions, and reconciliation status.
- **Reporting & Audit**
  - Access dashboards, standard reports, and audit logs with export (e.g., CSV/PDF).

### 6.2 Field Agent Mobile Application

- **Authentication & Assignment**
  - Secure login using JWT; device binding optional.
  - Display assigned administrative units and POS device.
- **Payer & Asset Operations**
  - Register new payers and assets in the field, with offline capture and background sync when connectivity returns.
  - Search for existing payers and view obligations and payment history.
- **Obligation & Collection**
  - View outstanding obligations for a payer and initiate payment (cash or POS).
  - Generate payment references/QR codes where applicable.
- **POS & Cash Handling**
  - Connect to assigned POS device via Bluetooth/NFC and process card payments.
  - Open/close cash sessions, view session totals, and record cash handovers.
- **Enforcement & Evidence**
  - Record enforcement actions with GPS location and timestamps.
  - Capture and upload photos or other media as evidence.
- **Notifications**
  - Receive task-related notifications (e.g., assigned inspections, dispute updates).

### 6.3 Citizen Mobile Application

- **Onboarding & Identity**
  - Allow citizens/businesses to register and link their identity with existing payer records where applicable.
- **Obligation Visibility**
  - Display all obligations with status, due dates, and amounts.
- **Payment Initiation**
  - Allow initiation of digital payments (e.g., card, bank transfer, wallet) and display payment status.
- **Receipts & History**
  - Provide access to receipts and historical payments and obligations.
- **Notifications**
  - Receive and view notifications about new obligations, overdue items, and payment confirmations.

## 7. Data & Event Requirements

- **Key Data Entities**
  - Payer, Asset, Obligation, ObligationEvent, Dispute, AdjustmentRequest, Payment, PaymentAllocation, Receipt, OfficerCashSession, CashHandover, PosTerminal, PosTransaction, EnforcementAction, EvidenceFile, AuditLog.
- **Event Types (Kafka)**
  - The system **must** emit or consume events such as: payer.created, asset.registered, rule.updated, obligation.created, obligation.overdue, payment.initiated, payment.confirmed, payment.failed, receipt.generated, cash.session.opened, cash.session.closed, enforcement.action.recorded, dispute.created.
- **Event-driven Behavior**
  - Downstream services (e.g., Reporting, Notification) **must** react to these events to update snapshots, send notifications, and maintain analytics.

## 8. Non-Functional Requirements

- **Performance & Scalability**
  - The system **should** handle concurrent usage by admins, field agents, and citizens without degradation of core workflows (login, payer search, payment, obligation view).
  - Services **should** be independently scalable (microservices) and leverage caching (Redis) for frequently accessed data.
- **Availability & Resilience**
  - Core services (Identity, Payer, Obligation, Payment) **must** be highly available during business hours, with graceful degradation of non-core services (e.g., Notifications) during outages.
  - The system **must** be able to continue accepting and queuing operations even if some downstream consumers (e.g., Reporting, Notification) are temporarily unavailable.
- **Security & Compliance**
  - All external communication **must** be encrypted (TLS).
  - Authentication **must** use JWT with appropriate expiry and refresh policies.
  - Sensitive data **must** be protected at rest according to regulatory requirements.
  - Comprehensive audit logging **must** support internal and external audits.
- **Monitoring & Observability**
  - The platform **must** provide metrics, logging, and tracing sufficient to diagnose issues and monitor key KPIs (e.g., transaction volumes, error rates).

## 9. Reporting & Analytics Requirements

- **Dashboards**
  - Provide configurable dashboards for collection performance, compliance rates, and enforcement activity.
- **Standard Reports**
  - Tax registry and fine registry snapshots as of specified dates.
  - Collection by period, administrative unit, payment method, and channel (field vs. self-service).
- **Data Export**
  - Allow authorized users to export report data for further analysis (CSV, Excel, or PDF).

## 10. Phasing & Release Plan

Aligning with the development stages:

- **Phase 1 – Core Platform Services (Stage 1)**
  - Deliver Identity & Access, Administrative Setup & Rules, and API Gateway capabilities.
  - Provide minimal Admin Desktop features for user, role, and admin-unit management.
- **Phase 2 – Core Revenue Services (Stage 2)**
  - Deliver Payer & Asset Registry and Obligation/Compliance core (obligation lifecycle, basic disputes).
  - Provide Admin and (initial) Field Agent app flows for registration, obligation viewing, and basic payments integration.
- **Phase 3 – Payment & Field Operations (Stage 3)**
  - Deliver full Payment & Cash Management, POS Device management, and Enforcement features.
  - Extend Field Agent app to support cash sessions, POS collection, enforcement recording, and evidence capture.
- **Phase 4 – Compliance & Analytics (Stage 5)**
  - Deliver Reporting & Audit and Notifications with full event-driven integration.
  - Provide dashboards, audit log access, regulatory reports, and multi-channel notifications.

Each requirement in this PRD **must** be tagged by implementation teams as **Must Have**, **Should Have**, or **Could Have** per phase to guide incremental delivery.

