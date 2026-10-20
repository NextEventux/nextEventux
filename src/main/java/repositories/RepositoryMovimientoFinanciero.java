package repositories;

import config.DatabaseConnection;
import entities.MovimientoFinanciero;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class RepositoryMovimientoFinanciero {

    /**
     * Consulta todos los movimientos financieros asociados a un evento desde la base de datos H2.
     */
    public List<MovimientoFinanciero> listarMovimientosPorEvento(int idEvento) {
        List<MovimientoFinanciero> lista = new ArrayList<>();
        String sql = "SELECT * FROM Movimiento_financiero WHERE id_evento = ? ORDER BY fecha DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, idEvento);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapearMovimiento(rs));
                }
            }
        } catch (SQLException e) {
            // Se propaga el error para que la vista lo muestre en vez de pintar una lista vacía.
            throw new RuntimeException("No se pudieron consultar los movimientos financieros.", e);
        }

        return lista;
    }

    // Busca un movimiento financiero específico por su ID.
    public MovimientoFinanciero buscarMovimiento(int idMovimiento) {
        String sql = "SELECT * FROM Movimiento_financiero WHERE id_movimiento = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, idMovimiento);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapearMovimiento(rs);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("No se pudo consultar el movimiento financiero.", e);
        }

        return null;
    }

    /**
     * Método auxiliar para convertir una fila del ResultSet a un objeto MovimientoFinanciero.
     */
    private MovimientoFinanciero mapearMovimiento(ResultSet rs) throws SQLException {
        MovimientoFinanciero mov = new MovimientoFinanciero();
        mov.setIdMovimiento(rs.getInt("id_movimiento"));
        mov.setIdPropuesta(rs.getInt("id_propuesta"));
        mov.setIdEvento(rs.getInt("id_evento"));
        mov.setIdContratacion(rs.getInt("id_contratacion"));
        mov.setValor(rs.getDouble("valor"));
        Timestamp fecha = rs.getTimestamp("fecha");
        mov.setFecha(fecha != null ? fecha.toLocalDateTime() : null);
        mov.setDescripcion(rs.getString("descripcion"));
        mov.setTipo(rs.getString("tipo"));
        mov.setEstado(rs.getString("estado"));
        return mov;
    }
}