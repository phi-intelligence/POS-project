# Notification Service (`notification-service`)

## Overview
- Sends email, SMS, push, and in-app notifications for key system events.
- Ensures citizens and officers are informed about obligations, payments, disputes, and enforcement actions.

## Responsibilities
- Consume events (from Kafka) about payments, obligations, disputes, enforcement, etc.
- Dispatch notifications via integrated providers (SMS gateway, email, push).
- Optionally maintain short-lived notification history and delivery status.

## Database
- No PostgreSQL schema owned.
- Uses:
  - Redis (for queues/cache and per-user notification state).
  - Kafka (for event consumption).

## APIs (High-Level)
- `POST /notifications/send` – send notification (internal/admin).
- `GET /notifications/user/{userId}` – fetch recent notifications for a user.
- `PUT /notifications/{id}/read` – mark as read (if persisted).

## Events
- Consume:
  - `payment.confirmed`, `payment.failed`.
  - `obligation.overdue`.
  - `dispute.created`, `enforcement.action.recorded`.
- Optionally publish:
  - `notification.sent`, `notification.failed` for monitoring/retries.

## Dependencies
- Infrastructure: Redis, Kafka, external SMS/email/push providers.
- Internal: Consumes events from Payment, Obligation, Enforcement, others.

## Implementation Notes
- Focus on reliability (retries, dead-letter queues), idempotency, and provider abstraction.