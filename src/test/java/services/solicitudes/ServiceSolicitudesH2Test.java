package services.solicitudes;

import entities.SesionUsuario;
import entities.eventos.EventoSolicitable;
import entities.solicitudes.MensajeSolicitud;
import entities.solicitudes.NecesidadAbierta;
import entities.solicitudes.PropuestaServicio;
import entities.solicitudes.SolicitudEjecucion;
import inicializacion.CargarDatosEjemplo;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;
import persistencia.DatabaseConnection;
import persistencia.PropiedadSistemaTemporal;
import repositories.solicitudes.RepositoryPropuestaServicio;
import repositories.solicitudes.RepositorySolicitudEjecucion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// estos repositorios usan nexteventux.db.url cuando no reciben una url
@ResourceLock(Resources.SYSTEM_PROPERTIES)
class ServiceSolicitudesH2Test {

    private String url;
    private PropiedadSistemaTemporal propiedadUrlBaseDatos;
    private ServiceSolicitudes organizador;
    private ServiceSolicitudes proveedor;
    private ServiceSolicitudes otroProveedor;

    @BeforeEach
    void preparar() throws Exception {
        // la base vive entre conexiones del servicio y tiene un nombre único en memoria
        url = "jdbc:h2:mem:solicitudes_" + UUID.randomUUID()
                + ";DB_CLOSE_DELAY=-1";
        // los repositorios de solicitudes leen esta propiedad global
        propiedadUrlBaseDatos
                = new PropiedadSistemaTemporal("nexteventux.db.url", url);

        // cargamos el seed solo en esta h2 temporal vacía
        try (Connection connection = DatabaseConnection.getConnection(url)) {
            CargarDatosEjemplo.cargar(connection);
        }

        organizador = crearServicio(sesion("ORGANIZADOR", 1, 1,
                "ACTIVO", "ACTIVO"));
        proveedor = crearServicio(sesion("PROVEEDOR", 1, 3,
                "ACTIVO", "APROBADO"));
        otroProveedor = crearServicio(sesion("PROVEEDOR", 2, 4,
                "ACTIVO", "VERIFICADO"));
    }

    @AfterEach
    void limpiar() throws Exception {
        try {
            if (url != null) {
                // usamos jdbc directo para apagar esta base temporal sin volver a inicializarla
                try (Connection connection
                        = DriverManager.getConnection(url, "sa", "");
                        Statement statement = connection.createStatement()) {
                    statement.execute("SHUTDOWN");
                }
            }
        } finally {
            if (propiedadUrlBaseDatos != null) {
                // devolvemos la propiedad a su valor anterior
                propiedadUrlBaseDatos.restaurar();
            }
        }
    }

    @Test
    void proveedorListaSoloSusSolicitudesYRechazaUnaPendienteConPersistencia()
            throws Exception {
        List<SolicitudEjecucion> solicitudes
                = proveedor.listarSolicitudesProveedor(1);

        for (SolicitudEjecucion solicitud : solicitudes) {
            assertEquals(1, solicitud.getIdProveedor());
        }
        SolicitudEjecucion pendiente = null;
        for (SolicitudEjecucion solicitud : solicitudes) {
            if (solicitud.getIdSolicitud() == 4) {
                pendiente = solicitud;
                break;
            }
        }
        assertNotNull(pendiente);
        assertEquals("PENDIENTE", pendiente.getEstado());
        assertEquals("Cumpleaños 15 de Sofía",
                pendiente.getContexto().nombreEvento());

        proveedor.rechazarSolicitud(1, 4, "  No tengo disponibilidad.  ");

        assertEquals("RECHAZADA", valor("SELECT estado FROM Solicitud_Ejecucion "
                + "WHERE id_solicitud = 4"));
        assertEquals("No tengo disponibilidad.", valor(
                "SELECT motivo_rechazo FROM Solicitud_Ejecucion "
                + "WHERE id_solicitud = 4"));
        assertNotNull(valor("SELECT fecha_respuesta FROM Solicitud_Ejecucion "
                + "WHERE id_solicitud = 4"));
    }

