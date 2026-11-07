package repositories;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import config.DatabaseConnection;

/**
 * Acceso a datos del modulo de administracion para CU-029
 * (suspender usuario) y su ampliacion (reactivar usuario).
 *
 * Todos los metodos reciben la conexion para que el servicio controle
 * la transaccion completa (estado, historial y notificacion).
 */
public class RepositoryAdministracion {

    /** Datos de una cuenta que el administrador puede consultar. */
    public record UsuarioAdministrable(
            int idUsuario,
            String nombre,
            String usuario,
            String tipoUsuario,
            String estado,
            LocalDateTime fechaRegistro) {

        @Override
        public String toString() {
            return nombre + " (" + usuario + ")";
        }
    }

    private static final String COLUMNAS_USUARIO =
            "id_usuario, nombre, usuario, tipo_usuario, estado, fecha_registro";

    private final String url;

    public RepositoryAdministracion() {
        this(null);
    }

    /** Con url nula se usa la base de la aplicacion; las pruebas pasan una base en memoria. */
    public RepositoryAdministracion(String url) {
        this.url = url;
    }

    public Connection abrirConexion() throws SQLException {
        return url == null
                ? DatabaseConnection.getConnection()
                : DatabaseConnection.getConnection(url);
    }

    /** Busca por nombre o nombre de usuario; likeUpper ya viene en mayusculas y con comodines. */
    public List<UsuarioAdministrable> listarUsuarios(Connection c, String likeUpper)
            throws SQLException {
        String sql = "SELECT " + COLUMNAS_USUARIO + " FROM Usuario "
                + "WHERE UPPER(nombre) LIKE ? OR UPPER(usuario) LIKE ? "
                + "ORDER BY nombre, id_usuario FETCH FIRST 100 ROWS ONLY";
        List<UsuarioAdministrable> usuarios = new ArrayList<>();
        try (PreparedStatement s = c.prepareStatement(sql)) {
            s.setString(1, likeUpper);
            s.setString(2, likeUpper);
            try (ResultSet r = s.executeQuery()) {
                while (r.next()) {
                    usuarios.add(leer(r));
                }
            }
        }
        return usuarios;
    }

    public Optional<UsuarioAdministrable> buscarUsuario(Connection c, int idUsuario)
            throws SQLException {
        return consultarUno(c, "SELECT " + COLUMNAS_USUARIO
                + " FROM Usuario WHERE id_usuario = ?", idUsuario);
    }

    /**
     * Bloquea todas las cuentas de administrador, en orden de clave primaria,
     * hasta el final de la transaccion. Todas las operaciones de CU-029 lo
     * llaman primero; asi se serializan entre si y siempre en el mismo orden.
     */
    public void bloquearAdministradores(Connection c) throws SQLException {
        String sql = "SELECT id_usuario FROM Usuario "
                + "WHERE UPPER(tipo_usuario) = 'ADMINISTRADOR' "
                + "ORDER BY id_usuario FOR UPDATE";
        try (PreparedStatement s = c.prepareStatement(sql);
             ResultSet r = s.executeQuery()) {
            while (r.next()) {
                // Solo se recorren las filas para tomar los bloqueos.
            }
        }
    }

    /** Bloquea la cuenta indicada hasta el final de la transaccion. */
    public Optional<UsuarioAdministrable> bloquearUsuario(Connection c, int idUsuario)
            throws SQLException {
        return consultarUno(c, "SELECT " + COLUMNAS_USUARIO
                + " FROM Usuario WHERE id_usuario = ? FOR UPDATE", idUsuario);
    }

    public int contarAdministradoresActivos(Connection c) throws SQLException {
        String sql = "SELECT COUNT(*) FROM Usuario "
                + "WHERE UPPER(tipo_usuario) = 'ADMINISTRADOR' AND UPPER(estado) = 'ACTIVO'";
        try (PreparedStatement s = c.prepareStatement(sql);
             ResultSet r = s.executeQuery()) {
            r.next();
            return r.getInt(1);
        }
    }

