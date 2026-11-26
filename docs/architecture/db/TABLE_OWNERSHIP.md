# Propiedad de tablas por servicio (ADR-0003)

Fuente de las columnas existentes: diagrama relacional del backend de referencia (Figura 12 del informe, "MySQL schema inferred from current JPA persistence entities and Sprint 4 target entities").
Cada tabla tiene **un único servicio propietario**. Solo el propietario lee y escribe; los demás usan API o eventos.

| Esquema | Servicio | Tablas existentes que migra | Tablas/columnas nuevas (ADD) |
|---|---|---|---|
| `ct_identity` | identity-service | organizations, users, roles, role_permissions, external_identities | — |
| `ct_asset` | asset-service | locations, assets, asset_settings, asset_settings_asset_types, asset_settings_iot_device_types, gateways, iot_devices, iot_device_measurement_parameters | `gateways.api_key_hash`; `asset_settings.settings_version`; `outbox_events` |
| `ct_monitoring` | monitoring-service | sensor_readings | UK `(iot_device_id, recorded_at, sequence_number)` y columna `sequence_number`; `monitored_gateways` (copia de heartbeat); `safe_range_replica`; `device_replica`; `asset_current_state`; `outbox_events` |
| `ct_alert` | alert-service | incidents, notifications, incident_ai_resolution_plans y sus colecciones de pasos, evidencia e incertidumbre | `incidents.assigned_to_user_id`, `incidents.assigned_at`; `processed_events`; `outbox_events` |
| `ct_report` | report-service | reports | `reading_aggregates`; `processed_events` |
| � | ai-assistance-service | Sin persistencia operativa; generaci�n estructurada y composici�n de evidencia | � |
| `ct_maintenance` | maintenance-service | maintenance_schedules, technical_service_requests | — |
| `ct_subscription` | subscription-service | subscription_plans, organization_subscriptions, billing_webhook_events | — |

## Notas de diseño de datos

- **`gateways` es de asset-service** (registro y provisión de claves). monitoring-service necesita el último contacto del gateway en la ruta crítica, por eso guarda su propia tabla `monitored_gateways` (gateway_id, uuid, api_key_hash, last_heartbeat_at), alimentada al provisionar el gateway (copia inicial en la migración + evento futuro). Así se respeta ADR-0003 sin llamadas síncronas.
- **`device_replica`** en monitoring guarda `iot_devices.uuid → (iot_device_id, asset_id, gateway_id, location_id, organization_id, reading_frequency_seconds)` para resolver cada lectura sin llamar a asset-service. Se carga en la migración inicial; su sincronización por evento queda en `OPEN_QUESTIONS.md` (Q-03).
- La hora de captura es **`sensor_readings.recorded_at`** (nombre real); en JSON/eventos: `recordedAt`.
- Una **alerta** es una fila de `incidents`.
- Copia inicial de datos al extraer cada contexto: `INSERT INTO ct_<svc>.<tabla> SELECT ... FROM <bd_actual>.<tabla>` dentro de la ventana de corte del Strangler Fig (ver IMPLEMENTATION_PLAN.md).
