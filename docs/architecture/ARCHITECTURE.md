# ColdTrace — Decisión arquitectónica consolidada

> Versión 1.1 · 2026-11-03 · Trazable al Capítulo IV del informe TF (secciones 4.1–4.3) y a los ADR de `docs/adr/`.

## 1. Decisión en una frase

ColdTrace evoluciona su backend brownfield (Java/Spring Boot, monolito modular por Bounded Contexts) hacia **8 microservicios cloud-native en Google Cloud Run, uno por Bounded Context, detrás de un API Gateway**, con **base de datos por servicio** (un esquema MySQL por servicio en Cloud SQL), **mensajería asíncrona con Pub/Sub + Transactional Outbox** para la telemetría y las alertas, y un **Edge Gateway Agent** en el local del cliente que conserva las lecturas durante interrupciones de conectividad. La migración es incremental con **Strangler Fig**.

## 2. Drivers arquitectónicos

Los atributos priorizados son disponibilidad, rendimiento e interoperabilidad. El diseño también considera la funcionalidad primaria, restricciones y concerns arquitectónicos. La atención guiada y el control de errores de interacción forman parte de los requisitos funcionales del producto.

| ID | Atributo | Business Goal | Escenario medible (criterio de aceptación arquitectónico) |
|---|---|---|---|
| QA-01 | Disponibilidad | BG01 | Ante un corte de red del gateway, el activo se muestra como **NO_DATA ≤ 30 s** después del umbral; al reconectar, **100 %** de lecturas retenidas se incorpora **sin duplicados en ≤ 5 min**. |
| QA-02 | Rendimiento | BG01, BG02 | Con **1 000 sensores** enviando en una ventana de 10 s: **p95 < 2 s** de confirmación (202), **p99 < 5 s** evaluado y persistido, **notificación < 10 s** desde la recepción de una lectura fuera de rango. |
| QA-03 | Interoperabilidad | BG01, BG03, BG04 | Dos perfiles MQTT/JSON y HTTP/JSON producen lecturas canónicas v1 con identidad, °C, %RH y UTC. Se aceptan 100 casos válidos por perfil, con error de conversión ≤ 0,01 °C; versiones y unidades no admitidas se rechazan antes de persistir. Incorporar el segundo perfil no modifica las reglas de evaluación. |

**Primary User Stories del ADD:** US03, US04, US06, US14, US16, US29, US30, US46, US47.
**Incremento Sprint 1:** US04, US06, US08, US09, US14, US16, US33, US46 y US47.
**Incrementos Sprint 2:** perfiles y adaptadores Edge, más validación y persistencia del contexto canónico en Monitoring. La administración completa de asociaciones y consulta de fuentes continúa como US03, US29 y US30.
**Constraints:** CON-01 brownfield · CON-02 microservicios + DDD + cloud-native · CON-03 Java/Spring Boot · CON-04 MySQL/Cloud SQL · CON-05 REST + OpenAPI · CON-06 contenedores en Google Cloud · CON-07 Stripe, OAuth/OIDC e IA detrás de adaptadores.

### Tácticas (3 por atributo)

| Atributo | Táctica | Implementación | ADR |
|---|---|---|---|
| Disponibilidad | Heartbeat + Timestamp (detectar) | Heartbeat del gateway cada 10 s; `SourceGapDetector` cada 10 s | ADR-0007 |
| Disponibilidad | Almacenamiento durable, reintentos y resincronización (recuperar) | Store-and-forward y confirmación de lotes en Edge; una instancia mínima evita escala a cero, sin acreditar redundancia | ADR-0005, ADR-0008 |
| Disponibilidad | Transacciones (prevenir) | Ingesta idempotente: UK `(iot_device_id, recorded_at, sequence_number)` + `Idempotency-Key` | ADR-0006 |
| Rendimiento | Aislar el tratamiento de eventos críticos | Tópico `threshold.breached` separado y con suscripción dedicada | ADR-0009 |
| Rendimiento | Introducir concurrencia + limitar colas | Pub/Sub push, inserción por lotes (≤ 500), límites de concurrencia y capacidad de consumidores en Cloud Run | ADR-0009, ADR-0010 |
| Rendimiento | Mantener múltiples copias de datos | `safe_range_replica`, `asset_current_state`, `reading_aggregates` | ADR-0011, ADR-0012 |
| Interoperabilidad | Discover Service | Registro explícito de sourceId, dispositivo y perfil versionado | ADR-0016 |
| Interoperabilidad | Tailor Interface | Adaptadores MQTT/JSON y HTTP/JSON, conversión de unidades y tiempos | ADR-0016 |
| Interoperabilidad | Orchestrate | Pipeline de resolución, validación, normalización y almacenamiento durable | ADR-0016 |