    public int contarSuspensionesVigentes(Connection c, int idUsuario) throws SQLException {
        String sql = "SELECT COUNT(*) FROM Historial_suspension_usuario "
                + "WHERE id_usuario = ? AND fecha_reactivacion IS NULL";
        try (PreparedStatement s = c.prepareStatement(sql)) {
            s.setInt(1, idUsuario);
            try (ResultSet r = s.executeQuery()) {
                r.next();
                return r.getInt(1);
            }
        }
    }

    public Optional<String> motivoSuspensionVigente(Connection c, int idUsuario)
            throws SQLException {
        String sql = "SELECT motivo FROM Historial_suspension_usuario "
                + "WHERE id_usuario = ? AND fecha_reactivacion IS NULL";
        try (PreparedStatement s = c.prepareStatement(sql)) {
            s.setInt(1, idUsuario);
            try (ResultSet r = s.executeQuery()) {
                return r.next() ? Optional.of(r.getString(1)) : Optional.empty();
            }
        }
    }

    /**
     * Cambia el estado solo si la cuenta sigue en el estado esperado.
     *
     * @return filas actualizadas (1 si el cambio se aplico)
     */
    public int actualizarEstadoUsuario(Connection c, int idUsuario,
            String estadoNuevo, String estadoEsperado) throws SQLException {
        String sql = "UPDATE Usuario SET estado = ? "
                + "WHERE id_usuario = ? AND UPPER(estado) = ?";
        try (PreparedStatement s = c.prepareStatement(sql)) {
            s.setString(1, estadoNuevo);
            s.setInt(2, idUsuario);
            s.setString(3, estadoEsperado);
            return s.executeUpdate();
        }
    }

    public void registrarSuspension(Connection c, int idUsuario, int idAdministrador,
            String motivo) throws SQLException {
        String sql = "INSERT INTO Historial_suspension_usuario "
                + "(id_usuario, id_administrador, motivo) VALUES (?, ?, ?)";
        try (PreparedStatement s = c.prepareStatement(sql)) {
            s.setInt(1, idUsuario);
            s.setInt(2, idAdministrador);
            s.setString(3, motivo);
            s.executeUpdate();
        }
    }

    /**
     * Cierra la suspension vigente sin borrarla.
     *
     * @return filas cerradas (debe ser exactamente 1)
     */
    public int cerrarSuspensionVigente(Connection c, int idUsuario, int idAdministrador)
            throws SQLException {
        String sql = "UPDATE Historial_suspension_usuario "
                + "SET id_administrador_reactivacion = ?, fecha_reactivacion = CURRENT_TIMESTAMP "
                + "WHERE id_usuario = ? AND fecha_reactivacion IS NULL";
        try (PreparedStatement s = c.prepareStatement(sql)) {
            s.setInt(1, idAdministrador);
            s.setInt(2, idUsuario);
            return s.executeUpdate();
        }
    }

    /** Notificacion interna dirigida a una cuenta, con el mismo esquema de CU-03. */
    public void notificar(Connection c, int idUsuario, String mensaje) throws SQLException {
        try (PreparedStatement s = c.prepareStatement(
                "INSERT INTO Notificacion_interna (id_usuario, mensaje) VALUES (?, ?)")) {
            s.setInt(1, idUsuario);
            s.setString(2, mensaje);
            s.executeUpdate();
        }
    }

    private Optional<UsuarioAdministrable> consultarUno(Connection c, String sql, int idUsuario)
            throws SQLException {
        try (PreparedStatement s = c.prepareStatement(sql)) {
            s.setInt(1, idUsuario);
            try (ResultSet r = s.executeQuery()) {
                return r.next() ? Optional.of(leer(r)) : Optional.empty();
            }
        }
    }

    private static UsuarioAdministrable leer(ResultSet r) throws SQLException {
        Timestamp registro = r.getTimestamp("fecha_registro");
        return new UsuarioAdministrable(
                r.getInt("id_usuario"),
                r.getString("nombre"),
                r.getString("usuario"),
                r.getString("tipo_usuario"),
                r.getString("estado"),
                registro == null ? null : registro.toLocalDateTime());
    }
}
