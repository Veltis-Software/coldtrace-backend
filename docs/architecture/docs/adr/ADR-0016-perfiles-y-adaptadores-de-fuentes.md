# ADR-0016 Perfiles y adaptadores de fuentes compatibles

- Estado: aceptado para el diseño; implementación incremental.
- Fecha: 2026-11-03.
- Iteración ADD: 3, interoperabilidad.
- Trazabilidad: QA-03, US03, US29, US30; DD-14 a DD-17.

## Contexto

Los equipos compatibles pueden entregar mediciones con protocolos, campos,
unidades y offsets distintos. Las reglas de rango seguro necesitan recibir
una interpretación común y conservar la identidad de origen y su contexto.

## Decisión

Mantener un registro explícito y versionado de fuentes. Resolver sourceId a
dispositivo, transporte y perfil; adaptar MQTT/JSON y HTTP/JSON; normalizar a °C,
%RH y UTC antes de almacenar en el Edge. Un pipeline coordina los pasos y rechaza
entradas incompatibles sin generar lecturas ni consumir secuencias.

Conservar SQLite, lotes estables, reintentos e Idempotency-Key del diseño de
disponibilidad. La ruta cloud mantiene validación, evaluación con réplica local,
proyección de estado, persistencia con Outbox y publicación asíncrona. Monitoring
debe validar y persistir el contrato y contexto canónicos antes de evaluar.

El primer incremento de Sprint 2 implementa el registro local, los adaptadores y
la normalización en `coldtrace-edge-gateway`. La administración de asociaciones
por organización, su historial y la persistencia del contexto canónico en
Monitoring se realizan como incrementos siguientes; las historias completas
conservan su estado de trabajo hasta integrar esos criterios.

## Consecuencias y verificación

Cada perfil admitido tiene una suite de contrato. Se contrastan 100 entradas por
perfil, equivalencia de unidades y UTC, rechazo de versiones/unidades no admitidas,
identidad configurada y estabilidad del lote tras reiniciar. La integración del
segundo perfil no modifica las reglas de evaluación de rangos.

El gateway mantiene rutas y filtros. Los adaptadores del Edge se separan de la
normalización y del buffer; no se incorpora una estructura hexagonal artificial
a componentes que solo configuran transporte o enrutamiento.
