package repositories;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import config.DatabaseConnection;

/**
 * Acceso a datos de CU-09 (aplicar sanciones a proveedores).
 *
 * No cambia el esquema. Las sanciones se guardan en la tabla Sancion, que solo
 * tiene id_evento, motivo, valor, estado y fecha. El proveedor, el tipo de falta
 * y el contrato afectado van en el campo motivo con un formato fijo
 * (ver ServiceSancionesProveedor.construirMotivo).
 */
public class RepositorySancionesProveedor {

    public record ResumenProveedor(int idProveedor, String nombre, String empresa, String estado) { }

    public record ProveedorSancionable(int idProveedor, int idUsuario, String nombre, String estado) { }

    public record EventoProveedor(int idEvento, String nombre) { }

    public record ContratoSancionable(int idContratacion, int idEvento, BigDecimal valorAcordado, String estado) { }

    private final String url;

    public RepositorySancionesProveedor() {
        this(null);
    }

    /** Con url nula se usa la base de la aplicación; las pruebas pasan una base en memoria. */
    public RepositorySancionesProveedor(String url) {
        this.url = url;
    }

    public Connection abrirConexion() throws SQLException {
        return url == null
                ? DatabaseConnection.getConnection()
                : DatabaseConnection.getConnection(url);
    }

    public List<ResumenProveedor> buscarProveedores(Connection c, String likeUpper, int idNumerico)
            throws SQLException {
        String sql = "SELECT id_proveedor, nombre, empresa, estado FROM Proveedor "
                + "WHERE id_proveedor = ? OR UPPER(nombre) LIKE ? "
                + "ORDER BY nombre FETCH FIRST 20 ROWS ONLY";
        List<ResumenProveedor> resultado = new ArrayList<>();
        try (PreparedStatement s = c.prepareStatement(sql)) {
            s.setInt(1, idNumerico);
            s.setString(2, likeUpper);
            try (ResultSet r = s.executeQuery()) {
                while (r.next()) {
                    resultado.add(new ResumenProveedor(
                            r.getInt("id_proveedor"),
                            r.getString("nombre"),
                            r.getString("empresa"),
                            r.getString("estado")));
                }
            }
        }
        return resultado;
    }

    /** Bloquea la fila del proveedor hasta el final de la transaccion. */
    public Optional<ProveedorSancionable> bloquearProveedor(Connection c, int idProveedor)
            throws SQLException {
        String sql = "SELECT id_proveedor, id_usuario, nombre, estado FROM Proveedor "
                + "WHERE id_proveedor = ? FOR UPDATE";
        try (PreparedStatement s = c.prepareStatement(sql)) {
            s.setInt(1, idProveedor);
            try (ResultSet r = s.executeQuery()) {
                if (!r.next()) {
                    return Optional.empty();
                }
                return Optional.of(new ProveedorSancionable(
                        r.getInt("id_proveedor"),
                        r.getInt("id_usuario"),
                        r.getString("nombre"),
                        r.getString("estado")));
            }
        }
    }

    /** Eventos en los que el proveedor tiene al menos una solicitud. */
    public List<EventoProveedor> eventosDelProveedor(Connection c, int idProveedor)
            throws SQLException {
        String sql = "SELECT DISTINCT e.id_evento, e.nombre FROM Solicitud_Ejecucion s "
                + "JOIN Evento e ON e.id_evento = s.id_evento "
                + "WHERE s.id_proveedor = ? ORDER BY e.nombre";
        List<EventoProveedor> eventos = new ArrayList<>();
        try (PreparedStatement s = c.prepareStatement(sql)) {
            s.setInt(1, idProveedor);
            try (ResultSet r = s.executeQuery()) {
                while (r.next()) {
                    eventos.add(new EventoProveedor(r.getInt("id_evento"), r.getString("nombre")));
                }
            }
        }
        return eventos;
    }

    /** Contratos activos del proveedor en un evento. Si bloquear es true, bloquea las filas. */
    public List<ContratoSancionable> contratosActivos(
            Connection c, int idProveedor, int idEvento, Integer idContratacion, boolean bloquear)
            throws SQLException {
        StringBuilder sql = new StringBuilder(
                "SELECT c.id_contratacion, c.id_evento, c.valor_acordado, c.estado "
                + "FROM Contratacion c "
                + "JOIN Propuesta p ON p.id_propuesta = c.id_propuesta "
                + "JOIN Solicitud_Ejecucion s ON s.id_solicitud = p.id_solicitud "
                + "WHERE s.id_proveedor = ? AND c.id_evento = ? "
                + "AND UPPER(c.estado) IN ('ACTIVA', 'CONFIRMADA')");
        if (idContratacion != null) {
            sql.append(" AND c.id_contratacion = ?");
        }
        sql.append(" ORDER BY c.id_contratacion");
        if (bloquear) {
            sql.append(" FOR UPDATE");
        }
        List<ContratoSancionable> contratos = new ArrayList<>();
        try (PreparedStatement s = c.prepareStatement(sql.toString())) {
            s.setInt(1, idProveedor);
            s.setInt(2, idEvento);
            if (idContratacion != null) {
                s.setInt(3, idContratacion);
            }
            try (ResultSet r = s.executeQuery()) {
                while (r.next()) {
                    contratos.add(new ContratoSancionable(
                            r.getInt("id_contratacion"),
                            r.getInt("id_evento"),
                            r.getBigDecimal("valor_acordado"),
                            r.getString("estado")));
                }
            }
        }
        return contratos;
    }

    /** Suma de las multas ya aplicadas a un contrato del proveedor. */
    public BigDecimal multasAplicadas(Connection c, int idProveedor, int idContratacion)
            throws SQLException {
        String sql = "SELECT COALESCE(SUM(valor), 0) FROM Sancion "
                + "WHERE UPPER(estado) = 'APLICADA' AND motivo LIKE ?";
        String patron = "CU09|MULTA|PROV=" + idProveedor + "|CONTRATO=" + idContratacion + "|%";
        try (PreparedStatement s = c.prepareStatement(sql)) {
            s.setString(1, patron);
            try (ResultSet r = s.executeQuery()) {
                r.next();
                return r.getBigDecimal(1);
            }
        }
    }

    public void insertarSancion(Connection c, int idEvento, String motivo, BigDecimal valor)
            throws SQLException {
        String sql = "INSERT INTO Sancion (id_evento, motivo, valor, estado, fecha) "
                + "VALUES (?, ?, ?, 'APLICADA', CURRENT_TIMESTAMP)";
        try (PreparedStatement s = c.prepareStatement(sql)) {
            s.setInt(1, idEvento);
            s.setString(2, motivo);
            s.setBigDecimal(3, valor);
            s.executeUpdate();
        }
    }

    public void actualizarEstadoProveedor(Connection c, int idProveedor, String estado)
            throws SQLException {
        try (PreparedStatement s = c.prepareStatement(
                "UPDATE Proveedor SET estado = ? WHERE id_proveedor = ?")) {
            s.setString(1, estado);
            s.setInt(2, idProveedor);
            s.executeUpdate();
        }
    }

    /** Notificacion interna, mismo esquema que usa CU-13. */
    public void notificar(Connection c, int idUsuario, int idEvento, String mensaje)
            throws SQLException {
        try (PreparedStatement s = c.prepareStatement(
                "INSERT INTO Notificacion_interna (id_usuario, id_evento, mensaje) VALUES (?, ?, ?)")) {
            s.setInt(1, idUsuario);
            s.setInt(2, idEvento);
            s.setString(3, mensaje);
            s.executeUpdate();
        }
    }
}
