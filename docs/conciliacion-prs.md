# Conciliación de las PR abiertas

Base revisada: main 4b6834d, después del PR #26. Ese commit conserva marcadores
de conflicto y referencias a migraciones ausentes. No se fusionan nuevas PR sobre
esa base sin reparar y validar primero su arranque.

## Orden de integración

1. Reparación de main: resolver DatabaseConnection, CargarDatosEjemplo y README.
2. PR #25: integrar CU01–CU05 sobre la reparación.
3. PR #17: queda sustituida por CU04 de #25; cerrar solo después de verificar y fusionar #25.
4. PR #16: corregir permisos de invitados y conservar el acceso y la galería de CU05.
5. PR #22: unificar el presupuesto con CU01 y conservar todas las acciones de Mis eventos.
6. PR #24: corregir y validar la contratación de organizadores y sus notificaciones.

Cada rama debe incluir las integraciones anteriores para que las fusiones futuras
se puedan aplicar en ese orden. Los commits de funcionalidades conservan su origen;
los commits de conciliación registran las adaptaciones necesarias.

## Reparación de main

- Se conserva la ruta absoluta configurable de H2 y su inicialización por URL.
- Solo se registran migraciones que existen en el árbol de esta versión.
- La migración 010 comprueba duplicados antes de exigir un organizador por usuario.
- Los metadatos de migración no impiden cargar ejemplos en una base de negocio vacía.
- Las bases recreadas con la misma URL se detectan y vuelven a inicializar.
- No se copian, borran ni modifican las bases reales durante las pruebas.

## Condiciones para fusionar

CONTRIBUTING exige una aprobación de otro compañero, las conversaciones resueltas,
ausencia de conflictos y pruebas correctas. Preparar y publicar las correcciones no
equivale a esa aprobación. La fusión a main queda pendiente hasta cumplirla.

CU13 sigue siendo la dependencia que produce contratos de servicios y confirma el
evento. CU26 contrata organizadores y no sustituye esa dependencia de CU05.

Resultado de la reparación: compilación Java 17 y 135 pruebas JUnit correctas,
incluidas cuatro nuevas de ruta, carga de ejemplos, reapertura y unicidad.
