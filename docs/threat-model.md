# Threat model

## Primary assets

Financial records, account identity, authorization roles, password hashes, signing
material, database credentials, and audit evidence.

## Trust boundaries

1. Browser to HTTPS edge.
2. Edge to stateless application.
3. Application to PostgreSQL.
4. CI/CD to container registry and deployment platform.

## Key threats and controls

| Threat | Control |
|---|---|
| Credential stuffing | Uniform login errors, BCrypt, five-attempt timed lockout |
| Cross-account record access | Repository queries require both record ID and owner ID |
| Retry-created duplicates | Database-backed idempotency key and request hash |
| Privilege escalation | Public registration always creates `VIEWER`; admin endpoints enforce RBAC |
| Admin repudiation | Transactional audit events for privileged and financial mutations |
| Token forgery | Minimum-length environment-provided HMAC secret and short token lifetime |
| Secret leakage | No secrets in API models, logs, repository, or health details |
| Schema drift | Versioned Flyway migrations and PostgreSQL container migration test |
| Dependency compromise | Dependabot, CodeQL, pinned lockfile, reproducible Maven/npm builds |

## Residual risks

- Stateless access tokens cannot be individually revoked before expiry. Keep the access
  TTL short; refresh-token rotation and per-session revocation are the next security
  milestone.
- In-memory application availability still depends on PostgreSQL. A production database
  plan needs backups, point-in-time recovery, and tested restore procedures.
- Free hosting is appropriate for demos, not the SLOs in `slo.md`.

