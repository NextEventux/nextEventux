# Guía de contribución de NextEventux

Esta guía define cómo registrar, desarrollar, revisar e integrar cambios en NextEventux. Su objetivo es mantener trazabilidad entre los casos de uso, el código, las pruebas y los documentos del proyecto.

## 1. Fuentes de información y trazabilidad

Cada herramienta cumple una función diferente:

| Recurso | Uso |
| --- | --- |
| SRS | Define los requisitos, los casos de uso y el comportamiento esperado del producto. |
| PMP | Define el proceso de trabajo, las responsabilidades y los controles de calidad. |
| `Control_de_Alcance_NextEventux.xlsx` | Es el seguimiento oficial acordado para tareas, estados, horas faltantes y métricas, incluido el Burndown Chart. |
| Git y GitHub | Conservan el historial del código, las pruebas y los cambios revisados. |

GitHub Issues, Project y labels pueden apoyar la organización cuando el equipo lo considere útil, pero no son pasos obligatorios para cada tarea, commit o Pull Request. No sustituyen el seguimiento oficial: cuando una tarea cambie de estado o de horas faltantes, se debe reflejar en `Control_de_Alcance_NextEventux.xlsx`.

## 2. Flujo de trabajo

1. Revisar la tarea, sus requisitos y sus dependencias.
2. Verificar el estado de Git y preservar cualquier trabajo local pendiente antes de cambiar de rama o actualizar.
3. Actualizar `main` desde `origin/main`.
4. Crear una rama temporal específica para la tarea.
5. Implementar los cambios y realizar commits pequeños, relacionados con un solo objetivo.
6. Ejecutar las pruebas aplicables y verificar la integración con los componentes afectados.
7. Si corresponde, incorporar a la rama de trabajo los cambios recientes de `main` mediante merge.
8. Publicar la rama y crear un Pull Request hacia `main`.
9. Obtener la revisión de otro integrante y atender sus comentarios.
10. Integrar a `main` cuando se cumplan las condiciones de revisión, pruebas y calidad.
11. Actualizar el seguimiento del proyecto en `Control_de_Alcance_NextEventux.xlsx`.

No se desarrollan funcionalidades directamente sobre `main`. El equipo no ha adoptado una rama permanente `develop`; se trabaja en ramas temporales que se integran en `main` mediante Pull Requests.

```bash
git switch main
git pull --ff-only origin main
git switch -c CU12-aceptar-rechazar-solicitud
```

## 3. Ramas

| Rama | Propósito | Ejemplo |
| --- | --- | --- |
| `main` | Versión estable y oficial del proyecto. | `main` |
| Rama de caso de uso | Implementación o validación de un caso de uso. | `CU11-gestionar-servicio` |
| Rama de tarea | Trabajo técnico o documental que no corresponde directamente a un CU. | `actualizar-flujo-git-documentacion` |

### Convención de nombres

Para los casos de uso:

`CUXX-descripcion-corta`

Ejemplos:

- `CU01-crear-evento`
- `CU11-gestionar-servicio`
- `CU28-aprobar-proveedores`

Ejemplos de ramas de soporte:

- `fix/validar-cierre-de-eventos`
- `docs/actualizar-guia-contribucion`

El nombre debe:

- representar el trabajo que se realiza;
- evitar espacios;
- ser corto pero identificable;
- utilizar el código del caso de uso cuando corresponda.

Las ramas son temporales. Una vez que el trabajo ha sido revisado e integrado a `main`, pueden eliminarse.

### Incorporar cambios recientes de `main`

Antes de actualizar ramas o cambiar de rama, revisa y preserva el trabajo local pendiente. No descartes cambios sin guardarlos o acordar cómo conservarlos. Desde la rama de trabajo, incorpora los cambios de `main` así:

```bash
git status -sb
git switch main
git pull --ff-only origin main
git switch nombre-rama
git merge main
```

Revisa los conflictos y resuélvelos según el contexto de cada cambio; no aceptes automáticamente una versión ni descartes cambios sin entender su impacto. Después, ejecuta las pruebas aplicables. El equipo no recomienda `push --force` ni `rebase` como procedimiento habitual.

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

`eventos`, `finanzas`, `proveedores`, `invitados`, `marketing`, `administracion`, `compartido`, `database`, `ui`, `docs`, `repo`.

La descripción se escribe en minúscula, en infinitivo, sin punto final y debe expresar un solo cambio. No se aceptan mensajes vagos como `cambios`, `arreglos`, `avance` o `commit final`.

### 4.4 Preparar, registrar y publicar cambios

- `git add` prepara los cambios seleccionados para el siguiente commit.
- `git commit` registra esos cambios en la rama actual.
- `git push` publica los commits de la rama en GitHub.
- Es recomendable publicar periódicamente los commits de la rama de trabajo, una vez verificados, para facilitar la colaboración y evitar acumular cambios locales durante demasiado tiempo; no existe una frecuencia obligatoria.
- Un Pull Request propone integrar una rama a `main` y permite revisar los cambios.
- El merge del Pull Request integra los cambios aprobados a `main`.
- Ejecutar `git merge main` desde una rama de trabajo incorpora los cambios de `main` a esa rama; no integra la rama de trabajo a `main`.

