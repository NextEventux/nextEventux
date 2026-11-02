
package repositories;

import config.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Acceso a datos del modulo de Administracion.
 * CU-029: consulta, suspension y reactivacion de usuarios.
 */
public class RepositoryAdministracion {

    private final String url;

    public RepositoryAdministracion() {
        this(null);
    }

    public RepositoryAdministracion(String url) {
        this.url = url;
    }

    /**
     * Resumen de usuario para la consulta administrativa.
     * Nunca expone la contrasena.
     */
    public record UsuarioResumen(
            int idUsuario,
            String nombre,
            String usuario,
            String tipoUsuario,
            String estado,
            Timestamp fechaRegistro) {
    }

    /**
     * Abre una conexion utilizando la configuracion compartida.
     */
    public Connection abrirConexion() throws SQLException {
        return url == null
                ? DatabaseConnection.getConnection()
                : DatabaseConnection.getConnection(url);
    }

    /**
     * Consulta los usuarios registrados.
     */
    public List<UsuarioResumen> listarUsuarios(
            Connection connection,
            String filtro) throws SQLException {

        String sql = """
                SELECT id_usuario, nombre, usuario,
                       tipo_usuario, estado, fecha_registro
                FROM Usuario
                WHERE (? = ''
                    OR UPPER(nombre) LIKE ?
                    OR UPPER(usuario) LIKE ?
                    OR CAST(id_usuario AS VARCHAR) = ?)
                ORDER BY nombre
                """;

        List<UsuarioResumen> usuarios = new ArrayList<>();

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            String texto = filtro == null ? "" : filtro.trim();
            String busqueda = "%" + texto.toUpperCase() + "%";

            statement.setString(1, texto);
            statement.setString(2, busqueda);
            statement.setString(3, busqueda);
            statement.setString(4, texto);

            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    usuarios.add(new UsuarioResumen(
                            result.getInt("id_usuario"),
                            result.getString("nombre"),
                            result.getString("usuario"),
                            result.getString("tipo_usuario"),
                            result.getString("estado"),
                            result.getTimestamp("fecha_registro")
                    ));
                }
            }
        }

        return usuarios;
    }

    /**
     * Bloquea la fila del usuario durante la operacion.
     */
    public Optional<UsuarioResumen> bloquearUsuario(
            Connection connection,
            int idUsuario) throws SQLException {

        String sql = """
                SELECT id_usuario, nombre, usuario,
                       tipo_usuario, estado, fecha_registro
                FROM Usuario
                WHERE id_usuario = ?
                FOR UPDATE
                """;

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, idUsuario);

            try (ResultSet result = statement.executeQuery()) {

                if (!result.next()) {
                    return Optional.empty();
                }

                return Optional.of(new UsuarioResumen(
                        result.getInt("id_usuario"),
                        result.getString("nombre"),
                        result.getString("usuario"),
                        result.getString("tipo_usuario"),
                        result.getString("estado"),
                        result.getTimestamp("fecha_registro")
                ));
            }
        }
    }

    /**
     * Suspende exclusivamente cuentas activas.
     */
    public int actualizarEstadoUsuario(
            Connection connection,
            int idUsuario) throws SQLException {

        String sql = """
                UPDATE Usuario
                SET estado = 'SUSPENDIDO'
                WHERE id_usuario = ?
                  AND UPPER(estado) = 'ACTIVO'
                """;

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, idUsuario);
            return statement.executeUpdate();
        }
    }

    /**
     * Registra el motivo y la trazabilidad de la suspension.
     */
    public void registrarSuspension(
            Connection connection,
            int idUsuario,
            int idAdministrador,
            String motivo) throws SQLException {

        String sql = """
                INSERT INTO Suspension_usuario
                    (id_usuario, id_administrador, motivo)
                VALUES (?, ?, ?)
                """;

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, idUsuario);
            statement.setInt(2, idAdministrador);
            statement.setString(3, motivo);
            statement.executeUpdate();
        }
    }

    /**
     * Registra una notificacion interna.
     */
    public void notificarUsuario(
            Connection connection,
            int idUsuario,
            String mensaje) throws SQLException {

        String sql = """
                INSERT INTO Notificacion_interna
                    (id_usuario, mensaje)
                VALUES (?, ?)
                """;

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, idUsuario);
            statement.setString(2, mensaje);
            statement.executeUpdate();
        }
    }

    // ==========================================================
    // CU-029: REACTIVACION DE USUARIOS
    // ==========================================================

    /**
     * Reactiva exclusivamente cuentas suspendidas.
     *
     * Retorna 1 si la cuenta fue reactivada.
     */
    public int actualizarEstadoUsuarioReactivacion(
            Connection connection,
            int idUsuario) throws SQLException {

        String sql = """
                UPDATE Usuario
                SET estado = 'ACTIVO'
                WHERE id_usuario = ?
                  AND UPPER(estado) = 'SUSPENDIDO'
                """;

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, idUsuario);
            return statement.executeUpdate();
        }
    }

    /**
     * Registra quien reactivo la cuenta y cuando.
     * El motivo es opcional.
     */
    public void registrarReactivacion(
            Connection connection,
            int idUsuario,
            int idAdministrador,
            String motivo) throws SQLException {

        String sql = """
                INSERT INTO Reactivacion_usuario
                    (id_usuario, id_administrador, motivo)
                VALUES (?, ?, ?)
                """;

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, idUsuario);
            statement.setInt(2, idAdministrador);

            if (motivo == null || motivo.isBlank()) {
                statement.setNull(3, java.sql.Types.VARCHAR);
            } else {
                statement.setString(3, motivo.trim());
            }

            statement.executeUpdate();
        }
    }

    /**
     * Cuenta los administradores que permanecen activos.
     * Se utiliza para proteger el acceso administrativo.
     */
    public int contarAdministradoresActivos(
            Connection connection) throws SQLException {

        String sql = """
                SELECT COUNT(*)
                FROM Usuario
                WHERE UPPER(tipo_usuario) = 'ADMINISTRADOR'
                  AND UPPER(estado) = 'ACTIVO'
                """;

        try (PreparedStatement statement =
                     connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {

            if (result.next()) {
                return result.getInt(1);
            }

            return 0;
        }
    }
}
