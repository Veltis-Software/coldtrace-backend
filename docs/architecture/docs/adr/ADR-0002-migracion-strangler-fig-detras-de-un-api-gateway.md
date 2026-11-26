# ADR-0002: Migración Strangler Fig detrás de un API Gateway

- **Estado:** Aceptado
- **Fecha:** 2026-10-09
- **Iteración ADD:** Iteración 0
- **Trazabilidad:** DD-01 (DC-01, DC-03) · Drivers: CON-01, AC-05

## Contexto
La solución es brownfield (CON-01) y no puede sustituirse en una sola entrega (AC-05). El frontend no debe conocer la topología interna.

## Decisión
Se introduce un **API Gateway (Spring Cloud Gateway)** como único punto de entrada. Cada Bounded Context se extrae a su servicio uno a la vez, en el orden monitoring → alert → asset → identity → report → ai-assistance → maintenance → subscription; las rutas aún no extraídas se enrutan al `coldtrace-backend`.

## Consecuencias
Entregas pequeñas y reversibles; convivencia temporal del backend de referencia con los servicios nuevos. El gateway es un componente adicional a operar.

## Alternativas descartadas
Reescritura completa (big bang): descartada por riesgo. Clientes llamando directo a cada servicio: descartado por acoplar el frontend a la topología.
