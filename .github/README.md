# Uso de la carpeta `.github`

GitHub reconoce automáticamente los archivos de esta carpeta y los utiliza para estandarizar Issues y Pull Requests.

## Carpeta `ISSUE_TEMPLATE`

Cada archivo `.yml` de `ISSUE_TEMPLATE` define un formulario diferente. Cuando una persona seleccione **New issue**, GitHub mostrará las plantillas disponibles y solicitará la información configurada en cada una.

| Archivo | Función |
| --- | --- |
| `tarea-kanban.yml` | Crea tareas concretas para el tablero y el Sprint Backlog. |
| `reporte-error.yml` | Registra errores con pasos de reproducción y evidencia. |
| `mejora.yml` | Presenta mejoras con su impacto y criterios de aceptación. |
| `caso-de-uso.yml` | Controla la implementación o revisión de un caso de uso completo. |
| `asunto-general.yml` | Registra consultas, decisiones o asuntos no cubiertos por otra plantilla. |
| `config.yml` | Impide Issues vacíos para que siempre se utilice una plantilla. |

### Cómo utilizar correctamente una plantilla

1. Seleccionar la plantilla que corresponda al trabajo real.
2. Escribir un título concreto sin borrar el prefijo sugerido.
3. Completar todos los campos obligatorios.
4. Asignar responsable, prioridad y módulo.
5. Agregar el Issue al tablero Kanban.
6. Moverlo de estado en el tablero a medida que avance.
7. Relacionar el Pull Request mediante `Closes #número`.
8. Verificar la evidencia antes de cerrar el Issue.

Las plantillas no deben utilizarse para simular avance. Un Issue abierto representa trabajo pendiente y uno cerrado debe tener un resultado verificable.

## Plantilla de Pull Request

El archivo `PULL_REQUEST_TEMPLATE.md` se carga automáticamente al crear un Pull Request. El autor debe completar todas las secciones y la lista de comprobación.

Los Pull Requests de `feature/*` y `fix/*` se dirigen normalmente hacia `develop`. Solo las versiones integradas y verificadas pasan mediante Pull Request de `develop` hacia `main`.

Cada Pull Request requiere una aprobación de una persona diferente al autor. La persona revisora debe comprobar la rama de destino, el alcance, las pruebas, las reglas de negocio y la ausencia de información sensible antes de aprobar.

## Labels

GitHub no crea automáticamente todos los labels personalizados solo por mencionarlos en las plantillas. Primero deben crearse en la configuración del repositorio utilizando los nombres, colores y descripciones indicados en [`LABELS.md`](LABELS.md).

El estado del trabajo se administra en el tablero Kanban. Los labels sirven para clasificar tipo, módulo, prioridad y bloqueos.
