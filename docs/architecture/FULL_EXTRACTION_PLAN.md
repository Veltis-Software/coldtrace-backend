# Extracción completa del backend

## Estado de continuación al cierre de las primeras pruebas

- Identity publicado: `f330f13`, rama `feature/extract-identity`; CI aprobada.
- Asset publicado: `e4f35a6`, rama `feature/extract-asset`; CI aprobada.
- Subscription publicado: `848d4f8`, rama `feature/extract-subscription`; CI aprobada.
- Pasaron 19 comprobaciones HTTP en el script vigente de Infrastructure, dos
  pruebas de Identity, tres de Subscription y 21 de Asset, incluidas las diez
  pruebas MySQL originales de asociaciones, versiones, historial y rollback.
- El Docker de Identity validó el despacho de errores: el fallo de persistencia
  inyectado devuelve 500 y revierte organización y Outbox. El primer Docker no
  tenía Actuator; se agregó la dependencia a los servicios nuevos antes de publicar.
- Alert tiene una API interna de incidentes, con clave de servicio y tenant.
  Su suite completa pasó 13 pruebas usando Java 21 en Docker. Aún no publicada.
- Maintenance reutiliza sus 59 clases y arranca en 18106. Se verificó 409 con el
  plan Base y 201 al crear un mantenimiento y una solicitud técnica con un plan
  Operations temporal en la base aislada. El fixture de suscripción se restauró
  a Base. Faltan pruebas automatizadas y publicación. No hay datos originales
  del usuario alterados.
- AI Assistance reutiliza sus 33 clases y compila con sus adaptadores HTTP. Se
  eliminó la dependencia directa de ReportRepository mediante ReportEvidenceReader
  y ReportEvidenceSnapshot. Sus contratos de lectura con Monitoring/Report son
  cambios locales aún sujetos a pruebas. No está extraído el ciclo completo de
  planes de resolución y resúmenes que antes pertenecía a Alerts/Reports.
- Monitoring añade consultas internas acotadas de lecturas (máximo 1000), UTC y
  aislamiento. Report publica métricas desde su resultado inmutable, conservando
  null cuando falta evidencia. Sus suites completas están en ejecución: sesiones
  33852 y 86239; logs `monitoring-owner-contract-tests.log` y
  `report-owner-contract-tests.log`, bajo Downloads/veltis-report. No asumir éxito.
- DOCX de trabajo: `revision-continuidad-20261103/ColdTrace_avance3_extraccion.docx`.
  Se corrigieron tres textos de stack/paquetes y el párrafo de refactorización.
  Exportado con Word: 158 páginas; 102 tablas. Se conservaron exactamente todas
  las propiedades de párrafos y runs y las demás partes del paquete. Las páginas
  con diferencias de texto son 122–129 y fueron inspeccionadas: sin desbordamientos.
  Se copiaron DOCX/PDF al repo report/docs/av3, aún sin publicar. Verificar índice
  y mantener los tres marcadores cloud; no modificar historia del Sprint 1.

### Recursos de ensayo y procesos

Todos los servicios extraídos utilizan esquemas `ct_*_runtime` de la instancia
aislada MySQL de Docker en 3308. No tocar la base original ni backend 8080/8088.
Para liberar memoria al ejecutar dos suites Java 21 en Docker se solicitó detener
solo los runtimes de este ensayo: Identity PID32668, Asset PID52164, Subscription
PID19252 y Maintenance PID56100, además de
`coldtrace-identity-extraction-verification`. La orden está en sesión 87710;
confirmar resultado antes de reiniciar. El intento de arranque de AI está en
sesión 65175 y log `ai-extraction-runtime.log`; no afirmar que arrancó.

Continuar con pruebas/publicación de los contratos, extracción completa de las
funciones de IA, configuración reproducible y migración de datos. Gateway conserva
el fallback deliberadamente hasta probar todos los flujos con el backend detenido.

