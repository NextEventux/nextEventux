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

`main` conserva la versión estable del proyecto. Cada requerimiento o tarea se desarrolla en una rama temporal independiente creada a partir de `main`. Cuando el trabajo está listo, probado y revisado, se integra nuevamente a `main` mediante Pull Request.

Para los casos de uso, el equipo utilizará nombres de rama con el formato:

`CUXX-descripcion-corta`

Por ejemplo:

`CU11-gestionar-servicio`

Para trabajos que no correspondan directamente a un caso de uso se utilizará un nombre breve y representativo de la tarea.

El proyecto combina Scrum con seguimiento Kanban. GitHub Issues y GitHub Project permiten visualizar el flujo técnico, mientras que `Control_de_Alcance_NextEventux.xlsx` continúa siendo el registro oficial de tareas, estados, horas faltantes, métricas y Burndown Chart. El responsable debe mantener consistencia entre ambos recursos.

## Ejecución local

Se requiere un JDK compatible con la versión que declare el archivo de construcción del proyecto, JavaFX, Git y Visual Studio Code con soporte para Java. H2 y JUnit deben incorporarse como dependencias administradas por el proyecto, no mediante archivos JAR personales sin documentar.

```bash
git clone <URL_DEL_REPOSITORIO>
cd NextEventux
git switch main
git pull
```

El proyecto utiliza Maven y Java 17 o superior. Para probar y abrir la aplicación JavaFX:

```bash
mvn clean test
mvn javafx:run
```

Se requiere una sesión gráfica de escritorio para abrir la ventana. Las
bibliotecas nativas de JavaFX se descargan mediante Maven para el sistema actual.
El repositorio todavía no incluye Maven Wrapper.

## Documentación del proyecto

El PMP, el SRS, el reporte gerencial, los diagramas y las matrices de seguimiento se almacenan en la carpeta institucional definida por el equipo. GitHub se utiliza principalmente para el código, las pruebas y la documentación técnica necesaria para comprender o ejecutar el repositorio.

## Equipo

NextEventux es desarrollado por:

| Integrante | Módulo principal | Github |
| --- | --- | --- |
| Sara Alejandra Coy Calderón | Gestión de Eventos |https://github.com/sarasscx|
| Valeria Salgado Cortés | Gestión Financiera |https://github.com/vsalgadoo |
| Isabella Hermosa Losada | Gestión de Proveedores |https://github.com/Isa21087 |
| Sofía Cortés Salazar | Gestión de Invitados | https://github.com/cortesssofia |
| Saúl Leonardo Cruz Romero | Marketing | https://github.com/Conecruz |
| Juan Diego Rojas Zárate | Administración | https://github.com/MaizenaDura |


## Contribución

Antes de realizar cambios, consulte [CONTRIBUTING.md](CONTRIBUTING.md). Allí se explican la convención de commits, el uso de ramas, las plantillas de Issues, la política de Pull Requests, la aprobación requerida y el cierre trazable de las tareas.

Todas las contribuciones deben realizarse mediante una rama independiente y un Pull Request. Las ramas de trabajo se crean a partir de `main` y deben representar claramente el requerimiento o tarea que se está desarrollando. Cada Pull Request debe dirigirse a `main` y requiere la revisión de una persona diferente al autor antes de integrarse.

Para el procedimiento completo de implementación, validación, pruebas y registro de hallazgos consulte [GUIA_IMPLEMENTACION_Y_VALIDACION.md](GUIA_IMPLEMENTACION_Y_VALIDACION.md).

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

Los Pull Requests de las ramas de trabajo se dirigen hacia `main`. Cada Pull Request requiere la aprobación de una persona diferente al autor antes de integrarse.

Cada Pull Request requiere una aprobación de una persona diferente al autor. La persona revisora debe comprobar la rama de destino, el alcance, las pruebas, las reglas de negocio y la ausencia de información sensible antes de aprobar.

## Labels

GitHub no crea automáticamente todos los labels personalizados solo por mencionarlos en las plantillas. Primero deben crearse en la configuración del repositorio utilizando los nombres, colores y descripciones indicados en [`LABELS.md`](LABELS.md).

El estado del trabajo se administra en el tablero Kanban. Los labels sirven para clasificar tipo, módulo, prioridad y bloqueos.

## Inicialización de H2

La conexión `config.DatabaseConnection.getConnection()` crea las tablas de
`src/main/resources/schema.sql` si todavía no existen. Al abrir nuevas conexiones
se conservan los registros existentes. Después aplica la migración repetible
`migrations/001_evento_cu01.sql` para añadir los campos de CU01 a bases existentes.
Los futuros cambios de estructura también necesitan una migración explícita.

