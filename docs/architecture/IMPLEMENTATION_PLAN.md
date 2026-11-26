# Plan de implementación

Orden obligatorio: Fase 0 → 1 → 2 → 3. Dentro de una fase se respetan las dependencias técnicas y la propiedad de cada repositorio.
Cada tarea termina con: código + pruebas (suite crítica verde) + OpenAPI generado coincide con `contracts/` + PR a `develop` con Conventional Commits.

## Fase 0 — Estructura (Iteración 0 · ADR-0001…0004)

| Tarea | Descripción | Depende de | Aceptación |
|---|---|---|---|
| T-01 | Crear repos `coldtrace-*` (ver ARCHITECTURE §4) con estructura acorde con su responsabilidad, Maven, Actuator, springdoc, Flyway, logs JSON con `X-Correlation-Id`, Dockerfile multietapa y workflow de GitHub Actions. | — | `mvn verify` y build de imagen en CI. |
| T-02 | `coldtrace-api-gateway`: rutas de ARCHITECTURE §4 (más específicas primero); rutas no extraídas → `coldtrace-backend`. Propaga JWT y `X-Correlation-Id`. | T-01 | Prueba de rutas: cada prefijo llega al destino correcto. |
| T-03 | `coldtrace-infrastructure`: Docker Compose local (MySQL 8 con 8 esquemas y 8 usuarios con GRANT por esquema, emulador Pub/Sub en 8681, creación de tópicos y suscripciones de ARCHITECTURE §6). | — | `docker compose up` deja todo listo. |
| T-04 | Reglas ArchUnit compartidas: `domain` no depende de Spring, JPA ni SDK de proveedores. | T-01 | Regla falla si se viola. |

## Fase 1 — Disponibilidad (Iteración 1 · ADR-0005…0008 · QA-01)

| Tarea | Descripción | Depende de | Aceptación |
|---|---|---|---|
| T-10 | Extraer **monitoring-service**: baseline `ct_monitoring` + migraciones `V2–V4`; copia de datos. | T-01, T-03 | Lecturas existentes consultables desde el nuevo servicio. |
| T-11 | `POST /telemetry/batches` y `/telemetry/heartbeats` según `contracts/openapi/monitoring-service.yaml`; validación de `X-Gateway-Key` contra `monitored_gateways.api_key_hash`; inserción idempotente. | T-10 | Reenviar el mismo lote 3 veces → 1 sola fila por lectura; respuesta `{accepted, duplicated}` correcta. |
| T-12 | `SourceGapDetector` (cada 10 s), estado `NO_DATA` en `asset_current_state`, evento `source.gap` (OPENED/CLOSED) vía outbox. | T-10 | Prueba con reloj controlado: brecha marcada ≤ 30 s después del umbral. |
| T-13 | `coldtrace-edge-gateway` (Python): MQTT → SQLite → lotes ordenados ≤ 500 con `Idempotency-Key`, backoff 1–60 s, borrado solo de confirmadas, heartbeat 10 s; modo simulador con N sensores y cortes configurables. | T-11 | Simulación de corte de 5 min sin pérdida. |
| T-14 | Despliegue dev: monitoring y alert con `--min-instances=1`, health checks, backups + PITR en Cloud SQL, dashboards en Cloud Monitoring. | T-10 | Health checks verdes en Cloud Run. |
| **QA-01** | **Prueba de aceptación**: simulador con 50 sensores, corte de red de 5 min. | T-11…T-14 | NO_DATA visible ≤ 30 s tras el umbral; 100 % de lecturas retenidas persistidas sin duplicados ≤ 5 min tras reconectar. |

## Fase 2 — Rendimiento (Iteración 2 · ADR-0009…0012 · QA-02)

| Tarea | Descripción | Depende de | Aceptación |
|---|---|---|---|
| T-20 | Outbox + `OutboxRelay` (cada 500 ms) en monitoring, asset y alert; publicador Pub/Sub con atributos del sobre. | T-10 | Evento nunca se pierde si el broker cae (prueba con emulador detenido). |
| T-21 | Extraer **alert-service**: consumidores idempotentes de `threshold.breached` y `source.gap`; abre incidencia (`NO_DATA` para brechas) y notifica por `NotificationChannel` con deep link. Publica `incident.opened`. | T-20 | Mismo evento entregado 2 veces → 1 incidencia. |
| T-22 | `safe_range_replica` + consumidor de `asset.settings-changed` (ignora versiones menores); evaluación sin llamadas a asset-service; proyección `asset_current_state`; endpoints `/assets/{id}/state` y `/assets/states`. | T-20 | Ningún HTTP saliente en la ruta de ingesta (verificado en test). |
| T-23 | asset-service publica `asset.settings-changed` (incrementa `settings_version`). | T-20 | Contrato AsyncAPI validado. |
| T-24 | report-service: `reading_aggregates` desde `readings.ingested`; reportes como trabajos asíncronos. | T-20 | Reporte no consulta `ct_monitoring`. |
| **QA-02** | **Prueba de carga** (k6 o el simulador de T-13): 1 000 sensores en ventana de 10 s, en dev. | T-20…T-24 | p95 confirmación < 2 s; p99 evaluado y persistido < 5 s; notificación de desviación < 10 s. |

## Fase 3 — Interoperabilidad (Iteración 3 · ADR-0016 · QA-03)

| Tarea | Descripción | Depende de | Aceptación |
|---|---|---|---|
| T-30 | Registro versionado de perfiles y asociación sourceId–deviceUuid en Edge. | T-13 | Fuente desconocida, perfil duplicado y transporte no admitido se rechazan. |
| T-31 | Adaptadores MQTT/JSON y HTTP/JSON hacia el mismo pipeline. | T-30 | Dos perfiles con campos diferentes producen unidades y tiempos canónicos. |
| T-32 | Normalización de °F/°C a °C, %RH y UTC antes del buffer. | T-31 | 100 entradas válidas por perfil, error ≤ 0,01 °C; entradas no admitidas no consumen secuencia ni se almacenan. |
| T-33 | Contrato canónico y contexto de origen persistido por Monitoring. | T-32 | Valida versión, identidad y unidades antes de evaluar; trazabilidad durable en cloud. |
| T-34 | Administración de asociaciones y sustitución con historial (US03/29/30). | T-30 | Autorización por organización, una asociación activa y conservación del historial. |
| QA-03 | Prueba integrada de incorporación del segundo perfil. | T-30…T-34 | Mismas reglas de evaluación, identidad conservada y rechazo sin efectos. |

La atención de alertas (detalle, reconocimiento, asignación, deshacer y actualización) se conserva como incremento funcional de Sprint 1 y en ADR-0013 a ADR-0015.

## Fase 4 — Extracción del resto de contextos (Strangler Fig)

identity → report (resto) → ai-assistance → maintenance → subscription, cada uno con baseline Flyway, copia de datos, cambio de ruta en el gateway y retiro del módulo en `coldtrace-backend`. Sin cambios de arquitectura: si alguno los necesita, nuevo ADR.
