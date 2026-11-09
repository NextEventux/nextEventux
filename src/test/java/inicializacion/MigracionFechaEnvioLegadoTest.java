package inicializacion;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import persistencia.DatabaseConnection;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Bases cargadas con un seed.sql antiguo tienen las propuestas demo 1-3 sin fecha de envío
 * (y los servicios demo 1-4 sin tipo). Las migraciones 016 y 017 completan solo esos datos
 * para que la sincronización de demostración del arranque los reconozca.
 */
class MigracionFechaEnvioLegadoTest {

    private static final String MIGRACION_016 = "016_catalogo_tipo_servicios_legado.sql";
    private static final String MIGRACION_017 = "017_historia_fecha_envio_legado.sql";

    /** Columnas de Propuesta que la 017 nunca debe modificar. */
    private static final String COLUMNAS_INTACTAS =
            "id_propuesta, id_solicitud, valor, descripcion, estado, numero_version, "
            + "condiciones, vigente, material_apoyo";

    private String nuevaBase() {
        return "jdbc:h2:mem:fecha_legado_" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1";
    }

    /**
     * Simula una base cargada con el seed antiguo: propuestas 1-3 sin fecha de envío ni
     * condiciones y con los estados de entonces, y servicios 1-4 sin tipo. Después olvida
     * las migraciones 016 y 017 para que se apliquen al volver a abrir la base.
     */
    private void prepararBaseAntigua(String url, String... sqlAdicional) throws Exception {
        try (Connection conexion = DatabaseConnection.getConnection(url);
                Statement statement = conexion.createStatement()) {
            assertTrue(CargarDatosEjemplo.cargarSiEstaVacia(conexion));
            statement.executeUpdate(
                    "UPDATE Servicio SET tipo = NULL WHERE id_servicio IN (1, 2, 3, 4)");
            statement.executeUpdate("UPDATE Propuesta SET fecha_envio = NULL, condiciones = NULL "
                    + "WHERE id_propuesta IN (1, 2, 3)");
            statement.executeUpdate(
                    "UPDATE Propuesta SET estado = 'ACEPTADA' WHERE id_propuesta IN (1, 2)");
            statement.executeUpdate(
                    "UPDATE Propuesta SET estado = 'PENDIENTE' WHERE id_propuesta = 3");
            for (String sql : sqlAdicional) {
                statement.executeUpdate(sql);
            }
            statement.executeUpdate("DELETE FROM Esquema_version WHERE nombre IN ('"
                    + MIGRACION_016 + "', '" + MIGRACION_017 + "')");
        }
        DatabaseConnection.reiniciarInicializacion();
    }

    private String fechaEnvio(Connection conexion, int idPropuesta) throws SQLException {
        return texto(conexion,
                "SELECT fecha_envio FROM Propuesta WHERE id_propuesta = " + idPropuesta);
    }

    private String texto(Connection conexion, String sql) throws SQLException {
        try (Statement statement = conexion.createStatement();
                ResultSet resultado = statement.executeQuery(sql)) {
            assertTrue(resultado.next());
            return resultado.getString(1);
        }
    }

    private int contar(Connection conexion, String sql) throws SQLException {
        try (Statement statement = conexion.createStatement();
                ResultSet resultado = statement.executeQuery(sql)) {
            assertTrue(resultado.next());
            return resultado.getInt(1);
        }
    }

    private List<String> filas(Connection conexion, String sql) throws SQLException {
        List<String> filas = new ArrayList<>();
        try (Statement statement = conexion.createStatement();
                ResultSet resultado = statement.executeQuery(sql)) {
            int columnas = resultado.getMetaData().getColumnCount();
            while (resultado.next()) {
                StringBuilder fila = new StringBuilder();
                for (int i = 1; i <= columnas; i++) {
                    fila.append(resultado.getString(i)).append('|');
                }
                filas.add(fila.toString());
            }
        }
        return filas;
    }

    private List<String> propuestasSinFecha(Connection conexion) throws SQLException {
        return filas(conexion, "SELECT " + COLUMNAS_INTACTAS + " FROM Propuesta ORDER BY id_propuesta");
    }

    private int registros(Connection conexion, String migracion) throws SQLException {
        return contar(conexion,
                "SELECT COUNT(*) FROM Esquema_version WHERE nombre = '" + migracion + "'");
    }

    /** Mismo orden de sincronización que NextEventuxApp.start(). */
    private String arrancar(Connection conexion) throws Exception {
        CargarDatosEjemplo.cargarSiEstaVacia(conexion);
        int cuentas = SincronizarCuentasDemo.sincronizar(conexion);
        var catalogo = SincronizarCatalogoDemo.sincronizar(conexion);
        var disponibilidad = SincronizarDisponibilidadDemo.sincronizar(conexion);
        var historia = SincronizarHistoriaDemo.sincronizar(conexion);
        assertNull(disponibilidad.advertencia());
        return cuentas + "|" + catalogo + "|" + historia;
    }

    @Test
    void completaSoloLaFechaDeEnvioDeLasPropuestasAntiguas() throws Exception {
        String url = nuevaBase();
        prepararBaseAntigua(url);
        List<String> antes;
        try (Connection conexion = DriverManager.abrir(url)) {
            antes = propuestasSinFecha(conexion);
        }

        try (Connection conexion = DatabaseConnection.getConnection(url)) {
            assertEquals("2026-10-08 12:00:00", fechaEnvio(conexion, 1));
            assertEquals("2026-10-08 13:00:00", fechaEnvio(conexion, 2));
            assertEquals("2026-10-09 15:00:00", fechaEnvio(conexion, 3));
            // Estados, condiciones y demás columnas quedan exactamente igual.
            assertEquals(antes, propuestasSinFecha(conexion));
            assertEquals("ACEPTADA", texto(conexion,
                    "SELECT estado FROM Propuesta WHERE id_propuesta = 1"));
            assertNull(texto(conexion,
                    "SELECT condiciones FROM Propuesta WHERE id_propuesta = 1"));
            assertEquals(1, registros(conexion, MIGRACION_017));
        }
    }