Los datos de ejemplo están separados en `src/main/resources/seed.sql`. Su carga es
opcional y debe realizarse una sola vez sobre una base recién inicializada; no se
cargan al abrir la aplicación ni deben ejecutarse nuevamente sobre datos existentes.

Con JDK 17 o superior y Maven instalado, ejecutar desde la raíz del repositorio:

```bash
mvn clean test-compile dependency:copy-dependencies
java -cp "target/classes:target/test-classes:target/dependency/*" config.TestSchema
```

En Windows se utiliza `;` como separador del classpath. `TestSchema` puede ejecutarse manualmente y también se verifica mediante
`SchemaPersistenciaTest` al ejecutar `mvn test`.
Comprueba creación del esquema, carga de ejemplos, reinicialización sin duplicados
y conservación de datos después de cerrar y volver a abrir la base. Utiliza una
base temporal y no modifica `data/nexteventux`.

### Conciliación de Evento con CU01

La entidad conserva nombre, tipo, presupuesto e invitados iniciales y representa
los campos requeridos por la tabla compartida. El constructor ahora recibe un
organizador explícito, descripción opcional y fechas de inicio y fin con hora.

| Entidad Java | Columna H2 |
| --- | --- |
| `idEvento` (`Integer`, nulo hasta guardar) | `id_evento` autogenerado |
| `idOrganizador` | `id_organizador`, clave foránea |
| `nombre`, `tipo`, `descripcion` | `nombre`, `tipo`, `descripcion` |
| `fechaInicio`, `fechaFin` (`LocalDateTime`) | `fecha_inicio`, `fecha_fin` (`TIMESTAMP`) |
| `ubicacion` (antes `lugar`) | `ubicacion` |
| `presupuesto` (`BigDecimal`) | `presupuesto` (`DECIMAL(12,2)`) |
| `invitadosIniciales` | `invitados_iniciales` |
| `estado` | `estado`, valor inicial `PLANEADO` |
| `fechaCreacion` (`LocalDateTime`) | `fecha_creacion` |

El presupuesto admite cero y hasta 9999999999.99 con precisión de centavos, sin
redondeo silencioso. Los invitados admiten cero. Las fechas son obligatorias y el
fin no puede ser anterior al inicio; no se añade una restricción sobre fechas
pasadas ni se exige duración mínima. Las longitudes se validan según las columnas.

Las columnas nuevas permiten `NULL` para conservar registros anteriores cuyos
datos reales no se conocen. La creación de nuevos eventos mediante el servicio
requiere los tres campos. La migración no rellena datos históricos. Los ejemplos
incluyen estos campos y adelantan sus contadores automáticos; al inicializar se
recupera también el contador de eventos de bases antiguas si quedó por debajo de
los identificadores existentes, sin reducir un contador ya adelantado.

Validaciones adicionales, después de compilar las clases de prueba:

```bash
java -cp "target/classes:target/test-classes:target/dependency/*" PruebaEventoSchema
java -cp "target/classes:target/test-classes:target/dependency/*" PruebaCU01
```

`PruebaEventoSchema` comprueba que H2 genera un identificador y conserva los
campos del evento. `TestSchema` verifica además la migración de tablas existentes.
Los ejemplos pueden ejecutarse manualmente. `ServiceEventosTest` cubre las
validaciones y el flujo del controlador con JUnit; `SchemaPersistenciaTest`
ejecuta las verificaciones H2 existentes. Ambos se ejecutan con `mvn test`.

`RepositoryEvento` guarda y consulta mediante JDBC. Antes de insertar verifica
que el organizador exista; la clave foránea mantiene la relación en H2. El evento
recibe el identificador generado después de confirmar la transacción. La consulta
recupera estado, fecha de creación y campos históricos nulos sin inventar valores.
Los invitados históricos usan `Integer` para distinguir desconocido (`NULL`) de cero.

Esta conciliación cambia atributos y tipos respecto de la primera iteración.
Debe reflejarse en el diseño de estructura (SDD 3.2), persistencia (SDD 3.4) e
interfaz (SDD 3.1) de los documentos institucionales, que no están en el repositorio.

## Pantalla JavaFX de creación de eventos

`app.NextEventuxApp` abre la pantalla de `ViewEventos`, construida con controles
JavaFX y un único archivo CSS en `src/main/resources/styles/eventos.css`. Sigue el
mockup aportado: menú lateral, barra superior, formulario, tarjetas informativas y
resumen del presupuesto en COP y de los invitados. El panel baja en ventanas
pequeñas y el contenido se puede desplazar para acceder a los botones.

El formulario añade los campos requeridos por la entidad conciliada: organizador
seleccionado por nombre desde H2, inicio y fin con hora y descripción opcional. Las horas usan
`HH:mm`, los invitados son enteros y los decimales del presupuesto usan punto.
El selector de tipo contiene Corporativo, Social, Cultural y Otro. La interfaz
muestra «En planeación» y el servicio conserva el valor `PLANEADO`.