    @Test
    void proveedorNoPuedeResponderSolicitudAjenaNiUnaSolicitudCerrada()
            throws Exception {
        assertThrows(IllegalStateException.class,
                () -> proveedor.rechazarSolicitud(1, 5, "Respuesta ajena."));
        assertThrows(IllegalStateException.class,
                () -> proveedor.rechazarSolicitud(1, 10, "Solicitud cerrada."));

        assertEquals("EN_NEGOCIACION", valor("SELECT estado FROM Solicitud_Ejecucion "
                + "WHERE id_solicitud = 5"));
        assertEquals("PROPUESTA_RECHAZADA", valor(
                "SELECT estado FROM Solicitud_Ejecucion WHERE id_solicitud = 10"));
    }

    @Test
    void rechazaRolIncorrectoYProveedorNoAprobadoSinModificarDatos()
            throws Exception {
        ServiceSolicitudes proveedorPendiente = crearServicio(sesion(
                "PROVEEDOR", 1, 3, "ACTIVO", "PENDIENTE"));
        ServiceSolicitudes usuarioInvalido = crearServicio(sesion(
                "PROVEEDOR", 1, 0, "ACTIVO", "APROBADO"));
        ServiceSolicitudes actorInvalido = crearServicio(sesion(
                "PROVEEDOR", 0, 3, "ACTIVO", "APROBADO"));
        ServiceSolicitudes rolDesconocido = crearServicio(sesion(
                "INVITADO", 1, 3, "ACTIVO", "ACTIVO"));
        ServiceSolicitudes organizadorNoActivo = crearServicio(sesion(
                "ORGANIZADOR", 1, 1, "ACTIVO", "PENDIENTE"));
        SesionUsuario sesionSinEstado = sesion(
                "PROVEEDOR", 1, 3, "ACTIVO", "APROBADO");
        sesionSinEstado.setEstadoUsuario(null);
        ServiceSolicitudes estadoUsuarioNulo
                = crearServicio(sesionSinEstado);

        assertThrows(IllegalStateException.class,
                () -> organizador.listarSolicitudesProveedor(1));
        assertThrows(IllegalStateException.class,
                () -> proveedorPendiente.listarSolicitudesProveedor(1));
        assertThrows(IllegalStateException.class,
                () -> usuarioInvalido.listarSolicitudesProveedor(1));
        assertThrows(IllegalStateException.class,
                () -> actorInvalido.listarSolicitudesProveedor(1));
        assertThrows(IllegalStateException.class,
                () -> rolDesconocido.listarSolicitudesProveedor(1));
        assertThrows(IllegalStateException.class,
                () -> organizadorNoActivo.listarSolicitudesProveedor(1));
        assertThrows(IllegalStateException.class,
                () -> estadoUsuarioNulo.listarSolicitudesProveedor(1));
        assertThrows(IllegalArgumentException.class,
                () -> new ServiceSolicitudes(
                        new RepositorySolicitudEjecucion(),
                        new RepositoryPropuestaServicio(),
                        null));

        assertEquals("PENDIENTE", valor("SELECT estado FROM Solicitud_Ejecucion "
                + "WHERE id_solicitud = 4"));
        assertEquals("0", valor("SELECT COUNT(*) FROM Propuesta "
                + "WHERE id_solicitud = 4"));
    }

    @Test
    void rechazaSolicitudConIdentificadoresOMotivoInvalidosSinCambios()
            throws Exception {
        assertThrows(IllegalArgumentException.class,
                () -> proveedor.rechazarSolicitud(1, 0, "Motivo válido"));
        assertThrows(IllegalArgumentException.class,
                () -> proveedor.rechazarSolicitud(0, 4, "Motivo válido"));
        assertThrows(IllegalArgumentException.class,
                () -> proveedor.rechazarSolicitud(1, 4, null));
        assertThrows(IllegalArgumentException.class,
                () -> proveedor.rechazarSolicitud(1, 4, "  "));
        assertThrows(IllegalArgumentException.class,
                () -> proveedor.rechazarSolicitud(1, 4, "m".repeat(501)));

        assertEquals("PENDIENTE", valor("SELECT estado FROM Solicitud_Ejecucion "
                + "WHERE id_solicitud = 4"));
        assertNull(valor("SELECT motivo_rechazo FROM Solicitud_Ejecucion "
                + "WHERE id_solicitud = 4"));
        assertNull(valor("SELECT fecha_respuesta FROM Solicitud_Ejecucion "
                + "WHERE id_solicitud = 4"));
    }

