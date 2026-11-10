import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import entities.ClienteMarketing;
import entities.Segmentacion;
import entities.SegmentoCalculado;
import entities.SolicitudEvento;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.h2.tools.RunScript;
import org.junit.jupiter.api.Test;
import persistencia.DatabaseConnection;
import repositories.RepositoryMarketing;
import services.ServiceMarketing;

class MarketingCU21Test {

    @Test
    void segmentaClienteSinFechaDeNacimientoYConMenosDeCincoClientes() throws Exception {
        String url = crearBase(1);
        agregarBusqueda(url, 2, "Boda");

        ServiceMarketing servicio = new ServiceMarketing(new RepositoryMarketing(url));
        assertFalse(new RepositoryMarketing(url).listarClientesActivos().get(0).isAceptaMarketing());
        List<Segmentacion> segmentos = servicio.segmentarClientes(1);

        assertEquals(1, segmentos.size());
        assertEquals("Bogotá - Boda", segmentos.get(0).getNombre());
        assertEquals(List.of(2), segmentos.get(0).getIdsClientes());
        assertEquals(1, contarMiembrosActivos(url));
        assertNull(datoUsuario(url, "fecha_nacimiento", 2));
    }

    @Test
    void rechazaLaSegmentacionCuandoElUsuarioNoEsAdministradorActivo() throws Exception {
        String url = crearBase(1);
        ServiceMarketing servicio = new ServiceMarketing(new RepositoryMarketing(url));

        assertThrows(IllegalArgumentException.class, () -> servicio.segmentarClientes(2));
        assertThrows(IllegalArgumentException.class, () -> servicio.segmentarClientes(9999));
    }

    @Test
    void clientesDeLaMismaCiudadConPreferenciasDistintasFormanSegmentosDistintos() throws Exception {
        List<ClienteMarketing> clientes = List.of(
                cliente(2, "Bogotá"), cliente(3, "Bogotá"));
        List<SolicitudEvento> preferencias = List.of(
                preferencia(2, "Boda"), preferencia(3, "Conferencia"));

        List<SegmentoCalculado> segmentos = servicio().calcularSegmentos(clientes, preferencias);

        assertEquals(2, segmentos.size());
        assertEquals(List.of("Boda", "Conferencia"),
                segmentos.stream().map(SegmentoCalculado::getTipoEvento).toList());
        assertEquals(List.of(2), segmentoPorTipo(segmentos, "Boda").getIdsClientes());
        assertEquals(List.of(3), segmentoPorTipo(segmentos, "Conferencia").getIdsClientes());
    }

    @Test
    void noMezclaClientesDePaisesDistintosAunqueCompartanCiudadYPreferencia() {
        List<ClienteMarketing> clientes = List.of(
                new ClienteMarketing(2, "Cliente 2", "Colombia", "Bogotá", false),
                new ClienteMarketing(3, "Cliente 3", "Argentina", "Bogotá", false));
        List<SolicitudEvento> preferencias = List.of(
                preferencia(2, "Boda"), preferencia(3, "Boda"));

        List<SegmentoCalculado> segmentos = servicio().calcularSegmentos(clientes, preferencias);

        assertEquals(2, segmentos.size());
        assertEquals(List.of("Argentina", "Colombia"),
                segmentos.stream().map(SegmentoCalculado::getPais).toList());
        assertEquals(List.of(3), segmentoPorPais(segmentos, "Argentina").getIdsClientes());
        assertEquals(List.of(2), segmentoPorPais(segmentos, "Colombia").getIdsClientes());
    }

    @Test
    void permiteVariasPreferenciasYNoDuplicaAlClienteDentroDelMismoSegmento() {
        List<ClienteMarketing> clientes = List.of(
                cliente(2, "Bogotá"), cliente(3, "Bogotá"));
        List<SolicitudEvento> preferencias = List.of(
                preferencia(2, "Boda", 1),
                preferencia(2, "Boda", 2),
                preferencia(2, "Social", 1),
                preferencia(3, "Boda", 1));

        List<SegmentoCalculado> segmentos = servicio().calcularSegmentos(clientes, preferencias);

        assertEquals(2, segmentos.size());
        assertEquals(List.of(2, 3), segmentoPorTipo(segmentos, "Boda").getIdsClientes());
        assertEquals(List.of(2), segmentoPorTipo(segmentos, "Social").getIdsClientes());
    }

