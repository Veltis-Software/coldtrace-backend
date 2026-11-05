# Source configuration

Asset Management remains the owner of devices, gateways and assets during the
Strangler migration. The source configuration use case reuses these aggregates,
repositories and location rules. The API Gateway continues to route this context
to the backend until its independent service is extracted.

## API

All routes require the existing JWT. IAM resolves the organization and permissions
from the authenticated user, not from a request body or query parameter.

| Method | Organization-scoped route | Permission |
|---|---|---|
| PUT | `/api/v1/organizations/{organizationId}/iot-devices/{deviceId}/source-configuration` | `assets:manage` |
| GET | same route | `assets:manage`, `assets:view` or `operations:view` |
| GET | same route + `/history` | same read permissions |
| GET | `/api/v1/organizations/{organizationId}/gateways/{gatewayId}/source-profiles` | same read permissions |

```json
{
  "assetId": 20,
  "expectedVersion": 0,
  "sourceId": "sonda-lacteos-01",
  "transport": "mqtt",
  "profileVersion": 1,
  "schemaVersion": 1,
  "fields": {
    "temperature": "temp_c",
    "humidity": "relative_humidity",
    "recordedAt": "measured_at"
  },
  "temperatureUnit": "C",
  "humidityUnit": "%RH",
  "reason": "Instalación y calibración inicial"
}
```

Initial association uses `expectedVersion: 0`. Replacement uses the current
`configurationVersion` and retains the device UUID, gateway and monitored asset.
The source identifier may change; the old revision remains immutable. An already
assigned legacy device cannot be moved to another asset by initial configuration.
The same source identifier cannot be active on two devices in an organization.
A changed interpretation of the same source needs a higher profile version;
an existing historical profile version cannot be redefined.

The profile matches Edge's MQTT/HTTP registry: contract 1, distinct field names,
temperature C or F and humidity %RH. Output units are C and %RH, with UTC timestamps.
REST identifiers and versions require integer JSON numbers: strings, decimals,
booleans and overflow are rejected. Validation failures return 400, absent
resources 404, tenant/permission failures 403 and conflicting revisions or source
assignments 409. No client-supplied actor is accepted.

## Persistence and concurrency

`source_configurations` contains the current snapshot;
`source_configuration_histories` contains immutable revisions, actor, timestamp,
reason and original association. The device row is locked for both configuration
and legacy update/delete operations. A unique organization/source constraint also
protects simultaneous claims from different devices. Device assignment, current
snapshot, history and `source.configuration-changed` outbox event join one database
transaction. Rollback restores all four. Existing telemetry is never rewritten.

Legacy metadata edits still work. Changing a managed device's UUID, gateway or
asset through the old CRUD endpoint returns 409, as does deleting that device.
The baseline backend uses Hibernate `ddl-auto=update` locally; the additive SQL
under `docs/database/source-configuration.sql` supports controlled schema rollout.
Apply it to the owning Asset database before enabling schema validation in a
deployment; it does not migrate or rewrite existing telemetry.

## Integration boundary

The gateway export is an operator-authorized document matching Edge's registry.
It is not a device credential. Provision it by atomically replacing the Edge profile
file in its read-only configuration-directory mount. Edge revalidates that file
every five seconds and swaps the complete registry without rewriting its buffer.
Invalid replacements retain the last valid configuration.

The existing emulator relay now chooses the topic from the event envelope, allowing
both `asset.settings-changed` and `source.configuration-changed`. Infrastructure
creates the local source topic/subscription; Monitoring projects ordered,
deduplicated revisions and enforces the source/profile effective at capture time.
Buffered readings preceding the replacement remain admissible and later readings
from the retired source are rejected atomically. The cloud relay still requires
authenticated delivery and deployment configuration; the local emulator adapter
must not be exposed as an unauthenticated cloud endpoint.

## Verification

The backend test suite includes real MySQL transactions and concurrent initial
association, replacement/history, stale edits, source uniqueness, tenant/location
validation and rollback. MVC tests cover permission checks, strict JSON versions
and Edge export. Legacy command tests protect identity/association and preserve
metadata editing. Frontend tests cover data loading, versioned saving, readers,
conflict recovery, ambiguous mappings and cancellation of stale device requests.
