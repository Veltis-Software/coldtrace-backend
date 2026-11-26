# ColdTrace — TP1 / Sprint 1

First executable Strangler increment. Target architecture is in `docs/architecture`;
it is a design proposal, not evidence that all eight services exist.

Implemented: monitoring ingestion/heartbeat/state, gateway routes and brownfield
fallback, shared event/correlation primitives, durable SQLite edge simulator/MQTT adapter,
monitoring outbox publication to the local Pub/Sub emulator and an idempotent alert
consumer with durable in-app notifications.
Remaining: Cloud OIDC push authentication, external notifications, complete BC extraction,
cloud deployment, sustained QA02 load and QA03 user study.

## Repository ownership

The six component directories are Git submodules, pinned to reviewed commits in
their independent repositories. They are not copies maintained in the backend.
Create feature branches and pull requests in the owning repository; update the
submodule pointer here after its tests pass. `sprint1` names the reproducible
migration checkpoint, not a production bounded context. Never run
`git submodule update --remote` in CI: builds must use committed revisions.

```powershell
git clone --recurse-submodules https://github.com/Veltis-Software/coldtrace-backend.git
# For an existing checkout:
git submodule update --init --recursive
```

## Build and verify

Use JDK 21 and the repository Maven wrapper:

```powershell
.\mvnw.cmd -B -f sprint1/pom.xml verify
python -m unittest discover -s sprint1/coldtrace-edge-gateway -v
```

The development host currently has JDK 26. The verified local invocation adds
`-Dnet.bytebuddy.experimental=true` for Mockito; compilation targets Java 21.
The complete reactor was verified inside Docker with Temurin JDK 21. The
host's JDK 26 remains required only for the brownfield application.

The default integration suite uses H2 MySQL compatibility mode. For an isolated
MySQL database, set `TEST_DB_URL`, `TEST_DB_USER`, `TEST_DB_PASSWORD`, and
`TEST_DB_DRIVER=com.mysql.cj.jdbc.Driver`. Tests delete fixture tables in that
database. Never point them at the legacy or production database.

## Local runtime

Provision an empty MySQL schema and a user restricted to it. Set `DB_URL`,
`DB_USER`, `DB_PASSWORD`. Run the packaged monitoring jar (port 8083).
Synthetic fixtures require explicit `SPRING_PROFILES_ACTIVE=demo`,
`DEMO_SENSORS=50`; they use a documented development key and must never be
enabled in a cloud deployment.

Run the gateway jar with `--server.port=18080` when port 8080 is occupied.
`MONITORING_URL` defaults to localhost:8083 and `BACKEND_URL` to localhost:8090.
User state queries require the brownfield IAM server and a valid user JWT.
Ingestion uses gateway credentials and has no synchronous IAM dependency.

Swagger: http://localhost:8083/swagger-ui.html

Edge environment: `API_BASE_URL`, `GATEWAY_UUID`, `GATEWAY_API_KEY`,
`DEVICE_UUID` (consecutive UUIDs for the synthetic sensors).

```powershell
python sprint1/coldtrace-edge-gateway/edge_agent.py --db edge.sqlite --sensors 50 --interval 10 --cut-seconds 300 --duration 320
```

Unconfirmed batches retain both their idempotency key and payload across
restarts. The buffer only removes a batch when accepted+duplicated equals its
size. Data older than 24 hours triggers a warning and is retained.

## Review limits

Threshold events currently represent instantaneous out-of-range readings and
use WARNING; the final duration/severity policy remains unresolved. Monitoring
outbox publication is opt-in for the emulator; incident.opened remains persisted
in the alert outbox. Local replicas are provisioned synthetic fixtures,
not a working asset.settings-changed subscriber. Domain independence has an
ArchUnit check; the complete application is not yet fully hexagonal.

The source migration history is not rewritten. No existing history was loaded
into the new monitoring schema. Cloud screenshots, video, Trello visibility and
individual member assignments remain pending; no contribution is invented.
