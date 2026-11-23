package persistencia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import inicializacion.CargarDatosEjemplo;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import javafx.application.Application;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;
import views.invitaciones.LanzadorInvitados;
import views.invitaciones.LanzadorMesaRegalos;
import views.invitaciones.LanzadorRespuestaInvitacion;

@ResourceLock(Resources.SYSTEM_PROPERTIES)
class DatabaseConnectionH2Test {

    @TempDir
    Path tempDir;
    private String url;

    @AfterEach
    void cerrarBase() throws Exception {
        if (url != null) {
            apagarBase();
        }
    }

    @Test
    void initializesFreshDatabaseRebuildsAndPreservesDemoData() throws Exception {
        url = "jdbc:h2:" + tempDir.resolve("database_initialization").toAbsolutePath()
                .toString().replace('\\', '/');
        Map<String, Long> countsAfterRebuild;

        try (Connection connection = DatabaseConnection.getConnection(url)) {
            assertTrue(CargarDatosEjemplo.cargarSiEstaVacia(connection));
            verifySchema(connection);
            assertFalse(CargarDatosEjemplo.cargarSiEstaVacia(connection));
            try (Statement statement = connection.createStatement()) {
                assertEquals(1, statement.executeUpdate(
                        "UPDATE Usuario SET nombre = 'Dato anterior' WHERE usuario = 'laura.gomez'"));
            }
            assertFalse(CargarDatosEjemplo.cargarSiEstaVacia(connection));
            assertEquals("Dato anterior", nombreUsuario(connection, "laura.gomez"));
            ejecutarScriptsReconstruccion(connection);
            verifySchema(connection);
            countsAfterRebuild = countDemoData(connection);
            assertEquals(12, count(connection, "Usuario"));
            assertEquals(4, count(connection, "Evento"));
            assertEquals(15, count(connection, "Servicio"));
            assertEquals(11, count(connection, "Disponibilidad_Servicio"));
            assertEquals(8, count(connection, "Necesidad_Servicio_Evento"));
            assertEquals(15, count(connection, "Solicitud_Ejecucion"));
            assertEquals(10, count(connection, "Propuesta"));
            assertEquals(2, count(connection, "Contratacion"));
            assertEquals(1, count(connection, "Presupuesto_marketing"));
            assertEquals("Laura Gómez", nombreUsuario(connection, "laura.gomez"));

            ejecutarScriptsReconstruccion(connection);
            verifySchema(connection);
            assertEquals(countsAfterRebuild, countDemoData(connection));
            assertEquals("Laura Gómez", nombreUsuario(connection, "laura.gomez"));
        }

        apagarBase();

        try (Connection connection = DatabaseConnection.getConnection(url)) {
            assertFalse(CargarDatosEjemplo.cargarSiEstaVacia(connection));
            assertEquals(countsAfterRebuild, countDemoData(connection));
            assertEquals(12, count(connection, "Usuario"));
            assertEquals(4, count(connection, "Evento"));
            assertEquals(15, count(connection, "Servicio"));
            assertEquals("Laura Gómez", nombreUsuario(connection, "laura.gomez"));
        }
    }

    @Test
    void refusesToInitializePartialSchemaWithoutChangingExistingData() throws Exception {
        url = "jdbc:h2:" + tempDir.resolve("database_partial").toAbsolutePath()
                .toString().replace('\\', '/');

        try (Connection connection = DatabaseConnection.getConnection(url);
                Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE Usuario (nombre VARCHAR(100))");
            statement.execute("INSERT INTO Usuario VALUES ('Dato que debe conservarse')");

            SQLException error = assertThrows(
                    SQLException.class,
                    () -> CargarDatosEjemplo.cargarSiEstaVacia(connection));
            SQLException cargaDirecta = assertThrows(
                    SQLException.class,
                    () -> CargarDatosEjemplo.cargar(connection));

            assertTrue(error.getMessage().contains("Faltan tablas"));
            assertTrue(error.getMessage().contains("PERMISO"));
            assertTrue(cargaDirecta.getMessage().contains("Faltan tablas"));
            assertEquals(1, count(connection, "Usuario"));
            assertEquals("Dato que debe conservarse", nombreUsuarioParcial(connection));
            assertFalse(tableExists(connection, "Permiso"));
        }
    }

