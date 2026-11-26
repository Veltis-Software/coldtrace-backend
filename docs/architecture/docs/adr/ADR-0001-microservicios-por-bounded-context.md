# ADR-0001: Microservicios por Bounded Context

- **Estado:** Aceptado
- **Fecha:** 2026-10-09
- **Iteraci贸n ADD:** Iteraci贸n 0
- **Trazabilidad:** DD-02 (DC-02) 路 Drivers: CON-02, CON-03

## Contexto
El backend de referencia es un monolito modular Spring Boot organizado en 8 Bounded Contexts. El curso exige microservicios con DDD (CON-02) y se debe reutilizar el c贸digo existente (CON-03).

## Decisi贸n
Se crea **un microservicio por Bounded Context** (8 servicios): identity, asset, monitoring, alert, report, maintenance, ai-assistance y subscription. Cada uno conserva su repositorio y sus contratos. Los servicios con reglas de negocio y proveedores variables separan dominio, aplicaci髇 y adaptadores; los componentes t閏nicos mantienen una estructura acorde a su funci髇. La generaci髇 de IA no adquiere la propiedad del estado de incidentes o informes (ADR-0018).

## Consecuencias
Alta cohesi贸n sem谩ntica y reutilizaci贸n directa del c贸digo. M谩s servicios que operar y desplegar.

## Alternativas descartadas
Tres servicios gruesos (operaci贸n, negocio, soporte): descartado porque mezcla contextos con ritmos de cambio distintos.
