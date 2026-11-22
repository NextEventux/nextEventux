package persistencia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import inicializacion.CargarDatosEjemplo;
import inicializacion.SincronizarCatalogoDemo;
import inicializacion.SincronizarCuentasDemo;
import inicializacion.SincronizarDisponibilidadDemo;
import inicializacion.SincronizarHistoriaDemo;
import inicializacion.SincronizarPreferenciasClientesDemo;
import inicializacion.SincronizarPresupuestoMarketing;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class DatabaseConnectionH2Test {

    private String url;

    @AfterEach
    void cerrarBase() throws Exception {
        if (url != null) {
            try (Connection connection = DriverManager.getConnection(url, "sa", "");
                    Statement statement = connection.createStatement()) {
                statement.execute("SHUTDOWN");
            }
        }
    }

    @Test
    void initializesFreshDatabaseLoadsSeedOnceAndPreservesDemoData() throws Exception {
        url = "jdbc:h2:mem:database_initialization_" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1";
        Map<String, Long> countsAfterFirstOpen;

        try (Connection connection = DatabaseConnection.getConnection(url)) {
            verifySchema(connection);
            assertTrue(CargarDatosEjemplo.cargarSiEstaVacia(connection));
            assertFalse(CargarDatosEjemplo.cargarSiEstaVacia(connection));
            synchronizeDemoData(connection);
            countsAfterFirstOpen = countDemoData(connection);
            assertTrue(countsAfterFirstOpen.get("REGISTRO_DEMO_CATALOGO") > 0);
        }

        try (Connection connection = DatabaseConnection.getConnection(url)) {
            assertFalse(CargarDatosEjemplo.cargarSiEstaVacia(connection));
            synchronizeDemoData(connection);
            assertEquals(countsAfterFirstOpen, countDemoData(connection));
            assertEquals(12, count(connection, "Usuario"));
            assertEquals(4, count(connection, "Evento"));
            assertEquals(15, count(connection, "Servicio"));
        }
    }

    private static void verifySchema(Connection connection) throws SQLException {
        String[] tables = {
            "Solicitud_publicacion", "Plantilla_evento", "Disponibilidad_Servicio",
            "Imagen_Servicio", "Pregunta_Servicio", "Necesidad_Servicio_Evento",
            "Notificacion_interna", "Finalizacion_evento", "Fotografia_evento",
            "Entrega_galeria", "Solicitud_Contratacion_Organizador",
            "Resena_Organizador", "Busqueda_evento", "Segmento_cliente",
            "Registro_demo_catalogo", "Presupuesto_marketing"
        };
        for (String table : tables) {
            assertTrue(tableExists(connection, table), "Falta la tabla " + table);
        }
        assertFalse(tableExists(connection, "Esquema_version"));

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
    }

    private static void synchronizeDemoData(Connection connection) throws SQLException {
        SincronizarCuentasDemo.sincronizar(connection);
        SincronizarPreferenciasClientesDemo.sincronizar(connection);
        SincronizarPresupuestoMarketing.sincronizar(connection);
        SincronizarCatalogoDemo.sincronizar(connection);
        SincronizarDisponibilidadDemo.sincronizar(connection);
        SincronizarHistoriaDemo.sincronizar(connection);
    }

    private static Map<String, Long> countDemoData(Connection connection) throws SQLException {
        String[] tables = {
            "Usuario", "Organizador", "Proveedor", "Evento", "Servicio",
            "Registro_demo_catalogo", "Disponibilidad_Servicio",
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
}
