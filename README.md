# nextEventux

NextEventux es una aplicación de escritorio para centralizar la planeación y el seguimiento de eventos. Reúne en un mismo sistema la información que normalmente queda repartida entre conversaciones, hojas de cálculo y notas independientes, y facilita la colaboración entre clientes, organizadores, proveedores e invitados.

## Navegación

| Sección | Contenido |
| --- | --- |
| [Visión](#visión-del-producto) | Problema que resuelve y usuarios principales. |
| [Alcance](#alcance) | Capacidades incluidas y límites del producto. |
| [Módulos](#módulos-funcionales) | Organización de los 30 casos de uso. |
| [Tecnologías](#tecnologías) | Herramientas de implementación y calidad. |
| [Estructura](#estructura-recomendada-del-repositorio) | Distribución prevista del código y la documentación técnica. |
| [Desarrollo](#flujo-de-desarrollo) | Ramas, Issues, Pull Requests y control de avance. |
| [Ejecución](#ejecución-local) | Preparación del entorno local. |
| [Contribución](#contribución) | Reglas que debe seguir el equipo. |

## Visión del producto

La aplicación permitirá que un cliente compare organizadores verificados y solicite la contratación de uno. El organizador podrá administrar la planeación, el presupuesto, las propuestas de proveedores, los servicios contratados y las invitaciones. Los proveedores gestionarán sus servicios y solicitudes, mientras que los invitados podrán consultar y responder información relacionada con su participación.

NextEventux busca ofrecer información actualizada para comparar alternativas, controlar recursos y detectar situaciones como compromisos superiores al presupuesto, cruces de horario de proveedores o una cantidad de asistentes mayor al aforo permitido.

## Alcance

El alcance contempla la gestión de eventos, información financiera, proveedores, invitados, actividades de marketing y funciones administrativas. También incluye autenticación, soporte multiusuario, pagos registrados, reseñas, servicios, campañas y mecanismos de fidelización.

NextEventux no presta directamente los servicios contratados ni ejecuta pagos bancarios. Es una aplicación de escritorio, no una aplicación web o móvil. El sistema contempla notificaciones internas; el posible envío de correos en procesos concretos debe quedar definido por el equipo antes de implementar esas funciones.

## Módulos funcionales

El producto se especifica mediante 30 casos de uso distribuidos de forma consecutiva en seis módulos.

| Módulo | Casos de uso | Responsable | Propósito general |
| --- | --- | --- | --- |
| Gestión de Eventos | CU-01 a CU-05 | Sara Alejandra Coy Calderón | Crear, actualizar, publicar, cancelar y finalizar eventos. |
| Gestión Financiera | CU-06 a CU-10 | Valeria Salgado Cortés | Consultar y controlar planificación, paquetes, sanciones y movimientos. |
| Gestión de Proveedores | CU-11 a CU-15 | Isabella Hermosa Losada | Administrar servicios, solicitudes, contrataciones, reseñas y portafolios. |
| Gestión de Invitados | CU-16 a CU-20 | Sofía Cortés Salazar | Administrar respuestas, invitaciones, necesidades, acompañantes y regalos. |
| Marketing | CU-21 a CU-25 | Saúl Leonardo Cruz Romero | Gestionar segmentación, fidelidad, campañas, consentimientos y canjes. |
| Administración | CU-26 a CU-30 | Juan Diego Rojas Zárate | Contratar organizadores y administrar registros, aprobaciones y suspensiones. |

La especificación detallada de cada comportamiento pertenece al SRS y a los anexos de casos de uso. El código no debe introducir reglas que contradigan esos documentos sin registrar y aprobar previamente una solicitud de cambio.

## Tecnologías

| Área | Tecnología | Uso |
| --- | --- | --- |
| Lenguaje | Java | Implementación de funcionalidades y reglas de negocio. |
| Interfaz | JavaFX | Construcción de las pantallas de escritorio. |
| Persistencia | H2 | Base de datos relacional embebida obligatoria para el proyecto. |
| Pruebas | JUnit | Pruebas automatizadas de clases, validaciones y reglas críticas. |
| Modelado | UML y draw.io | Representación visual de casos de uso y conceptos del sistema. |
| Desarrollo | Visual Studio Code | Edición, ejecución y depuración del código. |
| Versionamiento | Git y GitHub | Historial, colaboración, revisión e integración del código. |

## Organización del repositorio

```text
NextEventux/
├── .github/
│   ├── ISSUE_TEMPLATE/
│   └── pull_request_template.md
├── docs/
├── src/
│   ├── main/
│   │   ├── java/
│   │   └── resources/
│   └── test/
│       └── java/
├── .gitignore
├── CONTRIBUTING.md
└── README.md
```

Los scripts de creación o migración de la base de datos que sean necesarios para reproducir el sistema sí deben versionarse. Los archivos locales generados por H2, las compilaciones y los secretos no deben incluirse en el repositorio.

## Flujo de desarrollo

`main` conserva las versiones estables de entrega y `develop` integra el trabajo de cada sprint. Las nuevas funcionalidades se desarrollan en ramas `feature/*`; las correcciones, en ramas `fix/*`. Cada cambio comienza con un Issue, se relaciona mediante commits y termina en un Pull Request hacia `develop`.

El proyecto combina Scrum con seguimiento Kanban. GitHub Issues y GitHub Project permiten visualizar el flujo técnico, mientras que `Control_de_Alcance_NextEventux.xlsx` continúa siendo el registro oficial de tareas, estados, horas faltantes, métricas y Burndown Chart. El responsable debe mantener consistencia entre ambos recursos.

## Ejecución local

Se requiere un JDK compatible con la versión que declare el archivo de construcción del proyecto, JavaFX, Git y Visual Studio Code con soporte para Java. H2 y JUnit deben incorporarse como dependencias administradas por el proyecto, no mediante archivos JAR personales sin documentar.

```bash
git clone <URL_DEL_REPOSITORIO>
cd NextEventux
git switch develop
```

El comando de compilación y ejecución debe añadirse aquí cuando el equipo confirme y configure el gestor de construcción. Si se adopta Maven o Gradle, se recomienda versionar su wrapper para que todos ejecuten exactamente la misma versión.

## Documentación del proyecto

El PMP, el SRS, el reporte gerencial, los diagramas y las matrices de seguimiento se almacenan en la carpeta institucional definida por el equipo. GitHub se utiliza principalmente para el código, las pruebas y la documentación técnica necesaria para comprender o ejecutar el repositorio.

## Equipo

NextEventux es desarrollado por:

| Integrante | Módulo principal | Github |
| --- | --- | --- |
| Sara Alejandra Coy Calderón | Gestión de Eventos | |
| Valeria Salgado Cortés | Gestión Financiera | |
| Isabella Hermosa Losada | Gestión de Proveedores |https://github.com/Isa21087 |
| Sofía Cortés Salazar | Gestión de Invitados | https://github.com/cortesssofia |
| Saúl Leonardo Cruz Romero | Marketing | https://github.com/Conecruz |
| Juan Diego Rojas Zárate | Administración | |


## Contribución

Antes de realizar cambios, consulte [CONTRIBUTING.md](CONTRIBUTING.md). Allí se explican la convención de commits, el uso de ramas, las plantillas de Issues, la política de Pull Requests, la aprobación requerida y el cierre trazable de las tareas.

Todas las contribuciones deben realizarse mediante una rama y un Pull Request. Las ramas feature/* y fix/* nacen desde develop y sus Pull Requests regresan a develop. Cuando la versión integrada se encuentre verificada, se abre un Pull Request de develop hacia main.Cada Pull Request necesita la aprobación de una persona diferente al autor antes de integrarse a su rama de destino.

## Estado

NextEventux se encuentra en desarrollo académico incremental. Las funcionalidades se priorizan e implementan por sprint, y cada incremento debe quedar probado, revisado e integrado con los módulos relacionados antes de considerarse terminado.

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
