# Sprint 1 validation evidence

This folder records observed results, not a completed academic report.

| Sprint subsection | Evidence / limitation |
|---|---|
| Sprint Backlog | Selected increment: US47 recovery/idempotency, US46 no-data detection, US04 latest state, US16 consolidated state (backend), US06 notification (durable in-app only). No unverified assignment or estimated hours are attributed to a team member. Trello requires access. |
| Development Evidence | Actual Git commits and feature branch; baseline source preserved. Source refactoring is partial monitoring extraction and narrow alert consumer, not all eight BCs. |
| Testing Evidence | Maven Surefire XML, five Gherkin scenarios, architecture check, broker failure/retry, idempotent alert consumer; Python six persistence/recovery tests. |
| Execution Evidence | local-smoke.json records real HTTP results for gateway/IAM/monitoring/alert. database-checks.txt records real DB observations. Postman collection is runnable after local fixtures. No video exists yet. |
| Documentation Evidence | Generated /v3/api-docs and Swagger for running services. Monitoring operation IDs and required reading fields are checked automatically. Alert detail/acknowledgement/SSE target contract remains outside this increment. |
| Deployment Evidence | Actual Java services executed against isolated MySQL; Docker images built and local Compose infrastructure started. Container application health evidence is recorded after checking it. No Google Cloud deployment is claimed. |
| Collaboration Evidence | Use real Git history only. This assisted implementation is executed in David's workspace; it does not prove contributions by Alessandro or Renso. Team assignments and GitHub analytics screenshots remain pending. |
| Kanban Evidence | User supplied Trello board; both browser and connector access were unavailable. No board states or screenshots were invented. |

## Observations

The five-minute network-withholding run used 50 simulated sensors, 10-second
sampling, 320-second total duration. It ended with zero pending readings.
1,600 generated readings plus one preliminary smoke reading were present at
the first DB check. Subsequent smoke runs add rows, so current totals differ.
The first run did not record recovery latency and does not establish the
full QA01 cloud SLA. Future simulator runs emit measured recoverySeconds.

The subsequent instrumented Docker deployment run is in `edge-recovery.json`:
50 sensors, 300 seconds with no sends, 1,500 peak pending readings, 3.234 seconds
to empty the buffer, 1,650 generated/accepted, zero duplicate or pending readings.
See `TP1_IMPLEMENTATION_STATUS.md` for section-by-section scope and explicit limitations.

Broker interruption accepted ingestion (202), retained its outbox event, and
published it after emulator recovery. Its eventId yielded one incident.
The emulator loses topics on restart; bootstrap was rerun. This is distinct
from the durability properties of the managed Pub/Sub service.

QA02's complete notification benchmark and QA03's participant study are not
performed. Cloud billing/project preparation remains pending. No resources
were created in the unrelated existing Google project.