    @Test
    void auxiliaryLaunchersInitializeFreshDatabases() throws Exception {
        verificarInicializacion(new LanzadorMesaRegalos());
        verificarInicializacion(new LanzadorRespuestaInvitacion());
        verificarInicializacion(new LanzadorInvitados());
    }

    private static void verifySchema(Connection connection) throws SQLException {
        assertEquals(46, contarTablasPublicas(connection));

        String[] tables = {
            "Solicitud_publicacion", "Plantilla_evento", "Disponibilidad_Servicio",
            "Imagen_Servicio", "Pregunta_Servicio", "Necesidad_Servicio_Evento",
            "Notificacion_interna", "Finalizacion_evento", "Fotografia_evento",
            "Entrega_galeria", "Solicitud_Contratacion_Organizador",
            "Resena_Organizador", "Busqueda_evento", "Segmento_cliente",
            "Presupuesto_marketing"
        };
        for (String table : tables) {
            assertTrue(tableExists(connection, table), "Falta la tabla " + table);
        }
        assertFalse(tableExists(connection, "Esquema_version"));
        assertFalse(tableExists(connection, "Esquema_inicializado"));
        assertFalse(tableExists(connection, "Registro_demo_catalogo"));

        String[][] columns = {
            {"Usuario", "pais"}, {"Usuario", "ciudad"}, {"Usuario", "fecha_nacimiento"},
            {"Organizador", "estado_verificacion"}, {"Evento", "id_cliente"},
            {"Servicio", "anticipacion_minima"}, {"Solicitud_Ejecucion", "costo_comprometido"},
            {"Solicitud_Ejecucion", "id_necesidad_servicio"},
            {"Propuesta", "numero_version"}, {"Propuesta", "material_apoyo"},
            {"Contratacion", "id_disponibilidad"}, {"Contratacion", "alcance_propuesta"},
            {"Mensaje", "adjunto_url"}, {"Campana", "id_segmento"},
            {"Campana", "visualizaciones"}, {"Notificacion_interna", "id_invitado"}
        };
        for (String[] column : columns) {
            assertTrue(columnExists(connection, column[0], column[1]),
                    "Falta la columna " + column[0] + "." + column[1]);
        }

        assertTrue(uniqueIndexExists(connection, "Organizador", "UQ_ORGANIZADOR_USUARIO"));
        assertTrue(uniqueIndexExists(connection, "Propuesta", "UQ_PROPUESTA_VERSION"));
        assertTrue(uniqueIndexExists(connection, "Contratacion", "UQ_CONTRATACION_PROPUESTA"));
        assertTrue(uniqueIndexExists(connection, "Disponibilidad_Servicio", "UQ_DISPONIBILIDAD_SERVICIO"));
        assertTrue(foreignKeyExists(connection, "Rol_permiso", "id_permiso", "Permiso", "id_permiso"));
    }

    private void verificarInicializacion(Application launcher) throws Exception {
        String launcherUrl = "jdbc:h2:mem:launcher_" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1";
        String previousUrl = System.getProperty("nexteventux.db.url");
        try {
            System.setProperty("nexteventux.db.url", launcherUrl);
            launcher.init();
            try (Connection connection = DatabaseConnection.getConnection(launcherUrl)) {
                assertEquals(46, contarTablasPublicas(connection));
                assertFalse(CargarDatosEjemplo.cargarSiEstaVacia(connection));
            }
        } finally {
            if (previousUrl == null) {
                System.clearProperty("nexteventux.db.url");
            } else {
                System.setProperty("nexteventux.db.url", previousUrl);
            }
            try (Connection connection = DriverManager.getConnection(launcherUrl, "sa", "");
                    Statement statement = connection.createStatement()) {
                statement.execute("SHUTDOWN");
            }
        }
    }

