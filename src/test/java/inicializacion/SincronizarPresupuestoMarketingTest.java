package inicializacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import persistencia.DatabaseConnection;
import repositories.RepositoryMarketing;

class SincronizarPresupuestoMarketingTest {

    private static final BigDecimal INICIAL = new BigDecimal("20000000.00");

    private String nuevaBase() {
        return "jdbc:h2:mem:presupuesto_marketing_" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1";
    }

    private int contar(Connection conexion, String sql) throws Exception {
        try (Statement sentencia = conexion.createStatement();
                ResultSet resultado = sentencia.executeQuery(sql)) {
            resultado.next();
            return resultado.getInt(1);
        }
    }

    private void insertarUsuarioExistente(Connection conexion) throws Exception {
        try (Statement sentencia = conexion.createStatement()) {
            sentencia.executeUpdate("INSERT INTO Usuario (id_usuario, nombre, usuario, contraseña, estado, "
                    + "fecha_registro, acepta_marketing, tipo_usuario) "
                    + "VALUES (50, 'Cuenta propia', 'cuenta.propia', 'clave-propia', 'ACTIVO', "
                    + "CURRENT_TIMESTAMP, TRUE, 'ADMINISTRADOR')");
        }
    }

    @Test
    void unaBaseExistenteSinFilaRecibeElPresupuestoInicialYConservaSusDatos() throws Exception {
        String url = nuevaBase();
        try (Connection conexion = DatabaseConnection.getConnection(url)) {
            insertarUsuarioExistente(conexion);
            assertEquals(0, contar(conexion, "SELECT COUNT(*) FROM Presupuesto_marketing"));

            assertTrue(SincronizarPresupuestoMarketing.sincronizar(conexion));

            assertEquals(1, contar(conexion, "SELECT COUNT(*) FROM Usuario WHERE usuario = 'cuenta.propia'"));
        }
        assertEquals(INICIAL, new RepositoryMarketing(url).obtenerPresupuestoDisponible());
    }

    @Test
    void tambienAlcanzaBasesQueYaRegistraronLaMigracion() throws Exception {
        try (Connection conexion = DatabaseConnection.getConnection(nuevaBase())) {
            insertarUsuarioExistente(conexion);
            assertEquals(1, contar(conexion,
                    "SELECT COUNT(*) FROM Esquema_version WHERE nombre = '016_marketing_cu23.sql'"));

            assertTrue(SincronizarPresupuestoMarketing.sincronizar(conexion));
        }
    }

    @Test
    void noModificaUnPresupuestoYaDefinido() throws Exception {
        String url = nuevaBase();
        try (Connection conexion = DatabaseConnection.getConnection(url);
                Statement sentencia = conexion.createStatement()) {
            sentencia.executeUpdate("INSERT INTO Presupuesto_marketing (id_presupuesto, disponible) "
                    + "VALUES (1, 750000.00)");

            assertFalse(SincronizarPresupuestoMarketing.sincronizar(conexion));
        }
        assertEquals(new BigDecimal("750000.00"), new RepositoryMarketing(url).obtenerPresupuestoDisponible());
    }

    @Test
    void esIdempotenteYNoRestauraUnDisponibleYaGastado() throws Exception {
        String url = nuevaBase();
        try (Connection conexion = DatabaseConnection.getConnection(url);
                Statement sentencia = conexion.createStatement()) {
            assertTrue(SincronizarPresupuestoMarketing.sincronizar(conexion));
            sentencia.executeUpdate("UPDATE Presupuesto_marketing SET disponible = 0 WHERE id_presupuesto = 1");

            assertFalse(SincronizarPresupuestoMarketing.sincronizar(conexion));
            assertFalse(SincronizarPresupuestoMarketing.sincronizar(conexion));

            assertEquals(1, contar(conexion, "SELECT COUNT(*) FROM Presupuesto_marketing"));
        }
        assertEquals(0, new RepositoryMarketing(url).obtenerPresupuestoDisponible().signum());
    }

    @Test
    void unaBaseNuevaCreaUnaSolaFilaDespuesDeCargarLosEjemplos() throws Exception {
        String url = nuevaBase();
        try (Connection conexion = DatabaseConnection.getConnection(url)) {
            assertTrue(CargarDatosEjemplo.cargarSiEstaVacia(conexion));
            assertTrue(SincronizarPresupuestoMarketing.sincronizar(conexion));

            assertEquals(1, contar(conexion, "SELECT COUNT(*) FROM Presupuesto_marketing"));
        }
        assertEquals(INICIAL, new RepositoryMarketing(url).obtenerPresupuestoDisponible());
    }
}
