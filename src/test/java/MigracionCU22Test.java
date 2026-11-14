import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import persistencia.DatabaseConnection;
import repositories.RepositoryMarketing;

class MigracionCU22Test {

    private boolean existeColumna(Connection connection, String tabla, String columna) throws Exception {
        try (ResultSet result = connection.getMetaData().getColumns(null, null, tabla, columna)) {
            return result.next();
        }
    }

    @Test
    void laMigracionAgregaLasColumnasYLaTablaDePresupuesto() throws Exception {
        String url = DatosCampanaPrueba.crearBase();
        try (Connection connection = DatabaseConnection.getConnection(url)) {
            for (String columna : new String[]{"ID_USUARIO", "ID_SEGMENTO", "TIPO_OFERTA",
                    "PRESUPUESTO_MAXIMO", "ESTADO", "VISUALIZACIONES", "CONVERSIONES"}) {
                assertTrue(existeColumna(connection, "CAMPANA", columna), "Falta " + columna);
            }
            assertTrue(existeColumna(connection, "PRESUPUESTO_MARKETING", "DISPONIBLE"));
            try (ResultSet result = connection.getMetaData().getColumns(null, null, "CAMPANA", "ID_PROVEEDOR")) {
                assertTrue(result.next());
                assertEquals(java.sql.DatabaseMetaData.columnNullable, result.getInt("NULLABLE"));
            }
        }
    }

    @Test
    void sinPresupuestoCargadoElDisponibleEsCero() throws Exception {
        String url = "jdbc:h2:mem:marketing_cu22_vacia_" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1";
        assertEquals(0, new RepositoryMarketing(url).obtenerPresupuestoDisponible().signum());
    }

    @Test
    void laMigracionNoInsertaDatosYLaBaseNuevaSigueVacia() throws Exception {
        String url = "jdbc:h2:mem:marketing_cu22_nueva_" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1";
        try (Connection connection = DatabaseConnection.getConnection(url)) {
            assertTrue(inicializacion.CargarDatosEjemplo.estaVacia(connection));
        }
    }

    @Test
    void reabrirLaBaseNoReiniciaElPresupuestoDisponible() throws Exception {
        String url = DatosCampanaPrueba.crearBase();
        DatosCampanaPrueba.fijarPresupuesto(url, "750000.00");

        DatabaseConnection.reiniciarInicializacion();
        try (Connection connection = DatabaseConnection.getConnection(url);
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT COUNT(*) FROM Presupuesto_marketing")) {
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                assertEquals(1, result.getInt(1));
            }
        }
        assertEquals(new java.math.BigDecimal("750000.00"),
                new RepositoryMarketing(url).obtenerPresupuestoDisponible());
    }

    @Test
    void lasCampanasSinSegmentoNoAparecenEnElListadoDelCU22() throws Exception {
        String url = DatosCampanaPrueba.crearBase();
        try (Connection connection = DatabaseConnection.getConnection(url);
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO Campana (nombre, descripcion, fecha_inicio, fecha_fin) "
                             + "VALUES ('Campaña de proveedor', 'Sin segmento', "
                             + "TIMESTAMP '2026-11-01 00:00:00', TIMESTAMP '2026-12-31 23:59:59')")) {
            statement.executeUpdate();
        }

        assertTrue(new RepositoryMarketing(url).listarCampanas().isEmpty());
    }
}
