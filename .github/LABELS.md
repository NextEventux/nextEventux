# Labels recomendados para NextEventux

Los labels permiten clasificar Issues y Pull Requests para facilitar su búsqueda y organización.

Su uso es opcional, conforme a `CONTRIBUTING.md`. No es obligatorio crear labels para utilizar las plantillas de Issues ni para abrir Pull Requests.

Si el equipo decide utilizarlos, deben existir previamente en GitHub para que puedan asignarse automáticamente desde las plantillas.

Los labels no reemplazan el seguimiento oficial en `Control_de_Alcance_NextEventux.xlsx`.

## Labels de tipo

Cuando se utilicen labels de tipo, se recomienda asignar como máximo uno por Issue.

| Nombre | Color | Uso |
| --- | --- | --- |
| `tipo:funcionalidad` | `#1D76DB` | Implementación o revisión de un caso de uso. |
| `tipo:error` | `#D73A4A` | Comportamiento incorrecto o fallo reproducible. |
| `tipo:tarea` | `#5319E7` | Actividad concreta del Sprint Backlog. |
| `tipo:mejora` | `#A2EEEF` | Mejora de una función existente. |
| `tipo:documentación` | `#0075CA` | Cambios en documentos o explicaciones técnicas. |
| `tipo:pruebas` | `#BFDADC` | Creación o corrección de pruebas. |
| `tipo:consulta` | `#D4C5F9` | Pregunta o decisión que debe registrarse. |

## Labels de módulo

Cuando se utilicen labels de módulo, se recomienda elegir el módulo principalmente afectado. Para cambios transversales, puede utilizarse `módulo:compartido`.

| Nombre | Color | Uso |
| --- | --- | --- |
| `módulo:eventos` | `#0E8A16` | CU-01 a CU-05. |
| `módulo:finanzas` | `#FBCA04` | CU-06 a CU-10. |
| `módulo:proveedores` | `#006B75` | CU-11 a CU-15. |
| `módulo:invitados` | `#C2E0C6` | CU-16 a CU-20. |
| `módulo:marketing` | `#D876E3` | CU-21 a CU-25. |
| `módulo:administración` | `#B60205` | CU-26 a CU-30. |
| `módulo:compartido` | `#5EBEFF` | Cambio transversal o compartido. |

## Labels de prioridad

Cuando se utilicen labels de prioridad, se recomienda asignar como máximo uno por Issue, teniendo en cuenta el alcance y la iteración actual.

| Nombre | Color | Uso |
| --- | --- | --- |
| `prioridad:alta` | `#B60205` | Impide una entrega, bloquea otro trabajo o corrige un riesgo grave. |
| `prioridad:media` | `#FBCA04` | Necesario para la iteración, pero no bloquea inmediatamente. |
| `prioridad:baja` | `#C5DEF5` | Puede aplazarse sin comprometer la entrega actual. |

## Labels adicionales

| Nombre | Color | Uso |
| --- | --- | --- |
| `estado:bloqueado` | `#000000` | Existe un impedimento real que requiere intervención. |
| `requiere-decisión` | `#E99695` | El equipo o el profesor debe aprobar una opción. |
| `buena-primera-tarea` | `#7057FF` | Actividad pequeña y adecuada para familiarizarse con el repositorio. |

## Relación con el Kanban

Cuando el equipo utilice GitHub Project, puede organizar el trabajo mediante estados como `Por hacer`, `En progreso`, `En revisión` y `Terminado`.

No es necesario crear labels para duplicar esos estados. El label `estado:bloqueado` puede utilizarse cuando exista un impedimento real.

El tablero y los labels son herramientas de apoyo. El seguimiento oficial de tareas, estados y horas faltantes se mantiene en `Control_de_Alcance_NextEventux.xlsx`.

## Regla de uso

Una combinación típica sería:

```text
tipo:funcionalidad + módulo:proveedores + prioridad:alta
```

Si la actividad queda impedida, puede agregarse temporalmente `estado:bloqueado` y explicar el bloqueo en un comentario. Cuando se resuelva el impedimento, se retira el label.
