# ADR-0018 Generación de IA y decisiones operativas

- Estado: Aceptado
- Fecha: 2026-11-05
- Relación: ADR-0001, ADR-0003 y ADR-0004

## Contexto

El dominio original conserva el plan de resolución, su aprobación y el cierre del
incidente en Alerts. La generación estructurada es una capacidad separada que
también utilizan el dashboard y los resúmenes de Reports. El estado del incidente
y su acción correctiva deben confirmarse junto con la decisión humana.

## Decisión

AI Assistance conserva la generación de recomendaciones y la composición de
evidencia para el dashboard. Su API interna recibe contexto y devuelve contenido
validado o un fallo controlado. Los proveedores se implementan mediante el puerto
AiStructuredOutputPort y sus adaptadores existentes. El servicio no persiste
estado operativo ni necesita un esquema MySQL.

Alert conserva los agregados y repositorios de planes de resolución del código
original. La aprobación bloquea el plan, valida la organización y el permiso del
operador, registra la acción correctiva, resuelve el incidente y guarda el plan
en una transacción del mismo servicio. La identidad de quien decide se obtiene de
la sesión autenticada. El proveedor de IA no ejecuta decisiones ni modifica el
incidente.

Reports conserva sus resultados inmutables y ensambla el resumen a partir de
evidencia de sus propietarios. AI Assistance genera el texto estructurado; el
resumen no altera las mediciones ni el resultado del informe. La ausencia de un
valor se conserva como ausencia y no equivale a cero ni a cumplimiento completo.

## Consecuencias

La persistencia corresponde a los servicios que mantienen estado. Los planes de
incidente pertenecen a ct_alert; AI Assistance es stateless. No existe una
transacción distribuida para aprobar un plan. Las llamadas al proveedor quedan
fuera de la ingesta crítica de telemetría y tienen un límite de tiempo.

La API de generación requiere credenciales de servicio; el navegador utiliza las
API de negocio y su sesión. Un proveedor desactivado o no disponible devuelve el
fallo correspondiente, sin reemplazarlo por recomendaciones ficticias. Las
pruebas de generación utilizan dobles explícitos; la demostración de telemetría
se alimenta desde fuentes Edge y fixtures aislados.
