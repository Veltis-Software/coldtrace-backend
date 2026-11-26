# Preguntas abiertas (confirmar antes de implementar la tarea indicada)

| ID | Pregunta | Afecta | Valor por defecto si no hay respuesta |
|---|---|---|---|
| Q-01 | La startup es Veltis Software pero el paquete es `com.iceq.coldtrace`. ¿Se renombra? | Todos los servicios | Mantener `com.iceq.coldtrace` (no romper el código existente). |
| Q-02 | Valores reales de `incidents.type` y `incidents.severity` en el backend. | T-12, T-21 | Reutilizar los existentes; agregar solo `NO_DATA` en `type`. |
| Q-03 | Sincronización de `device_replica` y `monitored_gateways` cuando se registra o reemplaza un dispositivo/gateway (US03, US29, US30). | T-11 | Copia inicial en la migración; evento futuro `device.registered` requerirá ADR nuevo. Mientras tanto, asset-service llama a `POST /internal/v1/device-replica` de monitoring (ingress interno). |
| Q-04 | Severidad WARNING vs CRITICAL de `threshold.breached`. | T-21 | CRITICAL si la desviación supera `alert_threshold_minutes`; WARNING en caso contrario. |
| Q-05 | ¿Se migra el historial completo de `sensor_readings` o solo los últimos N meses? | T-10 | Migrar todo; `sequence_number = 0` para filas históricas. |
