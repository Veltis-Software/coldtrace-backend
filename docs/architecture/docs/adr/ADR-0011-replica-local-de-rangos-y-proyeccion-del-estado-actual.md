# ADR-0011: Réplica local de rangos y proyección del estado actual

- **Estado:** Aceptado
- **Fecha:** 2026-10-09
- **Iteración ADD:** Iteración 2 (Rendimiento)
- **Trazabilidad:** DD-12 (DC-14, DC-15) · Drivers: QA-02, US04, US06, US16

## Contexto
Evaluar cada lectura llamando a asset-service y leer el estado recorriendo el historial no cumple los tiempos de QA-02.

## Decisión
monitoring-service mantiene `safe_range_replica` (actualizada por `asset.settings-changed`; ignora `settingsVersion` menores a la almacenada), `device_replica` (uuid del dispositivo → activo, gateway, ubicación y frecuencia) y la proyección `asset_current_state` (una fila por activo, actualizada en la transacción de ingesta). `GET /api/v1/assets/{assetId}/state` y `GET /api/v1/assets/states` leen solo la proyección.

## Consecuencias
Consistencia eventual de segundos tras un cambio de rango. Táctica: maintain multiple copies of data.

## Alternativas descartadas
Consulta síncrona a asset-service por lectura: descartada.