    private static long contarTablasPublicas(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery(
                        "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES "
                        + "WHERE TABLE_SCHEMA = 'PUBLIC' AND TABLE_TYPE = 'BASE TABLE'")) {
            result.next();
            return result.getLong(1);
        }
    }

    private static Map<String, Long> countDemoData(Connection connection) throws SQLException {
        String[] tables = {
            "Usuario", "Organizador", "Proveedor", "Evento", "Servicio",
            "Disponibilidad_Servicio",
            "Necesidad_Servicio_Evento", "Solicitud_Ejecucion", "Propuesta",
            "Contratacion", "Presupuesto_marketing"
        };
        Map<String, Long> counts = new LinkedHashMap<>();
        for (String table : tables) {
            counts.put(table.toUpperCase(), count(connection, table));
        }
        return counts;
    }

    private static long count(Connection connection, String table) throws SQLException {
        try (Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM " + table)) {
            result.next();
            return result.getLong(1);
        }
    }

    private static void ejecutarScriptsReconstruccion(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("RUNSCRIPT FROM 'src/main/resources/data.sql'");
        }
    }

    private static String nombreUsuario(Connection connection, String usuario) throws SQLException {
        try (var statement = connection.prepareStatement(
                "SELECT nombre FROM Usuario WHERE usuario = ?")) {
            statement.setString(1, usuario);
            try (ResultSet result = statement.executeQuery()) {
                assertTrue(result.next());
                return result.getString(1);
            }
        }
    }

    private static String nombreUsuarioParcial(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery("SELECT nombre FROM Usuario")) {
            result.next();
            return result.getString(1);
        }
    }

    private void apagarBase() throws SQLException {
        try (Connection connection = DriverManager.getConnection(url, "sa", "");
                Statement statement = connection.createStatement()) {
            statement.execute("SHUTDOWN");
        }
    }

    private static boolean tableExists(Connection connection, String table) throws SQLException {
        try (var statement = connection.prepareStatement(
                "SELECT 1 FROM INFORMATION_SCHEMA.TABLES "
                + "WHERE TABLE_SCHEMA = 'PUBLIC' AND TABLE_NAME = ?")) {
            statement.setString(1, table.toUpperCase());
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        }
    }

    private static boolean columnExists(Connection connection, String table, String column)
            throws SQLException {
        try (var statement = connection.prepareStatement(
                "SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS "
                + "WHERE TABLE_SCHEMA = 'PUBLIC' AND TABLE_NAME = ? AND COLUMN_NAME = ?")) {
            statement.setString(1, table.toUpperCase());
            statement.setString(2, column.toUpperCase());
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        }
    }

    private static boolean uniqueIndexExists(Connection connection, String table, String index)
            throws SQLException {
        try (ResultSet result = connection.getMetaData().getIndexInfo(
                null, "PUBLIC", table.toUpperCase(), true, false)) {
            while (result.next()) {
                if (index.equalsIgnoreCase(result.getString("INDEX_NAME"))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean foreignKeyExists(
            Connection connection,
            String table,
            String column,
            String referencedTable,
            String referencedColumn) throws SQLException {
        try (ResultSet keys = connection.getMetaData().getImportedKeys(
                null, "PUBLIC", table.toUpperCase())) {
            while (keys.next()) {
                if (column.equalsIgnoreCase(keys.getString("FKCOLUMN_NAME"))
                        && referencedTable.equalsIgnoreCase(keys.getString("PKTABLE_NAME"))
                        && referencedColumn.equalsIgnoreCase(keys.getString("PKCOLUMN_NAME"))) {
                    return true;
                }
            }
        }
        return false;
    }
}