«Crear evento» construye la entidad y llama al controlador. Los errores aparecen
en el formulario y conservan lo ingresado. «Cancelar» limpia los campos, el
resumen y el mensaje, sin eliminar eventos ya creados. Después de crear se
conservan los campos y se muestra una confirmación, siguiendo el mockup.

La aplicación guarda eventos de forma persistente en `data/nexteventux.mv.db`.
No elimines ese archivo si necesitas conservar los registros. El organizador debe
estar previamente registrado. Los errores de validación o base de datos aparecen
en el formulario, sin mostrar confirmación de éxito ni borrar los campos.

No hay sesión de usuario. El menú de otros módulos y la cuenta son elementos
visuales sin navegación ni autenticación implementadas.

### Preparar una demostración en una base vacía

Para probar CU01 antes de disponer del módulo de registro de organizadores:

```bash
mvn test-compile dependency:copy-dependencies
java -cp "target/classes:target/dependency/*" config.CargarDatosEjemplo
mvn javafx:run
```

En Windows usa `;` como separador del classpath. La carga es explícita y se rechaza
si hay registros en cualquier tabla, para proteger los datos existentes. El desplegable muestra los nombres de los
organizadores de ejemplo. La aplicación no carga
estos ejemplos automáticamente. Si ya existen datos reales, usa un organizador
registrado y omite la carga de ejemplos.

### Comprobación del formulario

1. Abrir con `mvn javafx:run` y verificar la distribución y el desplazamiento.
2. Escribir presupuesto e invitados y comprobar el resumen en vivo.
3. Crear con nombre, tipo, organizador seleccionado, fechas y horas válidas, lugar,
   presupuesto no negativo con hasta dos decimales e invitados enteros no negativos.
4. Probar un campo obligatorio vacío, `NaN`, invitados decimales, hora `24:00` y
   fin anterior al inicio: deben mostrarse errores y conservarse los campos.
5. Pulsar Cancelar: deben limpiarse los campos y el mensaje, y el resumen volver a cero.
6. Reducir y ampliar la ventana: el formulario y sus acciones deben seguir accesibles.

Verificación de esta implementación: 16 pruebas JUnit sin fallos que cubren
validaciones, creación mediante controlador y repositorio JDBC, identificador
automático, organizador inexistente, recuperación tras cerrar y reabrir H2,
registros históricos nulos, error de conexión, migración y carga segura de ejemplos.
Las bases de prueba son temporales y no modifican los datos de la aplicación.
La interfaz fue validada visualmente por la usuaria y mediante una ejecución
controlada JavaFX con aserciones sobre resumen, rechazo de entradas inválidas,
creación, persistencia, cancelación y redimensionamiento.

Esta es una primera iteración de CU01; el caso de uso completo sigue pendiente.
Según la especificación original confirmada por la usuaria («Especificación Casos
de Uso-33.pdf», CU-001), faltan:

- Organizador autenticado con rol ORG y proveedor en estado Verificado como precondiciones.
- Envío de una solicitud de servicio dentro del flujo principal.
- Validación de que el costo comprometido no supere el presupuesto.
- Actualización del presupuesto disponible en el flujo exitoso.

La existencia del organizador en H2 no equivale a autenticación ni autorización.
La persistencia del evento no sustituye estas reglas. El PR se mantiene como
primera iteración para revisión; no debe registrarse CU01 como completado en el
Control de Alcance hasta implementar, probar e integrar los requisitos pendientes.

El campo Organizador consulta H2 y muestra únicamente los nombres de los usuarios
asociados a organizadores, ordenados por nombre. El identificador permanece
interno y se utiliza al guardar. No se selecciona un organizador automáticamente;
Cancelar borra la selección. Si no hay organizadores o falla la consulta, se
muestra un mensaje y no se permite crear sin seleccionar uno.

## CU03 — Subir evento al repositorio

La rama `CU03-subir-evento-repositorio` parte del avance local de CU01 para
reutilizar la pantalla JavaFX, el logo y la persistencia existentes. Sus cambios
se mantienen en commits separados; CU01 debe integrarse antes o coordinarse como
dependencia al preparar el PR de CU03.

Desde Crear evento, el botón **Subir al repositorio** abre tres secciones:

- **Solicitar publicación:** seleccionar un organizador por nombre y uno de sus
  eventos. Solo se aceptan eventos finalizados. Se registra la solicitud pendiente
  y se envía una notificación interna a los administradores activos.
- **Revisión del administrador:** seleccionar un administrador activo con el rol
  ADMINISTRADOR y una solicitud pendiente. Aprobar publica la plantilla; rechazar
  exige un motivo de hasta 250 caracteres. Ambas decisiones notifican al organizador.