    @Test
    void creaSolicitudEnvíaPropuestaYLaOrganizadoraLaSelecciona() throws Exception {
        List<EventoSolicitable> eventos
                = organizador.listarEventosSolicitables(1);
        boolean eventoEncontrado = false;
        for (EventoSolicitable evento : eventos) {
            if (evento.idEvento() == 1) {
                eventoEncontrado = true;
                break;
            }
        }
        assertTrue(eventoEncontrado);

        boolean necesidadExistente = false;
        for (NecesidadAbierta item : organizador.listarNecesidadesOrganizador(1)) {
            if (item.idEvento() == 1) {
                necesidadExistente = true;
                break;
            }
        }
        assertFalse(necesidadExistente);

        int idSolicitud = organizador.crearSolicitud(
                1, 1, 1, "  Decoración adicional para la recepción.  ");
        assertTrue(idSolicitud > 0);
        assertEquals("PENDIENTE", valor("SELECT estado FROM Solicitud_Ejecucion "
                + "WHERE id_solicitud = " + idSolicitud));
        assertEquals("Decoración adicional para la recepción.",
                valor("SELECT descripcion FROM Necesidad_Servicio_Evento "
                        + "WHERE id_necesidad_servicio = (SELECT id_necesidad_servicio "
                        + "FROM Solicitud_Ejecucion WHERE id_solicitud = "
                        + idSolicitud + ")"));

        NecesidadAbierta necesidad = null;
        for (NecesidadAbierta item : organizador.listarNecesidadesOrganizador(1)) {
            if (item.idEvento() == 1) {
                necesidad = item;
                break;
            }
        }
        assertNotNull(necesidad);
        assertEquals(idSolicitud, Integer.parseInt(valor(
                "SELECT id_solicitud FROM Solicitud_Ejecucion "
                + "WHERE id_necesidad_servicio = " + necesidad.idNecesidad())));

        int idPropuesta = proveedor.enviarPropuesta(
                1,
                idSolicitud,
                new BigDecimal("500000.00"),
                "  Montaje de recepción  ",
                "  Incluye instalación y retiro  ");
        assertTrue(idPropuesta > 0);
        assertEquals("EN_NEGOCIACION", valor("SELECT estado FROM Solicitud_Ejecucion "
                + "WHERE id_solicitud = " + idSolicitud));
        assertEquals("Montaje de recepción", valor("SELECT descripcion FROM Propuesta "
                + "WHERE id_propuesta = " + idPropuesta));

        organizador.seleccionarPropuesta(1, idPropuesta);

        assertEquals("SELECCIONADA", valor("SELECT estado FROM Necesidad_Servicio_Evento "
                + "WHERE id_necesidad_servicio = " + necesidad.idNecesidad()));
        assertEquals(Integer.toString(idPropuesta),
                valor("SELECT id_propuesta_seleccionada "
                        + "FROM Necesidad_Servicio_Evento "
                        + "WHERE id_necesidad_servicio = " + necesidad.idNecesidad()));
        PropuestaServicio seleccionada = null;
        for (PropuestaServicio propuesta : organizador.listarPropuestasOrganizador(1)) {
            if (propuesta.getIdPropuesta() == idPropuesta) {
                seleccionada = propuesta;
                break;
            }
        }
        assertNotNull(seleccionada);
        assertEquals("SELECCIONADA", seleccionada.getEstado());
    }

