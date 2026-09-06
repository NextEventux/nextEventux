# Guía de contribución de NextEventux

Esta guía define cómo registrar, desarrollar, revisar e integrar cambios en NextEventux. Su objetivo es mantener trazabilidad entre los casos de uso, el código, las pruebas y los documentos del proyecto.

## 1. Fuentes de información y trazabilidad

Cada herramienta cumple una función diferente:

| Recurso | Uso |
| --- | --- |
| SRS | Define los requisitos, los casos de uso y el comportamiento esperado del producto. |
| PMP | Define el proceso de trabajo, las responsabilidades y los controles de calidad. |
| GitHub Issues y Project | Funcionan como Kanban operativo para organizar y relacionar el trabajo técnico. |
| `Control_de_Alcance_NextEventux.xlsx` | Es la fuente oficial de estados, horas faltantes, métricas y Burndown Chart. |
| Git y GitHub | Conservan el historial del código, las pruebas y los cambios revisados. |

Los Issues de GitHub no sustituyen la actualización del Excel. Cuando una tarea cambie de estado o de horas faltantes, el responsable debe reflejarlo también en el archivo de Control de Alcance.

## 2. Flujo de trabajo

1. Seleccionar o crear un Issue con alcance y criterios de aceptación claros.
2. Asignar el Issue a la persona responsable y ubicarlo en el tablero Kanban.
3. Crear una rama a partir de `develop`.
4. Realizar commits pequeños y relacionados con un solo objetivo.
5. Ejecutar las pruebas aplicables y conservar la evidencia.
6. Publicar la rama y abrir un Pull Request hacia `develop`.
7. Atender los comentarios de revisión.
8. Obtener la aprobación de un compañero distinto del autor.
9. Fusionar el Pull Request y eliminar la rama remota si ya no se necesita.
10. Comprobar el cierre del Issue y actualizar el estado y las horas faltantes en Excel.

La rama `main` solo contendrá versiones estables aprobadas para una entrega. Los cambios ordinarios no deben enviarse directamente a `main`.

## 3. Ramas

| Rama | Propósito | Ejemplo |
| --- | --- | --- |
| `main` | Versiones estables y aprobadas para entrega. | `main` |
| `develop` | Integración del trabajo realizado durante los sprints. | `develop` |
| `feature/*` | Desarrollo de una funcionalidad o caso de uso. | `feature/CU-12-aceptar-rechazar-solicitud` |
| `fix/*` | Corrección de un error. | `fix/CU-12-conservar-version-propuesta` |
| `docs/*` | Cambio exclusivo de documentación técnica del repositorio. | `docs/actualizar-readme` |
| `test/*` | Adición o ajuste aislado de pruebas. | `test/CU-13-validacion-presupuesto` |

Las ramas `feature/*`, `fix/*`, `docs/*` y `test/*` nacen desde `develop` y regresan a `develop` mediante Pull Request.

## 4. Commits

### 4.1 Formato

Cada commit debe seguir esta estructura:

```text
tipo(módulo): descripción breve
```

Si el cambio corresponde a un caso de uso, se agrega el identificador al final:

```text
feat(proveedores): implementar respuesta a solicitud CU-12
fix(finanzas): impedir contratación sobre el presupuesto CU-09
docs(srs): precisar flujo alternativo del CU-12
test(eventos): cubrir cancelación de evento CU-04
```

### 4.2 Etiquetas de commit

| Etiqueta | Cuándo usarla |
| --- | --- |
| `feat` | Nueva funcionalidad visible o regla de negocio. |
| `fix` | Corrección de un comportamiento defectuoso. |
| `docs` | Cambio exclusivo de documentación. |
| `test` | Creación o modificación de pruebas. |
| `refactor` | Reorganización del código sin cambiar su comportamiento esperado. |
| `build` | Cambios en dependencias o configuración de construcción. |
| `chore` | Mantenimiento que no modifica la funcionalidad del producto. |

### 4.3 Módulos permitidos

`eventos`, `finanzas`, `proveedores`, `invitados`, `marketing`, `administracion`, `compartido`, `database`, `ui`, `docs`.

La descripción se escribe en minúscula, en infinitivo, sin punto final y debe expresar un solo cambio. No se aceptan mensajes vagos como `cambios`, `arreglos`, `avance` o `commit final`.

## 5. Pull Requests

### 5.1 Contenido obligatorio

Cada Pull Request debe:

1. Tener un título con el formato `[TIPO][MÓDULO] descripción`, por ejemplo: `[FEATURE][PROVEEDORES] Implementar CU-12`.
2. Relacionar el Issue correspondiente mediante `Closes #número` cuando deba cerrarse al fusionar.
3. Indicar el módulo y el caso de uso afectados.
4. Explicar el problema u objetivo y resumir los cambios realizados.
5. Describir las pruebas ejecutadas y su resultado.
6. Adjuntar evidencia cuando exista una interfaz, un error corregido o una validación importante.
7. Informar efectos sobre base de datos, requisitos, documentación u otros módulos.
8. Completar la lista de verificación de `.github/pull_request_template.md`.

