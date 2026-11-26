# Migraciones Flyway

- `V1__baseline.sql` de cada esquema = copia de la estructura actual de sus tablas (generar con `mysqldump --no-data` de las tablas listadas en `../TABLE_OWNERSHIP.md`). No se incluye aquí porque debe salir del esquema real.
- Los archivos `V2+` contienen **solo** los cambios introducidos por ADD y son obligatorios.
- ct_ai también necesita `processed_events` (copiar la definición de `ct_alert/V2`).
