# ADR-0003: Base de datos por servicio (un esquema por servicio)

- **Estado:** Aceptado
- **Fecha:** 2026-10-09
- **Iteración ADD:** Iteración 0
- **Trazabilidad:** DD-03 (DC-04) · Drivers: CON-04, AC-04

## Contexto
La base MySQL es compartida. Se requiere propiedad clara de los datos sin multiplicar instancias de Cloud SQL.

## Decisión
Una instancia Cloud SQL MySQL 8 con **un esquema y un usuario de BD por servicio** (`ct_<servicio>`), con permisos solo sobre su esquema. Referencias entre servicios únicamente por ID; datos ajenos por API o eventos. Migraciones con Flyway por servicio. Propiedad de tablas en `db/TABLE_OWNERSHIP.md`.

## Consecuencias
Sin joins entre servicios; se requiere migrar datos al extraer cada contexto.

## Alternativas descartadas
Base de datos compartida: descartada porque acopla servicios por el esquema.