## Objetivo
Ejecutar ColdTrace desde los repositorios de Veltis-Software sin iniciar coldtrace-backend. El backend permanece como referencia histórica, sin duplicar escrituras ni servir rutas de producción. Reutilizar las implementaciones actuales; conservar interfaz, contratos y datos.

## Inventario comprobado
Identity145 archivosJava/6archivosacoplados; Asset151/12; Subscription107/5; Maintenance59/9; AIAssistance33/3. Inventario detallado: EXTRACTION_DEPENDENCIES.json. Los cinco repositorios destino estaban vacíos y fueron clonados en coldtrace-sprint1-repositories. GitHub autenticado David-std2. No subir a otra organización.

## Orden de ejecución
1. Extraer Identity y Subscription coordinadamente: alta de organización/suscripción, sesión, permisos y entitlements mediante contratos explícitos. No copiar tablas ajenas al servicio ni introducir bucles de arranque.
2. Extraer Asset: ubicaciones, gateways, dispositivos, rangos, perfiles e historial y Outbox. Adaptar IAM y límites de suscripción a clientes HTTP, preservando transacciones propietarias.
3. Extraer Maintenance y AI Assistance: conservar casos de uso existentes y reemplazar ACL locales por contratos de sus propietarios.
4. Completar cualquier endpoint legado aún usado por Web, Monitoring, Alert y Report. No retirar fallback hasta comprobar todos los consumidores.
5. Actualizar Gateway e Infrastructure para rutas y configuración independientes; retirar BACKEND_URL y referencias runtime al backend. Mantener almacenamiento aislado y migraciones reproducibles.
6. Verificar migración de datos con cuentas y fixtures originales; pruebas por servicio y flujo completo de sesión/activos/configuración/telemetría/alertas/reportes/mantenimiento/IA/suscripción, con el backend detenido.
7. Publicar commits y PR por repositorio, CI y documentación/diagramas acordes. No modificar el entregable académico ya presentado para inventar extracción anterior; registrar evolución posterior.

## Criterios de cierre
- Todos los servicios destino contienen implementaciones funcionales, no esqueletos.
- El Gateway no enruta al backend y ningún servicio lo consulta.
- Cada servicio usa únicamente su base/esquema y sus contratos remotos.
- Se conservan permisos, aislamiento por organización, historial y contratos canónicos.
- Los flujos del frontend y las pruebas de integración funcionan con coldtrace-backend detenido.
- CI y construcción de imágenes por repositorio aprobados y cambios publicados en Veltis-Software.
- Evitar imponer hexagonal al gateway; respetar la complejidad y estructura propias de cada componente.

## Estado inicial
Inventario y clones completados. Ningún servicio nuevo fue presentado como implementado. Siguiente paso: inspeccionar las ACL de Identity/Subscription y establecer sus contratos antes de copiar el código propietario.

## Avance de extracción Identity
Se reutilizaron IAM145archivos y primitivas shared en coldtrace-identity-service,
rama feature/extract-identity. POM separado sin dependencias de IA/Stripe,
configuración exclusiva ct_identity y puerto8081; validaciónDDL en vez de crear
tablas de otros contextos. Contratos publicados Subscription copiados como DTO/ACL,
sin entidades/repositoriosBilling. Faltan adaptadorremoto, migraciones y pruebas.
Hallazgo crítico: signup original inicializaBase dentro de transacciónIAM. Separar
ese paso requiere idempotencia y recuperación; no usar un ciclo HTTP que consulte
una organización todavía no confirmada. Registrar decisión y comprobar rollback/
reintento antes de retirar rutaLegacy. DOCX se actualizará con decisión verificada,
no con una extracción solamente copiada.

## Integración Identity→Subscription implementada localmente
SubscriptionServiceAdapter reemplaza la llamada aBillinglocal: las consultasde
entitlements usan un clienteHTTP con límites de tiempo y clave interna obligatoria.
initializeBaseSubscription registra una filaOutbox medianteJdbcTemplate en la misma
transacciónIAM. Relayposcommit entrega PUTidempotente al propietarioSubscription,
elimina únicamente trasconfirmación y mantiene fallos con backoffhasta300seg.
Esto evita consultar una organizaciónIAMno confirmada desdeSubscriptiondurantealta.
El receptorSubscription, autenticacióninterna, migraciónOutbox y pruebas de fallo/
rollback deben completarse antes de activarGateway. CompilaciónIdentity75087en
proceso; no se declara servicio operativo ni cambiosDOCXconextracciónterminada.