    @Test
    void calculaPreferenciasDesdeBusquedasSolicitudesYEventos() throws Exception {
        String url = crearBase(1);
        insertarUsuario(url, 3, "Organizadora", "ORGANIZADOR", "Bogotá");
        try (Connection connection = DatabaseConnection.getConnection(url);
                Statement statement = connection.createStatement()) {
            statement.executeUpdate(
                    "INSERT INTO Organizador (id_organizador, id_usuario, estado) "
                            + "VALUES (1, 3, 'ACTIVO')");
            statement.executeUpdate(
                    "INSERT INTO Solicitud_Contratacion_Organizador "
                            + "(id_cliente, id_organizador, tipo_evento, ciudad, fecha_aproximada, "
                            + "presupuesto_estimado, estado) "
                            + "VALUES (2, 1, 'Social', 'Bogotá', DATE '2026-12-01', 100000, 'PENDIENTE')");
            statement.executeUpdate(
                    "INSERT INTO Evento "
                            + "(id_organizador, nombre, tipo, fecha_inicio, fecha_fin, estado, "
                            + "fecha_creacion, id_cliente) "
                            + "VALUES (1, 'Evento del cliente', 'Fiesta', CURRENT_TIMESTAMP, "
                            + "CURRENT_TIMESTAMP, 'ACTIVO', CURRENT_TIMESTAMP, 2)");
        }
        agregarBusqueda(url, 2, "Boda");
        agregarBusqueda(url, 2, "Boda");

        List<SolicitudEvento> preferencias =
                new RepositoryMarketing(url).contarSolicitudesPorTipo();

        assertEquals(3, preferencias.size());
        assertTrue(preferencias.stream().anyMatch(
                item -> item.getIdCliente() == 2 && item.getTipo().equals("Boda")
                        && item.getCantidad() == 2));
        assertTrue(preferencias.stream().anyMatch(
                item -> item.getIdCliente() == 2 && item.getTipo().equals("Social")
                        && item.getCantidad() == 1));
        assertTrue(preferencias.stream().anyMatch(
                item -> item.getIdCliente() == 2 && item.getTipo().equals("Fiesta")
                        && item.getCantidad() == 1));
        List<SegmentoCalculado> segmentos = servicio().calcularSegmentos(
                new RepositoryMarketing(url).listarClientesActivos(), preferencias);
        assertEquals(3, segmentos.size());
        assertTrue(segmentos.stream().allMatch(
                segmento -> segmento.getIdsClientes().equals(List.of(2))));
    }

    @Test
    void usaCiudadDeSolicitudRecienteCuandoElPerfilTieneCiudadVacia() throws Exception {
        String url = crearBase(1);
        insertarUsuario(url, 3, "Organizadora", "ORGANIZADOR", "Bogotá");
        try (Connection connection = DatabaseConnection.getConnection(url);
                Statement statement = connection.createStatement()) {
            statement.executeUpdate(
                    "UPDATE Usuario SET ciudad = '   ' WHERE id_usuario = 2");
            statement.executeUpdate(
                    "INSERT INTO Organizador (id_organizador, id_usuario, estado) "
                            + "VALUES (1, 3, 'ACTIVO')");
            statement.executeUpdate(
                    "INSERT INTO Solicitud_Contratacion_Organizador "
                            + "(id_cliente, id_organizador, tipo_evento, ciudad, fecha_aproximada, "
                            + "presupuesto_estimado, estado) "
                            + "VALUES (2, 1, 'Boda', 'Medellín', DATE '2026-12-01', 100000, 'PENDIENTE')");
        }

        ClienteMarketing cliente = new RepositoryMarketing(url).listarClientesActivos().get(0);

        assertEquals("Medellín", cliente.getCiudad());
        assertEquals(1, servicio().calcularSegmentos(
                List.of(cliente), List.of(preferencia(2, "Boda"))).size());
    }

    @Test
    void desactivaSegmentosCuandoDesaparecenLasPreferenciasQueLosSustentaban() throws Exception {
        String url = crearBase(1);
        agregarBusqueda(url, 2, "Boda");
        ServiceMarketing servicio = new ServiceMarketing(new RepositoryMarketing(url));

        Segmentacion anterior = servicio.segmentarClientes(1).get(0);
        try (Connection connection = DatabaseConnection.getConnection(url);
                Statement statement = connection.createStatement()) {
            statement.executeUpdate("DELETE FROM Busqueda_evento WHERE id_usuario = 2");
        }

        assertTrue(servicio.segmentarClientes(1).isEmpty());
        assertTrue(servicio.listarSegmentosActivos().isEmpty());
        assertEquals(Segmentacion.ESTADO_INACTIVA, estadoSegmento(url, anterior.getIdSegmento()));
        assertEquals(List.of(2), idsMiembros(url, anterior.getIdSegmento()));
        assertEquals(1, contarSegmentos(url));
    }

