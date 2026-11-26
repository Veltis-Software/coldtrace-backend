# ADR-0009: Mensajería asíncrona con Google Cloud Pub/Sub

- **Estado:** Aceptado
- **Fecha:** 2026-10-09
- **Iteración ADD:** Iteración 2 (Rendimiento)
- **Trazabilidad:** DD-10 (DC-12, DC-17) · Drivers: QA-02, AC-02, CON-06

## Contexto
Las llamadas síncronas de la Iteración 0 acoplan el tiempo de ingesta a alert, asset y report.

## Decisión
Pub/Sub con suscripciones **push autenticadas (OIDC)** hacia Cloud Run. Tópicos: `threshold.breached` (prioritario, suscripción dedicada), `source.gap`, `readings.ingested`, `asset.settings-changed`, `incident.opened`. `ackDeadline` 30 s, dead-letter tras 5 intentos, control de flujo con `maxOutstandingMessages`. Contratos en `contracts/asyncapi/coldtrace-events.yaml`.

## Consecuencias
Entrega al menos una vez → consumidores idempotentes obligatorios (ADR-0010). Tácticas: introduce concurrency, bound queue sizes, prioritize events.

## Alternativas descartadas
RabbitMQ (hay que operarlo) y Kafka (sobredimensionado y costoso): descartados.