    @Test
    void creaSolicitudParaNecesidadAbiertaYRechazaDuplicadoDelMismoProveedor()
            throws Exception {
        ServiceSolicitudes organizadorEventoTres
                = organizadorSolicitudesEventoTres();
        int necesidadesAntes = Integer.parseInt(valor(
                "SELECT COUNT(*) FROM Necesidad_Servicio_Evento WHERE id_evento = 3"));

        assertThrows(IllegalArgumentException.class,
                () -> organizadorEventoTres.crearSolicitudParaNecesidad(
                        0, 3, 3, 1));
        assertThrows(IllegalArgumentException.class,
                () -> organizadorEventoTres.crearSolicitudParaNecesidad(
                        3, 0, 3, 1));
        assertThrows(IllegalArgumentException.class,
                () -> organizadorEventoTres.crearSolicitudParaNecesidad(
                        3, 3, 0, 1));
        assertThrows(IllegalArgumentException.class,
                () -> organizadorEventoTres.crearSolicitudParaNecesidad(
                        3, 3, 3, 0));

        assertEquals("ABIERTA", valor(
                "SELECT estado FROM Necesidad_Servicio_Evento "
                        + "WHERE id_necesidad_servicio = 1 AND id_evento = 3"));
        assertEquals("0", valor("SELECT COUNT(*) FROM Solicitud_Ejecucion "
                + "WHERE id_necesidad_servicio = 1 AND id_proveedor = 2"));

        int idSolicitud = organizadorEventoTres.crearSolicitudParaNecesidad(
                3, 3, 3, 1);

        assertTrue(idSolicitud > 0);
        assertEquals("2", valor("SELECT id_proveedor FROM Solicitud_Ejecucion "
                + "WHERE id_solicitud = " + idSolicitud));
        assertEquals("3", valor("SELECT id_servicio FROM Solicitud_Ejecucion "
                + "WHERE id_solicitud = " + idSolicitud));
        assertEquals("1", valor("SELECT id_necesidad_servicio FROM Solicitud_Ejecucion "
                + "WHERE id_solicitud = " + idSolicitud));
        assertEquals("PENDIENTE", valor("SELECT estado FROM Solicitud_Ejecucion "
                + "WHERE id_solicitud = " + idSolicitud));

        assertThrows(IllegalStateException.class,
                () -> organizadorEventoTres.crearSolicitudParaNecesidad(
                        3, 3, 4, 1));

        assertEquals("1", valor("SELECT COUNT(*) FROM Solicitud_Ejecucion "
                + "WHERE id_necesidad_servicio = 1 AND id_proveedor = 2"));
        assertEquals(necesidadesAntes, Integer.parseInt(valor(
                "SELECT COUNT(*) FROM Necesidad_Servicio_Evento WHERE id_evento = 3")));
        assertEquals("ABIERTA", valor(
                "SELECT estado FROM Necesidad_Servicio_Evento "
                        + "WHERE id_necesidad_servicio = 1"));
    }

    @Test
    void rechazaPropuestasConPrecioOMaterialInvalidos()
            throws Exception {
        assertThrows(IllegalArgumentException.class,
                () -> proveedor.enviarPropuesta(1, 4, BigDecimal.ZERO,
                        "Alcance", "Condiciones"));
        assertThrows(IllegalArgumentException.class,
                () -> proveedor.enviarPropuesta(0, 4,
                        new BigDecimal("100.00"), "Alcance", "Condiciones"));
        assertThrows(IllegalArgumentException.class,
                () -> proveedor.enviarPropuesta(1, 0,
                        new BigDecimal("100.00"), "Alcance", "Condiciones"));
        assertThrows(IllegalArgumentException.class,
                () -> proveedor.enviarPropuesta(1, 4, null,
                        "Alcance", "Condiciones"));
        assertThrows(IllegalArgumentException.class,
                () -> proveedor.enviarPropuesta(1, 4,
                        new BigDecimal("100.001"), "Alcance", "Condiciones"));
        assertThrows(IllegalArgumentException.class,
                () -> proveedor.enviarPropuesta(1, 4,
                        new BigDecimal("10000000000.00"),
                        "Alcance", "Condiciones"));
        assertThrows(IllegalArgumentException.class,
                () -> proveedor.enviarPropuesta(1, 4,
                        new BigDecimal("100.00"), "Alcance", "Condiciones",
                        "http://ejemplo.com/catalogo"));
        assertThrows(IllegalArgumentException.class,
                () -> proveedor.enviarPropuesta(1, 4,
                        new BigDecimal("100.00"), null, "Condiciones"));
        assertThrows(IllegalArgumentException.class,
                () -> proveedor.enviarPropuesta(1, 4,
                        new BigDecimal("100.00"), " ", "Condiciones"));
        assertThrows(IllegalArgumentException.class,
                () -> proveedor.enviarPropuesta(1, 4,
                        new BigDecimal("100.00"), "Alcance", " "));
        assertThrows(IllegalArgumentException.class,
                () -> proveedor.enviarPropuesta(1, 4,
                        new BigDecimal("100.00"), "Alcance", null));
        assertThrows(IllegalArgumentException.class,
                () -> proveedor.enviarPropuesta(1, 4,
                        new BigDecimal("100.00"), "d".repeat(256), "Condiciones"));
        assertThrows(IllegalArgumentException.class,
                () -> proveedor.enviarPropuesta(1, 4,
                        new BigDecimal("100.00"), "Alcance", "c".repeat(1001)));
        assertThrows(IllegalArgumentException.class,
                () -> proveedor.enviarPropuesta(1, 4,
                        new BigDecimal("100.00"), "Alcance", "Condiciones",
                        "https://ejemplo.com/" + "a".repeat(1001)));

        assertEquals("0", valor("SELECT COUNT(*) FROM Propuesta "
                + "WHERE id_solicitud = 4"));
        assertEquals("PENDIENTE", valor("SELECT estado FROM Solicitud_Ejecucion "
                + "WHERE id_solicitud = 4"));
    }

