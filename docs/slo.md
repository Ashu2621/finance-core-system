# Service-level objectives

These objectives define the production target. They are not claims about the free Render
environment, which can cold-start and has no availability commitment.

## SLIs and objectives

| SLI | Objective | Window |
|---|---:|---:|
| Successful authenticated API requests | 99.9% | 30 days |
| Read API latency | p95 < 300 ms | 7 days |
| Write API latency | p95 < 500 ms | 7 days |
| Record-create correctness | 99.999% no duplicate accepted writes | 30 days |
| Readiness probe success | 99.95% | 30 days |

## Error budget policy

A 99.9% monthly availability objective permits about 43 minutes of failed requests.
When 50% of the monthly budget is consumed, pause risky feature releases. At 100%,
ship only reliability and security fixes until the rolling window recovers.

## Alerts

- Page when 5-minute API error ratio exceeds 5%.
- Page when readiness fails for 5 consecutive minutes.
- Ticket when p95 write latency exceeds 500 ms for 30 minutes.
- Page on authentication failure spikes above 5x the seven-day baseline.
- Ticket when database pool utilization remains above 80% for 15 minutes.

Prometheus-format metrics are exposed at `/actuator/prometheus`; production networking
must restrict that endpoint to the monitoring plane.

