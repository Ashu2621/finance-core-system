# Architecture and reliability model

Finaxis is a modular monolith: one deployable unit with strict package boundaries for
authentication, users, records, audit, and analytics. This keeps transactions local and
operational complexity low while preserving clear seams for future service extraction.

## Correctness guarantees

- Every record mutation and privileged user-management action writes an audit event in
  the same database transaction as the business change.
- Record creation requires an `Idempotency-Key`. Replaying the same key and payload
  returns the original record; reusing the key with another payload returns `409`.
- Database constraints enforce positive amounts, valid enum values, ownership, and
  idempotency uniqueness.
- JPA optimistic versions prevent silent last-write-wins behavior at the persistence
  layer.
- Flyway is the sole production schema authority; Hibernate validates but never mutates
  production schema.

## Scaling path

The API is stateless and can scale horizontally behind a load balancer. JWT validation,
idempotency, account lockout state, and audit history are database-backed, so behavior
does not depend on a specific application instance.

Before splitting services, scale PostgreSQL, add read replicas for analytical reads, and
introduce Redis only for non-authoritative caches. Financial writes and idempotency
records must remain in the same transactional boundary.

## Data classification

| Data | Classification | Handling |
|---|---|---|
| Password hash | Restricted | BCrypt; never serialized or logged |
| JWT signing key | Restricted | Secret manager/environment only |
| Financial records | Confidential | Owner-scoped authorization |
| Audit history | Confidential | Admin-only, append-only API |
| Metrics | Internal | No user payloads or credentials |