Un Pull Request debe atender un solo objetivo principal. No debe mezclar funcionalidades independientes, correcciones ajenas o cambios masivos de formato.

### 5.2 Aprobación y fusión

- Se requiere **una aprobación de un compañero distinto del autor** antes de fusionar a `develop`.
- El autor no puede contar como su propio revisor.
- Las conversaciones de revisión deben quedar resueltas o justificadas antes de la fusión.
- Si el cambio afecta otro módulo, se debe avisar a su responsable y dejar constancia en el Pull Request.
- Deben ejecutarse las pruebas aplicables. Si una prueba no puede ejecutarse, el motivo y el riesgo deben quedar escritos.
- No se fusionará código con errores conocidos que impidan cumplir los criterios de aceptación.
- `main` recibirá únicamente versiones verificadas que ya hayan sido integradas en `develop`.

## 6. Issues y tablero Kanban

### 6.1 Dónde están las plantillas

Las plantillas están en `.github/ISSUE_TEMPLATE/` y deben estar presentes en la rama predeterminada del repositorio para que GitHub las muestre al seleccionar **New issue**.

| Plantilla | Uso |
| --- | --- |
| `kanban_task.yml` | Tarea planificada de un sprint o actividad asociada a un caso de uso. |
| `general_issue.yml` | Situación válida que no encaja en las demás plantillas. |
| `bug_report.yml` | Comportamiento incorrecto y reproducible del software. |
| `change_request.yml` | Cambio propuesto en alcance, requisito, regla de negocio o diseño. |
| `documentation.yml` | Corrección o actualización de documentación. |
| `config.yml` | Configura el selector de plantillas y evita Issues vacíos sin estructura. |

Los archivos `.yml` son formularios: GitHub convierte las respuestas en el cuerpo del Issue. Los campos obligatorios evitan abrir tareas sin objetivo, evidencia o criterios de aceptación. Los labels automáticos solo se aplican si esos labels ya fueron creados en el repositorio.

### 6.2 Uso correcto de una tarea Kanban

Una tarjeta debe representar trabajo verificable y suficientemente pequeño para avanzar dentro de un sprint. Debe contener responsable, módulo, caso de uso cuando aplique, objetivo, criterios de aceptación, prioridad, horas estimadas, dependencias y evidencia esperada.

El tablero puede usar estas columnas:

| Estado en GitHub Project | Equivalencia en Control de Alcance |
| --- | --- |
| Backlog / Por hacer | No comenzado |
| En progreso | En progreso |
| En revisión | En progreso |
| Bloqueado | Bloqueado |
| Terminado | Completado, únicamente si cumple la Definition of Done |

La columna `En revisión` permite mostrar que la implementación terminó, pero todavía no ha sido aprobada o integrada. Por eso aún corresponde a `En progreso` en el control oficial.

### 6.3 Cierre de Issues

El método preferido es escribir `Closes #número` en el Pull Request. El Issue se cerrará automáticamente cuando el PR se fusione en la rama configurada por GitHub. No debe cerrarse solo porque se escribió el código o se abrió el PR.

Antes de cerrar una tarea se debe comprobar que:

- cumple sus criterios de aceptación;
- cuenta con las pruebas y evidencias aplicables;
- fue revisada por un compañero;
- está integrada en la rama correspondiente;
- no deja documentación necesaria sin actualizar;
- su estado y sus horas faltantes fueron actualizados en Excel.

Comentario de cierre por implementación:

```text
Se cierra porque el trabajo quedó implementado en el PR #___, revisado por @___ y validado mediante ___. Se actualizaron el estado y las horas faltantes en Control_de_Alcance_NextEventux.xlsx.
```

Comentario de cierre sin implementación:

```text
Se cierra sin implementar porque ___. La decisión quedó acordada en ___. El trabajo fue reemplazado por #___ / no hace parte del alcance vigente. Se actualizó el control correspondiente.
```

No se deben borrar Issues para ocultar errores o decisiones descartadas: cerrarlos con una explicación conserva la trazabilidad.

## 7. Labels personalizados

Cada Issue debe tener, como mínimo, un label de tipo, uno de módulo y uno de prioridad. Los labels de estado no deben reemplazar las columnas del Project; solo se utiliza `estado:bloqueado` como alerta temporal.

La propuesta completa de nombres, colores y descripciones está en `docs/ETIQUETAS.md`. No conviene crear un label por cada caso de uso: el identificador `CU-XX` se registra en el título y en el formulario, evitando mantener 30 labels innecesarios.

## 8. Definition of Done

Una funcionalidad se considera terminada cuando está implementada, probada, revisada por al menos un compañero, integrada con los módulos relacionados y disponible en el ambiente de pruebas. Además, el Issue debe tener evidencia y el Control de Alcance debe reflejar el estado real.
