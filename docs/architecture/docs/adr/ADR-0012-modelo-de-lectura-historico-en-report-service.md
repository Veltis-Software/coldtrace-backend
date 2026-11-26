# ADR-0012: Modelo de lectura histórico en report-service

- **Estado:** Aceptado
- **Fecha:** 2026-10-09
- **Iteración ADD:** Iteración 2 (Rendimiento)
- **Trazabilidad:** DD-13 (DC-16) · Drivers: US14, AC-02

## Contexto
Las consultas históricas y exportaciones compiten con la ingesta.

## Decisión
report-service consume `readings.ingested` y mantiene `reading_aggregates` por activo y hora; los reportes y exportaciones se ejecutan como trabajos asíncronos (estado `PENDING → READY`).

## Consecuencias
Los reportes reflejan datos con segundos de retraso.

## Alternativas descartadas
Consultar `sensor_readings` de monitoring: prohibido por ADR-0003.
