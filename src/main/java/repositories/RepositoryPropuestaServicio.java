package repositories;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Statement;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import config.DatabaseConnection;
import entities.PropuestaServicio;

public class RepositoryPropuestaServicio {

    public List<PropuestaServicio> listarHistorial(
            int idProveedor,
            int idSolicitud) {

        List<PropuestaServicio> propuestas = new ArrayList<>();

        String sql = """
                SELECT p.id_propuesta,
                       p.id_solicitud,
                       p.numero_version,
                       p.valor,
                       p.descripcion,
                       p.condiciones,
                       p.estado,
                       p.vigente,
                       p.fecha_envio
                FROM Propuesta p
                JOIN Solicitud_Ejecucion s
                  ON s.id_solicitud = p.id_solicitud
                WHERE s.id_solicitud = ?
                  AND s.id_proveedor = ?
                ORDER BY p.numero_version DESC
                """;

        try (Connection connection = DatabaseConnection.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, idSolicitud);
            statement.setInt(2, idProveedor);

            try (ResultSet result = statement.executeQuery()) {

                while (result.next()) {

                    PropuestaServicio propuesta = new PropuestaServicio();

                    propuesta.setIdPropuesta(
                            result.getInt("id_propuesta"));

                    propuesta.setIdSolicitud(
                            result.getInt("id_solicitud"));

                    propuesta.setNumeroVersion(
                            result.getInt("numero_version"));

                    propuesta.setValor(
                            result.getBigDecimal("valor"));

                    propuesta.setDescripcion(
                            result.getString("descripcion"));

                    propuesta.setCondiciones(
                            result.getString("condiciones"));

                    propuesta.setEstado(
                            result.getString("estado"));

                    propuesta.setVigente(
                            result.getBoolean("vigente"));

                    Timestamp fecha
                            = result.getTimestamp("fecha_envio");

                    if (fecha != null) {
                        propuesta.setFechaEnvio(fecha.toLocalDateTime());
                    }

                    propuestas.add(propuesta);
                }
            }

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "No se pudo consultar el historial de propuestas.",
                    e);
        }

        return propuestas;
    }

    public int guardarNuevaVersion(
            int idProveedor,
            int idSolicitud,
            BigDecimal valor,
            String descripcion,
            String condiciones) {

        try (Connection connection = DatabaseConnection.getConnection()) {

            connection.setAutoCommit(false);

            try {
                // Bloqueamos la solicitud mientras se prepara la nueva version
                // para que dos operaciones no calculen el mismo numero
                String consultarSolicitud = """
                    SELECT estado
                    FROM Solicitud_Ejecucion
                    WHERE id_solicitud = ?
                      AND id_proveedor = ?
                    FOR UPDATE
                    """;

                String estado;

                try (PreparedStatement statement
                        = connection.prepareStatement(consultarSolicitud)) {

                    statement.setInt(1, idSolicitud);
                    statement.setInt(2, idProveedor);

                    try (ResultSet result = statement.executeQuery()) {

                        if (!result.next()) {
                            throw new IllegalStateException(
                                    "La solicitud no pertenece al proveedor.");
                        }

                        estado = result.getString("estado");
                    }
                }

                if (!"PENDIENTE".equalsIgnoreCase(estado)
                        && !"EN_NEGOCIACION".equalsIgnoreCase(estado)) {

                    throw new IllegalStateException(
                            "La solicitud no permite enviar propuestas.");
                }

                // Calculamos la siguiente version sin borrar las anteriores
                int siguienteVersion = 1;

                String consultarVersion = """
                    SELECT COALESCE(MAX(numero_version), 0) + 1
                    FROM Propuesta
                    WHERE id_solicitud = ?
                    """;

                try (PreparedStatement statement
                        = connection.prepareStatement(consultarVersion)) {

                    statement.setInt(1, idSolicitud);

                    try (ResultSet result = statement.executeQuery()) {

                        if (result.next()) {
                            siguienteVersion = result.getInt(1);
                        }
                    }
                }

                // La version anterior deja de ser la actual
                String desactivarAnterior = """
                    UPDATE Propuesta
                    SET vigente = FALSE
                    WHERE id_solicitud = ?
                      AND vigente = TRUE
                    """;

                try (PreparedStatement statement
                        = connection.prepareStatement(desactivarAnterior)) {

                    statement.setInt(1, idSolicitud);
                    statement.executeUpdate();
                }

                String insertarPropuesta = """
                    INSERT INTO Propuesta
                    (id_solicitud, valor, descripcion, estado,
                     numero_version, condiciones, fecha_envio, vigente)
                    VALUES (?, ?, ?, 'ENVIADA', ?, ?, CURRENT_TIMESTAMP, TRUE)
                    """;

                int idPropuesta;

                try (PreparedStatement statement
                        = connection.prepareStatement(
                                insertarPropuesta,
                                Statement.RETURN_GENERATED_KEYS)) {

                    statement.setInt(1, idSolicitud);
                    statement.setBigDecimal(2, valor);
                    statement.setString(3, descripcion);
                    statement.setInt(4, siguienteVersion);
                    statement.setString(5, condiciones);

                    statement.executeUpdate();

                    try (ResultSet claves = statement.getGeneratedKeys()) {

                        if (!claves.next()) {
                            throw new IllegalStateException(
                                    "No se pudo obtener el ID de la propuesta.");
                        }

                        idPropuesta = claves.getInt(1);
                    }
                }

                String actualizarSolicitud = """
                    UPDATE Solicitud_Ejecucion
                    SET estado = 'EN_NEGOCIACION',
                        fecha_respuesta = COALESCE(
                            fecha_respuesta, CURRENT_TIMESTAMP)
                    WHERE id_solicitud = ?
                      AND id_proveedor = ?
                    """;

                try (PreparedStatement statement
                        = connection.prepareStatement(actualizarSolicitud)) {

                    statement.setInt(1, idSolicitud);
                    statement.setInt(2, idProveedor);
                    statement.executeUpdate();
                }

                // Todos los cambios se confirman juntos
                connection.commit();

                return idPropuesta;

            } catch (SQLException | RuntimeException e) {

                try {
                    connection.rollback();
                } catch (SQLException errorRollback) {
                    e.addSuppressed(errorRollback);
                }

                throw e;
            }

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "No se pudo guardar la propuesta.", e);
        }
    }
}
