# Entrega de implementación TP1 / Sprint 1

El alcance es un incremento funcional de Monitoring + Alert, agente Edge y
API Gateway. Las demás capacidades permanecen en el sistema base. No afirmar
que se implementaron los ocho microservicios, cuatro sprints ni el despliegue
integral 5.4. El documento Word original no fue modificado durante este cierre.

## Qué se puede documentar

| Sección | Material verificable |
|---|---|
| 5.1 | Pruebas Java 21, escenarios Cucumber ejecutados, seis pruebas Python, ArchUnit; biblioteca compartida sin entidades de negocio; refactor parcial de SensorReading; Strangler, outbox, consumidor idempotente y store-and-forward. |
| 5.2 | GitFlow: `main`, `develop`, ramas `feature/<story>-<description>`, `release/<version>`, `hotfix/<version>`; Conventional Commits y SemVer. Maven por servicio, Java 21/Boot 3.3.13 para extracciones, Java 26/Boot 4 para el base, Docker sin root, esquemas y usuarios separados. CI independiente y del sistema base aprobada; consultar `github-ci-results.json`. |
| 5.3.1.1 Sprint Backlog | US47 recuperación/idempotencia, US46 brechas, US04 estado actual, US16 consulta consolidada de backend, US06 notificación interna durable. Son capacidades implementadas parcialmente; no se realizó interfaz de usuario. El equipo debe confirmar asignaciones, horas y alcance de cada US en Trello. |
| 5.3.1.2 Development | Historial local real, fuentes y cambios a IAM; no atribuir implementación a Alessandro o Renso sin sus commits. |
| 5.3.1.3 Testing | `java21-test-results.json`, reportes de Maven, feature `telemetry.feature`, `python312-docker-tests.txt`, prueba de rollback/idempotencia y evento de cierre desordenado de Alert. |
| 5.3.1.4 Execution | `local-smoke.json`, colección Postman y ejecución `postman-newman.txt`, `edge-recovery.json`, consultas reales de MySQL y recuperación del broker. Video pendiente. |
| 5.3.1.5 Documentation | `openapi-monitoring.json`, `openapi-alert.json`; Swagger local en puertos 18083 y 18084. Los contratos objetivo del pack de Claude incluyen operaciones aún no implementadas y no deben reemplazar estos OpenAPI ejecutables. |
| 5.3.1.6 Deployment | Servicios Docker + MySQL 8 + emulador Pub/Sub realmente ejecutados; `docker-health.json` y configuración Compose. Captura de Google Cloud pendiente. |
| 5.3.1.7 Collaboration | Commits y PR publicados con CI aprobada. No hay evidencia validada de contribuciones de otros integrantes en este incremento. |
| 5.3.1.8 Kanban | Tablero indicado por el usuario: https://trello.com/b/6a963a0f21f9470e710e1f76 . Falta acceso/captura; se omite el token privado de invitación. |

## Resultados observados

La prueba local Edge usó 50 sensores con intervalo de 10 segundos y retuvo
envíos durante 300 segundos. Acumuló 1.500 lecturas pendientes; el búfer se
vació 3,234 segundos después de permitir los envíos. A los 330,046 segundos
se habían generado y aceptado 1.650 lecturas, con cero duplicados y pendientes.
La consulta de MySQL, incluyendo siete lecturas de otras pruebas, registró
1.657 filas y 50 dispositivos, cero claves duplicadas y cero eventos pendientes.
Esto demuestra esa ejecución local, no una garantía de SLA cloud.

La interrupción real del emulador permitió aceptar ingesta con HTTP 202,
retener el evento en outbox y publicarlo al recuperar el broker. El consumidor
generó una incidencia para ese evento. El emulador es efímero: hubo que recrear
sus tópicos tras reiniciarlo.

Postman/Newman ejecutó ocho peticiones y nueve aserciones sin fallos. El tiempo
medio de esas solicitudes no equivale al benchmark sostenido de QA02.

