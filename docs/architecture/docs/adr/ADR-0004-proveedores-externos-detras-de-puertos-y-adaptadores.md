# ADR-0004: Proveedores externos detrás de puertos y adaptadores

- **Estado:** Aceptado
- **Fecha:** 2026-10-09
- **Iteración ADD:** Iteración 0
- **Trazabilidad:** DD-04 (DC-05) · Drivers: CON-05, CON-07

## Contexto
Stripe, Google/Apple OIDC, OpenAI/Ollama y los canales de notificación no deben aparecer en el dominio.

## Decisión
Puertos en el dominio con adaptadores en infraestructura: `BillingGateway` (Stripe), `ExternalIdentityVerifier` (Google, Apple), `ResolutionPlanGenerator` (Spring AI), `NotificationChannel` (SendGrid, FCM Web Push, WhatsApp). Contratos REST versionados en `/api/v1` y documentados con springdoc-openapi.

## Consecuencias
Más interfaces por contexto; los dobles de prueba se implementan sobre los puertos.

## Alternativas descartadas
Usar los SDK de proveedores directamente en servicios de aplicación: descartado.