## 5. Pull Requests

### 5.1 Contenido obligatorio

Cada Pull Request debe:

1. Tener un título con el formato `[TIPO][MÓDULO] descripción`, por ejemplo: `[FEATURE][PROVEEDORES] Implementar CU-12`.
2. Relacionar un Issue mediante `Closes #número` solamente si existe un Issue relacionado que deba cerrarse al fusionar.
3. Indicar el módulo y, cuando corresponda, el caso de uso afectados.
4. Explicar el problema u objetivo y resumir los cambios realizados.
5. Describir las pruebas ejecutadas y su resultado.
6. Adjuntar evidencia cuando exista una interfaz, un error corregido o una validación importante.
7. Informar efectos sobre base de datos, requisitos, documentación u otros módulos.
8. Completar la lista de verificación de `.github/pull_request_template.md`.

Los cambios documentales y de configuración pueden no corresponder a un caso de uso específico.

Un Pull Request debe atender un solo objetivo principal. No debe mezclar funcionalidades independientes, correcciones ajenas o cambios masivos de formato.

### 5.2 Aprobación y fusión

- Se requiere **una aprobación de un compañero distinto del autor** antes de fusionar a `main`. Este requisito es un acuerdo del equipo.
- El autor no puede contar como su propio revisor.
- Las conversaciones de revisión deben quedar resueltas o justificadas antes de la fusión.
- Si el cambio afecta otro módulo, se debe avisar a su responsable y dejar constancia en el Pull Request.
- Deben ejecutarse las pruebas aplicables. Si una prueba no puede ejecutarse, el motivo y el riesgo deben quedar escritos.
- No se fusionará código con errores conocidos que impidan cumplir los criterios de aceptación.

**Condiciones para integrar a main**

1. Tiene al menos una aprobación válida.
2. No presenta conflictos con `main`.
3. Las pruebas aplicables pasan correctamente.
4. La plantilla del Pull Request está completa.
5. El cambio tiene evidencia suficiente.

## 6. Issues y tablero Kanban (uso opcional)

GitHub Issues, Project y labels son recursos disponibles para organizar el trabajo, pero su uso no es un requisito para cada tarea ni reemplaza el seguimiento oficial en `Control_de_Alcance_NextEventux.xlsx`. Las siguientes pautas aplican cuando el equipo decide utilizar esos recursos.

### 6.1 Dónde están las plantillas

Las plantillas existentes están en `.github/ISSUE_TEMPLATE/`; si se utilizan, deben estar presentes en la rama predeterminada del repositorio para que GitHub las muestre al seleccionar **New issue**.

| Plantilla | Uso |
| --- | --- |
| `kanban_task.yml` | Tarea planificada de un sprint o actividad asociada a un caso de uso. |
| `general_issue.yml` | Situación válida que no encaja en las demás plantillas. |
| `bug_report.yml` | Comportamiento incorrecto y reproducible del software. |
| `change_request.yml` | Cambio propuesto en alcance, requisito, regla de negocio o diseño. |
| `documentation.yml` | Corrección o actualización de documentación. |
| `config.yml` | Configura el selector de plantillas y evita Issues vacíos sin estructura. |

Los archivos `.yml` son formularios: GitHub convierte las respuestas en el cuerpo del Issue. Sus campos ayudan a describir objetivo, evidencia o criterios de aceptación. Los labels automáticos solo se aplican si esos labels ya fueron creados en el repositorio.

**Pautas para crear un Issue, cuando se utilice**

El título debe describir una sola necesidad. Cuando sea pertinente, el cuerpo puede incluir objetivo, responsable, módulo, criterios de aceptación, dependencias y evidencia esperada. Para tareas de programación, se puede relacionar el caso de uso correspondiente.

Si el equipo utiliza un Issue, puede añadirle:

- Un label de tipo.
- Un label de módulo cuando corresponda.
- Un label de prioridad.
- Responsable asignado, cuando corresponda.
- Ubicación en el tablero del proyecto, si se está utilizando.

Cuando se utiliza el tablero, el estado puede administrarse mediante sus columnas o el campo `Status`. No se deben crear labels duplicados para `Por hacer`, `En progreso` y `Terminado`. El label `estado:bloqueado` se utiliza únicamente cuando existe un impedimento real.

### 6.2 Uso correcto de una tarea Kanban

Cuando se utiliza una tarjeta Kanban, esta debe representar trabajo verificable y suficientemente pequeño para avanzar dentro de un sprint. Puede contener responsable, módulo, caso de uso cuando aplique, objetivo, criterios de aceptación, prioridad, horas estimadas, dependencias y evidencia esperada.

El tablero, si se usa, puede tener estas columnas:

| Estado en GitHub Project | Equivalencia en Control de Alcance |
| --- | --- |
| Backlog / Por hacer | No comenzado |
| En progreso | En progreso |
| En revisión | En progreso |
| Bloqueado | Bloqueado |
| Terminado | Completado, únicamente si cumple la Definition of Done |

La columna `En revisión` permite mostrar que la implementación terminó, pero todavía no ha sido aprobada o integrada. Por eso aún corresponde a `En progreso` en el control oficial.

### 6.3 Cierre de Issues, cuando aplique

Si existe un Issue relacionado que deba cerrarse, se puede escribir `Closes #número` en el Pull Request. El Issue se cerrará automáticamente cuando el PR se fusione en la rama configurada por GitHub. No debe cerrarse solo porque se escribió el código o se abrió el PR.

Antes de cerrar un Issue implementado, se debe comprobar que:

- cumple sus criterios de aceptación;
- cuenta con las pruebas y evidencias aplicables;
- fue revisada por un compañero;
- está integrada en la rama correspondiente;
- no deja documentación necesaria sin actualizar;
- su estado y sus horas faltantes fueron actualizados en `Control_de_Alcance_NextEventux.xlsx`.

Cuando un Issue se cierra sin implementar, no se exige que exista código desarrollado, pruebas de código ni merge del PR, siempre que la decisión esté justificada y quede registrada. En esos casos debe documentarse la razón del descarte o reemplazo, la alternativa adoptada y la actualización del control correspondiente.

Comentario de cierre por implementación:

```text
Se cierra porque el trabajo quedó implementado en el PR #___, revisado por @___ y validado mediante ___. Se actualizaron el estado y las horas faltantes en Control_de_Alcance_NextEventux.xlsx.
```

Comentario de cierre sin implementación:

```text
Se cierra sin implementar porque ___. La decisión quedó acordada en ___. El trabajo fue reemplazado por #___ / no hace parte del alcance vigente. Se actualizó el control correspondiente.
```

No se deben borrar Issues para ocultar errores o decisiones descartadas: si se cierra uno, una explicación conserva la trazabilidad.

## 7. Labels personalizados (uso opcional)

La lista inicial y sus colores está documentada en [`.github/LABELS.md`](.github/LABELS.md). Si se utilizan labels, para evitar confusiones cada Issue debe utilizar como máximo uno de tipo, uno de módulo y uno de prioridad, además de `estado:bloqueado` cuando corresponda.

Los labels no reemplazan la asignación de responsables ni el estado del tablero Kanban cuando estos recursos se utilizan.

## 8. Componentes compartidos

Los archivos compartidos, como `MenuCompartido.java`, `Navegacion.java`, `NextEventuxApp.java`, la configuración de H2 y los estilos comunes, pueden modificarse desde la rama de trabajo de cualquier integrante. No es necesario crear ramas compartidas especiales.

Antes de modificar un componente compartido:

- Revisar quiénes lo utilizan y qué dependencias existen.
- Identificar el impacto sobre otros módulos.
- Conservar la compatibilidad cuando sea posible.
- Coordinar con responsables de las funcionalidades afectadas.
- Ejecutar pruebas de integración y regresión.
- Documentar en el Pull Request las dependencias, los riesgos y las pruebas.

Evitar implementar correcciones incompatibles o duplicadas en distintas ramas. Si dos cambios afectan la misma funcionalidad compartida, coordinar su integración para evitar divergencias.

## 9. Calidad y definición de terminado

Una funcionalidad puede considerarse terminada dentro de la iteración cuando cumple los criterios de aceptación, fue probada, recibió revisión de otra persona, quedó integrada en `main` y cuenta con evidencia. Para un caso de uso también deben estar controlados el flujo principal, los flujos alternativos, las excepciones y las reglas de negocio correspondientes. Probar cada funcionalidad por separado no basta cuando intercambia información o depende de otros módulos.

Cuando una funcionalidad entrega información a otra, las pruebas deben comprobar, según corresponda:

- Los datos compartidos.
- La persistencia en H2.
- Los flujos completos entre módulos.
- Los casos de error y alternativos.
- La regresión de funcionalidades relacionadas.
- La interacción real en JavaFX.

Las pruebas automáticas se realizarán con JUnit. El término correcto para comprobar la interacción entre componentes es **pruebas de integración**, no “pruebas integrales”.

Para cambios exclusivamente documentales, revisar el contenido, los enlaces y el diff. No se requieren las pruebas completas de Java si no se modifica código.

## 10. Comunicación y archivos del proyecto

Las decisiones formales deben quedar registradas en Issues, Pull Requests, actas o documentos institucionales. WhatsApp puede utilizarse para avisos rápidos, pero no debe ser la única evidencia de una decisión.

GitHub almacenará el código fuente y su historial. Los documentos académicos en Word, Excel o PowerPoint continuarán en la carpeta institucional acordada por el equipo, salvo que el grupo decida versionar alguna copia expresamente.
 