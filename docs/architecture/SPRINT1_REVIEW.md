# Review of the Claude handoff — 2026-10-09

Scope confirmed by the student: TP1 / Sprint 1. The handoff is preserved as
input; executable code and observed results determine implementation status.

| Decision | Assessment / concrete correction |
|---|---|
| Strangler + eight bounded contexts | Keep gradual extraction. Sprint 1 extracts monitoring routes; the other contexts remain brownfield. |
| Java 21 / Boot 3.3 | New reactor uses Boot 3.3.13, Cloud 2023.0.6, Java release 21. Legacy remains Java 26 / Boot 4.0.6. |
| com.iceq package | Existing code uses com.acme.coldtrace.platform; retain it for traceable reuse. |
| MySQL migrations | Add missing baseline V1 before the provided V2–V4; V5 protects idempotency-key reuse with a payload hash. |
| Email-only JWT | Resolve organization through an authenticated IAM context endpoint; do not fabricate organization claims. |
| Store-and-forward | Implement SQLite WAL, stable batches, persistent sequences and confirmation-driven deletion. |
| Transactional Outbox | Atomic persistence, emulator publication and alert consumer deduplication implemented. Cloud push OIDC and alert incident.opened publication remain pending. |
| minInstances=1 | Avoids scale-to-zero; does not demonstrate active redundancy or an SLA. |
| Scheduled gap checks on Cloud Run | Background timers need instance-based CPU allocation while the service is idle. Request-only CPU is insufficient for this design. |
| Pub/Sub priority | Separate topics isolate traffic; they do not guarantee strict priority ordering. maxOutstandingMessages is a pull flow-control option, not a push concurrency setting. |
| SSE | Multi-instance broadcast, reconnect/cursors and slow clients need design and tests before claiming live fan-out. |
| QA01 | Local recovery/duplicate checks are evidence of this increment, not a complete availability/SLA validation. |
| QA02 | No verified sustained 1,000-sensor benchmark or p95/p99 result. |
| QA03 | No 10-participant usability study. |
| Provider integrations | Billing, AI and notifications are inherited/planned; the report must not present the proposed doubles as existing tests. |

## Cloud preparation, not deployment

The proposed resource names and cost constraints are maintained in
`sprint1/coldtrace-infrastructure/CLOUD_PREPARATION.md`. The project ID must be
globally unique and confirmed after creation. These are proposals, not deployed
resources. The proposed region is us-central1 for evaluating applicable free
quotas; the final location also needs a latency/data residency decision.

Use scale-to-zero for low-cost intermittent demonstrations. To evaluate the
timer-based gap scenario, temporarily use minimum one instance and
instance-based CPU; document that this incurs charges beyond request-based
free quotas. MySQL Cloud SQL has no permanent free-tier instance. Keep MySQL
local until a bounded paid academic session is explicitly budgeted. Trial
credits depend on eligibility and are not a guarantee of zero cost.

Official references:
- https://cloud.google.com/run/pricing
- https://docs.cloud.google.com/run/docs/configuring/cpu-allocation
- https://docs.cloud.google.com/free/docs/free-cloud-features
- https://cloud.google.com/sql/pricing

No cloud project, billing resource, deployment or paid service was created.

## Framework support review

Java 21 is compatible with Spring Boot 3.3.13, but compatibility is not ongoing
maintenance. Spring announced the end of open-source support for 3.3.x on
2025-06-19. The extracted services retain the handoff stack for this verified
checkpoint; a coordinated Boot/Cloud upgrade must precede a production release.
Do not describe this version as a currently maintained production baseline.

References:
- https://docs.spring.io/spring-boot/3.3/system-requirements.html
- https://spring.io/blog/2025/06/19/spring-boot-3-3-13-available-now/
- https://microservices.io/patterns/refactoring/strangler-application.html
- https://microservices.io/patterns/data/transactional-outbox.html