    @Test
    void regenerarSinCambiosConservaSegmentosAnterioresComoHistorial() throws Exception {
        String url = crearBase(2);
        agregarBusqueda(url, 2, "Boda");
        agregarBusqueda(url, 3, "Boda");
        ServiceMarketing servicio = new ServiceMarketing(new RepositoryMarketing(url));

        Segmentacion anterior = servicio.segmentarClientes(1).get(0);
        Segmentacion actual = servicio.segmentarClientes(1).get(0);

        assertNotEquals(anterior.getIdSegmento(), actual.getIdSegmento());
        assertEquals(Segmentacion.ESTADO_INACTIVA, estadoSegmento(url, anterior.getIdSegmento()));
        assertEquals(List.of(2, 3), idsMiembros(url, anterior.getIdSegmento()));
        assertEquals(Segmentacion.ESTADO_ACTIVA, estadoSegmento(url, actual.getIdSegmento()));
        assertEquals(List.of(2, 3), actual.getIdsClientes());
        assertEquals(2, contarSegmentos(url));
    }

    @Test
    void revierteLaDesactivacionYLasInsercionesSiFallaElGuardado() throws Exception {
        String url = crearBase(1);
        agregarBusqueda(url, 2, "Boda");
        RepositoryMarketing repositorio = new RepositoryMarketing(url);
        Segmentacion anterior = new ServiceMarketing(repositorio).segmentarClientes(1).get(0);
        Segmentacion invalido = new Segmentacion(1, "Segmento inválido", "AUTO|fallo",
                "Prueba de rollback", Segmentacion.ESTADO_ACTIVA, List.of(9999));

        assertThrows(IllegalStateException.class,
                () -> repositorio.reemplazarSegmentosAutomaticos(List.of(invalido)));

        assertEquals(1, contarSegmentos(url));
        assertEquals(Segmentacion.ESTADO_ACTIVA, estadoSegmento(url, anterior.getIdSegmento()));
        assertEquals(List.of(2), idsMiembros(url, anterior.getIdSegmento()));
    }

    @Test
    void migracionMarketingConservaDatosPersonalesFaltantesYSeRegistraUnaSolaVez() throws Exception {
        String url = "jdbc:h2:mem:marketing_migracion_" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1";
        try (Connection connection = DriverManager.getConnection(url, "sa", "")) {
            try (InputStream schema = DatabaseConnection.class.getResourceAsStream("/schema.sql")) {
                assertTrue(schema != null, "No se encontró schema.sql");
                RunScript.execute(connection, new InputStreamReader(schema, StandardCharsets.UTF_8));
            }
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO Usuario (id_usuario, nombre, usuario, contraseña, estado, "
                            + "fecha_registro, acepta_marketing, tipo_usuario) "
                            + "VALUES (2, 'Cliente legado', 'cliente_legado', 'x', 'ACTIVO', "
                            + "CURRENT_TIMESTAMP, FALSE, 'CLIENTE')")) {
                statement.executeUpdate();
            }
        }

        try (Connection connection = DatabaseConnection.getConnection(url)) {
            assertEquals(1, contarMigracionMarketing(connection));
            ClienteMarketing cliente = new RepositoryMarketing(url).listarClientesActivos().get(0);
            assertEquals("No especificado", cliente.getPais());
            assertNull(cliente.getCiudad());
            assertFalse(cliente.isAceptaMarketing());
            assertTrue(servicio().calcularSegmentos(
                    List.of(cliente), List.of(preferencia(2, "Boda"))).isEmpty());
            assertEquals("Cliente legado", nombreUsuario(connection, 2));
            assertNull(datoUsuario(connection, "pais", 2));
            assertNull(datoUsuario(connection, "ciudad", 2));
            assertNull(datoUsuario(connection, "fecha_nacimiento", 2));
        }

