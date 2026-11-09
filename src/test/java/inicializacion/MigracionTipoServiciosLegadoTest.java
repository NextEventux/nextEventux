package inicializacion;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import persistencia.DatabaseConnection;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Bases cargadas con un seed.sql antiguo tienen los servicios demo 1-4 sin tipo.
 * La migración 016 los completa para que SincronizarCatalogoDemo los reconozca.
 */
class MigracionTipoServiciosLegadoTest {

    private static final String MIGRACION = "016_catalogo_tipo_servicios_legado.sql";

    private String nuevaBase() {
        return "jdbc:h2:mem:tipo_legado_" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1";
    }

    /** Simula una base cargada antes de que el seed incluyera el tipo de los servicios. */
    private void prepararBaseAntigua(String url, String sqlAdicional) throws Exception {
        try (Connection conexion = DatabaseConnection.getConnection(url);
                Statement statement = conexion.createStatement()) {
            assertTrue(CargarDatosEjemplo.cargarSiEstaVacia(conexion));
            statement.executeUpdate(
                    "UPDATE Servicio SET tipo = NULL WHERE id_servicio IN (1, 2, 3, 4)");
            if (sqlAdicional != null) {
                statement.executeUpdate(sqlAdicional);
            }
            statement.executeUpdate(
                    "DELETE FROM Esquema_version WHERE nombre = '" + MIGRACION + "'");
        }
        DatabaseConnection.reiniciarInicializacion();
    }

    private String tipo(Connection conexion, int idServicio) throws SQLException {
        try (Statement statement = conexion.createStatement();
                ResultSet resultado = statement.executeQuery(
                        "SELECT tipo FROM Servicio WHERE id_servicio = " + idServicio)) {
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

    @Test
    void baseAntiguaSinTipoSincronizaElCatalogoSinDuplicar() throws Exception {
        String url = nuevaBase();
        prepararBaseAntigua(url, null);

        try (Connection conexion = DatabaseConnection.getConnectionParaSincronizarDemo(url)) {
            assertEquals("Decoración", tipo(conexion, 1));
            assertEquals("Decoración", tipo(conexion, 2));
            assertEquals("Catering", tipo(conexion, 3));
            assertEquals("Postres", tipo(conexion, 4));
            assertEquals(1, contar(conexion,
                    "SELECT COUNT(*) FROM Esquema_version WHERE nombre = '" + MIGRACION + "'"));

            assertEquals(0, SincronizarCuentasDemo.sincronizar(conexion));
            var resultado = SincronizarCatalogoDemo.sincronizar(conexion);
            assertEquals(0, resultado.eventosCreados());
            assertEquals(0, resultado.serviciosCreados());
            assertEquals(15, contar(conexion, "SELECT COUNT(*) FROM Servicio"));
            assertEquals(19, contar(conexion, "SELECT COUNT(*) FROM Registro_demo_catalogo"));
        }
    }

    @Test
    void servicioQueNoCoincideConLaFirmaConservaSuTipoYSigueEnConflicto() throws Exception {
        String url = nuevaBase();
        prepararBaseAntigua(url,
                "UPDATE Servicio SET descripcion = 'Descripción editada' WHERE id_servicio = 1");

        try (Connection conexion = DatabaseConnection.getConnectionParaSincronizarDemo(url)) {
            assertNull(tipo(conexion, 1));
            assertEquals("Decoración", tipo(conexion, 2));
            assertEquals("Catering", tipo(conexion, 3));
            assertEquals("Postres", tipo(conexion, 4));

            SQLException error = assertThrows(SQLException.class,
                    () -> SincronizarCatalogoDemo.sincronizar(conexion));
            assertTrue(error.getMessage().contains("Decoración floral"));
            assertEquals(0, contar(conexion, "SELECT COUNT(*) FROM Registro_demo_catalogo"));
            assertEquals(15, contar(conexion, "SELECT COUNT(*) FROM Servicio"));
        }
    }

    @Test
    void noSobrescribeUnTipoYaAsignado() throws Exception {
        String url = nuevaBase();
        prepararBaseAntigua(url, "UPDATE Servicio SET tipo = 'Otro tipo' WHERE id_servicio = 3");

        try (Connection conexion = DatabaseConnection.getConnection(url)) {
            assertEquals("Otro tipo", tipo(conexion, 3));
            assertEquals("Decoración", tipo(conexion, 1));
        }
    }
}
