package repositories;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import config.DatabaseConnection;
import entities.SolicitudEjecucion;

public class RepositorySolicitudEjecucion {

    public List<SolicitudEjecucion> listarPorProveedor(int idProveedor) {

        List<SolicitudEjecucion> solicitudes = new ArrayList<>();

        String sql = """
                SELECT id_solicitud,
                       id_proveedor,
                       id_evento,
                       id_servicio,
                       fecha_solicitud,
                       estado,
                       motivo_rechazo,
                       fecha_respuesta
                FROM Solicitud_Ejecucion
                WHERE id_proveedor = ?
                ORDER BY fecha_solicitud DESC
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, idProveedor);

            try (ResultSet result = statement.executeQuery()) {

                while (result.next()) {

                    SolicitudEjecucion solicitud = new SolicitudEjecucion();

                    solicitud.setIdSolicitud(result.getInt("id_solicitud"));
                    solicitud.setIdProveedor(result.getInt("id_proveedor"));
                    solicitud.setIdEvento(result.getInt("id_evento"));
                    solicitud.setIdServicio(result.getInt("id_servicio"));

                    solicitud.setFechaSolicitud(
                            result.getTimestamp("fecha_solicitud").toLocalDateTime());

                    solicitud.setEstado(result.getString("estado"));
                    solicitud.setMotivoRechazo(result.getString("motivo_rechazo"));

                    if (result.getTimestamp("fecha_respuesta") != null) {
                        solicitud.setFechaRespuesta(
                                result.getTimestamp("fecha_respuesta").toLocalDateTime());
                    }

                    solicitudes.add(solicitud);
                }
            }

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "No se pudieron consultar las solicitudes del proveedor.", e);
        }

        return solicitudes;
    }

    public boolean rechazarSolicitud(
            int idProveedor,
            int idSolicitud,
            String motivo) {

        String sql = """
                UPDATE Solicitud_Ejecucion
                SET estado = 'RECHAZADA',
                    motivo_rechazo = ?,
                    fecha_respuesta = CURRENT_TIMESTAMP
                WHERE id_solicitud = ?
                  AND id_proveedor = ?
                  AND estado = 'PENDIENTE'
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, motivo);
            statement.setInt(2, idSolicitud);
            statement.setInt(3, idProveedor);

            return statement.executeUpdate() == 1;

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "No se pudo rechazar la solicitud.", e);
        }
    }
}