## Avance Subscription
Se reutilizaron107archivosBilling y primitivas técnicas en su repositorio propio.
InternalSubscriptionController implementa PUTbase-subscription idempotente y
GETentitlements/decision, con clave interna obligatoria y comparación constante.
La identidad del workload no concede al navegador esos endpoints. Falta completar
SecurityFilterChain, ACLIAMremota y consultasdeusage remotas; las tablasbilling
permanecen exclusivasSubscription. Identitycompila163archivos con éxito (log
identity-extraction-compile.log), aún sinpruebaderuntime ni migraciones.

ContratosinternosIdentity(GETorgs/exists/usercount/userexists) implementados convalidacionclaveconstanteencadacontrolador;SecurityChainpermiterutainternaparavalidacionworkloadcontrolada,sinJWTnavegador. SubscriptionIdentityServiceAdapter implementaIamContextFacadeporHTTPcontimeouts, sinreposIAM. FaltaAssetUsageHTTP yseguridadSubscriptionantesruntime.

SubscriptionusageahorapuertoAssetUsageReader(application) conadaptadorHTTPinfra,cantidadesvalidadas,sinaccesodirectoAssetRepository;fallono equivaleauso0. ArranqueSubscriptionseedsolocatalogolocal;organizacionesviayoutboxIdentity,idempotenciaowner. POMSubscriptionseparadosinAI,configct_subscription;compile17521activo. Mantenerpasopendientesecurity/migraciones/tests. Portedithechodespuésiniciocompile, repetirsólosicambiobuildrequiere.

## Seguridad de Subscription
SubscriptionSecurity incorpora filtro stateless: sesión remotaIdentity, rechazo401
sinJWT,403si organización de ruta no coincide y503si Identity no estádisponible.
Los endpointsinternos verifican claveworkload comparadaconstante; unJWTnavegador
no sustituye esa clave. CatálogopúblicoGET,OpenAPI/healthGET y webhookStripePOST
conservan sus rutas (webhook mantiene validación de firma en el caso de uso).
CompilaciónSubscriptionanterior124archivoscorrecta; nuevafiltroencorrida69002.
Pendientepruebasdeclaves/aislamiento/permisosdefacturación/CORS,migraciones yruntime;
noactivarGatewaytodavía. Mantenerlasreglasdeautorizaciónfuncional explícitas,
noasumirque perteneceralaorganizaciónconcedegestionarunplan.

MigracionesownerextraidasdelDDLMySQL8 existenteSINdatos,autoincrementcontadoreslimpiados:Identity6tablas+outbox,Subscription4tablas. Aplicadascorrectamenteenesquemasnuevosaisladosct_identity_extraction/ct_subscription_extraction dentrocontenedordepruebas, sin tocarcoldtrace-ui-review. FlywaystarterMySQLañadidoaPOM,validacionHibernateevitacreartablasimplicitamente. Subscriptionsecuritycompile69002SUCCESS; faltatestsejecutableJWT/crossorg/missingkeyyFlywayboot.

RuntimeIdentity56568/Subscription45958descargandoFlyway;enesquemasnuevosct_identity_runtime/ct_subscription_runtime,puertos18101/18107,no backend. TestunitarioInternalIdentityController2casosagregado(sinMockitoparaevitarJava26inline): clavesnulas/incorrectas yconsultasowneracotadas. SubscriptionPOSTbillingexigeadministrators:manage(superadminseedexistente),GETmismaorgpermiteconsultaplan; probarrolinferior403enintegración. Noasumiraprobadoslos2tests hastaejecución.