## 3. Stack fijo

| Capa | Tecnología (no cambiar sin ADR) |
|---|---|
| Microservicios | Java 21, Spring Boot 3.3.x y Maven. Separación de dominio, aplicación y adaptadores donde existen reglas e integraciones variables |
| API Gateway | Spring Cloud Gateway con rutas, filtros y configuración; sin un dominio artificial |
| Persistencia | MySQL 8 (Cloud SQL), Spring Data JPA, **Flyway** por servicio |
| Mensajería | Google Cloud Pub/Sub (`spring-cloud-gcp-starter-pubsub`), emulador en local |
| Documentación API | springdoc-openapi (Swagger UI en `/swagger-ui.html`, spec en `/v3/api-docs`) |
| Observabilidad | Spring Boot Actuator, logs JSON con `X-Correlation-Id`, Cloud Logging / Cloud Monitoring |
| Frontend | Web Application Angular existente (Firebase Hosting) |
| Edge Gateway Agent | Python 3.12, `paho-mqtt`, SQLite y adaptador HTTP de entrada |
| Despliegue | Docker multietapa → Artifact Registry → Cloud Run (`southamerica-west1`), GitHub Actions |
| Pruebas | JUnit 5, Mockito, ArchUnit, Testcontainers (MySQL, emulador Pub/Sub), Cucumber (Gherkin) |

## 4. Servicios (Iteración 0)

| Servicio | Bounded Context | Repo | Puerto local | Esquema DB | Orden de extracción |
|---|---|---|---|---|---|
| api-gateway | — | `coldtrace-api-gateway` | 8080 | — | 0 |
| monitoring-service | Monitoring | `coldtrace-monitoring-service` | 8083 | `ct_monitoring` | 1 |
| alert-service | Alerts | `coldtrace-alert-service` | 8084 | `ct_alert` | 2 |
| asset-service | Asset Management | `coldtrace-asset-service` | 8082 | `ct_asset` | 3 |
| identity-service | Identity & Access | `coldtrace-identity-service` | 8081 | `ct_identity` | 4 |
| report-service | Reports & Compliance | `coldtrace-report-service` | 8085 | `ct_report` | 5 |
| ai-assistance-service | AI Assistance | `coldtrace-ai-assistance-service` | 8087 | `ct_ai` | 6 |
| maintenance-service | Maintenance Management | `coldtrace-maintenance-service` | 8086 | `ct_maintenance` | 7 |
| subscription-service | Subscription & Billing | `coldtrace-subscription-service` | 8088 | `ct_subscription` | 8 |
| edge-gateway-agent | — (cliente) | `coldtrace-edge-gateway` | — | SQLite local | con monitoring |

- Paquete base: `com.iceq.coldtrace.<contexto>` (ver `OPEN_QUESTIONS.md` Q-01).
- Mientras un contexto no se extrae, el API Gateway enruta su ruta al **backend de referencia** (`coldtrace-backend`).
- Emulador Pub/Sub en local: puerto **8681** (no usar 8085, lo ocupa report-service).

### Rutas del API Gateway

