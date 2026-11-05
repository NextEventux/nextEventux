# CU-12 — solicitudes y propuestas

Fuente: ficha CU-12 de «Especificación Casos de Uso.xlsx». El organizador pide
un servicio, el proveedor responde con una propuesta y ambos negocian hasta que
el organizador selecciona una (la contratación es CU-13).

## Qué hace cada pantalla

### Proveedor — Solicitudes recibidas

- Lista de tarjetas con el nombre del evento, el servicio, la fecha y el estado.
  Los identificadores solo se muestran si faltan los datos.
- Al elegir una solicitud aparece su detalle (lugar, invitados, organizador,
  necesidad) y las acciones según el estado.
- **Enviar propuesta** abre un formulario en la misma pantalla con precio,
  alcance, condiciones y material de apoyo opcional (un enlace https). Antes de
  enviar se muestra un resumen. Si ya hay una propuesta, el botón pasa a **Enviar
  nueva versión**.
- **Rechazar solicitud** pide el motivo en un formulario en pantalla.
- **Conversación** muestra los mensajes en burbujas, con imagen, archivo o enlace
  adjunto, y permite responder. **Historial de propuestas** lista las versiones.

### Organizador — Propuestas recibidas

- Lista de tarjetas con servicio, evento, proveedor, precio y estado.
- El detalle muestra el evento y la oferta completa. Hay una sola acción
  principal según el estado: **Seleccionar propuesta** o **Contratar servicio**.
- **Solicitar ajuste** queda vinculado a la versión vigente y se ve en la
  conversación como «Ajuste solicitado sobre la versión N».
- **Rechazar propuesta** pide confirmación en pantalla. Una propuesta contratada
  o no seleccionada ya no admite cambios y la pantalla lo explica.

### Organizador — Solicitar un servicio

En el detalle del servicio se elige el evento por su nombre y una necesidad
abierta de ese evento, o se crea una nueva. No se escriben identificadores.

## Reglas de la ficha que cubre

| Regla de la ficha | Dónde se cumple |
| --- | --- |
| Precio inválido (E2) | `DatosPropuesta` avisa antes de enviar; `ServiceSolicitudes` valida de nuevo |
| Propuestas versionadas, una vigente | `RepositoryPropuestaServicio.guardarNuevaVersion` |
| Material de apoyo | columna `Propuesta.material_apoyo` (migración 013) |
| Ajuste vinculado a la versión vigente (A2) | columna `Mensaje.id_propuesta` (migración 013) |
| Solicitud ya resuelta (E1) | `ServiceSolicitudes.rechazarSolicitud` |
| Aislamiento entre proveedores | las consultas filtran por `id_proveedor` y `id_organizador` |

## Organización del código

Las pantallas grandes no se ampliaron: las piezas nuevas están en
`views/solicitudes/` y cada una tiene una sola responsabilidad.

- `PresentacionSolicitud`: textos, colores de estado y qué acciones se habilitan.
  No usa JavaFX, por eso se prueba sin abrir una ventana.
- `DatosPropuesta` y `SolicitudNueva`: validan lo escrito en los formularios.
- `FormularioPropuesta`, `FormularioMotivo` y `FormularioSolicitudServicio`:
  formularios dentro de la pantalla.
- `PanelConversacion`, `HistorialPropuestas`, `TarjetaSolicitud`,
  `TarjetaPropuesta`, `DetalleSolicitud` y `DetallePropuesta`: piezas de lectura.
- `views/components/ComponentesUi`: títulos, chips, avisos y botones que se
  repiten en los casos de uso de proveedores.

## Pruebas

- `ContextoSolicitudH2Test`, `MaterialApoyoPropuestaH2Test`,
  `AjustePropuestaH2Test` y `SolicitudDeServicioH2Test`: base H2 en memoria.
- `PresentacionSolicitudTest`, `DatosPropuestaTest`, `SolicitudNuevaTest` y
  `AdjuntoMensajeTest`: reglas de presentación y validación.

## Evidencias

- `evidencias/cu12-proveedor-propuesta-resumen.png`
- `evidencias/cu12-proveedor-conversacion.png`
- `evidencias/cu12-organizador-propuesta.png`