    @Test
    void aceptaPropuestaConCerosDecimalesRedundantesYMaterialDeApoyo()
            throws Exception {
        int idPropuesta = proveedor.enviarPropuesta(
                1,
                4,
                new BigDecimal("100.000"),
                "  Montaje sencillo  ",
                "  Incluye instalación  ",
                "  https://ejemplo.com/catalogo.pdf  ");

        assertTrue(idPropuesta > 0);
        assertEquals(new BigDecimal("100.00"),
                new BigDecimal(valor("SELECT valor FROM Propuesta "
                        + "WHERE id_propuesta = " + idPropuesta)));
        assertEquals("https://ejemplo.com/catalogo.pdf",
                valor("SELECT material_apoyo FROM Propuesta "
                        + "WHERE id_propuesta = " + idPropuesta));
        assertEquals("EN_NEGOCIACION", valor(
                "SELECT estado FROM Solicitud_Ejecucion WHERE id_solicitud = 4"));
    }

    @Test
    void admiteMaterialDeApoyoEnBlancoYRechazaUrlConSintaxisInvalida()
            throws Exception {
        int idPropuesta = proveedor.enviarPropuesta(
                1, 4, new BigDecimal("100.00"), "Alcance", "Condiciones", "   ");

        assertTrue(idPropuesta > 0);
        assertTrue(valor("SELECT material_apoyo FROM Propuesta "
                + "WHERE id_propuesta = " + idPropuesta) == null);
        assertThrows(IllegalArgumentException.class,
                () -> proveedor.enviarPropuesta(
                        1, 4, new BigDecimal("100.00"), "Alcance", "Condiciones",
                        "https://[::1"));
        assertThrows(IllegalArgumentException.class,
                () -> proveedor.enviarPropuesta(
                        1, 4, new BigDecimal("100.00"), "Alcance", "Condiciones",
                        "https:///catalogo"));
        assertEquals("1", valor("SELECT COUNT(*) FROM Propuesta "
                + "WHERE id_solicitud = 4"));
    }

    @Test
    void noCreaSolicitudSinDescripcion() throws Exception {
        assertThrows(IllegalArgumentException.class,
                () -> organizador.crearSolicitud(1, 1, 1, " "));
        assertThrows(IllegalArgumentException.class,
                () -> organizador.crearSolicitud(0, 1, 1, "Necesidad válida"));
        assertThrows(IllegalArgumentException.class,
                () -> organizador.crearSolicitud(1, 0, 1, "Necesidad válida"));
        assertThrows(IllegalArgumentException.class,
                () -> organizador.crearSolicitud(1, 1, 0, "Necesidad válida"));
        assertThrows(IllegalArgumentException.class,
                () -> organizador.crearSolicitud(1, 1, 1, null));
        assertThrows(IllegalArgumentException.class,
                () -> organizador.crearSolicitud(1, 1, 1, "d".repeat(501)));

        assertEquals("0", valor("SELECT COUNT(*) FROM Solicitud_Ejecucion "
                + "WHERE id_evento = 1 AND id_necesidad_servicio IS NOT NULL "
                + "AND id_solicitud > 15"));
    }

