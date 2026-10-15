package repositories;

import config.DatabaseConnection;
import entities.Proveedor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Acceso a la persistencia de proveedores para el CU-028. */
public class RepositoryProveedor {
    private final String url;

    public RepositoryProveedor() {
        this(null);
    }

    /** Permite usar una base H2 aislada en las pruebas. */
    public RepositoryProveedor(String url) {
        this.url = url;
    }

    private Connection conectar() throws SQLException {
        return url == null ? DatabaseConnection.getConnection() : DatabaseConnection.getConnection(url);
    }

    /** Obtiene un proveedor por su identificador; retorna null si no existe. */
    public Proveedor buscarProveedor(int idProveedor) {
        String sql = "SELECT id_proveedor, id_usuario, nombre, empresa, correo, telefono, direccion, estado, motivo_rechazo "
                + "FROM Proveedor WHERE id_proveedor = ?";
        try (Connection connection = conectar();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, idProveedor);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? mapearProveedor(result) : null;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo consultar el proveedor. Revisa la conexión con la base de datos.", e);
        }
    }

    /** Lista los registros que se encuentran pendientes de verificación. */
    public List<Proveedor> listarProveedoresPendientes() {
        List<Proveedor> proveedores = new ArrayList<>();
        String sql = "SELECT id_proveedor, id_usuario, nombre, empresa, correo, telefono, direccion, estado, motivo_rechazo "
                + "FROM Proveedor WHERE " + condicionPendiente("estado") + " ORDER BY id_proveedor";
        try (Connection connection = conectar();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                proveedores.add(mapearProveedor(result));
            }
            return proveedores;
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudieron consultar los proveedores pendientes.", e);
        }
    }

    /** Actualiza el estado únicamente si el proveedor todavía está pendiente. */
    public void actualizarEstadoProveedor(int idProveedor, String estado) {
        if (estado == null || estado.isBlank()) {
            throw new IllegalArgumentException("El estado del proveedor es obligatorio.");
        }
        String sql = "UPDATE Proveedor SET estado = ?, motivo_rechazo = NULL WHERE id_proveedor = ? AND "
                + condicionPendiente("estado");
        actualizarEstadoPendiente(idProveedor, estado, null, sql, false);
    }

    /** Rechaza un proveedor y conserva el motivo en la misma operación. */
    public void rechazarProveedor(int idProveedor, String motivo) {
        if (motivo == null || motivo.isBlank()) {
            throw new IllegalArgumentException("Indica el motivo del rechazo.");
        }
        String sql = "UPDATE Proveedor SET estado = ?, motivo_rechazo = ? WHERE id_proveedor = ? AND "
                + condicionPendiente("estado");
        actualizarEstadoPendiente(idProveedor, Proveedor.ESTADO_RECHAZADO, motivo.trim(), sql, true);
    }

    private void actualizarEstadoPendiente(int idProveedor, String estado, String motivo,
                                           String sql, boolean incluyeMotivo) {
        try (Connection connection = conectar()) {
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                int indice = 1;
                statement.setString(indice++, estado);
                if (incluyeMotivo) {
                    statement.setString(indice++, motivo);
                }
                statement.setInt(indice, idProveedor);
                int actualizados = statement.executeUpdate();
                if (actualizados != 1) {
                    connection.rollback();
                    throw new IllegalArgumentException(
                            "El proveedor no existe o ya no está pendiente de verificación. Actualiza la lista e inténtalo de nuevo.");
                }
                connection.commit();
            } catch (SQLException | RuntimeException e) {
                try {
                    connection.rollback();
                } catch (SQLException rollback) {
                    e.addSuppressed(rollback);
                }
                throw e;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo actualizar el estado del proveedor.", e);
        }
    }

    private String condicionPendiente(String columna) {
        // Acepta la representación utilizada por documentos previos y la forma canónica actual.
        return "UPPER(REPLACE(REPLACE(TRIM(" + columna + "), ' ', '_'), '-', '_')) IN "
                + "('PENDIENTE', 'PENDIENTE_VERIFICACION', 'PENDIENTE_DE_VERIFICACION', "
                + "'PENDIENTE_REVISION', 'PENDIENTE_DE_REVISION', 'PENDIENTE_VERIFICACIÓN', 'PENDIENTE_DE_VERIFICACIÓN')";
    }

    private Proveedor mapearProveedor(ResultSet result) throws SQLException {
        return new Proveedor(
                result.getInt("id_proveedor"),
                result.getInt("id_usuario"),
                result.getString("nombre"),
                result.getString("empresa"),
                result.getString("correo"),
                result.getString("telefono"),
                result.getString("direccion"),
                result.getString("estado"),
                result.getString("motivo_rechazo"));
    }
}
