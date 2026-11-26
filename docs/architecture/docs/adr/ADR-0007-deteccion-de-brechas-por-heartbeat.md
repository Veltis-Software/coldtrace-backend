# ADR-0007: Detección de brechas por heartbeat

- **Estado:** Aceptado
- **Fecha:** 2026-10-09
- **Iteración ADD:** Iteración 1 (Disponibilidad)
- **Trazabilidad:** DD-07 (DC-08) · Drivers: QA-01, US46, AC-01

## Contexto
Una ausencia de lecturas no debe interpretarse como estado normal; debe ser visible en ≤ 30 s después del umbral.

## Decisión
`monitored_gateways.last_heartbeat_at` (tabla propia de monitoring-service, ver db/TABLE_OWNERSHIP.md) se actualiza con `POST /api/v1/telemetry/heartbeats`. El componente `SourceGapDetector` (`@Scheduled(fixedRate = 10000)`) marca `asset_current_state.status = NO_DATA` cuando `now - max(last_heartbeat_at, last_recorded_at) > max(30 s, 3 × reading_frequency_seconds)` y publica `source.gap`; al recibir datos de nuevo publica `source.gap` con `state = CLOSED`. alert-service crea una incidencia `type = NO_DATA`, distinta de la alerta térmica.

## Consecuencias
Ejecución periódica dentro de monitoring-service (requiere instancia mínima activa, ADR-0008). Tácticas: heartbeat y timestamp.

## Alternativas descartadas
Cloud Scheduler: descartado (frecuencia mínima de 1 minuto).
