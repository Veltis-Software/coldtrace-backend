workspace "ColdTrace - Veltis Software" "Modelo C4 de ColdTrace (TF 1ASI0657 - Grupo 13). Iteraciones ADD 0-3. Disponibilidad, rendimiento e interoperabilidad." {

    !identifiers hierarchical

    !impliedRelationships false

    model {
        gustavo = person "Encargado de negocio" "Dueño o encargado de minimarket, carnicería, pescadería o restaurante (User Persona: Gustavo Fernández)."
        sofia = person "Responsable de operaciones / calidad" "Supervisa varios activos y sedes, atiende incidencias y sustenta auditorías (User Persona: Sofía Ramírez)."

        sensors = softwareSystem "Sensores IoT" "Miden temperatura y humedad en cámaras, almacenes y transporte refrigerado." "External"
        httpSources = softwareSystem "Fuentes HTTP/JSON" "Equipos compatibles que entregan lecturas mediante HTTP y un perfil de origen configurado." "External"
        notif = softwareSystem "Proveedores de notificación" "SendGrid (correo), FCM (Web Push) y WhatsApp Business API." "External"
        idp = softwareSystem "Google / Apple Identity" "Inicio de sesión externo mediante OAuth 2.0 / OIDC." "External"
        stripe = softwareSystem "Stripe" "Checkout, Customer Portal y Webhooks de suscripción." "External"
        ai = softwareSystem "Proveedor de IA" "OpenAI (desplegado) u Ollama (local) vía Spring AI." "External"

        coldtrace = softwareSystem "ColdTrace" "Plataforma web de monitoreo de cadena de frío." {
            edge = container "Edge Gateway Agent" "Adapta fuentes compatibles; conserva lecturas y lotes en SQLite durante cortes; reenvía con confirmación y heartbeat." "Python, SQLite" "Edge" {
                profiles = component "Source Profile Resolver" "Resuelve fuente, dispositivo y perfil de conversión desde configuración versionada." "Application Service"
                mqttInput = component "MQTT Input Adapter" "Recibe el tópico de una fuente configurada y extrae su payload JSON." "paho-mqtt"
                httpInput = component "HTTP Input Adapter" "Recibe JSON de fuentes admitidas y valida su perfil de origen." "HTTP/JSON"
                normalizer = component "Reading Normalizer" "Convierte unidades admitidas a °C y %RH, fechas a UTC y conserva identidad de origen." "Domain Service"
                pipeline = component "Incorporation Pipeline" "Coordina resolución, normalización y escritura durable; rechaza entradas incompatibles antes de almacenar." "Application Service"
                buffer = component "Durable Buffer" "SQLite WAL conserva secuencias y lotes estables hasta confirmación completa." "SQLite Repository"
                sender = component "Durable Batch Sender" "Reintenta lotes canónicos v1 con la misma clave idempotente y envía heartbeat." "HTTP Adapter"
            }
            web = container "Web Application" "Paneles de monitoreo, alertas, incidencias y reportes (responsive)." "Angular" "Web"
            gateway = container "API Gateway" "Punto de entrada único; enruta cada ruta al microservicio o al backend de referencia (Strangler Fig)." "Spring Cloud Gateway"

            identity = container "identity-service" "Usuarios, roles, organizaciones y sesiones (JWT)." "Java, Spring Boot"
            asset = container "asset-service" "Activos, rangos seguros, gateways y dispositivos." "Java, Spring Boot"
            monitoring = container "monitoring-service" "Ingesta de telemetría, evaluación de rango seguro y estado actual del activo." "Java, Spring Boot" {
                validator = component "Canonical Contract Validator" "Valida versión, identidad, unidades canónicas y contexto antes de evaluar y persistir." "Application Service"
                ingestion = component "Telemetry Ingestion" "Recibe lotes, valida la clave del dispositivo e inserta de forma idempotente." "Spring MVC Controller"
                gap = component "Source Gap Detector" "Cada 10 s detecta fuentes sin heartbeat ni lecturas y marca el activo como 'sin datos'." "Spring @Scheduled"
                evaluator = component "Safe Range Evaluator" "Evalúa la lectura contra la réplica local de rangos seguros." "Domain Service"
                replica = component "Safe Range Replica" "Copia local de rangos, actualizada por el evento asset.settings-changed." "Domain Repository"
                projection = component "Current State Projection" "Mantiene asset_current_state (una fila por activo) para paneles rápidos." "Domain Repository"
                outbox = component "Outbox Relay" "Publica en Pub/Sub los eventos guardados en la tabla outbox." "Spring @Scheduled"
            }
            alert = container "alert-service" "Alertas, incidencias y notificaciones." "Java, Spring Boot" {
                alertDetail = component "Alert Detail Query" "Devuelve en una sola llamada activo, lectura, rango, estado, acciones por rol y responsable sugerido." "Spring MVC Controller"
                ackAssign = component "Acknowledge & Assign Command" "Reconoce y asigna la alerta en una transacción; permite deshacer durante 10 s." "Application Service"
                stream = component "Alert Stream Publisher" "Envía alert.updated a las sesiones abiertas." "Server-Sent Events"
                dispatcher = component "Notification Dispatcher" "Envía la notificación con enlace directo /alerts/{id} por el canal configurado." "NotificationChannel (puerto)"
                breachConsumer = component "Event Consumer" "Consume threshold.breached y source.gap de forma idempotente y abre la incidencia." "Pub/Sub push handler"
            }
            report = container "report-service" "Históricos, agregados por hora y reportes de trazabilidad (trabajos asíncronos)." "Java, Spring Boot"
            maintenance = container "maintenance-service" "Mantenimientos preventivos y solicitudes técnicas." "Java, Spring Boot"
            aiService = container "ai-assistance-service" "Planes de resolución asistidos por IA con aprobación humana." "Java, Spring Boot, Spring AI"
            subscription = container "subscription-service" "Planes, suscripciones y webhooks de facturación." "Java, Spring Boot"

            bus = container "Event Bus" "Tópicos threshold.breached, source.gap, readings.ingested, asset.settings-changed, source.configuration-changed, incident.opened." "Google Cloud Pub/Sub" "Bus"
            db = container "Cloud SQL" "Un esquema por microservicio; ningún servicio accede a tablas de otro." "MySQL 8" "Database"
        }

        # --- Relaciones a nivel de sistema (diagrama de contexto) ---
        gustavo -> coldtrace "Vigila sus equipos de refrigeración y atiende alertas"
        sofia -> coldtrace "Supervisa activos, gestiona incidencias y genera reportes de trazabilidad"
        sensors -> coldtrace "Envía lecturas de temperatura y humedad"
        coldtrace -> notif "Envía alertas por correo, Web Push o WhatsApp"
        coldtrace -> idp "Verifica identidad externa (OIDC)"
        coldtrace -> stripe "Gestiona suscripciones y pagos"
        coldtrace -> ai "Solicita planes de resolución"

        # --- Relaciones de usuarios ---
        gustavo -> coldtrace.web "Consulta el estado de sus equipos y atiende alertas" "HTTPS"
        sofia -> coldtrace.web "Supervisa activos, gestiona incidencias y genera reportes" "HTTPS"
        coldtrace.web -> coldtrace.gateway "Llama a la API" "REST/JSON"
        notif -> gustavo "Entrega la alerta"
        notif -> sofia "Entrega la alerta"

        # --- Iteración 0 (línea base): sensores directo a la nube ---
        rSensorDirect = sensors -> coldtrace.gateway "Envía lecturas" "HTTPS"
        # --- Iteración 1: sensores -> Edge Gateway Agent ---
        rSensorEdge = sensors -> coldtrace.edge "Publica lecturas" "MQTT (red local)"
        rEdgeGw = coldtrace.edge -> coldtrace.gateway "Envía lotes y heartbeat" "HTTPS + Idempotency-Key"

        coldtrace.gateway -> coldtrace.identity "Enruta /authentication, /users" "REST/JSON"
        coldtrace.gateway -> coldtrace.asset "Enruta /assets, /devices" "REST/JSON"
        coldtrace.gateway -> coldtrace.monitoring "Enruta telemetría y estado" "REST/JSON"
        coldtrace.gateway -> coldtrace.alert "Enruta /alerts, /incidents" "REST/JSON"
        coldtrace.gateway -> coldtrace.report "Enruta /reports" "REST/JSON"
        coldtrace.gateway -> coldtrace.maintenance "Enruta /maintenance" "REST/JSON"
        coldtrace.gateway -> coldtrace.aiService "Enruta /resolution-plans" "REST/JSON"
        coldtrace.gateway -> coldtrace.subscription "Enruta /billing" "REST/JSON"

        # Iteración 0: comunicación síncrona entre servicios (ACL)
        rMonAlertSync = coldtrace.monitoring -> coldtrace.alert "Notifica desviación (síncrono)" "REST/JSON"
        rMonAssetSync = coldtrace.monitoring -> coldtrace.asset "Consulta rango seguro por lectura" "REST/JSON"

        # Iteración 2: mensajería asíncrona
        rMonBus = coldtrace.monitoring -> coldtrace.bus "Publica threshold.breached, source.gap, readings.ingested" "Pub/Sub"
        rAssetBus = coldtrace.asset -> coldtrace.bus "Publica cambios de rangos y fuentes" "Pub/Sub"
        rBusAlert = coldtrace.bus -> coldtrace.alert "Entrega threshold.breached, source.gap" "Push autenticado"
        rBusReport = coldtrace.bus -> coldtrace.report "Entrega readings.ingested" "Push autenticado"
        rBusMon = coldtrace.bus -> coldtrace.monitoring "Entrega cambios de rangos y fuentes" "Push autenticado"

        coldtrace.identity -> coldtrace.db "Lee/escribe esquema identity" "JDBC"
        coldtrace.asset -> coldtrace.db "Lee/escribe esquema asset" "JDBC"
        coldtrace.monitoring -> coldtrace.db "Lee/escribe esquema monitoring" "JDBC"
        coldtrace.alert -> coldtrace.db "Lee/escribe esquema alert" "JDBC"
        coldtrace.report -> coldtrace.db "Lee/escribe esquema report" "JDBC"
        coldtrace.maintenance -> coldtrace.db "Lee/escribe esquema maintenance" "JDBC"
        coldtrace.aiService -> coldtrace.db "Lee/escribe esquema ai" "JDBC"
        coldtrace.subscription -> coldtrace.db "Lee/escribe esquema billing" "JDBC"

        coldtrace.alert -> notif "Envía alertas con enlace directo" "HTTPS"
        coldtrace.identity -> idp "Verifica identidad externa" "OIDC"
        coldtrace.subscription -> stripe "Gestiona suscripción" "HTTPS"
        stripe -> coldtrace.gateway "Webhooks de facturación" "HTTPS"
        coldtrace.aiService -> ai "Genera plan de resolución" "HTTPS"

        # --- Componentes monitoring-service (Iteraciones 1 y 2) ---
        coldtrace.edge -> coldtrace.monitoring.ingestion "POST /api/v1/telemetry/batches, /heartbeats" "HTTPS"
        coldtrace.monitoring.ingestion -> coldtrace.monitoring.evaluator "Evalúa cada lectura"
        coldtrace.monitoring.evaluator -> coldtrace.monitoring.replica "Lee rango seguro"
        coldtrace.monitoring.evaluator -> coldtrace.monitoring.projection "Actualiza estado actual"
        coldtrace.monitoring.ingestion -> coldtrace.db "Inserta lectura + evento outbox (misma transacción)" "JDBC"
        coldtrace.monitoring.gap -> coldtrace.db "Revisa last_heartbeat_at" "JDBC"
        coldtrace.monitoring.gap -> coldtrace.monitoring.projection "Marca activo 'sin datos'"
        coldtrace.monitoring.projection -> coldtrace.db "asset_current_state" "JDBC"
        coldtrace.monitoring.outbox -> coldtrace.db "Lee eventos pendientes" "JDBC"
        coldtrace.monitoring.outbox -> coldtrace.bus "Publica eventos" "Pub/Sub"
        coldtrace.bus -> coldtrace.monitoring.replica "asset.settings-changed" "Push"
        coldtrace.web -> coldtrace.monitoring.projection "Consulta estado actual (vía API Gateway)" "REST/JSON"

        # --- Componentes alert-service (Iteración 3) ---
        coldtrace.bus -> coldtrace.alert.breachConsumer "threshold.breached, source.gap" "Push"
        coldtrace.alert.breachConsumer -> coldtrace.alert.dispatcher "Solicita notificación"
        coldtrace.alert.dispatcher -> notif "Notificación con deep link /alerts/{id}" "HTTPS"
        coldtrace.web -> coldtrace.alert.alertDetail "GET /api/v1/alerts/{id}/detail" "REST/JSON"
        coldtrace.web -> coldtrace.alert.ackAssign "POST / DELETE /api/v1/alerts/{id}/acknowledgement" "REST/JSON"
        coldtrace.web -> coldtrace.alert.stream "GET /api/v1/alerts/stream" "SSE"
        coldtrace.alert.ackAssign -> coldtrace.alert.stream "Emite alert.updated"
        coldtrace.alert.alertDetail -> coldtrace.db "Lee alerta e incidencia" "JDBC"
        coldtrace.alert.ackAssign -> coldtrace.db "Actualiza estado y responsable" "JDBC"
        coldtrace.alert.breachConsumer -> coldtrace.db "Abre incidencia, processed_events" "JDBC"

        # --- Iteración 3: interoperabilidad sobre la ruta de las iteraciones 1 y 2 ---
        rHttpEdge = httpSources -> coldtrace.edge "Entrega lecturas con perfil configurado" "HTTP/JSON"
        sensors -> coldtrace.edge.mqttInput "Publica lecturas del dispositivo" "MQTT/JSON"
        httpSources -> coldtrace.edge.httpInput "Entrega lectura de origen" "HTTP/JSON"
        coldtrace.edge.mqttInput -> coldtrace.edge.pipeline "Entrega payload y sourceId"
        coldtrace.edge.httpInput -> coldtrace.edge.pipeline "Entrega payload y sourceId"
        coldtrace.edge.pipeline -> coldtrace.edge.profiles "Resuelve perfil vigente"
        coldtrace.edge.pipeline -> coldtrace.edge.normalizer "Normaliza con el perfil resuelto"
        coldtrace.edge.pipeline -> coldtrace.edge.buffer "Guarda lectura canónica y secuencia"
        coldtrace.edge.sender -> coldtrace.edge.buffer "Obtiene lote estable y confirma entrega"
        coldtrace.edge.sender -> coldtrace.gateway "Envía lote v1 y heartbeat" "HTTPS + Idempotency-Key"
        coldtrace.asset -> coldtrace.edge.profiles "Exporta perfiles versionados para recarga local" "Archivo JSON administrado"
        coldtrace.monitoring.ingestion -> coldtrace.monitoring.validator "Valida contrato canónico v1"
        coldtrace.monitoring.validator -> coldtrace.monitoring.evaluator "Entrega lectura compatible"

        # --- Despliegue ---
        prod = deploymentEnvironment "Producción" {
            deploymentNode "Dispositivo del usuario" "Escritorio o móvil" "Navegador web" {
                containerInstance coldtrace.web
            }
            deploymentNode "Local del cliente" "Mini PC / Raspberry Pi" "Linux, Docker" {
                containerInstance coldtrace.edge
            }
            deploymentNode "Google Cloud Platform" "southamerica-west1 (Santiago)" "" {
                deploymentNode "Firebase Hosting" "CDN + HTTPS" "" {
                    infrastructureNode "Archivos estáticos Angular" "Build de ng build" "Firebase"
                }
                deploymentNode "Cloud Run" "Servicios serverless con autoescalado" "" {
                    deploymentNode "API Gateway" "Ingress público" "Contenedor Docker" {
                        containerInstance coldtrace.gateway
                    }
                    deploymentNode "Servicios críticos" "min-instances=1, health checks" "Contenedor Docker" {
                        containerInstance coldtrace.monitoring
                        containerInstance coldtrace.alert
                    }
                    deploymentNode "Servicios de soporte" "Ingress interno, escala a 0" "Contenedor Docker" {
                        containerInstance coldtrace.identity
                        containerInstance coldtrace.asset
                        containerInstance coldtrace.report
                        containerInstance coldtrace.maintenance
                        containerInstance coldtrace.aiService
                        containerInstance coldtrace.subscription
                    }
                }
                deploymentNode "Pub/Sub" "Servicio gestionado" "" {
                    containerInstance coldtrace.bus
                }
                deploymentNode "Cloud SQL" "Respaldos automáticos + PITR" "MySQL 8" {
                    containerInstance coldtrace.db
                }
                infrastructureNode "Observabilidad y secretos" "Cloud Logging, Cloud Monitoring, Secret Manager, Artifact Registry" "GCP"
            }
        }
    }

    views {
        systemContext coldtrace "Contexto" "Diagrama de contexto de ColdTrace." {
            include *
            autoLayout tb 300 200
        }

        container coldtrace "It0_Contenedores" "Iteración 0 - Diagrama de contenedores: estructura general de microservicios por Bounded Context." {
            include gustavo sofia sensors notif idp stripe ai
            include coldtrace.web coldtrace.gateway coldtrace.identity coldtrace.asset coldtrace.monitoring coldtrace.alert coldtrace.report coldtrace.maintenance coldtrace.aiService coldtrace.subscription coldtrace.db
            exclude rSensorEdge rEdgeGw rMonBus rAssetBus rBusAlert rBusReport rBusMon
            exclude "notif -> *"
            autoLayout tb 200 150
        }

        container coldtrace "It1_Contenedores" "Iteración 1 (Disponibilidad) - Edge Gateway Agent y servicios críticos con instancia activa." {
            include gustavo sofia sensors notif
            include coldtrace.edge coldtrace.web coldtrace.gateway coldtrace.asset coldtrace.monitoring coldtrace.alert coldtrace.db
            exclude rSensorDirect rMonBus rAssetBus rBusAlert rBusReport rBusMon
            exclude "notif -> *"
            autoLayout tb 200 150
        }

        component coldtrace.monitoring "It1_Componentes_Monitoring" "Iteración 1 (Disponibilidad) - Componentes de monitoring-service para continuidad del dato." {
            include coldtrace.edge coldtrace.monitoring.ingestion coldtrace.monitoring.gap coldtrace.monitoring.evaluator coldtrace.monitoring.projection coldtrace.db
            autoLayout tb 200 150
        }

        container coldtrace "It2_Contenedores" "Iteración 2 (Rendimiento) - Pipeline asíncrono de telemetría con Pub/Sub." {
            include gustavo sofia sensors notif
            include coldtrace.edge coldtrace.web coldtrace.gateway coldtrace.asset coldtrace.monitoring coldtrace.alert coldtrace.report coldtrace.bus coldtrace.db
            exclude rSensorDirect rMonAlertSync rMonAssetSync
            exclude "notif -> *"
            autoLayout tb 200 150
        }

        component coldtrace.monitoring "It2_Componentes_Monitoring" "Iteración 2 (Rendimiento) - Ruta crítica de una lectura en monitoring-service." {
            include coldtrace.edge coldtrace.web coldtrace.monitoring.ingestion coldtrace.monitoring.evaluator coldtrace.monitoring.replica coldtrace.monitoring.projection coldtrace.monitoring.outbox coldtrace.bus coldtrace.db
            autoLayout tb 200 150
        }

        container coldtrace "It3_Contenedores" "Iteración 3 (Interoperabilidad) - Fuentes compatibles sobre el pipeline asíncrono de la Iteración 2." {
            include gustavo sofia sensors httpSources notif
            include coldtrace.edge coldtrace.web coldtrace.gateway coldtrace.asset coldtrace.monitoring coldtrace.alert coldtrace.report coldtrace.bus coldtrace.db
            exclude rSensorDirect rMonAlertSync rMonAssetSync
            exclude "notif -> *"
            autoLayout tb 200 150
        }

        component coldtrace.monitoring "It3_Componentes_Monitoring" "Iteración 3 (Interoperabilidad) - La validación canónica precede a la evaluación; se conservan réplica, proyección, detección de brechas y Outbox." {
            include coldtrace.edge coldtrace.web coldtrace.monitoring.ingestion coldtrace.monitoring.validator coldtrace.monitoring.gap coldtrace.monitoring.evaluator coldtrace.monitoring.replica coldtrace.monitoring.projection coldtrace.monitoring.outbox coldtrace.bus coldtrace.db
            exclude "coldtrace.monitoring.ingestion -> coldtrace.monitoring.evaluator"
            autoLayout tb 200 150
        }

        component coldtrace.edge "It3_Componentes_Edge" "Iteración 3 (Interoperabilidad) - Adaptadores MQTT y HTTP, perfiles y normalización antes del buffer y reenvío durable de la Iteración 1." {
            include sensors httpSources coldtrace.gateway coldtrace.asset
            include coldtrace.edge.mqttInput coldtrace.edge.httpInput coldtrace.edge.pipeline coldtrace.edge.profiles coldtrace.edge.normalizer coldtrace.edge.buffer coldtrace.edge.sender
            exclude rSensorDirect
            autoLayout tb 200 150
        }

        deployment coldtrace prod "Despliegue" "Diagrama de despliegue de ColdTrace en Google Cloud." {
            include *
            exclude rMonAlertSync rMonAssetSync
            exclude "coldtrace.identity -> coldtrace.db"
            exclude "coldtrace.maintenance -> coldtrace.db"
            exclude "coldtrace.aiService -> coldtrace.db"
            exclude "coldtrace.subscription -> coldtrace.db"
            exclude "coldtrace.report -> coldtrace.db"
            exclude "coldtrace.asset -> coldtrace.db"
            autoLayout tb 200 150
        }

        branding {
            font "Arial"
        }

        styles {
            element "Element" {
                color #ffffff
                fontSize 22
            }
            element "Person" {
                shape person
                background #08427b
            }
            element "Software System" {
                background #1168bd
            }
            element "Container" {
                background #438dd5
            }
            element "Component" {
                background #85bbf0
                color #000000
            }
            element "External" {
                background #999999
            }
            element "Database" {
                shape cylinder
            }
            element "Web" {
                shape webBrowser
            }
            element "Bus" {
                shape pipe
            }
            element "Edge" {
                shape box
                background #2e7d32
            }
            element "Deployment Node" {
                color #000000
                fontSize 26
            }
            element "Infrastructure Node" {
                background #ffffff
                color #000000
            }
            relationship "Relationship" {
                fontSize 20
            }
        }
    }
}
