# ADR-0005: Edge Gateway Agent con store-and-forward

- **Estado:** Aceptado
- **Fecha:** 2026-10-09
- **Iteración ADD:** Iteración 1 (Disponibilidad)
- **Trazabilidad:** DD-05 (DC-06) · Drivers: QA-01, US47, AC-01

## Contexto
Ante cortes de red los sensores pierden lecturas; no tienen memoria para retener 24 h.

## Decisión
Se agrega el contenedor **Edge Gateway Agent** (Python 3.12, SQLite) en el local del cliente: recibe lecturas por MQTT (`coldtrace/{deviceUuid}/readings`), las guarda con `recorded_at` y `sequence_number`, las envía en lotes ordenados (≤ 500) a `POST /api/v1/telemetry/batches` con `Idempotency-Key`, reintenta con backoff exponencial (1 s a 60 s), borra solo lo confirmado y retiene hasta 24 h. Envía heartbeat cada 10 s. En el alcance académico es un simulador que reproduce sensores y cortes.

## Consecuencias
Requiere software en el local del cliente. Táctica: retry + state resynchronization.

## Alternativas descartadas
Envío directo sensor→nube con reintentos: descartado (memoria insuficiente).
