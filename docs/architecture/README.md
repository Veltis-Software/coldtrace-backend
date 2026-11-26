# ColdTrace — Paquete de arquitectura para implementación

Fuente única de verdad de la arquitectura de **ColdTrace** (Veltis Software · Grupo 13 · 1ASI0657 Fundamentos de Arquitectura de Software, UPC 202620).
Refleja exactamente el Capítulo IV corregido del informe del Trabajo Final (ADD 3.0: Iteración 0 + 3 iteraciones; 3 drivers de calidad).

## Reglas para los agentes / desarrolladores

1. **No reinterpretar.** Si algo no está en este paquete, no se inventa: se registra en `OPEN_QUESTIONS.md` y se consulta al Team Leader (Alessandro Condori).
2. **Orden de lectura obligatorio:** `ARCHITECTURE.md` → `docs/adr/` → `contracts/` → `db/` → `IMPLEMENTATION_PLAN.md`.
3. **Los nombres son contrato:** servicios, repositorios, esquemas, tablas, columnas, tópicos, eventos, endpoints y paquetes se usan tal cual aparecen aquí.
4. **Los diagramas son código.** Si cambia la arquitectura, primero se edita `diagrams/` y el ADR correspondiente; luego el código. Nunca al revés.
5. **Cada PR referencia** su tarea del plan (`T-xx`) y el ADR/driver que implementa, con Conventional Commits (`feat(monitoring): ... [ADR-0006]`).

## Contenido

| Ruta | Qué es |
|---|---|
| `ARCHITECTURE.md` | Decisión arquitectónica consolidada: drivers, stack, servicios, datos, eventos, APIs, despliegue y convenciones. |
| `docs/adr/` | 15 Architecture Decision Records (formato Nygard) trazados a DD-01…DD-17 del informe. |
| `diagrams/c4/workspace.dsl` | Modelo C4 en Structurizr DSL (contexto, contenedores Iteración 0/1/2, componentes, despliegue). |
| `diagrams/uml/*.puml` | Diagramas de secuencia PlantUML de las 3 iteraciones. |
| `contracts/openapi/` | Contratos OpenAPI 3.0 de los endpoints nuevos o modificados por ADD. |
| `contracts/asyncapi/coldtrace-events.yaml` | Contrato AsyncAPI 2.6 de los eventos de integración (Pub/Sub). |
| `db/` | Propiedad de tablas por servicio y migraciones Flyway (MySQL 8) de los cambios de esquema. |
| `IMPLEMENTATION_PLAN.md` | Fases, tareas, dependencias y criterios de aceptación medibles (QA-01…QA-03). |
| `OPEN_QUESTIONS.md` | Puntos que el equipo debe confirmar antes de implementarlos. |

## Cómo renderizar los diagramas

- **C4:** abrir https://structurizr.com/dsl y pegar `diagrams/c4/workspace.dsl`, o `docker run -it --rm -p 8080:8080 -v $PWD/diagrams/c4:/usr/local/structurizr structurizr/lite`.
- **Secuencias:** `java -jar plantuml.jar diagrams/uml/*.puml` (o la extensión PlantUML de VS Code).
- **Contratos:** `npx @redocly/cli lint contracts/openapi/*.yaml` y `npx @asyncapi/cli validate contracts/asyncapi/coldtrace-events.yaml`.