## Primer runtime independiente comprobado
Los dos servicios arrancan MySQL/Flyway/Hibernatevalidate sinbackend. El fallo de
arranqueCORS heredado se corrigió con propiedadespropias. Alta organización y
signin/sessionContext de Identity ejecutados contra18101, JWT generado propietario.
ConfirmarOutboxvacío yfilasuscripcion trasrelay antesdeafirmarentregaprobada.
RuntimeactualIdentity47337/Subscription34742, puerto18101/18107. Subscriptioncors
habilitadoenSecurityChain, cambioaunrequiererestart/test. No registrarcredenciales
pruebaoJWTenrepositorios. Pendientes: testsJUnit yfallos/redelivery, Assetendpoint
usage todavía inexistente, por eso consultaentitlements no es flujo completo.

OutboxIdentityruntimeconfirmado0pendientes,Subscription1orgbaseFREE,signup/signin18101sinbackend. Assetextraccioniniciada151archivospropietariosramaextractasset;OrganizationRepositoryreemplazadoporpuertoOrganizationDirectoryyHTTPIdentity, sincopiarentidadesIAM. Faltasecuritysessionsadapter/Subscriptionadapterusagesendpoint/config/migraciones/tests, noafirmarruntimeAssetlisto.

Assetsession/securityreutilizapatronSubscriptionconpermisoassets:manageescritura,GETassets:view/operations:view,scopeorganizacion;AuthenticationprincipaluserIDparaconservaSourceConfigurationactor. SessionContextFacadeadaptadorsinreposIAMleecontextovalidadoporfiltro. Endpointinternalusageimplementado,claveworkloadenfiltro. AdapterSubscriptionreutilizaDTOclient(entitlements);noownersBilling. compileAssetenproceso(logasset-extraction-compile.log). Faltamigraciones/testsecurityincluidocrossorg/perfilhistorial/ingesta,evaluarpararevisarDOCXsiinteraccioncambia.

## Asset baseline y compilación
Assetcompila173archivosJava (buildSUCCESS). MigraciónownerV1de12tablas(extraida
DDLtestexistente,sinfilas,contadoresAUTO_INCREMENTlimpios) enrepositorioAsset.
Runtimeaislado18102/ct_asset_runtime iniciado; nohabilitarDDLupdate. Cliente
SubscriptionresuelveIdentity18101/Subscription18107. Outboxlocalusaemulador8681
explicitamente; validarnombredepropiedadenvcorrectoantesconsiderarpublishactivo.
TestsIdentityInternalIdentityControllerTestencorridaindependiente,logsidentity-
extraction-tests.log. NingunendpointGatewayha sidocambiadoaestosruntimesaun.

Identitytests2casospasan0fallos(InternalIdentityController). Assetruntime42099termino sinpuertoactivoaunqueMavenexit0; revisarApplicationFailed/Causedby enasset-extraction-runtime.log; NOafirmarAssetarrancado porBUILD SUCCESS deDevTools.


## Verificación integrada del 4 de noviembre

Identity (18101), Asset (18102) y Subscription (18107) arrancan con Flyway y
validación de Hibernate sobre esquemas aislados. El ensayo HTTP reproducible
`tests/Verify-ServiceBoundaries.ps1` está en el checkout vigente de Infrastructure:
`coldtrace-services/sprint1/coldtrace-infrastructure`, rama
`feature/source-configuration-routing`. Pasaron sus once comprobaciones: alta,
sesión, suscripción eventual, ubicación, límite Base, aislamiento de Asset y
Subscription, credencial interna, anonimato y preflight CORS.

Pasaron además dos pruebas de Identity y tres pruebas de contrato del cliente de
Subscription en Asset. El cliente rechaza respuestas de otra organización y
traduce indisponibilidad a 503. Se quitó del cliente Asset la capacidad de entregar
órdenes de alta, que pertenece exclusivamente a Identity.

El ensayo de caída de Subscription dejó la orden de la organización 2 con tres
intentos; al recuperarse quedó una sola suscripción y ninguna orden pendiente.
La inyección de fallo MySQL al insertar el usuario revirtió organización y Outbox.
Ese ensayo descubrió que la seguridad heredada ocultaba errores internos como
401; se permite el despacho ERROR para conservar el estado real. Repetir el ensayo
tras reiniciar Identity para validar esa corrección de respuesta HTTP.