        DatabaseConnection.reiniciarInicializacion();
        try (Connection connection = DatabaseConnection.getConnection(url)) {
            assertEquals(1, contarMigracionMarketing(connection));
            assertEquals("Cliente legado", nombreUsuario(connection, 2));
            assertNull(datoUsuario(connection, "pais", 2));
            assertNull(datoUsuario(connection, "ciudad", 2));
            assertNull(datoUsuario(connection, "fecha_nacimiento", 2));
        }
    }

    private ServiceMarketing servicio() {
        return new ServiceMarketing(new RepositoryMarketing());
    }

    private ClienteMarketing cliente(int id, String ciudad) {
        return new ClienteMarketing(id, "Cliente " + id, "Colombia", ciudad, true);
    }

    private SolicitudEvento preferencia(int idCliente, String tipo) {
        return preferencia(idCliente, tipo, 1);
    }

    private SolicitudEvento preferencia(int idCliente, String tipo, int cantidad) {
        return new SolicitudEvento(idCliente, tipo, cantidad);
    }

    private SegmentoCalculado segmentoPorTipo(List<SegmentoCalculado> segmentos, String tipo) {
        return segmentos.stream()
                .filter(segmento -> segmento.getTipoEvento().equals(tipo))
                .findFirst()
                .orElseThrow();
    }

    private SegmentoCalculado segmentoPorPais(List<SegmentoCalculado> segmentos, String pais) {
        return segmentos.stream()
                .filter(segmento -> segmento.getPais().equals(pais))
                .findFirst()
                .orElseThrow();
    }

    private String crearBase(int cantidadClientes) throws Exception {
        String url = "jdbc:h2:mem:marketing_cu21_" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1";
        try (Connection connection = DatabaseConnection.getConnection(url)) {
            insertarUsuario(connection, 1, "Administradora", "ADMINISTRADOR", "Bogotá");
            for (int id = 2; id < 2 + cantidadClientes; id++) {
                insertarUsuario(connection, id, "Cliente " + id, "CLIENTE", "Bogotá");
            }
        }
        return url;
    }

    private void agregarBusqueda(String url, int idCliente, String tipo) throws Exception {
        try (Connection connection = DatabaseConnection.getConnection(url);
                PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO Busqueda_evento (id_usuario, tipo, fecha_busqueda) "
                                + "VALUES (?, ?, CURRENT_TIMESTAMP)")) {
            statement.setInt(1, idCliente);
            statement.setString(2, tipo);
            statement.executeUpdate();
        }
    }

    private void insertarUsuario(
            Connection connection, int id, String nombre, String tipo, String ciudad)
            throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO Usuario (id_usuario, nombre, usuario, contraseña, estado, "
                        + "fecha_registro, acepta_marketing, tipo_usuario, pais, ciudad) "
                        + "VALUES (?, ?, ?, 'x', 'ACTIVO', CURRENT_TIMESTAMP, FALSE, ?, ?, ?)")) {
            statement.setInt(1, id);
            statement.setString(2, nombre);
            statement.setString(3, "marketing_" + id);
            statement.setString(4, tipo);
            statement.setString(5, ciudad == null ? null : "Colombia");
            statement.setString(6, ciudad);
            statement.executeUpdate();
        }
    }

    private void insertarUsuario(String url, int id, String nombre, String tipo, String ciudad)
            throws Exception {
        try (Connection connection = DatabaseConnection.getConnection(url)) {
            insertarUsuario(connection, id, nombre, tipo, ciudad);
        }
    }

    private int contarSegmentos(String url) throws Exception {
        try (Connection connection = DatabaseConnection.getConnection(url);
                Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM Segmentacion")) {
            result.next();
            return result.getInt(1);
        }
    }

    private int contarMiembrosActivos(String url) throws Exception {
        try (Connection connection = DatabaseConnection.getConnection(url);
                Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery(
                        "SELECT COUNT(*) FROM Segmento_cliente sc "
                                + "JOIN Segmentacion s ON s.id_segmento = sc.id_segmento "
                                + "WHERE s.estados = 'ACTIVA'")) {
            result.next();
            return result.getInt(1);
        }
    }

    private List<Integer> idsMiembros(String url, int idSegmento) throws Exception {
        try (Connection connection = DatabaseConnection.getConnection(url);
                PreparedStatement statement = connection.prepareStatement(
                        "SELECT id_usuario FROM Segmento_cliente WHERE id_segmento = ? "
                                + "ORDER BY id_usuario")) {
            statement.setInt(1, idSegmento);
            try (ResultSet result = statement.executeQuery()) {
                List<Integer> ids = new ArrayList<>();
                while (result.next()) {
                    ids.add(result.getInt(1));
                }
                return ids;
            }
        }
    }

    private String estadoSegmento(String url, int idSegmento) throws Exception {
        try (Connection connection = DatabaseConnection.getConnection(url);
                PreparedStatement statement = connection.prepareStatement(
                        "SELECT estados FROM Segmentacion WHERE id_segmento = ?")) {
            statement.setInt(1, idSegmento);
            try (ResultSet result = statement.executeQuery()) {
                assertTrue(result.next());
                return result.getString(1);
            }
        }
    }

    private int contarMigracionMarketing(Connection connection) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT COUNT(*) FROM Esquema_version WHERE nombre = ?")) {
            statement.setString(1, "013_marketing_cu21.sql");
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getInt(1);
            }
        }
    }

    private String nombreUsuario(Connection connection, int idUsuario) throws Exception {
        return datoUsuario(connection, "nombre", idUsuario).toString();
    }

    private Object datoUsuario(Connection connection, String columna, int idUsuario)
            throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT " + columna + " FROM Usuario WHERE id_usuario = ?")) {
            statement.setInt(1, idUsuario);
            try (ResultSet result = statement.executeQuery()) {
                assertTrue(result.next());
                return result.getObject(1);
            }
        }
    }

    private Object datoUsuario(String url, String columna, int idUsuario) throws Exception {
        try (Connection connection = DatabaseConnection.getConnection(url)) {
            return datoUsuario(connection, columna, idUsuario);
        }
    }
}