| Prefijo | Destino |
|---|---|
| `/api/v1/telemetry/**`, `/api/v1/assets/*/state`, `/api/v1/assets/states` | monitoring-service |
| `/api/v1/alerts/**`, `/api/v1/incidents/**`, `/api/v1/notifications/**` | alert-service |
| `/api/v1/assets/**`, `/api/v1/locations/**`, `/api/v1/devices/**`, `/api/v1/gateways/**` | asset-service |
| `/api/v1/authentication/**`, `/api/v1/users/**`, `/api/v1/roles/**`, `/api/v1/organizations/**` | identity-service |
| `/api/v1/reports/**` | report-service |
| `/api/v1/resolution-plans/**`, `/api/v1/incidents/*/resolution-plans` | ai-assistance-service (**precede** a la regla de alert-service) |
| `/api/v1/maintenance/**`, `/api/v1/technical-service-requests/**` | maintenance-service |
| `/api/v1/billing/**`, `/api/v1/subscriptions/**` | subscription-service |

Las rutas más específicas se declaran primero. La autenticación JWT existente (emitida por identity-service) se conserva y el gateway la propaga junto con `X-Correlation-Id`.

## 5. Datos (Database per Service)

- Una instancia Cloud SQL MySQL 8; **un esquema y un usuario de BD por servicio** con `GRANT` solo sobre su esquema. Ningún servicio consulta tablas de otro: los datos ajenos se obtienen por API o por eventos.
- Referencias entre servicios **solo por ID** (sin FK física entre esquemas).
- Propiedad completa de tablas: `db/TABLE_OWNERSHIP.md`. Cambios de esquema: `db/migrations/`.
- monitoring-service no lee `ct_asset`: mantiene copias propias `monitored_gateways` (heartbeat), `device_replica` y `safe_range_replica` (ADR-0007, ADR-0011).
- Nombres reales de columnas (del backend de referencia): la hora de captura de una lectura es **`sensor_readings.recorded_at`**; el informe y los eventos usan `recordedAt`.

## 6. Comunicación

| Tipo | Uso | Regla |
|---|---|---|
| REST síncrono (vía gateway) | Web App → servicios; Edge Agent → monitoring | Contratos en `contracts/openapi/` |
| REST interno | Consultas puntuales entre servicios (ACL) | Prohibido en la ruta crítica de una lectura |
| Pub/Sub (asíncrono) | Telemetría, alertas, cambios de rango, incidencias | Contrato en `contracts/asyncapi/`; Transactional Outbox en el productor; consumidor idempotente (`processed_events`) |
| SSE | alert-service → Web App | `GET /api/v1/alerts/stream`, evento `alert.updated` |

### Eventos

| Evento (tópico) | Productor | Suscripciones (consumidor) | Prioridad |
|---|---|---|---|
| `threshold.breached` | monitoring-service | `alert-service.threshold-breached` | **Alta** (suscripción dedicada) |
| `source.gap` | monitoring-service | `alert-service.source-gap` | Alta |
| `readings.ingested` | monitoring-service | `report-service.readings-ingested` | Normal |
| `asset.settings-changed` | asset-service | `monitoring-service.asset-settings-changed` | Normal |
| `incident.opened` | alert-service | `report-service.incident-opened`, `ai-assistance-service.incident-opened` | Normal |

En `dev` los tópicos llevan sufijo `-dev` (p. ej. `threshold.breached-dev`). Sobre común en `contracts/asyncapi`.

## 7. Flujos críticos (resumen; detalle en `diagrams/uml/`)

1. **Lectura normal (QA-02):** Edge Agent → `POST /telemetry/batches` → monitoring evalúa contra `safe_range_replica` → en **una transacción**: `INSERT sensor_readings` + `UPSERT asset_current_state` + `INSERT outbox_events` → `202 {accepted, duplicated}` → Outbox Relay (cada 500 ms) publica → alert-service abre incidencia y notifica con deep link.
2. **Corte de red (QA-01):** Edge Agent retiene en SQLite y reintenta con backoff (1 s → 60 s máx.) → `SourceGapDetector` (cada 10 s) detecta `now - max(last_heartbeat_at, última lectura) > umbral` → `asset_current_state.status = NO_DATA` y evento `source.gap` → alert-service avisa (incidencia `type = NO_DATA`). Al reconectar: reenvío en orden de `recorded_at`, inserción idempotente, cierre de la brecha.
3. **Atención funcional (US08, US09 y US33):** notificación con `{WEB_BASE_URL}/alerts/{incidentId}` → `GET /alerts/{id}/detail` → `POST /alerts/{id}/acknowledgement {assigneeUserId}` → 200 + SSE `alert.updated` → undo con `DELETE` ≤ 10 s.