    @Test
    void propuestaEditadaConservaSuFechaNulaYLaHistoriaSigueEnConflicto() throws Exception {
        String url = nuevaBase();
        prepararBaseAntigua(url,
                "UPDATE Propuesta SET descripcion = 'Descripción editada' WHERE id_propuesta = 1");

        try (Connection conexion = DatabaseConnection.getConnectionParaSincronizarDemo(url)) {
            assertNull(fechaEnvio(conexion, 1));
            assertEquals("2026-10-08 13:00:00", fechaEnvio(conexion, 2));
            assertEquals("2026-10-09 15:00:00", fechaEnvio(conexion, 3));

            SQLException error = assertThrows(SQLException.class, () -> arrancar(conexion));
            assertTrue(error.getMessage().contains("propuesta-1"), error.getMessage());
        }
    }

    @Test
    void solicitudConOtraIdentidadNoSeCompleta() throws Exception {
        String url = nuevaBase();
        prepararBaseAntigua(url,
                "UPDATE Solicitud_Ejecucion SET fecha_solicitud = TIMESTAMP '2026-10-09 09:30:00' "
                + "WHERE id_solicitud = 3");

        try (Connection conexion = DatabaseConnection.getConnection(url)) {
            assertNull(fechaEnvio(conexion, 3));
            assertEquals("2026-10-08 12:00:00", fechaEnvio(conexion, 1));
        }
    }

    @Test
    void noSobrescribeUnaFechaDeEnvioExistente() throws Exception {
        String url = nuevaBase();
        prepararBaseAntigua(url, "UPDATE Propuesta SET fecha_envio = TIMESTAMP '2026-10-20 09:15:00' "
                + "WHERE id_propuesta = 2");

        try (Connection conexion = DatabaseConnection.getConnection(url)) {
            assertEquals("2026-10-20 09:15:00", fechaEnvio(conexion, 2));
            assertEquals("2026-10-08 12:00:00", fechaEnvio(conexion, 1));
        }
    }

    @Test
    void seAplicaUnaSolaVezAunqueLaBaseSeVuelvaAAbrir() throws Exception {
        String url = nuevaBase();
        prepararBaseAntigua(url);
        try (Connection conexion = DatabaseConnection.getConnection(url);
                Statement statement = conexion.createStatement()) {
            assertEquals("2026-10-08 12:00:00", fechaEnvio(conexion, 1));
            // Un cambio posterior del usuario no debe revertirse al reabrir.
            statement.executeUpdate("UPDATE Propuesta SET fecha_envio = NULL WHERE id_propuesta = 1");
        }
        DatabaseConnection.reiniciarInicializacion();

        try (Connection conexion = DatabaseConnection.getConnection(url)) {
            assertNull(fechaEnvio(conexion, 1));
            assertEquals(1, registros(conexion, MIGRACION_016));
            assertEquals(1, registros(conexion, MIGRACION_017));
        }
    }

    @Test
    void migraciones016Y017PermitenElArranqueCompletoSinDuplicar() throws Exception {
        String url = nuevaBase();
        prepararBaseAntigua(url);
        List<String> estadosAntes;
        try (Connection conexion = DriverManager.abrir(url)) {
            estadosAntes = filas(conexion, "SELECT id_propuesta, estado FROM Propuesta ORDER BY 1");
        }

        try (Connection conexion = DatabaseConnection.getConnectionParaSincronizarDemo(url)) {
            assertEquals("Decoración", texto(conexion,
                    "SELECT tipo FROM Servicio WHERE id_servicio = 1"));
            assertEquals("2026-10-08 12:00:00", fechaEnvio(conexion, 1));

            arrancar(conexion);
            int servicios = contar(conexion, "SELECT COUNT(*) FROM Servicio");
            int propuestas = contar(conexion, "SELECT COUNT(*) FROM Propuesta");
            int contrataciones = contar(conexion, "SELECT COUNT(*) FROM Contratacion");

            // Segundo arranque: nada nuevo que crear.
            assertEquals("0|Resultado[eventosCreados=0, serviciosCreados=0]|"
                    + "Resultado[necesidadesCreadas=0, solicitudesCreadas=0, "
                    + "propuestasCreadas=0, contratacionesCreadas=0]", arrancar(conexion));
            assertEquals(servicios, contar(conexion, "SELECT COUNT(*) FROM Servicio"));
            assertEquals(propuestas, contar(conexion, "SELECT COUNT(*) FROM Propuesta"));
            assertEquals(contrataciones, contar(conexion, "SELECT COUNT(*) FROM Contratacion"));

            // Los estados antiguos de las propuestas existentes no se tocan.
            List<String> estadosDespues = filas(conexion,
                    "SELECT id_propuesta, estado FROM Propuesta WHERE id_propuesta <= "
                    + estadosAntes.size() + " ORDER BY 1");
            assertEquals(estadosAntes, estadosDespues);
        }
    }

    /** Conexión directa, sin aplicar migraciones, para leer el estado previo. */
    private static final class DriverManager {
        static Connection abrir(String url) throws SQLException {
            return java.sql.DriverManager.getConnection(url, "sa", "");
        }
    }
}
