package services.eventos;

import entities.SesionUsuario;
import entities.Organizador;
import entities.eventos.EdicionEvento;
import entities.eventos.Evento;
import entities.eventos.Fotografia;
import entities.finanzas.PresupuestoEvento;
import entities.servicios.ServicioSolicitable;
import entities.solicitudes.SolicitudServicioEvento;
import inicializacion.CargarDatosEjemplo;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import persistencia.DatabaseConnection;
import repositories.eventos.RepositoryCancelacion;
import repositories.eventos.RepositoryEvento;
import repositories.eventos.RepositoryFinalizacion;
import repositories.solicitudes.RepositorySolicitudServicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServiceEventosH2Test {

    private static final int EVENTO = 1;
    private static final int ORGANIZADOR = 1;
    private String url;
    private ServiceEventos service;

    @BeforeEach
    void preparar() throws Exception {
        // la base vive entre conexiones del servicio y tiene un nombre único en memoria
        url = "jdbc:h2:mem:eventos_" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1";

        // cargamos los datos de ejemplo solo en esta base temporal vacía
        try (Connection connection = DatabaseConnection.getConnection(url)) {
            CargarDatosEjemplo.cargar(connection);
        }

        service = new ServiceEventos(
                new RepositoryEvento(url),
                new RepositoryCancelacion(url),
                new RepositoryFinalizacion(url, Clock.systemDefaultZone()),
                new RepositorySolicitudServicio(url));
    }

    @AfterEach
    void limpiar() throws Exception {
        if (url != null) {
            // usamos jdbc directo para apagar esta base temporal sin volver a inicializarla
            try (Connection connection = DriverManager.getConnection(url, "sa", "");
                    Statement statement = connection.createStatement()) {
                statement.execute("SHUTDOWN");
            }
        }
    }

    @Test
    void creaEventoAutenticadoYLoPersisteComoPlaneado() throws Exception {
        Evento evento = eventoNuevo(ORGANIZADOR, "Evento de integración");
        evento.setEstado("CONFIRMADO");

        service.crearEvento(evento, sesionOrganizador());

        assertNotNull(evento.getIdEvento());
        assertTrue(evento.getIdEvento() > 0);
        assertEquals(Evento.ESTADO_INICIAL, evento.getEstado());
        assertEquals("Evento de integración",
                valor("SELECT nombre FROM Evento WHERE id_evento = " + evento.getIdEvento()));
        assertEquals(Evento.ESTADO_INICIAL,
                valor("SELECT estado FROM Evento WHERE id_evento = " + evento.getIdEvento()));
    }

    @Test
    void rechazaCrearEventoQuePerteneceAOtroOrganizador() throws Exception {
        IllegalArgumentException eventoAjeno = assertThrows(IllegalArgumentException.class,
                () -> service.crearEvento(eventoNuevo(ORGANIZADOR, "Evento ajeno"), sesion(2, 2)));
        assertEquals("El evento debe pertenecer a tu cuenta.", eventoAjeno.getMessage());
        assertThrows(IllegalArgumentException.class,
                () -> service.crearEvento(null, sesionOrganizador()));
        assertEquals("4", valor("SELECT COUNT(*) FROM Evento"));
    }

    @Test
    void exigeProveedorVerificadoAntesDeCrearEvento() throws Exception {
        ejecutar("UPDATE Proveedor SET estado = 'PENDIENTE'");
        IllegalArgumentException sinProveedor = assertThrows(IllegalArgumentException.class,
                () -> service.crearEvento(eventoNuevo(ORGANIZADOR, "Evento sin proveedor"), sesionOrganizador()));
        assertEquals("Debe existir al menos un proveedor verificado en el directorio antes de crear el evento.",
                sinProveedor.getMessage());
        assertEquals("4", valor("SELECT COUNT(*) FROM Evento"));
    }

    @Test
    void aceptaLosValoresMaximosYMinimosDeUnEvento() {
        LocalDateTime inicio = LocalDateTime.now().plusDays(30).withSecond(0).withNano(0);
        Evento enLimites = new Evento(ORGANIZADOR, "E".repeat(150), "T".repeat(100), "D".repeat(255),
                inicio, inicio.plusHours(1), "L".repeat(255), new BigDecimal("9999999999.99"), 0);
        assertTrue(service.validarInformacionEvento(enLimites));
    }

    @Test
    void rechazaDatosGeneralesYPresupuestoInvalidos() {
        LocalDateTime inicio = LocalDateTime.now().plusDays(30).withSecond(0).withNano(0);
        Evento presupuestoConTresDecimales = new Evento(ORGANIZADOR, "Evento", "Boda", null,
                inicio, inicio.plusHours(1), "Salón", new BigDecimal("1000.001"), 0);
        assertFalse(service.validarInformacionEvento(null));
        assertFalse(service.validarInformacionEvento(presupuestoConTresDecimales));
        assertFalse(service.validarInformacionEvento(eventoConFechasInvertidas()));
    }

    @Test
    void requiereOrganizadorActivoParaGestionarEventos() {
        SesionUsuario inactiva = sesionOrganizador();
        inactiva.setEstadoUsuario("INACTIVO");

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> service.nombreOrganizador(inactiva));

        assertEquals("Inicia sesión como organizador activo para gestionar tus eventos.",
                error.getMessage());

        SesionUsuario actorInactivo = sesionOrganizador();
        actorInactivo.setEstadoActor("PENDIENTE");
        assertThrows(IllegalStateException.class,
                () -> service.nombreOrganizador(actorInactivo));
        SesionUsuario rolIncorrecto = sesionOrganizador();
        rolIncorrecto.setTipoUsuario("PROVEEDOR");
        assertThrows(IllegalStateException.class,
                () -> service.nombreOrganizador(rolIncorrecto));
    }

    @Test
    void listaServiciosDisponiblesParaElOrganizador() {
        List<ServicioSolicitable> servicios = service.listarServiciosSolicitables(sesionOrganizador());
        ServicioSolicitable decoracion = null;
        for (ServicioSolicitable servicio : servicios) {
            if (servicio.id() == 1) {
                decoracion = servicio;
                break;
            }
        }

        assertNotNull(decoracion);
        assertEquals("Decoración floral", decoracion.nombre());
        assertEquals("Mariana López", decoracion.proveedor());
        assertMonto("1200000.00", decoracion.costo());
    }

    @Test
    void consultaPresupuestoDisponibleYComprometidoDelEvento() {
        PresupuestoEvento presupuesto = service.consultarPresupuesto(EVENTO, sesionOrganizador());
        assertMonto("15000000.00", presupuesto.total());
        assertMonto("4000000.00", presupuesto.comprometido());
        assertMonto("11000000.00", presupuesto.disponible());
    }

    @Test
    void listaSolicitudesDeServicioDelEvento() {
        List<SolicitudServicioEvento> solicitudes =
                service.listarSolicitudesServicio(EVENTO, sesionOrganizador());
        boolean primeraEncontrada = false;
        boolean segundaEncontrada = false;
        for (SolicitudServicioEvento solicitud : solicitudes) {
            if (solicitud.id() == 1) {
                primeraEncontrada = true;
            }
            if (solicitud.id() == 2) {
                segundaEncontrada = true;
            }
        }
        assertTrue(primeraEncontrada);
        assertTrue(segundaEncontrada);
    }

    @Test
    void solicitarServicioPersisteLaSolicitudYActualizaElCompromiso() throws Exception {
        int solicitud = service.solicitarServicio(EVENTO, 1, sesionOrganizador());

        assertTrue(solicitud > 0);
        assertEquals("PENDIENTE",
                valor("SELECT estado FROM Solicitud_Ejecucion WHERE id_solicitud = " + solicitud));
        assertEquals("1200000.00",
                valor("SELECT costo_comprometido FROM Solicitud_Ejecucion WHERE id_solicitud = " + solicitud));
        assertEquals("1", valor("SELECT COUNT(*) FROM Necesidad_Servicio_Evento "
                + "WHERE id_evento = 1 AND estado = 'ABIERTA'"));

        PresupuestoEvento presupuesto = service.consultarPresupuesto(EVENTO, sesionOrganizador());
        assertMonto("5200000.00", presupuesto.comprometido());
        assertEquals("1", valor("SELECT COUNT(*) FROM Notificacion_interna "
                + "WHERE id_usuario = 3 AND id_evento = 1 "
                + "AND mensaje LIKE 'Recibiste una solicitud de servicio%'"));
    }

    @Test
    void noRegistraUnaSolicitudQueExcedeElPresupuesto() throws Exception {
        ejecutar("UPDATE Evento SET presupuesto = 4000000.00 WHERE id_evento = " + EVENTO);
        String solicitudesAntes = valor(
                "SELECT COUNT(*) FROM Solicitud_Ejecucion WHERE id_evento = " + EVENTO);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.solicitarServicio(EVENTO, 1, sesionOrganizador()));

        assertTrue(error.getMessage().contains("Presupuesto insuficiente."));
        assertEquals(solicitudesAntes,
                valor("SELECT COUNT(*) FROM Solicitud_Ejecucion WHERE id_evento = " + EVENTO));
        assertEquals("0", valor("SELECT COUNT(*) FROM Necesidad_Servicio_Evento "
                + "WHERE id_evento = " + EVENTO));
    }

    @Test
    void retiraUnaSolicitudPendienteSinPropuesta() throws Exception {
        int solicitud = service.solicitarServicio(EVENTO, 1, sesionOrganizador());
        String necesidad = valor("SELECT id_necesidad_servicio FROM Solicitud_Ejecucion "
                + "WHERE id_solicitud = " + solicitud);

        service.retirarSolicitudServicio(EVENTO, solicitud, sesionOrganizador());

        assertEquals("RETIRADA",
                valor("SELECT estado FROM Solicitud_Ejecucion WHERE id_solicitud = " + solicitud));
        assertEquals("CANCELADA",
                valor("SELECT estado FROM Necesidad_Servicio_Evento WHERE id_necesidad_servicio = " + necesidad));
        boolean retiradaEnLista = false;
        for (SolicitudServicioEvento item
                : service.listarSolicitudesServicio(EVENTO, sesionOrganizador())) {
            if (item.id() == solicitud && item.estado().equals("RETIRADA")) {
                retiradaEnLista = true;
            }
        }
        assertTrue(retiradaEnLista);
        assertEquals("1", valor("SELECT COUNT(*) FROM Notificacion_interna "
                + "WHERE id_usuario = 3 AND id_evento = 1 "
                + "AND mensaje = 'El organizador retiró una solicitud de servicio pendiente.'"));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.retirarSolicitudServicio(EVENTO, solicitud, sesionOrganizador()));
        assertTrue(error.getMessage().contains("Solo puedes retirar una solicitud pendiente"));
    }

    @Test
    void ajustaPresupuestoSinBajarDeLosCompromisos() throws Exception {
        service.ajustarPresupuesto(EVENTO, new BigDecimal("20000000.00"), sesionOrganizador());
        assertMonto("20000000.00",
                service.consultarPresupuesto(EVENTO, sesionOrganizador()).total());

        IllegalArgumentException nulo = assertThrows(IllegalArgumentException.class,
                () -> service.ajustarPresupuesto(EVENTO, null, sesionOrganizador()));
        assertTrue(nulo.getMessage().contains("Ingresa un presupuesto entre 0 y"));
        IllegalArgumentException negativo = assertThrows(IllegalArgumentException.class,
                () -> service.ajustarPresupuesto(EVENTO, new BigDecimal("-1"), sesionOrganizador()));
        assertTrue(negativo.getMessage().contains("Ingresa un presupuesto entre 0 y"));
        IllegalArgumentException excedido = assertThrows(IllegalArgumentException.class,
                () -> service.ajustarPresupuesto(EVENTO, new BigDecimal("10000000000.00"), sesionOrganizador()));
        assertTrue(excedido.getMessage().contains("Ingresa un presupuesto entre 0 y"));

        IllegalArgumentException insuficiente = assertThrows(IllegalArgumentException.class,
                () -> service.ajustarPresupuesto(EVENTO, new BigDecimal("3000000.00"), sesionOrganizador()));
        assertTrue(insuficiente.getMessage().contains("Presupuesto insuficiente."));

        IllegalArgumentException decimales = assertThrows(IllegalArgumentException.class,
                () -> service.ajustarPresupuesto(EVENTO, new BigDecimal("5000000.001"), sesionOrganizador()));
        assertEquals("El presupuesto debe tener como máximo dos decimales.", decimales.getMessage());
        assertEquals("20000000.00", valor("SELECT presupuesto FROM Evento WHERE id_evento = " + EVENTO));
    }

    @Test
    void listaEventosOrganizadoresYNombreDeLaSesion() {
        assertTrue(contieneEvento(
                service.listarEventosParaFinalizar(sesionOrganizador()), EVENTO));
        assertTrue(contieneEvento(
                service.listarEventosParaCancelar(sesionOrganizador()), EVENTO));
        assertTrue(contieneEvento(
                service.listarEventosParaEditar(sesionOrganizador()), EVENTO));

        boolean organizadorEncontrado = false;
        for (Organizador organizador : service.listarOrganizadores()) {
            if (organizador.id() == ORGANIZADOR) {
                organizadorEncontrado = true;
                break;
            }
        }
        assertTrue(organizadorEncontrado);
        assertEquals("Laura Gómez", service.nombreOrganizador(sesionOrganizador()));
    }

    @Test
    void consultaYEditaUnEventoPlaneadoConservandoLosDatosNoEditables() throws Exception {
        Evento original = service.consultarEventoParaEditar(EVENTO, sesionOrganizador());
        EdicionEvento cambios = new EdicionEvento(
                "Boda actualizada", "Celebración",
                LocalDateTime.of(2027, 11, 15, 15, 0),
                LocalDateTime.of(2027, 11, 15, 23, 0),
                "Jardín Central");

        service.editarEvento(EVENTO, sesionOrganizador(), cambios);

        assertEquals("Boda actualizada", service.consultarEventoParaEditar(EVENTO, sesionOrganizador()).getNombre());
        assertEquals("Celebración", valor("SELECT tipo FROM Evento WHERE id_evento = " + EVENTO));
        assertEquals("Jardín Central", valor("SELECT ubicacion FROM Evento WHERE id_evento = " + EVENTO));
        assertEquals(original.getDescripcion(), valor("SELECT descripcion FROM Evento WHERE id_evento = " + EVENTO));
        assertMonto("15000000.00", new BigDecimal(
                valor("SELECT presupuesto FROM Evento WHERE id_evento = " + EVENTO)));
        assertEquals(120, Integer.parseInt(
                valor("SELECT invitados_iniciales FROM Evento WHERE id_evento = " + EVENTO)));
        assertEquals(Evento.ESTADO_INICIAL, valor("SELECT estado FROM Evento WHERE id_evento = " + EVENTO));
    }

    @Test
    void rechazaEditarCuandoElFinEsAnteriorAlInicio() {
        IllegalArgumentException fechas = assertThrows(IllegalArgumentException.class,
                () -> service.editarEvento(EVENTO, sesionOrganizador(), new EdicionEvento(
                        "Evento", "Boda",
                        LocalDateTime.of(2027, 11, 16, 23, 0),
                        LocalDateTime.of(2027, 11, 16, 15, 0),
                        "Salón")));
        assertEquals("El fin del evento no puede ser anterior al inicio.", fechas.getMessage());
    }

    @Test
    void noPermiteConsultarUnEventoDeOtroOrganizador() {
        IllegalArgumentException ajeno = assertThrows(IllegalArgumentException.class,
                () -> service.consultarEventoParaEditar(EVENTO, sesion(2, 2)));
        assertEquals("Selecciona un evento existente de tu cuenta.", ajeno.getMessage());
    }

    @Test
    void noPermiteEditarUnEventoQueYaNoEstaPlaneado() throws Exception {
        ejecutar("UPDATE Evento SET estado = 'CONFIRMADO' WHERE id_evento = " + EVENTO);
        IllegalArgumentException estado = assertThrows(IllegalArgumentException.class,
                () -> service.consultarEventoParaEditar(EVENTO, sesionOrganizador()));
        assertEquals("No se puede editar: el evento está en estado CONFIRMADO.", estado.getMessage());
    }

    @Test
    void exigeConfirmacionAntesDeCancelar() throws Exception {
        IllegalArgumentException sinConfirmar = assertThrows(IllegalArgumentException.class,
                () -> service.cancelarEvento(EVENTO, sesionOrganizador(), false));
        assertEquals("Confirma explícitamente que deseas cancelar el evento.", sinConfirmar.getMessage());
        assertEquals("PLANEADO", valor("SELECT estado FROM Evento WHERE id_evento = " + EVENTO));
    }

    @Test
    void cancelaElEventoYActualizaSusRelaciones() throws Exception {
        prepararReservasContratacionesEvento();
        service.cancelarEvento(EVENTO, sesionOrganizador(), true);

        assertEquals("CANCELADO", valor("SELECT estado FROM Evento WHERE id_evento = " + EVENTO));
        assertEquals("2", valor("SELECT COUNT(*) FROM Contratacion "
                + "WHERE id_evento = 1 AND estado = 'CANCELADA'"));
        assertEquals("2", valor("SELECT COUNT(*) FROM Invitacion "
                + "WHERE id_evento = 1 AND estado_respuesta = 'REVOCADA'"));
        assertEquals("2", valor("SELECT COUNT(*) FROM Solicitud_Ejecucion "
                + "WHERE id_evento = 1 AND estado = 'CANCELADA'"));
        boolean avisoEncontrado = false;
        for (String aviso : service.listarAvisosCancelacion(EVENTO, sesionOrganizador())) {
            if (aviso.contains("Se canceló un evento asociado")) {
                avisoEncontrado = true;
                break;
            }
        }
        assertTrue(avisoEncontrado);
        assertEquals("DISPONIBLE", valor("SELECT estado FROM Disponibilidad_Servicio "
                + "WHERE id_disponibilidad = 1001"));
        IllegalArgumentException yaCancelado = assertThrows(IllegalArgumentException.class,
                () -> service.cancelarEvento(EVENTO, sesionOrganizador(), true));
        assertEquals("No puedes cancelar un evento cancelado o finalizado.", yaCancelado.getMessage());
    }

    @Test
    void soloFinalizaEventosConfirmadosQueYaTerminaron() throws Exception {
        List<Fotografia> fotos = List.of(fotografiaValida());
        IllegalArgumentException sinConfirmar = assertThrows(IllegalArgumentException.class,
                () -> service.finalizarEvento(EVENTO, sesionOrganizador(), fotos));
        assertTrue(sinConfirmar.getMessage().contains("Solo puedes finalizar un evento confirmado"));

        // movemos el evento al futuro para comprobar que todavía no se pueda finalizar
        ejecutar("UPDATE Evento SET estado = 'CONFIRMADO', "
                + "fecha_inicio = TIMESTAMP '2099-01-01 15:00:00', "
                + "fecha_fin = TIMESTAMP '2099-01-01 23:00:00' WHERE id_evento = " + EVENTO);
        IllegalArgumentException futuro = assertThrows(IllegalArgumentException.class,
                () -> service.finalizarEvento(EVENTO, sesionOrganizador(), fotos));
        assertTrue(futuro.getMessage().contains("Podrás finalizar el evento después del"));
        assertEquals("0", valor("SELECT COUNT(*) FROM Finalizacion_evento WHERE id_evento = " + EVENTO));
    }

    @Test
    void finalizaEventoGuardaLaFotoYEntregaLaGaleria() throws Exception {
        prepararReservasContratacionesEvento();
        // usamos una fecha pasada para que el evento ya haya terminado
        ejecutar("UPDATE Evento SET estado = 'CONFIRMADO', "
                + "fecha_inicio = TIMESTAMP '2020-01-01 15:00:00', "
                + "fecha_fin = TIMESTAMP '2020-01-01 23:00:00' WHERE id_evento = " + EVENTO);

        service.finalizarEvento(EVENTO, sesionOrganizador(), List.of(fotografiaValida()));

        assertEquals("FINALIZADO", valor("SELECT estado FROM Evento WHERE id_evento = " + EVENTO));
        assertEquals("TRUE", valor("SELECT enviada FROM Finalizacion_evento WHERE id_evento = " + EVENTO));
        assertEquals("1", valor("SELECT COUNT(*) FROM Fotografia_evento "
                + "WHERE id_evento = 1 AND nombre = 'recuerdo.png'"));
        assertEquals("2", valor("SELECT COUNT(*) FROM Entrega_galeria WHERE id_evento = " + EVENTO));
        assertEquals("2", valor("SELECT COUNT(*) FROM Contratacion "
                + "WHERE id_evento = 1 AND estado = 'FINALIZADA'"));
        assertFalse(service.galeriaPendiente(EVENTO, sesionOrganizador()));
        assertTrue(service.proveedoresParaCalificar(EVENTO, sesionOrganizador())
                .containsAll(List.of("Mariana López", "Andrés Torres")));
    }

    @Test
    void informaCuandoLaGaleriaEstaPendienteDeEnvio() throws Exception {
        ejecutar("INSERT INTO Finalizacion_evento (id_evento, enviada) VALUES (1, FALSE)");

        assertTrue(service.galeriaPendiente(EVENTO, sesionOrganizador()));
    }

    @Test
    void rechazaFotografiasInvalidasConElMotivoEspecifico() throws Exception {
        IllegalArgumentException sinFotos = assertThrows(IllegalArgumentException.class,
                () -> service.finalizarEvento(EVENTO, sesionOrganizador(), null));
        assertEquals("Selecciona hasta 10 fotografías PNG o JPG, de hasta 5 MB cada una.",
                sinFotos.getMessage());

        // repetimos una fotografía de prueba para superar el máximo de diez
        List<Fotografia> demasiadasFotos = new ArrayList<>();
        for (int i = 0; i < 11; i++) {
            demasiadasFotos.add(new Fotografia("foto.png", new byte[]{1}));
        }
        IllegalArgumentException demasiadas = assertThrows(IllegalArgumentException.class,
                () -> service.finalizarEvento(EVENTO, sesionOrganizador(), demasiadasFotos));
        assertEquals("Selecciona hasta 10 fotografías PNG o JPG, de hasta 5 MB cada una.",
                demasiadas.getMessage());

        IllegalArgumentException formato = assertThrows(IllegalArgumentException.class,
                () -> service.finalizarEvento(EVENTO, sesionOrganizador(),
                        List.of(new Fotografia("foto.png", new byte[]{1, 2, 3}))));
        assertEquals("Selecciona fotografías PNG o JPG válidas.", formato.getMessage());
    }

    @Test
    void rechazaFotografiasNulasSinFinalizarNiModificarElEvento() throws Exception {
        String estadoAntes = valor("SELECT estado FROM Evento WHERE id_evento = " + EVENTO);
        String finalizacionesAntes = valor(
                "SELECT COUNT(*) FROM Finalizacion_evento WHERE id_evento = " + EVENTO);
        String fotografiasAntes = valor(
                "SELECT COUNT(*) FROM Fotografia_evento WHERE id_evento = " + EVENTO);
        List<Fotografia> listaConNula = new ArrayList<>();
        listaConNula.add(null);

        IllegalArgumentException elementoNulo = assertThrows(
                IllegalArgumentException.class,
                () -> service.finalizarEvento(EVENTO, sesionOrganizador(), listaConNula));
        assertEquals("Cada fotografía debe tener un nombre de hasta 150 caracteres y pesar hasta 5 MB.",
                elementoNulo.getMessage());

        IllegalArgumentException contenidoNulo = assertThrows(
                IllegalArgumentException.class,
                () -> service.finalizarEvento(EVENTO, sesionOrganizador(),
                        List.of(new Fotografia("recuerdo.png", null))));
        assertEquals("Cada fotografía debe tener un nombre de hasta 150 caracteres y pesar hasta 5 MB.",
                contenidoNulo.getMessage());

        IllegalArgumentException nombreExcesivo = assertThrows(
                IllegalArgumentException.class,
                () -> service.finalizarEvento(EVENTO, sesionOrganizador(),
                        List.of(new Fotografia("n".repeat(151), new byte[]{1}))));
        assertEquals("Cada fotografía debe tener un nombre de hasta 150 caracteres y pesar hasta 5 MB.",
                nombreExcesivo.getMessage());

        assertEquals(estadoAntes,
                valor("SELECT estado FROM Evento WHERE id_evento = " + EVENTO));
        assertEquals(finalizacionesAntes,
                valor("SELECT COUNT(*) FROM Finalizacion_evento WHERE id_evento = " + EVENTO));
        assertEquals(fotografiasAntes,
                valor("SELECT COUNT(*) FROM Fotografia_evento WHERE id_evento = " + EVENTO));
    }

    private Evento eventoNuevo(int organizador, String nombre) {
        LocalDateTime inicio = LocalDateTime.now().plusDays(30).withNano(0);
        return new Evento(organizador, nombre, "Boda", "Evento creado para integración",
                inicio, inicio.plusHours(8), "Salón de prueba",
                new BigDecimal("1000000.00"), 10);
    }

    private Evento eventoConFechasInvertidas() {
        LocalDateTime inicio = LocalDateTime.now().plusDays(30).withSecond(0).withNano(0);
        return new Evento(ORGANIZADOR, "Evento", "Boda", null,
                inicio, inicio.minusHours(1), "Salón",
                BigDecimal.ZERO, 0);
    }

    private SesionUsuario sesionOrganizador() {
        return sesion(1, ORGANIZADOR);
    }

    private SesionUsuario sesion(int usuario, int organizador) {
        SesionUsuario sesion = new SesionUsuario();
        sesion.setIdUsuario(usuario);
        sesion.setNombre("Laura Gómez");
        sesion.setTipoUsuario("ORGANIZADOR");
        sesion.setEstadoUsuario("ACTIVO");
        sesion.setIdActor(organizador);
        sesion.setEstadoActor("ACTIVO");
        return sesion;
    }

    private Fotografia fotografiaValida() throws Exception {
        // generamos un png pequeño y real porque el servicio revisa el contenido de la foto
        BufferedImage imagen = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        ImageIO.write(imagen, "png", salida);
        return new Fotografia("recuerdo.png", salida.toByteArray());
    }

    private void prepararReservasContratacionesEvento() throws Exception {
        ejecutar("INSERT INTO Disponibilidad_Servicio "
                + "(id_disponibilidad, id_servicio, fecha, hora_inicio, hora_fin, estado) VALUES "
                + "(1001, 1, CURRENT_DATE, TIME '15:00:00', TIME '18:00:00', 'RESERVADO'), "
                + "(1002, 3, CURRENT_DATE, TIME '18:00:00', TIME '23:00:00', 'RESERVADO')");
        ejecutar("UPDATE Contratacion SET id_disponibilidad = 1001, "
                + "fecha_servicio = CURRENT_DATE, hora_inicio = TIME '15:00:00', "
                + "hora_fin = TIME '18:00:00' WHERE id_contratacion = 1");
        ejecutar("UPDATE Contratacion SET id_disponibilidad = 1002, "
                + "fecha_servicio = CURRENT_DATE, hora_inicio = TIME '18:00:00', "
                + "hora_fin = TIME '23:00:00' WHERE id_contratacion = 2");
    }

    private void ejecutar(String sql) throws Exception {
        // usamos sql directo para preparar estados y relaciones que el servicio no crea
        try (Connection connection = DatabaseConnection.getConnection(url);
                Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        }
    }

    private String valor(String sql) throws Exception {
        // leemos h2 para confirmar los cambios que dejó el servicio
        try (Connection connection = DatabaseConnection.getConnection(url);
                Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery(sql)) {
            assertTrue(result.next());
            return result.getString(1);
        }
    }

    private void assertMonto(String esperado, BigDecimal actual) {
        assertEquals(0, new BigDecimal(esperado).compareTo(actual));
    }

    private boolean contieneEvento(List<Evento> eventos, int idEvento) {
        for (Evento evento : eventos) {
            if (evento.getIdEvento() == idEvento) {
                return true;
            }
        }
        return false;
    }
}
