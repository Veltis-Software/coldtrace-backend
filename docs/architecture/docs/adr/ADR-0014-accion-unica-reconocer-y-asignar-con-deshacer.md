# ADR-0014: Acción única Reconocer y asignar con deshacer

- **Estado:** Aceptado
- **Fecha:** 2026-10-09
- **Ámbito:** Atención funcional de alertas, Sprint 1
- **Trazabilidad:** US08 y US09

## Contexto
Dos pasos separados aumentan el tiempo y los errores del usuario.

## Decisión
`POST /api/v1/alerts/{id}/acknowledgement {assigneeUserId}` cambia `OPEN → ACKNOWLEDGED` y asigna en **una transacción** (columnas nuevas `assigned_to_user_id`, `assigned_at` en `incidents`). `DELETE /api/v1/alerts/{id}/acknowledgement` revierte a `OPEN` si han pasado ≤ 10 s; si no, `409 UNDO_WINDOW_EXPIRED`.

## Consecuencias
La lógica de responsable sugerido debe mantenerse. Tácticas: aggregate y undo.

## Alternativas descartadas
Reconocer y asignar en pasos separados: descartado.