- **Plantillas publicadas:** lista las estructuras aprobadas, disponibles para
  futura reutilización. Esta pantalla no implementa una operación de clonación.

### Alcance y privacidad

La selección de perfiles es **solo para demostración**. No autentica usuarios y
no sustituye la sesión que está desarrollando el equipo. Antes de dar CU03 por
completado debe integrarse esa sesión para obtener el organizador/administrador
autenticado, proteger las consultas y ocultar las funciones de otros roles. La
comprobación del rol activo en H2 no equivale a una autenticación.

La publicación copia únicamente el tipo y la duración en minutos. Genera un título
genérico a partir del tipo; no copia el nombre original, descripción, lugar,
fechas exactas, invitados, identificadores de personas ni presupuesto. La tabla
pública de plantillas no contiene esas columnas. La referencia interna a la
solicitud se utiliza para asegurar una sola plantilla por aprobación, y no se
expone en la vista pública. El evento original no cambia de estado ni pierde datos.

No se permiten solicitudes duplicadas mientras haya una pendiente o aprobada.
Después de un rechazo, el organizador puede solicitar de nuevo expresamente; se
conservan la solicitud anterior y su motivo. Las notificaciones se guardan en H2
por usuario y se consultan dentro de la aplicación; no se envían correos.

### Persistencia y errores

La migración repetible `migrations/002_publicacion_eventos.sql` añade:

- `Solicitud_publicacion`, con estado, motivo y administrador revisor.
- `Plantilla_evento`, con los datos estructurales de la plantilla.
- `Notificacion_interna`, con destinatario, mensaje y fecha.

La comprobación del evento, registro de solicitud y notificaciones forman una
transacción. La aprobación también confirma conjuntamente la plantilla, decisión
y notificación. Ante un fallo se revierten los cambios, se muestra un mensaje
claro y se conserva el formulario para reintentar. Se bloquea el evento al procesar
la solicitud y se vuelve a comprobar su estado antes de aprobar. Las consultas
usan parámetros JDBC.

Si no hay administradores activos con el rol correspondiente, la solicitud se
bloquea con un mensaje; no se simula un envío ni una aprobación automática.
El botón Actualizar recarga solicitudes, catálogo y avisos de los perfiles
seleccionados. Para cargar nuevos perfiles creados desde otra pantalla, volver a
abrir CU03.

### Demostración en una base vacía

Con Maven instalado:

```bash
mvn test-compile dependency:copy-dependencies
java -cp "target/classes:target/dependency/*" config.CargarDemoPublicacion
mvn javafx:run
```

En Windows, sustituir `:` por `;` en el classpath. No ejecutar previamente
`CargarDatosEjemplo`: la demostración CU03 ya lo llama y exige una base vacía.
El comando no borra datos existentes; rechaza la carga si alguna tabla contiene
registros. Los ejemplos no se cargan al abrir la aplicación.

Elegir Laura Gómez y «Evento finalizado de demostración», enviar la solicitud y
revisarla como «Administrador de demostración». Probar rechazo con motivo o
aprobación; consultar después Mis solicitudes, Notificaciones y Plantillas.
El evento finalizado es una fixture de demostración; este comando no implementa
ni ejecuta el proceso de finalización CU05. Si ya hay datos reales, usar los
organizadores, eventos finalizados y administradores registrados, sin cargar
la demostración ni eliminar la base de datos.

### Verificación de CU03

- 30 pruebas JUnit en total: 16 anteriores y 14 de publicación/demostración.
- Cobertura nueva: solicitud pendiente, propiedad del evento, estados, administrador
  con rol activo, motivo obligatorio, duplicados, publicación sin datos sensibles,
  conservación del original, notificaciones, rechazo y nuevo envío, persistencia,
  reversión por fallo y reintento.
- Prueba JavaFX con H2 temporal: solicitud bloqueada por estado, envío, rechazo sin
  motivo, rechazo válido, notificación, reenvío, fallo durante aprobación con
  conservación del formulario, reintento exitoso y consulta de plantilla.
- Inspección de capturas de las tres secciones de JavaFX.

En este equipo no hay Maven instalado: la verificación se ejecutó compilando con
`javac --release 17` y usando JUnit Platform Console 1.10.2, con las dependencias
JavaFX/H2 ya disponibles. En un entorno con Maven, usar `mvn clean test`.
Las pruebas usan bases temporales; no modifican los registros de la aplicación.
La comprobación visual se realizó en macOS; no se ha validado esta pantalla en Windows.

Quedan pendientes la integración con la autenticación real y la revisión del equipo
de las tablas/interacciones que afectan a Administración y a las notificaciones.
No debe marcarse CU03 como completado en el control institucional hasta integrar
esas dependencias y superar la revisión correspondiente.
