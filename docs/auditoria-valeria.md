# Auditoría — tareas de Valeria (V1–V7)

**Fecha:** 2026-10-20
**Responsable:** Valeria Salgado Cortés (`vsalgadoo`)
**Base:** `origin/main` en `4b8e338` (merge del PR #28). La auditoría original se hizo sobre `9b11752`.

> **Cambio desde la auditoría:** el PR #28 (`a3ee68d`) **ya se fusionó en `main`** (`4b8e338`, 2026-10-20 02:55, por Isa). Se fusionó antes de ejecutar `mvn test`, así que el paso 1 queda *integrado sin validar* hasta que se completen V1 y V2.

---

## V1 — `mvn test` sobre `9b11752` (paso 0)

Entorno: Windows 11, Java 25.0.1 (Oracle), Maven 3.9.9. Comando: `mvn -B test "-Dnexteventux.home=C:\pruebas\bd-<x>"` (BD aislada para no tocar la base real), en worktrees separados.

| Commit | Tests | Fallos | Errores | Resultado | Detalle |
|---|---|---|---|---|---|
| `9b11752` (paso 0, antes de #28) | 222 | 3 | 1 | **BUILD FAILURE** | Las 4 son de `ReparacionMainTest`. `baseNuevaCargaEjemplos…:28` y `reconstruyeUnaBaseBorrada…:43`: esperaban 1 migración registrada y hay 2 (la 011 de CU26 también se registra). `unicidadNoBorraOrganizadores…:51`: `JdbcSQLSyntaxError`, el índice `UQ_ORGANIZADOR_USUARIO` pertenece a una restricción. `rutaAbsolutaConfigurable…:17`: separador `\` frente a `/` en Windows |
| `4b8e338` (main con #28) | 223 | 1 | 0 | **BUILD FAILURE** | El PR #28 corrigió 3 de las 4 pruebas. Queda `rutaAbsolutaConfigurable…:20`, que **solo falla en Windows**: el código normaliza la ruta a `/` a propósito y el test la comparaba con `\` |
| `e19caac` (rama `fix/test-ruta-windows`) | 223 | 0 | 0 | **BUILD SUCCESS** | Cambio de una línea en el test: `.toString().replace('\\','/')`. No toca código de producción |

**Conclusión:** `main` estaba roto desde `9b11752` (la migración 011 de CU26 entró sin actualizar `ReparacionMainTest`). Con el PR #28 y `e19caac`, la suite pasa completa.

## V2 — PR #28 (`a3ee68d`): revisión de código

Archivos: `RepositoryInvitado.java`, `seed.sql`, `DatosDemostracionH2Test`, `ServiceInvitadosTest`, `ReparacionMainTest`.

- **`RepositoryInvitado.contarAsistentesPorInvitacion`:** ahora cuenta solo invitaciones activas (`INVITACION_ACTIVA`) e invitados que asisten (`INVITADO_ASISTE`). Las dos consultas (invitados y acompañantes) comparten el mismo filtro. Es correcto: antes contaba también a quienes rechazaban.
- **`seed.sql`:** los regalos 1, 2 y 3 pasan a `RESERVADO` y la reserva 2 apunta al regalo 3 (antes estaba duplicada sobre el regalo 1). Así los datos de ejemplo quedan coherentes: cada regalo reservado tiene una sola reserva. El cambio toca solo datos de ejemplo, no migraciones (respeta `f0b2621`).
- **`ReparacionMainTest`:** se actualizó para contar 2 migraciones registradas (010 y 011) y para quitar la clave foránea antes de borrar el índice único. Así se corrigen 3 fallos que ya existían en `9b11752`.
- **Ejecución sobre `4b8e338`:** `DatosDemostracionH2Test` 1/1, `ServiceInvitadosTest` 17/17, `SchemaPersistenciaTest` 2/2. `ReparacionMainTest` pasa en Linux; en Windows falla un único test de ruta, ajeno al PR (ver V1), que se corrige con `e19caac`.

**Veredicto del paso 1: OK.** Nota: el PR se fusionó antes de ejecutar `mvn test`. La evidencia queda registrada aquí de forma retroactiva.

## V3 — R7 Acompañantes

- **La tabla `Acompanante` sí existe:** `schema.sql`, línea 118 (`CREATE TABLE IF NOT EXISTS Acompanante`). `seed.sql` le inserta datos (línea 218) y reinicia su contador (línea 450).
- La usan `RepositoryInvitado`, `ServiceInvitados`, `ViewRespuestaInvitacion` y tres tests (`DatosInvitacionesPrueba`, `RespuestaInvitacionTest`, `ServiceInvitadosTest`).
- **El riesgo R7 técnico queda descartado:** las consultas no fallan, porque la tabla se crea en el esquema base. La auditoría no la encontró porque buscó solo en las migraciones.
- **Decisión pendiente (de negocio, no técnica):** si el equipo mantiene "sin acompañantes", hay que quitar la tabla, las consultas y los datos de ejemplo en un PR aparte. Si no, la regla de negocio se actualiza. **Propuesta:** mantener la tabla (ya funciona y tiene tests) y confirmar la regla con Sofía, que es la responsable de CU16–CU20.

## V4 — CU04-cancelar-evento

| Commit | Contenido | ¿Está en main? | Acción |
|---|---|---|---|
| `a725250` | Conservar reservas compartidas sin vínculo directo | **Sí, de forma equivalente.** La lógica se trasladó a `repositories/ReservasEvento.java` (líneas 27–33: `NOT EXISTS … c.id_disponibilidad IS NULL AND s.id_servicio=d.id_servicio AND c.fecha_servicio=d.fecha …`). Sus 3 tests están en `IntegracionCancelacionModulosTest` (líneas 77, 85 y 94) | Nada que portar |
| `57135bf` | Cancelación con el organizador autenticado | **Sí.** `ViewCancelacion` recibe `SesionUsuario` y muestra al organizador | Nada que portar |
| `f3d712d` | Tests de reservas y avisos entre módulos | **Sí.** Los 7 tests del commit existen en `IntegracionCancelacionModulosTest` de main | Nada que portar |
| `d83cab6`, `76f335a` | Test de sesión y documentación | Sí (según la auditoría) | Nada |
| `f7310ba`, `5c71096` | Documentación | Revisar solo el texto | Opcional |

Los parches no se aplican literalmente (`git apply -R --check` falla) porque `main` refactorizó los archivos. Por eso la comparación se hizo por contenido.
**Propuesta para Sara:** el paso 2 se puede cerrar **sin portar código**. `SesionCancelacionTest` también está en `main` y pasa 8/8. Queda archivar la rama, con el acuerdo del equipo.

## V5 — CU21-segmentar-clientes (30 commits, Saúl)

`git merge-tree` no da conflictos de texto. Sí hay **3 problemas funcionales** que hay que corregir antes de fusionar:

1. **La migración no se carga.** `002_marketing_cu21.sql` no aparece en `DatabaseConnection.inicializarEsquema` ni en `main` ni en la rama. Sin ella no existen `Usuario.pais`, `ciudad`, `fecha_nacimiento` ni `Evento.id_cliente`, y los `UPDATE` del `seed.sql` de CU21 fallan.
2. **Usuario duplicado en `seed.sql`.** La rama inserta `admin.nexteventux`, que ya existe en `main` (`seed.sql`, línea 40, id 7). Hay que quitar ese `INSERT` y su `Usuario_rol` del bloque de marketing.
3. **Numeración.** Ya hay dos `002_*` en `main`. **Propuesta:** renombrar la migración a `012_marketing_cu21.sql` y registrarla con `ejecutarUnaVez(connection, "012_marketing_cu21.sql", null);` después de la `011` (según el acuerdo J1).

**Compatibilidad con Eventos:**
- `entities/SolicitudEvento` (CU21) significa "cuántas veces buscó un cliente un tipo de evento". **No choca por nombre de archivo** con las clases de Eventos (`SolicitudServicioEvento`, `SolicitudEjecucion`, `SolicitudPublicacion`), pero el nombre se confunde con las solicitudes de evento de CU01 (`009_solicitudes_evento.sql`). **Propuesta:** renombrarla a `BusquedaEvento`, que coincide con su tabla `Busqueda_evento`.
- `Evento.id_cliente` es una columna nueva. CU26 usa `id_cliente` solo en sus propias tablas (`Solicitud_Contratacion_Organizador`, reseñas), así que no hay choque. Hay que verificar que `RepositoryEvento` no rompa los `INSERT` (la columna es *nullable*).
- Las tablas nuevas `Busqueda_evento` y `Segmento_cliente` no existen en `main`. Ojo: `main` ya tiene `Segmentacion`, que es otra tabla. Conviene confirmar con Saúl que no la duplica.
- No integrar `MarketingDemoApp` ni `MarketingLauncher` (R8).

## V6 — CU06–CU10

- CU06: integrado (PR #22), test `ServiceFinancieraH2Test`.
- CU07–CU10: sin código, rama ni PR en el remoto. **Pendiente:** preguntar al equipo si alguien tiene trabajo local sin subir.

## V7 — Columna "Pruebas" (CU01–CU05, CU16–CU20)

Ejecución real sobre `main` `4b8e338` + `e19caac` (223/223 en verde). Entre paréntesis, tests pasados / ejecutados cuando la cifra aparece en el log revisado. El resto pasa como parte del total de 223 sin fallos.

| CU | Tests | Resultado |
|---|---|---|
| 01 | `ServiceEventosTest` (7/7), `RepositoryEventoTest` (7/7), `SolicitudesEventoTest` (13/13), `PruebaCU01`, `PruebaEventoSchema` | Pasa |
| 02 | `EdicionEventoTest` (8/8), `SesionEdicionTest` (7/7) | Pasa |
| 03 | `PublicacionTest` (13/13), `PublicacionDemoTest` (1/1), `SesionPublicacionTest` (9/9) | Pasa |
| 04 | `IntegracionCancelacionModulosTest` (10/10), `SesionCancelacionTest` (8/8), `CancelacionTest` | Pasa. `SesionCancelacionTest` **sí está en main** (la auditoría decía que solo estaba en la rama) |
| 05 | `FinalizacionTest` (15/15) | Pasa |
| 06 | `ServiceFinancieraH2Test` (17/17) | Pasa |
| 16 | `RespuestaInvitacionTest` (10/10) | Pasa |
| 17 | `ServiceInvitadosTest` (17/17), `RepositoryInvitacionTest` (2/2) | Pasa |
| 18, 19 | — | Sin código (E) |
| 20 | `MesaRegalosTest` (17/17), `DatosDemostracionH2Test` (1/1) | Pasa |

Ningún CU se marca como **A** solo con las pruebas: falta el arranque de la app con BD vacía y con BD migrada, y entrar con cada rol (sección 7 de la auditoría).