ADR-0017 registra el protocolo y sus consecuencias. La configuración por defecto
respeta los puertos del documento: Subscription 8088, AI Assistance 8087. Los
puertos 18101/18102/18107 son exclusivos del ensayo. El stack del repositorio ya
distingue Java 26/Spring Boot 4 reutilizado de Java 21/Spring Boot 3 previo.

Los checkouts antiguos en `coldtrace-sprint1-repositories` de Gateway, Monitoring,
Alert, Report e Infrastructure NO son la versión vigente del Sprint 2. Trabajar
sobre sus submódulos en `coldtrace-services/sprint1`, que sí contienen los commits
publicados. Los nuevos repositorios Identity/Asset/Subscription siguen en la raíz
hermana; no sobrescribir la copia histórica modificada de Report.

En ejecución: pruebas originales de SourceConfigurationIntegrationTest copiadas a
Asset y habilitadas explícitamente con COLDTRACE_MYSQL_INTEGRATION=true; construcción
Docker de Identity. No considerar esos procesos aprobados hasta revisar sus logs.
Sigue pendiente la extracción Maintenance/AI, los contratos de lectura restantes,
la migración de datos, el cambio del Gateway y la validación sin backend. DOCX:
incorporar únicamente resultados comprobados, sin alterar la historia del Sprint 1.


## Continuaci�n del 5 de noviembre

Las suites de lectura de propietarios terminaron: Monitoring 46 pruebas y Report
7, ambas aprobadas. Se reutilizaron las clases originales de planes IA en Alert y
las de res�menes en Report. La generaci�n se invoca mediante un puerto HTTP hacia
AI Assistance, con contratos de resultados y fallos controlados. Alert mantiene
la aprobaci�n y el cierre del incidente en una sola transacci�n local; no crear
una saga ni trasladar sus tablas a AI (ADR-0018).

AI Assistance pas� seis pruebas: tres de su cliente propietario y tres HTTP de
arranque sin base, autenticaci�n interna y proveedor desactivado sin contenido
ficticio. Se retiraron sus dependencias de persistencia y se renombr� el arranque
a AiAssistanceApplication. Es stateless; el esquema ct_ai_runtime previo era un
ensayo vac�o y ya no se utiliza.

Alert compila y pas� sus 13 pruebas existentes con la integraci�n JPA de planes,
cuatro tablas propietarias reutilizadas y Flyway V3. Faltan las pruebas del ciclo
humano de aprobaci�n/rollback/concurrencia. Report compila con su lector local de
evidencia y las clases de resumen adaptadas a DTO, sin importar su antigua entidad.
Falta ejecutar su nueva suite. Ninguno de estos cambios nuevos est� publicado a�n.

Herramienta de ensayo Java 21: Temurin 21.0.13+9 se descarg� desde la API oficial
Adoptium, se comprob� SHA-256 y se extrajo �nicamente en
C:/Users/david/.cache/coldtrace-toolchains/jdk21. No se cambi� JAVA_HOME global.
Usar JAVA_HOME de ese JDK solo en los procesos Maven de servicios Java 21; los
servicios reutilizados de Spring Boot 4 siguen usando el JDK26 original.

Hallazgo de integraci�n: ReplicaProvisioning utiliza una sola URL para consultar
sesi�n y activos. Debe dividirse Identity/Asset antes de cambiar el Gateway.
Tambi�n faltan las consultas p�blicas de sensor-readings y la compatibilidad de
API de organizaci�n de Alert/Report. Auditar las llamadas reales del frontend;
no quitar el fallback para ocultar rutas que a�n dependen del backend.

