# ADR-0017 Alta de organización y suscripción entre servicios

- Estado: Aceptado
- Fecha: 2026-11-04
- Alcance: extracción de Identity, Asset y Subscription
- Relación: ADR-0002, ADR-0003 y ADR-0010

## Contexto

El backend crea la organización, solicita la suscripción Base y guarda el primer
usuario dentro de una transacción local. Al extraer Billing, una llamada HTTP
durante esa transacción consultaría una organización todavía no confirmada.
Además, la disponibilidad de Subscription no debe determinar si Identity puede
guardar un alta válida.

## Decisión

Identity confirma organización, usuario y una orden en
`subscription_initialization_outbox` en su propia transacción. Un relay entrega
`PUT /internal/organizations/{organizationId}/base-subscription` a Subscription.
La orden solo se elimina después de recibir confirmación; los fallos se conservan
y se reintentan con espera exponencial limitada a 300 segundos.

Subscription comprueba la organización mediante Identity y mantiene una única
suscripción por organización. Su inicialización es idempotente: repetir la orden
no añade suscripciones ni modifica un plan existente. La entrega es al menos una
vez; no se presupone entrega exactamente una vez.

Asset conserva sus reglas y transacciones locales. Consulta la existencia de la
organización por un puerto de aplicación y obtiene decisiones de plan de
Subscription. Subscription consulta cantidades de usuarios y activos a sus
propietarios. Ninguno lee tablas del otro. Estas consultas administrativas quedan
fuera de la ruta crítica de ingesta de telemetría, que mantiene sus réplicas locales.

Las rutas internas requieren una credencial de servicio; un JWT del navegador no
la sustituye. Las operaciones públicas validan sesión, permisos y organización.
Un fallo del proveedor no concede capacidad ni se transforma en una denegación
del plan: la respuesta es de indisponibilidad. Asset comprueba que la organización
y el entitlement de la respuesta corresponden a la consulta.

## Ejecución

Los procesos locales y contenedores mantienen activo el relay programado. El
despliegue debe proporcionar tiempo de CPU a ese proceso o ejecutar la entrega
desde un worker; una instancia HTTP suspendida sin solicitudes no constituye
una garantía de progreso para el Outbox.

La extracción conserva Java 26 y Spring Boot 4.0.6 del código original en
Identity, Asset y Subscription. Monitoring, Alert, Report y API Gateway conservan
sus compilaciones Java 21 y Spring Boot 3. La integración usa contratos HTTP y
eventos, sin dependencias binarias entre esos runtimes. Los puertos locales son
8081, 8082 y 8088 respectivamente; 8087 pertenece a AI Assistance.

## Verificación

El ensayo en bases aisladas registra un alta con Subscription detenido, conserva
la orden durante los reintentos y crea una sola suscripción al recuperarse el
servicio. Un fallo de persistencia inyectado al crear el usuario revierte la
organización y la orden. Las pruebas HTTP cubren sesión, creación de ubicación,
límite Base, aislamiento por organización, separación de credenciales y CORS.

## Consecuencias

La suscripción inicial tiene consistencia eventual. Los clientes pueden volver a
consultarla durante el aprovisionamiento. Las decisiones de cuota requieren a sus
propietarios disponibles y no representan una reserva atómica de capacidad entre
escrituras concurrentes. La migración de datos y el cambio de rutas del Gateway
se validan como operaciones separadas de la construcción de cada servicio.
