# ADR-0015: Confirmación síncrona y actualización por Server-Sent Events

- **Estado:** Aceptado
- **Fecha:** 2026-10-09
- **Ámbito:** Atención funcional de alertas, Sprint 1
- **Trazabilidad:** US08

## Contexto
El usuario necesita confirmación en < 2 s y el equipo debe ver el cambio sin recargar.

## Decisión
La respuesta del comando confirma de inmediato. alert-service expone `GET /api/v1/alerts/stream` (SSE, `text/event-stream`) filtrado por la organización del token y emite `alert.updated` ante cada cambio. Heartbeat SSE cada 25 s para mantener la conexión.

## Consecuencias
Conexiones abiertas por sesión (Cloud Run admite streaming hasta el timeout configurado; el cliente reconecta automáticamente).

## Alternativas descartadas
Polling (retraso y carga) y WebSocket (más complejo de lo necesario): descartados.
