package services;

import entities.Contratacion;
import entities.Organizador;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import persistencia.DatabaseConnection;
import persistencia.DatabaseTestUtils;
import repositories.RepositoryContratacion;
import repositories.RepositoryOrganizador;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServiceAdministracionH2Test {

    private static final int CLIENTE = 9001;
    private static final int ORGANIZADOR = 9201;

    private String url;
    private ServiceAdministracion service;

    @BeforeEach
    void preparar() throws Exception {
        // la base vive entre conexiones del servicio y tiene un nombre único en memoria
        url = "jdbc:h2:mem:administracion_" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1";

        try (Connection connection = DatabaseConnection.getConnection(url)) {
            DatabaseTestUtils.crearEsquemaVacio(connection);
        }

        // creamos los usuarios y organizadores que necesita este escenario
        insertarUsuario(CLIENTE, "Cliente prueba", "CLIENTE", "ACTIVO");
        insertarUsuario(9002, "Organizadora Bogotá", "ORGANIZADOR", "ACTIVO");
        insertarUsuario(9003, "Organizador costoso", "ORGANIZADOR", "ACTIVO");
        insertarUsuario(9004, "Organizador pendiente", "ORGANIZADOR", "ACTIVO");

        ejecutar("INSERT INTO Organizador "
                + "(id_organizador, id_usuario, empresa, telefono, estado, ciudad, "
                + "especialidad, portafolio, tarifa_base, estado_verificacion) "
                + "VALUES (9201, 9002, 'Eventos Bogotá', '3000000001', 'ACTIVO', "
                + "'Bogota', 'Bodas y eventos sociales', 'Portafolio', 1000000, 'VERIFICADO')");
        ejecutar("INSERT INTO Organizador "
                + "(id_organizador, id_usuario, empresa, telefono, estado, ciudad, "
                + "especialidad, tarifa_base, estado_verificacion) "
                + "VALUES (9202, 9003, 'Eventos premium', '3000000002', 'ACTIVO', "
                + "'Bogota', 'Bodas', 2000000, 'VERIFICADO')");
        ejecutar("INSERT INTO Organizador "
                + "(id_organizador, id_usuario, empresa, telefono, estado, ciudad, "
                + "especialidad, tarifa_base, estado_verificacion) "
                + "VALUES (9203, 9004, 'Eventos pendientes', '3000000003', 'ACTIVO', "
                + "'Bogota', 'Bodas', 500000, 'PENDIENTE')");

        service = new ServiceAdministracion(
                new RepositoryContratacion(url),
                new RepositoryOrganizador(url));
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
    void consultaOrganizadoresAplicandoCiudadTipoPresupuestoYDisponibilidad() {
        Map<String, Object> filtros = Map.of(
                "ciudad", " Bogota ",
                "tipoEvento", "bodas",
                "presupuestoEstimado", new BigDecimal("1500000"),
                "fechaAproximada", LocalDate.now().plusDays(30));

        List<Organizador> encontrados = service.consultarOrganizadores(filtros);

        assertEquals(1, encontrados.size());
        assertEquals(ORGANIZADOR, encontrados.get(0).id());
        assertEquals("Bogota", encontrados.get(0).ciudad());
        assertEquals(new BigDecimal("1000000.00"), encontrados.get(0).tarifaBase());
    }

    @Test
    void contratarGuardaSolicitudPendienteYNotificaAlOrganizador() throws Exception {
        Contratacion solicitud = new Contratacion(
                CLIENTE,
                9999,
                " Bodas ",
                " Bogota ",
                LocalDate.of(2030, 5, 12),
                new BigDecimal("2500000"),
                "Evento familiar");

        service.contratarOrganizador(ORGANIZADOR, solicitud);

        assertTrue(solicitud.getIdContratacion() > 0);
        assertEquals(ORGANIZADOR, solicitud.getIdOrganizador());
        assertEquals("PENDIENTE", solicitud.getEstado());
        assertNotNull(solicitud.getFechaSolicitud());

        List<Contratacion> guardadas = service.consultarContrataciones(
                Map.of("idCliente", CLIENTE, "estado", "PENDIENTE"));
        assertEquals(1, guardadas.size());
        assertEquals(solicitud.getIdContratacion(), guardadas.get(0).getIdContratacion());
        assertEquals("Bodas", guardadas.get(0).getTipoEvento());
        assertEquals("Bogota", guardadas.get(0).getCiudad());
        assertEquals(1, consultarEntero(
                "SELECT COUNT(*) FROM Notificacion_interna WHERE id_usuario = 9002"));
    }

    @Test
    void rechazaFiltrosInvalidosYOrganizadoresNoDisponibles() {
        assertThrows(IllegalArgumentException.class,
                () -> service.consultarOrganizadores(null));
        assertThrows(IllegalArgumentException.class,
                () -> service.consultarOrganizadores(Map.of(
                        "ciudad", "Bogota",
                        "tipoEvento", "Bodas",
                        "presupuestoEstimado", -1,
                        "fechaAproximada", LocalDate.now().plusDays(30))));
        assertThrows(IllegalArgumentException.class,
                () -> service.seleccionarOrganizador(0));
        assertThrows(IllegalStateException.class,
                () -> service.seleccionarOrganizador(9203));
    }

    @Test
    void seleccionaOrganizadorDisponibleYValidaIdentificadores() {
        Organizador seleccionado = service.seleccionarOrganizador(ORGANIZADOR);

        assertEquals(ORGANIZADOR, seleccionado.id());
        assertEquals("Eventos Bogotá", seleccionado.empresa());
        assertFalse(service.verificarDisponibilidadOrganizador(
                9999, LocalDate.now().plusDays(30)));
        assertThrows(IllegalArgumentException.class,
                () -> service.verificarDisponibilidadOrganizador(
                        0, LocalDate.now().plusDays(30)));
        assertThrows(IllegalArgumentException.class,
                () -> new ServiceAdministracion(null,
                        new RepositoryOrganizador(url)));
        assertThrows(IllegalArgumentException.class,
                () -> new ServiceAdministracion(
                        new RepositoryContratacion(url), null));
    }

    @Test
    void detectaEliminacionDelPerfilDespuesDeVerificarlo() throws Exception {
        ServiceAdministracion coordinado = new ServiceAdministracion(
                new RepositoryContratacion(url),
                new RepositoryOrganizador(url),
                this::eliminarOrganizador);

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> coordinado.seleccionarOrganizador(ORGANIZADOR));

        assertEquals("No se encontro el perfil del organizador seleccionado.", error.getMessage());
        assertEquals(0, consultarEntero(
                "SELECT COUNT(*) FROM Organizador WHERE id_organizador = " + ORGANIZADOR));
        assertEquals(1, consultarEntero(
                "SELECT COUNT(*) FROM Usuario WHERE id_usuario = 9002"));
    }

    @Test
    void rechazaCadaCriterioDeBusquedaObligatorioCuandoFaltaOVacio() {
        IllegalArgumentException ciudadFaltante = assertThrows(
                IllegalArgumentException.class,
                () -> service.consultarOrganizadores(Map.of(
                        "tipoEvento", "Bodas",
                        "presupuestoEstimado", BigDecimal.ZERO,
                        "fechaAproximada", LocalDate.now().plusDays(30))));
        assertTrue(ciudadFaltante.getMessage().contains("Completa el tipo de evento"));

        assertThrows(IllegalArgumentException.class,
                () -> service.consultarOrganizadores(Map.of(
                        "ciudad", "Bogota",
                        "presupuestoEstimado", BigDecimal.ZERO,
                        "fechaAproximada", LocalDate.now().plusDays(30))));
        assertThrows(IllegalArgumentException.class,
                () -> service.consultarOrganizadores(Map.of(
                        "ciudad", "Bogota",
                        "tipoEvento", "Bodas",
                        "fechaAproximada", LocalDate.now().plusDays(30))));
        assertThrows(IllegalArgumentException.class,
                () -> service.consultarOrganizadores(Map.of(
                        "ciudad", "Bogota",
                        "tipoEvento", "Bodas",
                        "presupuestoEstimado", BigDecimal.ZERO)));
        assertThrows(IllegalArgumentException.class,
                () -> service.consultarOrganizadores(Map.of(
                        "ciudad", " ",
                        "tipoEvento", "Bodas",
                        "presupuestoEstimado", BigDecimal.ZERO,
                        "fechaAproximada", LocalDate.now().plusDays(30))));
        assertThrows(IllegalArgumentException.class,
                () -> service.consultarOrganizadores(Map.of(
                        "ciudad", "Bogota",
                        "tipoEvento", " ",
                        "presupuestoEstimado", BigDecimal.ZERO,
                        "fechaAproximada", LocalDate.now().plusDays(30))));
        assertThrows(IllegalArgumentException.class,
                () -> service.contratarOrganizador(0,
                        nuevaContratacion(CLIENTE, "Bodas", "Bogota",
                                LocalDate.now().plusDays(30),
                                BigDecimal.ZERO, null)));
    }

    @Test
    void rechazaFiltrosIncompletosTiposIncorrectosYTextoExcesivo() throws Exception {
        assertThrows(IllegalArgumentException.class,
                () -> service.consultarOrganizadores(Map.of("ciudad", "Bogota")));
        assertThrows(IllegalArgumentException.class,
                () -> service.consultarOrganizadores(Map.of(
                        "ciudad", "Bogota",
                        "tipoEvento", "Bodas",
                        "presupuestoEstimado", "1500000",
                        "fechaAproximada", LocalDate.now().plusDays(30))));
        assertThrows(IllegalArgumentException.class,
                () -> service.consultarOrganizadores(Map.of(
                        "ciudad", "Bogota",
                        "tipoEvento", "Bodas",
                        "presupuestoEstimado", new BigDecimal("1500000"),
                        "fechaAproximada", "2030-05-12")));
        assertThrows(IllegalArgumentException.class,
                () -> service.consultarOrganizadores(Map.of(
                        "ciudad", "c".repeat(151),
                        "tipoEvento", "Bodas",
                        "presupuestoEstimado", new BigDecimal("1500000"),
                        "fechaAproximada", LocalDate.now().plusDays(30))));
        assertThrows(IllegalArgumentException.class,
                () -> service.consultarOrganizadores(Map.of(
                        "ciudad", "Bogota",
                        "tipoEvento", "t".repeat(101),
                        "presupuestoEstimado", new BigDecimal("1500000"),
                        "fechaAproximada", LocalDate.now().plusDays(30))));

        assertEquals(0, consultarEntero(
                "SELECT COUNT(*) FROM Solicitud_Contratacion_Organizador"));
        assertEquals(0, consultarEntero("SELECT COUNT(*) FROM Notificacion_interna"));
    }

    @Test
    void rechazaContratacionesInvalidasSinCrearSolicitudesNiNotificaciones()
            throws Exception {
        int solicitudesAntes = consultarEntero(
                "SELECT COUNT(*) FROM Solicitud_Contratacion_Organizador");
        int notificacionesAntes = consultarEntero(
                "SELECT COUNT(*) FROM Notificacion_interna");
        LocalDate fecha = LocalDate.now().plusDays(40);

        assertThrows(IllegalArgumentException.class,
                () -> service.contratarOrganizador(ORGANIZADOR, null));
        assertThrows(IllegalArgumentException.class,
                () -> service.contratarOrganizador(ORGANIZADOR,
                        nuevaContratacion(CLIENTE, "Bodas", "Bogota",
                                null, new BigDecimal("1500000"), "Evento familiar")));
        assertThrows(IllegalArgumentException.class,
                () -> service.contratarOrganizador(ORGANIZADOR,
                        nuevaContratacion(CLIENTE, " ", "Bogota",
                                fecha, new BigDecimal("1500000"), "Evento familiar")));
        assertThrows(IllegalArgumentException.class,
                () -> service.contratarOrganizador(ORGANIZADOR,
                        nuevaContratacion(CLIENTE, "Bodas", "Bogota",
                                fecha, new BigDecimal("-1"), "Evento familiar")));
        assertThrows(IllegalArgumentException.class,
                () -> service.contratarOrganizador(ORGANIZADOR,
                        nuevaContratacion(CLIENTE, "Bodas", "Bogota",
                                fecha, new BigDecimal("1500000"), "c".repeat(501))));
        assertThrows(IllegalArgumentException.class,
                () -> service.contratarOrganizador(ORGANIZADOR,
                        nuevaContratacion(9002, "Bodas", "Bogota",
                                fecha, new BigDecimal("1500000"), "Evento familiar")));

        assertEquals(solicitudesAntes, consultarEntero(
                "SELECT COUNT(*) FROM Solicitud_Contratacion_Organizador"));
        assertEquals(notificacionesAntes, consultarEntero(
                "SELECT COUNT(*) FROM Notificacion_interna"));
    }

    @Test
    void rechazaFechaOcupadaYContratacionParaEsaFecha() throws Exception {
        assertThrows(IllegalArgumentException.class,
                () -> service.verificarDisponibilidadOrganizador(ORGANIZADOR, null));
        // usamos una fecha futura para que la prueba no caduque con el tiempo
        LocalDate fechaOcupada = LocalDate.now().plusDays(30);
        assertTrue(service.verificarDisponibilidadOrganizador(ORGANIZADOR, fechaOcupada));

        ejecutar("INSERT INTO Evento "
                + "(id_evento, id_organizador, nombre, tipo, fecha_inicio, fecha_fin, "
                + "ubicacion, estado, fecha_creacion) "
                + "VALUES (9501, 9201, 'Evento ocupado', 'Bodas', TIMESTAMP '"
                + fechaOcupada.minusDays(2) + " 10:00:00', TIMESTAMP '"
                + fechaOcupada.plusDays(2) + " 10:00:00', "
                + "'Bogota', 'PUBLICADO', CURRENT_TIMESTAMP)");

        assertFalse(service.verificarDisponibilidadOrganizador(ORGANIZADOR, fechaOcupada));
        assertThrows(IllegalStateException.class,
                () -> service.contratarOrganizador(
                        ORGANIZADOR,
                        new Contratacion(
                                CLIENTE, ORGANIZADOR, "Bodas", "Bogota",
                                fechaOcupada, new BigDecimal("2000000"), null)));
    }

    @Test
    void permiteContratarEnFechaOcupadaSoloPorEventoCancelado() throws Exception {
        LocalDate fechaEvento = LocalDate.now().plusDays(45);
        ejecutar("INSERT INTO Evento "
                + "(id_evento, id_organizador, nombre, tipo, fecha_inicio, fecha_fin, "
                + "ubicacion, estado, fecha_creacion) "
                + "VALUES (9502, 9201, 'Evento cancelado', 'Bodas', TIMESTAMP '"
                + fechaEvento.minusDays(1) + " 10:00:00', TIMESTAMP '"
                + fechaEvento.plusDays(1) + " 10:00:00', "
                + "'Bogota', 'CANCELADO', CURRENT_TIMESTAMP)");

        assertTrue(service.verificarDisponibilidadOrganizador(ORGANIZADOR, fechaEvento));
        service.contratarOrganizador(ORGANIZADOR, new Contratacion(
                CLIENTE,
                9999,
                "Bodas",
                "Bogota",
                fechaEvento,
                new BigDecimal("2500000"),
                "Evento familiar"));

        assertEquals(1, consultarEntero(
                "SELECT COUNT(*) FROM Solicitud_Contratacion_Organizador "
                + "WHERE id_cliente = " + CLIENTE + " AND id_organizador = "
                + ORGANIZADOR + " AND fecha_aproximada = DATE '" + fechaEvento + "'"));
        assertEquals(1, consultarEntero(
                "SELECT COUNT(*) FROM Notificacion_interna WHERE id_usuario = 9002"));
    }

    private void insertarUsuario(int id, String nombre, String tipo, String estado) throws Exception {
        ejecutar("INSERT INTO Usuario "
                + "(id_usuario, nombre, usuario, contraseña, estado, fecha_registro, "
                + "acepta_marketing, tipo_usuario) VALUES ("
                + id + ", '" + nombre + "', 'usuario" + id + "', 'clave', '"
                + estado + "', CURRENT_TIMESTAMP, FALSE, '" + tipo + "')");
    }

    private Contratacion nuevaContratacion(
            int idCliente,
            String tipoEvento,
            String ciudad,
            LocalDate fecha,
            BigDecimal presupuesto,
            String criterios) {
        return new Contratacion(
                idCliente, 9999, tipoEvento, ciudad, fecha, presupuesto, criterios);
    }

    private void ejecutar(String sql) throws Exception {
        // preparamos en h2 los datos que el servicio no puede crear por su cuenta
        try (Connection connection = DatabaseConnection.getConnection(url);
                Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        }
    }

    private void eliminarOrganizador(int idOrganizador) {
        try (Connection connection = DatabaseConnection.getConnection(url);
                Statement statement = connection.createStatement()) {
            statement.executeUpdate(
                    "DELETE FROM Organizador WHERE id_organizador = " + idOrganizador);
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo preparar el cambio concurrente.", e);
        }
    }

    private int consultarEntero(String sql) throws Exception {
        try (Connection connection = DatabaseConnection.getConnection(url);
                Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery(sql)) {
            result.next();
            return result.getInt(1);
        }
    }
}