## Diferencias y pendientes arquitectónicos

- La desviación actual es instantánea y WARNING; aún falta acordar y aplicar
  la política de duración/tolerancia y severidad completa.
- La notificación es IN_APP/AVAILABLE persistida; no se enviaron emails/SMS.
- No están implementados acknowledge/assign, undo ni SSE del contrato objetivo.
- `incident.opened` se guarda en outbox de Alert, pero todavía no se publica.
- Réplicas de dispositivos y rangos son fixtures locales explícitos; no existe
  todavía el consumidor de `asset.settings-changed`.
- El publicador y receptor actuales funcionan con el emulador local. Para cloud
  faltan autenticación del publicador y validación OIDC del push.
- QA02 (1.000 sensores y percentiles) y QA03 (estudio con participantes) pendientes.
- No se migró el historial de datos del curso anterior a los nuevos esquemas.
- Los PNG del handoff original son diagramas de diseño, no capturas de despliegue;
  permanecen en el ZIP original y en la copia local, no se versionan otra vez.

Consultar `docs/architecture/SPRINT1_REVIEW.md` para las correcciones justificadas
al pack y `sprint1/coldtrace-infrastructure/CLOUD_PREPARATION.md` para nombres
propuestos, cuotas gratuitas y límites. No se creó ningún recurso facturable.

Los seis repositorios están publicados en Veltis-Software desde David-std2.
Sus PR #2 se integraron tras aprobar CI. `main` conserva el incremento aprobado
y `develop` es la rama activa del siguiente ciclo; es la rama predeterminada
de los seis repositorios nuevos. Las ramas temporales de implementación fueron
eliminadas. El repositorio consolidado también integró PR #2; su historial de
ramas del curso anterior se conserva. El tag `v0.1.0-sprint1` de coldtrace-shared
conserva la revisión exacta fijada por los consumidores. Consultar
`repository-publication.json` y `github-ci-results.json` para las referencias
de publicación y validación. Los checkouts locales están en
`C:/Users/david/IdeaProjects/coldtrace-sprint1-repositories`.

## Revisi�n de conformidad y propiedad

El repositorio base se llama `coldtrace-backend`, conforme a ADR-0002.
Los seis componentes de `sprint1/` son referencias Git fijadas, no fuentes
duplicadas. Cada cambio pertenece al repositorio del componente; el backend
actualiza su referencia despu�s de revisar y validar ese cambio.

El incremento demuestra patrones arquitect�nicos est�ndar con pruebas, pero
no cumple todav�a toda la arquitectura objetivo de los cap�tulos anteriores.
El n�mero de l�neas no acredita ni invalida una historia: deben verificarse
sus criterios de aceptaci�n. Las historias citadas arriba siguen siendo
parciales y el equipo debe aprobar su alcance en el Sprint Backlog. No hay
un denominador validado para afirmar que se alcanz� el porcentaje de
refactorizaci�n exigido por la entrega.

5.1 y 5.2 tienen material real para redactarse con sus l�mites. Las ocho
subsecciones de 5.3.1 pueden redactarse como estado del incremento, pero no
marcarse todas completas: faltan backlog validado, asignaciones y horas,
capturas de despliegue, video, colaboraci�n verificable y evidencia Kanban.
El enunciado reserva el despliegue integral cloud para TF1; esto no elimina
la evidencia propia de despliegue exigida al Sprint 1. Docker local aporta
evidencia de ejecuci�n, pero no acredita un despliegue en Google Cloud.

Las pol�ticas de tolerancia/severidad, r�plicas reales de activos, publicaci�n
de eventos de Alert y autenticaci�n cloud siguen pendientes. Deben aparecer
como arquitectura objetivo y trabajo posterior, no como implementaciones
terminadas. No deben cambiarse los requisitos del ADD para encubrir estas
brechas.
