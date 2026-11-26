# ADR-0008: Disponibilidad de servicios críticos, respaldos y observabilidad

- **Estado:** Aceptado
- **Fecha:** 2026-10-09
- **Iteración ADD:** Iteración 1 (Disponibilidad)
- **Trazabilidad:** DD-08, DD-09 (DC-09, DC-10, DC-11) · Drivers: QA-01, AC-06, CON-06

## Contexto
Arranques en frío o instancias caídas afectan la detección de brechas y las alertas.

## Decisión
monitoring-service y alert-service con `--min-instances=1` y health checks de Actuator (`/actuator/health/liveness`, `/readiness`). Cloud SQL con backups automáticos y recuperación a un punto en el tiempo. Logs JSON con `X-Correlation-Id` propagado por el gateway y por los atributos de Pub/Sub; métricas en Cloud Monitoring.

## Consecuencias
Costo de instancias siempre activas. Sin conmutación automática de Cloud SQL en etapa piloto.

## Alternativas descartadas
Cloud SQL HA regional: descartado por costo en etapa de pilotos.
