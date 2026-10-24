# NextEventux

NextEventux es una aplicación de escritorio para planear y gestionar eventos desde un mismo lugar. Busca reducir la dispersión de información entre hojas de cálculo, mensajes y registros separados, conectando la gestión del evento con presupuestos, servicios, proveedores e invitados.

Es un proyecto académico de ingeniería de software: su alcance se desarrolla por incrementos y el código del repositorio no representa la finalización de todos los requisitos especificados.

## Estado del proyecto

La aplicación tiene una interfaz JavaFX, autenticación por sesión y funcionalidades implementadas en distintos módulos. El alcance académico especifica 30 casos de uso distribuidos en seis módulos; **no se debe interpretar esa especificación como 30 casos ya terminados, integrados o aceptados**. La finalización de cada caso depende también de sus criterios, pruebas, revisión e integración.

Las capacidades actuales incluyen, entre otros incrementos, la gestión de eventos; solicitudes de servicios y presupuesto; publicación de plantillas; servicios y respuestas de proveedores; invitaciones, respuestas y mesa de regalos; consulta de galerías; operaciones financieras; sanciones a proveedores y contratación de organizadores. Las suites de `src/test/java/` contienen pruebas JUnit de estos flujos. La presencia de una pantalla o de código no implica por sí sola que un caso de uso completo esté cerrado.

### Perfiles que maneja la aplicación

| Perfil | Capacidades disponibles en la aplicación |
| --- | --- |
| **Organizador** | Gestionar sus eventos, solicitudes de servicios, invitaciones y funciones asociadas a sus eventos. |
| **Proveedor** | Gestionar servicios y atender solicitudes; consultar avisos internos. |
| **Administrador** | Revisar solicitudes de publicación y acceder a funciones administrativas, incluidas sanciones a proveedores. |
| **Cliente** | Consultar organizadores y gestionar solicitudes para contratarlos. |
| **Invitado** | No tiene cuenta de usuario ni sesión propia. Consulta invitaciones y galerías mediante los datos de contacto registrados. |

Los nombres de los perfiles autenticados corresponden a los roles usados por el código: `ORGANIZADOR`, `PROVEEDOR`, `ADMINISTRADOR` y `CLIENTE`. El acceso del invitado no equivale a autenticación mediante una cuenta.

### Funcionalidades implementadas y límites conocidos

Las pruebas automatizadas cubren reglas de negocio y persistencia en H2 para los incrementos implementados. Por ejemplo, existen pruebas para eventos, autenticación, solicitudes y presupuesto, publicación, cancelación, finalización, invitaciones, mesa de regalos, finanzas, sanciones y contratación de servicios y organizadores.

- La publicación de eventos ofrece un catálogo de plantillas; no clona automáticamente un evento.
- Los avisos son internos a la aplicación. No se envían correos ni se procesan pagos bancarios.
- Los invitados consultan información con datos de contacto registrados, no con una cuenta autenticada.
- El sistema permite consultar reseñas de organizadores asociadas a su contratación. El flujo de calificación de proveedores (CU14) todavía no está verificado como una funcionalidad integrada. La existencia de la tabla `Resena` no demuestra por sí sola que ese caso de uso esté completo.
- La integración de reservas de lugares requiere verificar el flujo que persiste la reserva y su vínculo con el evento; consulta la [nota de integración de CU04](docs/integracion-cu04.md).
- La especificación y los incrementos están sujetos a revisión e integración con los módulos relacionados. Consulta las fichas técnicas enlazadas más abajo; no extrapoles el estado de un módulo a los 30 casos de uso.

## Capturas de funcionalidades

Las siguientes capturas están incluidas en el repositorio como evidencia de incrementos funcionales. Documentan pantallas reales, pero pueden diferir de la interfaz más reciente.

**Solicitudes de servicios y presupuesto**

![Pantalla de solicitudes y presupuesto de un evento](docs/evidencias/cu01-servicios-presupuesto.png)

**Avisos internos para proveedores**

![Bandeja de avisos de un proveedor](docs/evidencias/cu04-avisos-proveedor.png)

**Finalización de evento y entrega de galería**

![Pantalla de finalización de evento](docs/evidencias/cu05-finalizacion.png)

## Tecnologías y arquitectura

Versiones declaradas en [`pom.xml`](pom.xml):

| Tecnología | Versión o uso |
| --- | --- |
| Java | 17 como versión de compilación (`source` y `target`). |
| JavaFX Controls | 21.0.2; interfaz de escritorio. |
| H2 | 2.3.232; base de datos relacional embebida. |
| JUnit Jupiter | 5.10.2; pruebas automatizadas. |
| Maven Surefire | 3.2.5; ejecución de pruebas Maven. |
| JavaFX Maven Plugin | 0.0.8; ejecución de la aplicación con Maven. |

La aplicación sigue una separación por capas: `app` inicia la interfaz y organiza la navegación; `views` presenta pantallas JavaFX; `controllers` conecta las acciones de vista; `services` concentra reglas de negocio; `repositories` accede a H2 mediante JDBC; y `entities` representa los datos. El esquema y los cambios de base de datos están en `src/main/resources/` y sus migraciones.

## Clonar, compilar, probar y ejecutar

Se necesita un JDK 17 o superior compatible, Maven y una sesión gráfica de escritorio. JavaFX y las dependencias de pruebas se resuelven desde Maven.

```bash
git clone https://github.com/NextEventux/nextEventux.git
cd nextEventux
mvn clean test
mvn javafx:run
```

