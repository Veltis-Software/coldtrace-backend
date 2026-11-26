# ADR-0010: Transactional Outbox y consumidores idempotentes

- **Estado:** Aceptado
- **Fecha:** 2026-10-09
- **Iteración ADD:** Iteración 2 (Rendimiento)
- **Trazabilidad:** DD-11 (DC-13) · Drivers: QA-02, QA-01

## Contexto
Escribir en la BD y publicar en el broker por separado puede perder eventos.

## Decisión
El productor guarda el evento en `outbox_events` en la **misma transacción** que el cambio de negocio. Un `OutboxRelay` (`@Scheduled(fixedDelay = 500)`) publica los pendientes y marca `published_at`. Cada consumidor registra `(consumer, event_id)` en `processed_events` dentro de su transacción y descarta repetidos.

## Consecuencias
Tabla y proceso adicionales; garantía de no pérdida y no duplicación.

## Alternativas descartadas
Escritura dual BD + broker: descartada.
