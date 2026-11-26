# ADR-0006: Ingesta idempotente de telemetría

- **Estado:** Aceptado
- **Fecha:** 2026-10-09
- **Iteración ADD:** Iteración 1 (Disponibilidad)
- **Trazabilidad:** DD-06 (DC-07) · Drivers: QA-01, US47

## Contexto
Los reintentos del Edge Agent pueden reenviar lecturas ya almacenadas.

## Decisión
Restricción única `uk_sensor_readings_device_time_seq (iot_device_id, recorded_at, sequence_number)` en `sensor_readings` e inserción con `INSERT ... ON DUPLICATE KEY UPDATE id = id`. La respuesta `202` informa `{accepted, duplicated}`. El historial se ordena por `recorded_at`.

## Consecuencias
Índice adicional; duplicados imposibles a nivel de BD. Táctica: transactions (prevenir).

## Alternativas descartadas
Deduplicación posterior por batch: descartada (duplicados visibles temporalmente).