`mvn clean test` compila y ejecuta las pruebas automatizadas. `mvn javafx:run` abre la aplicación; al terminar las pruebas, se puede ejecutar desde la raíz del proyecto. El repositorio no incluye Maven Wrapper.

## Persistencia local con H2

Por defecto, H2 conserva la base en la carpeta del usuario del sistema:

- Linux/macOS: `~/.nexteventux/nexteventux.mv.db`
- Windows: `%USERPROFILE%\.nexteventux\nexteventux.mv.db`

La ubicación de la carpeta se puede cambiar con la propiedad Java `nexteventux.home`; la URL H2 completa se puede especificar con `nexteventux.db.url`. Al abrir una base vacía se inicializan el esquema y las migraciones, y la aplicación carga datos de ejemplo solo si no encuentra registros de negocio. Una base con datos existentes no se reemplaza por los ejemplos. Cambiar la ubicación no copia la base anterior.

Los datos persisten entre ejecuciones. **No borres ni reemplaces el archivo de H2 para solucionar problemas si contiene información que quieras conservar.** Antes de cambiar la ubicación o manipular la base, cierra la aplicación y haz una copia de seguridad. La [documentación de persistencia y presupuesto](docs/cu01-solicitudes-presupuesto.md) explica reglas y limitaciones adicionales.

## Módulos del alcance y equipo

Los módulos y rangos de casos de uso siguientes reflejan la organización académica del alcance, no el estado de implementación:

| Módulo | Casos especificados | Responsable | GitHub |
| --- | --- | --- | --- |
| Gestión de Eventos | CU-01 a CU-05 | Sara Alejandra Coy Calderón | [sarasscx](https://github.com/sarasscx) |
| Gestión Financiera | CU-06 a CU-10 | Valeria Salgado Cortés | [vsalgadoo](https://github.com/vsalgadoo) |
| Gestión de Proveedores | CU-11 a CU-15 | Isabella Hermosa Losada | [Isa21087](https://github.com/Isa21087) |
| Gestión de Invitados | CU-16 a CU-20 | Sofía Cortés Salazar | [cortesssofia](https://github.com/cortesssofia) |
| Marketing | CU-21 a CU-25 | Saúl Leonardo Cruz Romero | [Conecruz](https://github.com/Conecruz) |
| Administración | CU-26 a CU-30 | Juan Diego Rojas Zárate | [MaizenaDura](https://github.com/MaizenaDura) |

## Documentación técnica

- [Solicitudes de eventos y presupuesto](docs/cu01-solicitudes-presupuesto.md)
- [Integración de cancelaciones con proveedores e invitados](docs/integracion-cu04.md)
- [Finalización de eventos](docs/cu05-finalizar-evento.md)
- [Navegación e interfaz compartidas](docs/navegacion-compartida.md)
- [Guía de implementación y validación](GUIA_IMPLEMENTACION_Y_VALIDACION.md)
- [Conciliación de Pull Requests](docs/conciliacion-prs.md) — referencia histórica de integración; no es el estado en tiempo real de GitHub.

Los documentos académicos institucionales (por ejemplo, SRS, PMP y controles de alcance) se mantienen en la ubicación acordada por el equipo; no todos forman parte de este repositorio.

## Contribuir

Consulta [`CONTRIBUTING.md`](CONTRIBUTING.md) antes de trabajar. El flujo del equipo usa ramas temporales para tareas o casos de uso y Pull Requests hacia `main`; no se desarrollan funcionalidades directamente sobre `main`. Cada contribución debe incluir las pruebas aplicables y recibir la revisión de otro integrante antes de integrarse. La plantilla vigente del repositorio está en [`.github/pull_request_template.md`](.github/pull_request_template.md).

GitHub Issues y el tablero Kanban son recursos opcionales; no sustituyen el control oficial de alcance acordado por el equipo. La guía de contribución contiene las convenciones, los criterios de revisión y el procedimiento detallado.

## Registro histórico de iteraciones

<details>
<summary>README original, detalles técnicos y cifras de iteraciones anteriores</summary>

La [versión original del README en el historial de Git](https://github.com/NextEventux/nextEventux/blob/6a43de7/README.md) conserva documentación de iteraciones anteriores. Es una referencia histórica, no una descripción vigente de todas las funcionalidades; para el estado y los detalles técnicos actuales consulta las fichas de `docs/` y el código.

Como contexto técnico no repetido en las fichas actuales, aquella versión describía que CU02 limitaba la edición a los datos generales de eventos propios en planeación, y que CU03 separaba la solicitud de publicación del organizador de la revisión administrativa. La publicación guardaba una plantilla estructural —sin trasladar datos personales ni presupuesto del evento— y agrupaba decisión y avisos en transacciones. Son apuntes de una iteración anterior, no garantías sobre el estado actual; consulta [CU01 y presupuesto](docs/cu01-solicitudes-presupuesto.md), [integración de CU04](docs/integracion-cu04.md) y [finalización CU05](docs/cu05-finalizar-evento.md) para los detalles técnicos documentados.

El README histórico registraba resultados de pruebas de distintas iteraciones: 16 pruebas para la primera implementación de CU01, 82 para una iteración de CU02, 91 para una iteración de CU03, 117 para una iteración de CU04 y 134 para una iteración de CU05. Son **datos históricos de estados distintos**: no describen la suite actual, no se deben sumar entre sí ni usar como métricas vigentes.

</details>