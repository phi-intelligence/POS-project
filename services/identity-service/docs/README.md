# Identity Service Change Log Index

- 001 – Identity DB and Repositories – 2026-03-16

## Conventions

- Files live in this directory and use the pattern:
  - `<SI.NO>_<kebab-case-title>_<YYYY-MM-DD>.md`
- `SI.NO` is a three-digit, zero-padded sequence (001, 002, 003, ...).
- The `Date` and `Change ID` inside each file must match the filename.

## Recommended Template

Each change-log file should follow this structure:

```md
# <Short Title>

**Date**: YYYY-MM-DD  
**Change ID**: NNN  
**Service**: identity-service

## Summary
- Short bullet summary of the change.

## Details
- What was added or changed (entities, endpoints, migrations, config).
- Any important design decisions or trade-offs.

## Impact
- Affected APIs, database tables, or other services.
- Migration or deployment notes (if any).
```