    @Test
    void organizadoraSolicitaAjusteYAmbasPartesPersistenMensajesDeLaConversacion()
            throws Exception {
        organizadorSolicitudesEventoTres().solicitarAjuste(
                3, 4, "  ¿Puedes incluir retoque de fotografías?  ",
                "  https://ejemplo.com/referencias  ");

        List<MensajeSolicitud> mensajesOrganizador
                = organizadorSolicitudesEventoTres().listarMensajes(5, 8);
        MensajeSolicitud ajuste = null;
        for (MensajeSolicitud mensaje : mensajesOrganizador) {
            if (mensaje.esSolicitudDeAjuste()) {
                ajuste = mensaje;
                break;
            }
        }
        assertNotNull(ajuste);
        assertEquals(4, ajuste.getIdPropuesta());
        assertEquals(1, ajuste.getVersionPropuesta());
        assertEquals("¿Puedes incluir retoque de fotografías?",
                ajuste.getContenido());
        assertEquals("https://ejemplo.com/referencias", ajuste.getAdjuntoUrl());
        assertEquals("1", valor("SELECT COUNT(*) FROM Mensaje "
                + "WHERE id_propuesta = 4 "
                + "AND contenido = '¿Puedes incluir retoque de fotografías?'"));

        otroProveedor.enviarMensaje(5, 4, "  Sí, está incluido.  ", null);

        List<MensajeSolicitud> mensajesProveedor
                = otroProveedor.listarMensajes(5, 4);
        boolean respuestaEncontrada = false;
        for (MensajeSolicitud mensaje : mensajesProveedor) {
            if ("Sí, está incluido.".equals(mensaje.getContenido())
                    && mensaje.getIdUsuarioRemitente() == 4) {
                respuestaEncontrada = true;
                break;
            }
        }
        assertTrue(respuestaEncontrada);
        assertEquals("1", valor("SELECT COUNT(*) FROM Mensaje "
                + "WHERE id_usuario_remitente = 4 "
                + "AND contenido = 'Sí, está incluido.'"));
    }

    @Test
    void organizadoraSolicitaAjusteSinAdjuntoYLoPersisteVinculadoALaPropuesta()
            throws Exception {
        String ajuste = "Solicito el detalle del horario de montaje.";
        organizadorSolicitudesEventoTres().solicitarAjuste(
                3, 4, "  " + ajuste + "  ", null);
        String ajusteSinEnlace = "Solicito también el detalle de instalación.";
        organizadorSolicitudesEventoTres().solicitarAjuste(
                3, 4, ajusteSinEnlace, "  ");

        assertEquals("1", valor("SELECT COUNT(*) FROM Mensaje "
                + "WHERE id_propuesta = 4 AND contenido = '" + ajuste + "'"));
        assertNull(valor("SELECT adjunto_url FROM Mensaje "
                + "WHERE id_propuesta = 4 AND contenido = '" + ajuste + "'"));
        assertEquals("1", valor("SELECT COUNT(*) FROM Mensaje "
                + "WHERE id_propuesta = 4 AND contenido = '"
                + ajusteSinEnlace + "' AND adjunto_url IS NULL"));
    }

