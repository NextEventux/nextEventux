import config.DatabaseConnection;
import controllers.ControllerAdministracion;
import entities.Contratacion;
import entities.SesionUsuario;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import repositories.RepositoryAutenticacion;
import repositories.RepositoryContratacion;
import repositories.RepositoryOrganizador;
import services.ServiceAdministracion;
import services.ServiceAutenticacion;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Ampliación de CU-026: el organizador consulta las solicitudes que recibe
 * y las acepta o rechaza; el cliente recibe una notificación interna y ve
 * el resultado en «Mis solicitudes». Base H2 aislada por prueba.
 */
public class CU026SolicitudesOrganizadorTest {

    private static final int CLIENTE = 9001;
    private static final int USUARIO_ORGANIZADOR = 9002;
    private static final int ORGANIZADOR = 9001;
    private static final int OTRO_CLIENTE = 9003;
    private static final int USUARIO_OTRO_ORGANIZADOR = 9004;
    private static final int OTRO_ORGANIZADOR = 9002;

    private String url;
    private ServiceAdministracion servicio;
    private ControllerAdministracion cliente;
    private ControllerAdministracion organizador;
    private final LocalDate fecha = LocalDate.now().plusDays(45);

    @BeforeEach
    void preparar() throws Exception {
        url = "jdbc:h2:mem:cu26_org_" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1";

        try (Connection connection = DatabaseConnection.getConnection(url);
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    INSERT INTO Usuario
                    (id_usuario, nombre, usuario, contraseña, estado,
                     fecha_registro, acepta_marketing, tipo_usuario)
                    VALUES
                    (9001, 'Cliente CU26', 'cliente_cu26', 'clave', 'ACTIVO', CURRENT_TIMESTAMP, FALSE, 'CLIENTE'),
                    (9002, 'Organizador CU26', 'organizador_cu26', 'clave', 'ACTIVO', CURRENT_TIMESTAMP, FALSE, 'ORGANIZADOR'),
                    (9003, 'Otro cliente CU26', 'otro_cliente_cu26', 'clave', 'ACTIVO', CURRENT_TIMESTAMP, FALSE, 'CLIENTE'),
                    (9004, 'Otro organizador CU26', 'organizador2_cu26', 'clave', 'ACTIVO', CURRENT_TIMESTAMP, FALSE, 'ORGANIZADOR')
                    """);
            statement.executeUpdate("""
                    INSERT INTO Organizador
                    (id_organizador, id_usuario, empresa, telefono, estado,
                     ciudad, especialidad, portafolio, tarifa_base, estado_verificacion)
                    VALUES
                    (9001, 9002, 'Eventos CU26', '3000000000', 'ACTIVO',
                     'Bogota', 'Bodas', 'Bodas', 1500000, 'VERIFICADO'),
                    (9002, 9004, 'Otros CU26', '3000000001', 'ACTIVO',
                     'Bogota', 'Bodas', 'Bodas', 1000000, 'VERIFICADO')
                    """);
        }

        servicio = new ServiceAdministracion(
                new RepositoryContratacion(url), new RepositoryOrganizador(url));

        // Sesiones reales obtenidas con el inicio de sesión del sistema.
        ServiceAutenticacion auth = new ServiceAutenticacion(new RepositoryAutenticacion(url));
        cliente = new ControllerAdministracion(servicio, auth.iniciarSesion("cliente_cu26", "clave"));
        organizador = new ControllerAdministracion(servicio, auth.iniciarSesion("organizador_cu26", "clave"));
    }

    @AfterEach
    void limpiar() throws Exception {
        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement()) {
            statement.execute("DROP ALL OBJECTS");
        }
    }

    // ------------------------------------------------------------------
    // Consulta
    // ------------------------------------------------------------------

    @Test
    void o01_elOrganizadorVeSusSolicitudesConLosDatosDelCliente() {
        Contratacion enviada = enviar(cliente, ORGANIZADOR, fecha, "Jardin al aire libre");

        List<Contratacion> recibidas = organizador.consultarSolicitudesRecibidas();

        assertEquals(1, recibidas.size());
        Contratacion s = recibidas.get(0);
        assertEquals(enviada.getIdContratacion(), s.getIdContratacion());
        assertEquals("Cliente CU26", s.getNombreCliente());
        assertEquals("Boda", s.getTipoEvento());
        assertEquals("Bogota", s.getCiudad());
        assertEquals(fecha, s.getFechaAproximada());
        assertEquals(0, new BigDecimal("2000000").compareTo(s.getPresupuestoEstimado()));
        assertEquals("Jardin al aire libre", s.getCriteriosBusqueda());
        assertEquals("PENDIENTE", s.getEstado());
    }

    @Test
    void o02_soloVeLasSolicitudesDirigidasASuCuenta() {
        enviar(cliente, ORGANIZADOR, fecha, null);
        enviar(cliente, OTRO_ORGANIZADOR, fecha, null);

        assertEquals(1, organizador.consultarSolicitudesRecibidas().size());
        assertEquals(ORGANIZADOR,
                organizador.consultarSolicitudesRecibidas().get(0).getIdOrganizador());
        assertEquals(1, otroOrganizador().consultarSolicitudesRecibidas().size());
    }

    @Test
    void o03_lasPendientesAparecenPrimero() {
        Contratacion primera = enviar(cliente, ORGANIZADOR, fecha, null);
        Contratacion segunda = enviar(cliente, ORGANIZADOR, fecha.plusDays(1), null);
        organizador.rechazarSolicitud(segunda.getIdContratacion());

        List<Contratacion> recibidas = organizador.consultarSolicitudesRecibidas();

        assertEquals(List.of(primera.getIdContratacion(), segunda.getIdContratacion()),
                recibidas.stream().map(Contratacion::getIdContratacion).toList());
        assertEquals("PENDIENTE", recibidas.get(0).getEstado());
        assertEquals("RECHAZADA", recibidas.get(1).getEstado());
    }

    // ------------------------------------------------------------------
    // Aceptar y rechazar
    // ------------------------------------------------------------------

    @Test
    void o04_aceptarActualizaElEstadoYNotificaAlCliente() throws Exception {
        Contratacion enviada = enviar(cliente, ORGANIZADOR, fecha, null);
        int avisosAntes = contar("Notificacion_interna");

        Contratacion respondida = organizador.aceptarSolicitud(enviada.getIdContratacion());

        assertEquals("ACEPTADA", respondida.getEstado());
        assertEquals(1, contar("Solicitud_Contratacion_Organizador WHERE estado = 'ACEPTADA'"));
        assertEquals(avisosAntes + 1, contar("Notificacion_interna"));
        assertEquals(1, contar("Notificacion_interna WHERE id_usuario = " + CLIENTE
                + " AND id_usuario_destinatario = " + CLIENTE
                + " AND mensaje LIKE 'Organizador CU26 aceptó tu solicitud de contratación #"
                + enviada.getIdContratacion() + "%Estado: Aceptada%'"));
    }

    @Test
    void o05_rechazarActualizaElEstadoYNotificaAlCliente() throws Exception {
        Contratacion enviada = enviar(cliente, ORGANIZADOR, fecha, null);

        Contratacion respondida = organizador.rechazarSolicitud(enviada.getIdContratacion());

        assertEquals("RECHAZADA", respondida.getEstado());
        assertEquals(1, contar("Notificacion_interna WHERE id_usuario = " + CLIENTE
                + " AND mensaje LIKE '%rechazó tu solicitud%Estado: Rechazada%'"));
    }

    @Test
    void o06_elClienteVeElResultadoEnMisSolicitudes() {
        Contratacion a = enviar(cliente, ORGANIZADOR, fecha, null);
        Contratacion b = enviar(cliente, ORGANIZADOR, fecha.plusDays(2), null);

        organizador.aceptarSolicitud(a.getIdContratacion());
        organizador.rechazarSolicitud(b.getIdContratacion());

        List<String> estados = cliente.consultarContrataciones(null).stream()
                .sorted((x, y) -> Integer.compare(x.getIdContratacion(), y.getIdContratacion()))
                .map(Contratacion::getEstado)
                .toList();

        assertEquals(List.of("ACEPTADA", "RECHAZADA"), estados);
    }

    @Test
    void o07_unaSolicitudRespondidaNoSeProcesaDeNuevo() throws Exception {
        Contratacion enviada = enviar(cliente, ORGANIZADOR, fecha, null);
        organizador.aceptarSolicitud(enviada.getIdContratacion());
        int avisos = contar("Notificacion_interna");

        IllegalStateException rechazo = assertThrows(IllegalStateException.class,
                () -> organizador.rechazarSolicitud(enviada.getIdContratacion()));
        IllegalStateException otraVez = assertThrows(IllegalStateException.class,
                () -> organizador.aceptarSolicitud(enviada.getIdContratacion()));

        assertEquals(RepositoryContratacion.SOLICITUD_YA_RESPONDIDA, rechazo.getMessage());
        assertEquals(RepositoryContratacion.SOLICITUD_YA_RESPONDIDA, otraVez.getMessage());
        assertEquals(1, contar("Solicitud_Contratacion_Organizador WHERE estado = 'ACEPTADA'"));
        assertEquals(avisos, contar("Notificacion_interna"));
    }

    @Test
    void o08_otroOrganizadorNoPuedeResponderNiVer() throws Exception {
        Contratacion enviada = enviar(cliente, ORGANIZADOR, fecha, null);
        int avisos = contar("Notificacion_interna");

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> otroOrganizador().aceptarSolicitud(enviada.getIdContratacion()));

        assertEquals(RepositoryContratacion.SOLICITUD_NO_ENCONTRADA, error.getMessage());
        assertTrue(otroOrganizador().consultarSolicitudesRecibidas().isEmpty());
        assertEquals(1, contar("Solicitud_Contratacion_Organizador WHERE estado = 'PENDIENTE'"));
        assertEquals(avisos, contar("Notificacion_interna"));
    }

    @Test
    void o09_solicitudInexistente() {
        assertThrows(IllegalArgumentException.class,
                () -> organizador.aceptarSolicitud(999999));
        assertThrows(IllegalArgumentException.class,
                () -> organizador.rechazarSolicitud(0));
    }

    // ------------------------------------------------------------------
    // Sesión, rol y suspensión (CU-029)
    // ------------------------------------------------------------------

    @Test
    void o10_soloUnaSesionDeOrganizadorPuedeConsultarYResponder() {
        Contratacion enviada = enviar(cliente, ORGANIZADOR, fecha, null);

        List<ControllerAdministracion> ajenos = List.of(
                cliente,
                new ControllerAdministracion(servicio),
                new ControllerAdministracion(servicio, sesion(9100, "ADMINISTRADOR", "ACTIVO")),
                new ControllerAdministracion(servicio, sesion(9101, "PROVEEDOR", "ACTIVO")),
                new ControllerAdministracion(servicio, sesion(USUARIO_ORGANIZADOR, "ORGANIZADOR", "SUSPENDIDO")));

        for (ControllerAdministracion ajeno : ajenos) {
            assertThrows(IllegalStateException.class, ajeno::consultarSolicitudesRecibidas);
            assertThrows(IllegalStateException.class,
                    () -> ajeno.aceptarSolicitud(enviada.getIdContratacion()));
            assertThrows(IllegalStateException.class,
                    () -> ajeno.rechazarSolicitud(enviada.getIdContratacion()));
        }

        assertEquals(1, contar("Solicitud_Contratacion_Organizador WHERE estado = 'PENDIENTE'"));
    }

    @Test
    void o11_organizadorSuspendidoConSesionAbiertaNoResponde() throws Exception {
        Contratacion enviada = enviar(cliente, ORGANIZADOR, fecha, null);
        int avisos = contar("Notificacion_interna");
        sql("UPDATE Usuario SET estado = 'SUSPENDIDO' WHERE id_usuario = " + USUARIO_ORGANIZADOR);

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> organizador.aceptarSolicitud(enviada.getIdContratacion()));

        assertEquals(RepositoryContratacion.ORGANIZADOR_NO_ACTIVO, error.getMessage());
        assertEquals(1, contar("Solicitud_Contratacion_Organizador WHERE estado = 'PENDIENTE'"));
        assertEquals(avisos, contar("Notificacion_interna"));
    }

    @Test
    void o12_perfilDeOrganizadorInactivoNoResponde() throws Exception {
        Contratacion enviada = enviar(cliente, ORGANIZADOR, fecha, null);
        sql("UPDATE Organizador SET estado = 'INACTIVO' WHERE id_organizador = " + ORGANIZADOR);

        assertThrows(IllegalStateException.class,
                () -> organizador.rechazarSolicitud(enviada.getIdContratacion()));
        assertEquals(1, contar("Solicitud_Contratacion_Organizador WHERE estado = 'PENDIENTE'"));
    }

    // ------------------------------------------------------------------
    // Transacción
    // ------------------------------------------------------------------

    @Test
    void o13_siFallaLaNotificacionLaSolicitudSigueEnPendiente() throws Exception {
        Contratacion enviada = enviar(cliente, ORGANIZADOR, fecha, null);
        int avisos = contar("Notificacion_interna");
        sql("ALTER TABLE Notificacion_interna ADD CONSTRAINT fallo_respuesta_cu26 "
                + "CHECK (id_usuario <> " + CLIENTE + ")");

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> organizador.aceptarSolicitud(enviada.getIdContratacion()));

        assertEquals(RepositoryContratacion.ERROR_RESPUESTA, error.getMessage());
        assertEquals(1, contar("Solicitud_Contratacion_Organizador WHERE estado = 'PENDIENTE'"));
        assertEquals(avisos, contar("Notificacion_interna"));

        // El organizador puede reintentar.
        sql("ALTER TABLE Notificacion_interna DROP CONSTRAINT fallo_respuesta_cu26");
        assertEquals("ACEPTADA",
                organizador.aceptarSolicitud(enviada.getIdContratacion()).getEstado());
    }

    @Test
    void o14_responderNoAfectaOtrasSolicitudesNiOtrosClientes() throws Exception {
        Contratacion propia = enviar(cliente, ORGANIZADOR, fecha, null);
        Contratacion ajena = enviar(otroCliente(), ORGANIZADOR, fecha, null);

        organizador.aceptarSolicitud(propia.getIdContratacion());

        assertEquals("PENDIENTE", buscar(ajena.getIdContratacion()).getEstado());
        assertEquals(0, contar("Notificacion_interna WHERE id_usuario = " + OTRO_CLIENTE));
        assertEquals(1, otroCliente().consultarContrataciones(null).size());
        assertEquals("PENDIENTE", otroCliente().consultarContrataciones(null).get(0).getEstado());
    }

    @Test
    void o15_rechazadaPermiteQueElClienteVuelvaASolicitar() {
        Contratacion enviada = enviar(cliente, ORGANIZADOR, fecha, null);
        organizador.rechazarSolicitud(enviada.getIdContratacion());

        assertNotNull(enviar(cliente, ORGANIZADOR, fecha, null));
        assertEquals(2, cliente.consultarContrataciones(null).size());
    }

    @Test
    void o16_aceptadaTampocoBloqueaUnaSolicitudPosteriorNiCreaEventos() throws Exception {
        int eventos = contar("Evento");
        Contratacion enviada = enviar(cliente, ORGANIZADOR, fecha, null);
        organizador.aceptarSolicitud(enviada.getIdContratacion());

        // El duplicado solo considera solicitudes PENDIENTES (decisión aprobada).
        assertNotNull(enviar(cliente, ORGANIZADOR, fecha, null));
        // La aceptación no crea eventos: eso pertenece al módulo de eventos.
        assertEquals(eventos, contar("Evento"));
    }

    // ------------------------------------------------------------------
    // Concurrencia
    // ------------------------------------------------------------------

    @Test
    void o17_aceptarYRechazarALaVezSoloRegistraUnaDecision() throws Exception {
        for (int i = 0; i < 10; i++) {
            Contratacion enviada = enviar(cliente, ORGANIZADOR, fecha.plusDays(i), null);
            CountDownLatch salida = new CountDownLatch(1);
            List<CompletableFuture<String>> intentos = new ArrayList<>();

            for (boolean aceptar : new boolean[]{true, false}) {
                intentos.add(CompletableFuture.supplyAsync(() -> {
                    try {
                        salida.await();
                        return (aceptar
                                ? organizador.aceptarSolicitud(enviada.getIdContratacion())
                                : organizador.rechazarSolicitud(enviada.getIdContratacion()))
                                .getEstado();
                    } catch (IllegalStateException e) {
                        return e.getMessage();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return "INTERRUMPIDO";
                    }
                }));
            }
            salida.countDown();

            List<String> resultados = new ArrayList<>();
            for (CompletableFuture<String> intento : intentos) {
                resultados.add(intento.get(15, TimeUnit.SECONDS));
            }

            assertEquals(1, resultados.stream()
                    .filter(r -> r.equals("ACEPTADA") || r.equals("RECHAZADA")).count(),
                    resultados.toString());
            assertTrue(resultados.contains(RepositoryContratacion.SOLICITUD_YA_RESPONDIDA),
                    resultados.toString());
            assertEquals(1, contar("Notificacion_interna WHERE id_usuario = " + CLIENTE
                    + " AND mensaje LIKE '%solicitud de contratación #"
                    + enviada.getIdContratacion() + ".%'"));
        }
    }

    @Test
    void o18_suspensionEnCursoDelOrganizadorBloqueaLaRespuesta() throws Exception {
        Contratacion enviada = enviar(cliente, ORGANIZADOR, fecha, null);

        try (Connection suspension = DatabaseConnection.getConnection(url)) {
            suspension.setAutoCommit(false);
            try (Statement st = suspension.createStatement()) {
                st.execute("UPDATE Usuario SET estado = 'SUSPENDIDO' WHERE id_usuario = "
                        + USUARIO_ORGANIZADOR);
            }

            CompletableFuture<Contratacion> respuesta = CompletableFuture.supplyAsync(
                    () -> organizador.aceptarSolicitud(enviada.getIdContratacion()));

            assertThrows(TimeoutException.class,
                    () -> respuesta.get(700, TimeUnit.MILLISECONDS));

            suspension.commit();

            ExecutionException error = assertThrows(ExecutionException.class,
                    () -> respuesta.get(15, TimeUnit.SECONDS));
            assertEquals(RepositoryContratacion.ORGANIZADOR_NO_ACTIVO,
                    error.getCause().getMessage());
        }

        assertEquals("PENDIENTE", buscar(enviada.getIdContratacion()).getEstado());
    }

    // ------------------------------------------------------------------

    private Contratacion enviar(ControllerAdministracion quien, int idOrganizador,
                                LocalDate dia, String criterios) {
        Contratacion datos = new Contratacion(0, idOrganizador, "Boda", "Bogota",
                dia, new BigDecimal("2000000"), criterios);
        quien.contratarOrganizador(idOrganizador, datos);
        return datos;
    }

    private ControllerAdministracion otroOrganizador() {
        return new ControllerAdministracion(servicio,
                new ServiceAutenticacion(new RepositoryAutenticacion(url))
                        .iniciarSesion("organizador2_cu26", "clave"));
    }

    private ControllerAdministracion otroCliente() {
        return new ControllerAdministracion(servicio,
                new ServiceAutenticacion(new RepositoryAutenticacion(url))
                        .iniciarSesion("otro_cliente_cu26", "clave"));
    }

    private Contratacion buscar(int idSolicitud) {
        return new RepositoryContratacion(url).buscarContratacion(idSolicitud);
    }

    private SesionUsuario sesion(int idUsuario, String tipo, String estado) {
        SesionUsuario s = new SesionUsuario();
        s.setIdUsuario(idUsuario);
        s.setTipoUsuario(tipo);
        s.setEstadoUsuario(estado);
        return s;
    }

    private void sql(String sentencia) throws Exception {
        try (Connection connection = DatabaseConnection.getConnection(url);
             Statement statement = connection.createStatement()) {
            statement.execute(sentencia);
        }
    }

    private int contar(String tablaYCondicion) {
        try (Connection connection = DatabaseConnection.getConnection(url);
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM " + tablaYCondicion)) {
            result.next();
            return result.getInt(1);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