### Reglas de negocio fijadas

| Regla | Valor |
|---|---|
| Umbral de brecha | `max(30 s, 3 × iot_devices.reading_frequency_seconds)` |
| Heartbeat del Edge Agent | cada 10 s |
| Lote máximo de telemetría | 500 lecturas |
| Retención local en Edge Agent | 24 h; borra solo lecturas confirmadas |
| Ventana de undo | 10 s desde `acknowledged_at` (después → 409) |
| Alerta | Es una fila de `incidents` (alert-service). Estados: `OPEN` → `ACKNOWLEDGED` → `RESOLVED`; undo vuelve a `OPEN` |
| Tipos de incidencia nuevos | `NO_DATA` para brechas; desviación térmica usa el tipo ya existente en el backend |
| Responsable sugerido | Usuario que reconoció la incidencia más reciente del mismo activo en los últimos 30 días; si no hay, el usuario autenticado |
| Estado del activo | `NORMAL`, `OUT_OF_RANGE`, `NO_DATA` |

4. **Interoperabilidad (QA-03):** sourceId → perfil versionado → adaptador MQTT/HTTP → normalización a °C, %RH y UTC → SQLite → lote estable → validación canónica en Monitoring → evaluación, Outbox y consumidores.

## 8. Despliegue

Proyecto de desarrollo: `veltis-coldtrace-dev` (`311618880556`). La tabla describe la asignación objetivo; el estado ejecutado se registra por sprint. Edge requiere almacenamiento persistente en el local del cliente.

| Nodo | Elementos | Configuración |
|---|---|---|
| Cloud Run `southamerica-west1` | api-gateway (ingress **público**); 8 servicios (ingress **interno**) | monitoring y alert: `--min-instances=1`, health checks `/actuator/health/liveness` y `/readiness`; resto: `--min-instances=0` |
| Cloud SQL MySQL 8 | 8 esquemas | Backups automáticos + PITR (sin HA regional en etapa piloto) |
| Pub/Sub | 5 tópicos, suscripciones push autenticadas (OIDC) hacia Cloud Run | `ackDeadline` 30 s, dead-letter tras 5 intentos |
| Firebase Hosting | Web Application | CDN + HTTPS |
| Secret Manager / Artifact Registry / Cloud Logging / Cloud Monitoring | Secretos, imágenes, observabilidad | Secretos nunca en el repo |
| Local del cliente | Edge Gateway Agent | Docker o servicio systemd; config: `GATEWAY_UUID`, `GATEWAY_API_KEY`, `API_BASE_URL` |

Perfiles: `SPRING_PROFILES_ACTIVE=local|dev|prod` (local: Docker Compose + MySQL + emulador; dev desde `develop`; prod desde `main`).

## 9. Convenciones

GitFlow (`feature/<us-id>-<desc>`, `release/x.y.z`, `hotfix/x.y.z`) · Conventional Commits · SemVer por servicio · API en `/api/v1` · código en inglés · lenguaje ubicuo (Asset, SafeRange, SensorReading, Incident, CorrectiveAction, ResolutionPlan) · Google Java Style · eventos de integración `<contexto>.<hecho>` · eventos de dominio en participio (`ThresholdBreachedEvent`).

## 10. Fuera de alcance de estas decisiones

Seguridad avanzada (mTLS, rotación de claves RS256), HA regional de Cloud SQL, Kafka/RabbitMQ, WebSocket, cambios de tecnología del frontend. Cualquier propuesta en estos temas requiere un ADR nuevo.