    @Test
    void restringeLecturaYEnvioDeMensajesALaCuentaAutenticada()
            throws Exception {
        assertThrows(IllegalStateException.class,
                () -> organizador.listarMensajes(5, 4));
        assertThrows(IllegalArgumentException.class,
                () -> organizador.listarMensajes(0, 1));
        assertThrows(IllegalArgumentException.class,
                () -> organizador.listarMensajes(5, 0));
        assertThrows(IllegalStateException.class,
                () -> proveedor.enviarMensaje(5, 8, "Mensaje ajeno", null));
        assertThrows(IllegalArgumentException.class,
                () -> organizador.enviarMensaje(0, 1, "Mensaje válido", null));
        assertThrows(IllegalArgumentException.class,
                () -> organizador.enviarMensaje(4, 0, "Mensaje válido", null));
        assertThrows(IllegalArgumentException.class,
                () -> organizador.enviarMensaje(4, 1, "  ", null));

        assertEquals("2", valor("SELECT COUNT(*) FROM Mensaje "
                + "WHERE id_conversacion = 4"));
    }

    @Test
    void rechazaMensajesYAjustesInvalidosSinGuardarMensajes() throws Exception {
        ServiceSolicitudes organizadorEventoTres
                = organizadorSolicitudesEventoTres();
        int mensajesAntes = Integer.parseInt(valor(
                "SELECT COUNT(*) FROM Mensaje WHERE id_conversacion = 4"));

        assertThrows(IllegalArgumentException.class,
                () -> organizadorEventoTres.enviarMensaje(
                        5, 8, "m".repeat(1001), null));
        assertThrows(IllegalArgumentException.class,
                () -> organizadorEventoTres.enviarMensaje(
                        5, 8, null, null));
        assertThrows(IllegalArgumentException.class,
                () -> organizadorEventoTres.enviarMensaje(
                        5, 8, "Mensaje válido",
                        "https://ejemplo.com/" + "a".repeat(1001)));
        assertThrows(IllegalArgumentException.class,
                () -> organizadorEventoTres.solicitarAjuste(3, 4, "  ", null));
        assertThrows(IllegalArgumentException.class,
                () -> organizadorEventoTres.solicitarAjuste(
                        0, 4, "Solicitar cambio", null));
        assertThrows(IllegalArgumentException.class,
                () -> organizadorEventoTres.solicitarAjuste(
                        3, 0, "Solicitar cambio", null));
        assertThrows(IllegalArgumentException.class,
                () -> organizadorEventoTres.solicitarAjuste(3, 4, null, null));
        assertThrows(IllegalArgumentException.class,
                () -> organizadorEventoTres.solicitarAjuste(
                        3, 4, "a".repeat(1001), null));
        assertThrows(IllegalStateException.class,
                () -> organizadorEventoTres.solicitarAjuste(
                        3, 9999, "Solicitar cambio", null));

        assertEquals(mensajesAntes, Integer.parseInt(valor(
                "SELECT COUNT(*) FROM Mensaje WHERE id_conversacion = 4")));
    }

    @Test
    void enviaMensajesConAdjuntoVacioOVinculadoYLosPersiste()
            throws Exception {
        otroProveedor.enviarMensaje(5, 4, "Mensaje sin adjunto", "  ");
        otroProveedor.enviarMensaje(
                5, 4, "Mensaje con adjunto", "  https://ejemplo.com/foto.jpg  ");

        assertEquals("1", valor("SELECT COUNT(*) FROM Mensaje "
                + "WHERE contenido = 'Mensaje sin adjunto' "
                + "AND adjunto_url IS NULL"));
        assertEquals("https://ejemplo.com/foto.jpg", valor(
                "SELECT adjunto_url FROM Mensaje "
                        + "WHERE contenido = 'Mensaje con adjunto'"));
    }

    @Test
    void rechazaPropuestaVigenteYElProveedorConsultaElHistorialActualizado()
            throws Exception {
        organizadorSolicitudesEventoTres().rechazarPropuesta(3, 9);

        assertEquals("RECHAZADA", valor("SELECT estado FROM Propuesta "
                + "WHERE id_propuesta = 9"));
        assertEquals("PROPUESTA_RECHAZADA", valor(
                "SELECT estado FROM Solicitud_Ejecucion WHERE id_solicitud = 14"));
        PropuestaServicio rechazada = crearServicio(sesion("PROVEEDOR", 5, 13,
                "ACTIVO", "VERIFICADO"))
                .listarHistorialPropuestas(5, 14).get(0);
        assertEquals(9, rechazada.getIdPropuesta());
        assertFalse(rechazada.isVigente());
        assertEquals("RECHAZADA", rechazada.getEstado());
    }

