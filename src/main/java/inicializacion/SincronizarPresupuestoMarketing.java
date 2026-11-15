package inicializacion;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Crea el presupuesto de marketing inicial en bases H2 que todavía no lo tienen.
 * Una fila existente, incluido un disponible igual a cero, nunca se modifica.
 */
public final class SincronizarPresupuestoMarketing {

    public static final BigDecimal PRESUPUESTO_INICIAL = new BigDecimal("20000000.00");

    private SincronizarPresupuestoMarketing() {
    }

    /**
     * @return true si creó la fila; false si ya existía y no se tocó.
     */
    public static synchronized boolean sincronizar(Connection conexion) throws SQLException {
        String sql = "INSERT INTO Presupuesto_marketing (id_presupuesto, disponible) "
                + "SELECT 1, CAST(? AS DECIMAL(12,2)) FROM DUAL "
                + "WHERE NOT EXISTS (SELECT 1 FROM Presupuesto_marketing WHERE id_presupuesto = 1)";
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setBigDecimal(1, PRESUPUESTO_INICIAL);
            return sentencia.executeUpdate() > 0;
        } catch (SQLException error) {
            if (esClaveDuplicada(error) && existePresupuestoInicial(conexion)) {
                return false;
            }
            throw error;
        }
    }

    private static boolean existePresupuestoInicial(Connection conexion) throws SQLException {
        try (PreparedStatement consulta = conexion.prepareStatement(
                "SELECT 1 FROM Presupuesto_marketing WHERE id_presupuesto = 1");
                ResultSet resultado = consulta.executeQuery()) {
            return resultado.next();
        }
    }

    private static boolean esClaveDuplicada(SQLException error) {
        for (SQLException actual = error; actual != null; actual = actual.getNextException()) {
            if ("23505".equals(actual.getSQLState())) {
                return true;
            }
        }
        return false;
    }
}