Hallazgo de datos: MonitoringStore del frontend genera lecturas autom�ticamente
mediante demo-generations. Separar esa simulaci�n heredada de la consulta del
producto: la ingesta can�nica y sus efectos deben proceder de Edge, sin insertar
lecturas por abrir el dashboard. La simulaci�n y los fixtures siguen disponibles
como herramientas expl�citas de prueba; no inventar telemetr�a para disimular una
fuente sin datos.


## Compatibilidad y eliminaci�n del proxy hist�rico

Se a�adieron las consultas de sensor-readings del navegador en Monitoring, usando
el DTO original y SQL propietario con l�mite e �ndice por organizaci�n/fecha.
ReplicaProvisioning separa Identity y Asset: sus tres nuevas pruebas HTTP usan
servidores distintos y pasaron (sesi�n solo IAM, datos solo Asset, fallo de
historial sin r�plica parcial, rol de lectura sin aprovisionamiento).

El frontend dej� de generar lecturas autom�ticamente al refrescar el dashboard.
Tambi�n dej� de evaluar y crear incidentes/escalamientos desde el navegador: esa
responsabilidad permanece en Monitoring y Alert. Se conservaron las pantallas y
su formato. Pasaron 37 pruebas de Web antes de los �ltimos cambios de rutas.
Los m�todos de simulaci�n de pantallas heredadas no activas no deben reintroducirse
en la ruta principal. OfflineReading del viejo store crea estados locales de
muestra; la vista activa no los utiliza. La persistencia offline real es SQLite
Edge y se valida como tal, no mediante esas filas de interfaz.

Alert publica las consultas de organizaci�n que usa el dashboard y el selector
IA; no se implementa un segundo motor de incidentes desde lecturas del navegador.
Report publica el historial de reportes terminados con el DTO original. Sus siete
pruebas pasaron con el nuevo resumen propietario y puerto de generaci�n.

Gateway est� preparado en c�digo para ocho destinos, sin BACKEND_URL, sin ruta
brownfield y sin exponer /internal. Se a�adieron rutas espec�ficas por organizaci�n
y tiempos para generaci�n IA. Se mantiene la ejecuci�n antigua intacta hasta
validar el entorno completo; no declarar retirado el backend solo por modificar
el router. Suite Gateway en sesi�n 7798, log gateway-extraction-tests.log.

El runtime Alert actual arranc� con MySQL real, Flyway V3 y Hibernate validate en
ct_alert_runtime, puerto18104, PID43092, sesi�n46954; despu�s se a�adieron consultas
compatibles, por lo que debe reconstruirse/reiniciarse para probarlas. Sus17pruebas
pasaron y luego se agregaron aserciones HTTP de esas consultas al test transaccional.
Los tests nuevos completos deben ejecutarse una vez antes de publicar.

Pendiente inmediato: crear ramas feature/complete-service-extraction para los
repos previamente existentes, publicar los cambios verificados, consolidar todos
los clones independientes, configuraci�n Compose y migraci�n de fixtures con
usuarios restringidos por esquema. El despliegue integral cloud sigue separado,
con los tres marcadores autorizados. Mantener activo el objetivo hasta ejecutar
el frontend y todos los servicios sin el backend.


## Publicación y orquestación independiente

Gateway 2ece8ff, Monitoring 7938003, Alert 1fbfa1f, Report 8fe634f y Web
d5c8eff se publicaron en feature/complete-service-extraction. Maintenance
05cb426 y AI Assistance 0b6a5c7 se publicaron en sus ramas feature/extract.
Infrastructure d6f5679 introduce el Compose de los ocho servicios, siete
esquemas con cuentas propietarias, Pub/Sub local, Gateway en 28080 y MySQL
en 23306. La configuración de Compose pasó. Las rutas internas y desconocidas
devuelven 404 en la suite Gateway, con seis casos aprobados.

Se solicitó iniciar Docker Desktop al encontrar ausente su motor Linux.
El build de integración se lanzó en sesión 36298 y registra su salida en
Downloads/veltis-report/extraction-compose-build.log. Confirmar el resultado
antes de iniciar los servicios. Siguen por verificar migración de datos,
recorrido de navegador y Edge, y convergencia del informe y diagramas.