    @Test
    void operacionesRechazanRolCuentaEIdentificadoresQueNoCorresponden()
            throws Exception {
        ServiceSolicitudes cuentaAjena = crearServicio(sesion(
                "PROVEEDOR", 2, 4, "ACTIVO", "APROBADO"));
        ServiceSolicitudes inactivo = crearServicio(sesion(
                "PROVEEDOR", 1, 3, "INACTIVO", "APROBADO"));
        ServiceSolicitudes cliente = crearServicio(sesion(
                "CLIENTE", 8, 8, "ACTIVO", "ACTIVO"));
        ServiceSolicitudes usuarioSinId = crearServicio(sesion(
                "CLIENTE", 1, 0, "ACTIVO", "ACTIVO"));
        ServiceSolicitudes actorSinId = crearServicio(sesion(
                "CLIENTE", 0, 8, "ACTIVO", "ACTIVO"));
        int mensajesAntes = Integer.parseInt(valor(
                "SELECT COUNT(*) FROM Mensaje WHERE id_conversacion = 4"));

        assertThrows(IllegalStateException.class,
                () -> cuentaAjena.listarSolicitudesProveedor(1));
        assertThrows(IllegalStateException.class,
                () -> inactivo.listarSolicitudesProveedor(1));
        assertThrows(IllegalStateException.class,
                () -> cliente.listarMensajes(5, 8));
        assertThrows(IllegalStateException.class,
                () -> usuarioSinId.listarMensajes(5, 8));
        assertThrows(IllegalStateException.class,
                () -> actorSinId.listarMensajes(5, 8));
        assertThrows(IllegalArgumentException.class,
                () -> proveedor.listarSolicitudesProveedor(0));
        assertThrows(IllegalArgumentException.class,
                () -> organizador.listarEventosSolicitables(-1));
        assertThrows(IllegalArgumentException.class,
                () -> organizador.listarPropuestasOrganizador(0));
        assertThrows(IllegalArgumentException.class,
                () -> organizador.rechazarPropuesta(0, 9));
        assertThrows(IllegalArgumentException.class,
                () -> organizador.rechazarPropuesta(3, 0));
        assertThrows(IllegalArgumentException.class,
                () -> organizador.seleccionarPropuesta(0, 9));
        assertThrows(IllegalArgumentException.class,
                () -> organizador.seleccionarPropuesta(3, 0));
        assertThrows(IllegalArgumentException.class,
                () -> proveedor.listarHistorialPropuestas(0, 14));
        assertThrows(IllegalArgumentException.class,
                () -> proveedor.listarHistorialPropuestas(5, 0));
        assertEquals(mensajesAntes, Integer.parseInt(valor(
                "SELECT COUNT(*) FROM Mensaje WHERE id_conversacion = 4")));
    }

    private ServiceSolicitudes organizadorSolicitudesEventoTres() {
        return crearServicio(sesion("ORGANIZADOR", 3, 8,
                "ACTIVO", "ACTIVO"));
    }

    private ServiceSolicitudes crearServicio(SesionUsuario sesion) {
        return new ServiceSolicitudes(
                new RepositorySolicitudEjecucion(),
                new RepositoryPropuestaServicio(),
                sesion);
    }

    private SesionUsuario sesion(
            String tipo,
            int idActor,
            int idUsuario,
            String estadoUsuario,
            String estadoActor) {
        SesionUsuario sesion = new SesionUsuario();
        sesion.setTipoUsuario(tipo);
        sesion.setIdActor(idActor);
        sesion.setIdUsuario(idUsuario);
        sesion.setEstadoUsuario(estadoUsuario);
        sesion.setEstadoActor(estadoActor);
        return sesion;
    }

    private String valor(String sql) throws Exception {
        // consultamos h2 para comprobar los cambios que dejó el servicio
        try (Connection connection = DatabaseConnection.getConnection();
                Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery(sql)) {
            assertTrue(result.next(), "La consulta debe devolver un valor.");
            return result.getString(1);
        }
    }
}
