package repositories;

import config.DatabaseConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RepositoryCancelacion {
    private final String url;
    public RepositoryCancelacion() { this(null); }
    public RepositoryCancelacion(String url) { this.url = url; }
    private Connection conectar() throws SQLException {
        return url == null ? DatabaseConnection.getConnection() : DatabaseConnection.getConnection(url);
    }

    public void cancelar(int evento, int organizador) {
        try (Connection connection = conectar()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement consulta = connection.prepareStatement(
                        "SELECT estado FROM Evento WHERE id_evento=? AND id_organizador=? FOR UPDATE")) {
                    consulta.setInt(1, evento); consulta.setInt(2, organizador);
                    try (ResultSet result = consulta.executeQuery()) {
                        if (!result.next()) throw new IllegalArgumentException("Selecciona un evento existente de tu cuenta.");
                        String estado = result.getString(1);
                        if ("CANCELADO".equalsIgnoreCase(estado) || "FINALIZADO".equalsIgnoreCase(estado))
                            throw new IllegalArgumentException("No puedes cancelar un evento cancelado o finalizado.");
                    }
                }
                comprobarReservas(connection, evento);
                // Bloquear también los recursos evita liberaciones incoherentes entre cancelaciones simultáneas.
                actualizar(connection, "UPDATE Disponibilidad_Servicio SET estado=estado WHERE id_disponibilidad IN "
                        + "(SELECT id_disponibilidad FROM Contratacion WHERE id_evento=? AND estado='ACTIVA')", evento);
                actualizar(connection, "UPDATE Lugar SET estado=estado WHERE id_lugar IN (SELECT id_lugar FROM Evento WHERE id_evento=?)", evento);
                // Libera solo las reservas de este evento que no tengan otro compromiso activo.
                actualizar(connection, "UPDATE Disponibilidad_Servicio d SET estado='DISPONIBLE' WHERE "
                        + "d.id_disponibilidad IN (SELECT id_disponibilidad FROM Contratacion WHERE id_evento=? AND estado='ACTIVA') "
                        + "AND NOT EXISTS (SELECT 1 FROM Contratacion c WHERE c.id_disponibilidad=d.id_disponibilidad "
                        + "AND c.estado='ACTIVA' AND c.id_evento<>?)", evento, evento);
                actualizar(connection, "UPDATE Lugar l SET estado='DISPONIBLE' WHERE l.id_lugar IN "
                        + "(SELECT id_lugar FROM Evento WHERE id_evento=?) AND NOT EXISTS "
                        + "(SELECT 1 FROM Evento e WHERE e.id_lugar=l.id_lugar AND e.id_evento<>? "
                        + "AND UPPER(e.estado) NOT IN ('CANCELADO','FINALIZADO'))", evento, evento);
                actualizar(connection, "INSERT INTO Notificacion_interna (id_usuario,id_evento,mensaje) "
                        + "SELECT DISTINCT p.id_usuario,?, 'Se canceló un evento con un servicio contratado. Consulta tus contrataciones.' "
                        + "FROM Contratacion c JOIN Propuesta pr ON pr.id_propuesta=c.id_propuesta "
                        + "JOIN Solicitud_Ejecucion s ON s.id_solicitud=pr.id_solicitud "
                        + "JOIN Proveedor p ON p.id_proveedor=s.id_proveedor WHERE c.id_evento=? AND c.estado='ACTIVA'", evento, evento);
                actualizar(connection, "INSERT INTO Notificacion_interna (id_invitado,id_evento,mensaje) "
                        + "SELECT g.id_invitado,?, 'El evento al que estabas invitado fue cancelado. Tu invitación quedó revocada.' "
                        + "FROM Invitado g JOIN Invitacion i ON i.id_invitacion=g.id_invitacion "
                        + "WHERE i.id_evento=? AND (i.estado_respuesta IS NULL OR i.estado_respuesta NOT IN ('BORRADOR','REVOCADA'))", evento, evento);
                actualizar(connection, "UPDATE Contratacion SET estado='CANCELADA' WHERE id_evento=? AND estado='ACTIVA'", evento);
                actualizar(connection, "UPDATE Invitacion SET estado_respuesta='REVOCADA' WHERE id_evento=? "
                        + "AND (estado_respuesta IS NULL OR estado_respuesta NOT IN ('BORRADOR','REVOCADA'))", evento);
                actualizar(connection, "UPDATE Evento SET estado='CANCELADO' WHERE id_evento=?", evento);
                connection.commit();
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo completar la cancelación. No se guardaron cambios; vuelve a intentarlo.", e);
        }
    }

    private void comprobarReservas(Connection connection, int evento) throws SQLException {
        String sql = "SELECT c.id_contratacion FROM Contratacion c JOIN Propuesta p ON p.id_propuesta=c.id_propuesta "
                + "JOIN Solicitud_Ejecucion s ON s.id_solicitud=p.id_solicitud "
                + "LEFT JOIN Disponibilidad_Servicio d ON d.id_disponibilidad=c.id_disponibilidad "
                + "LEFT JOIN Servicio v ON v.id_servicio=d.id_servicio "
                + "WHERE c.id_evento=? AND c.estado='ACTIVA' "
                + "AND (d.id_disponibilidad IS NULL OR v.id_proveedor<>s.id_proveedor OR s.id_evento<>c.id_evento)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, evento);
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) throw new IllegalArgumentException(
                        "Hay una contratación sin una reserva válida. Revisa su disponibilidad antes de cancelar.");
            }
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT i.id_lugar FROM Invitacion i JOIN Lugar l ON l.id_lugar=i.id_lugar "
                + "JOIN Evento e ON e.id_evento=i.id_evento WHERE e.id_evento=? AND (e.id_lugar IS NULL OR e.id_lugar<>i.id_lugar) AND l.estado='RESERVADO'")) {
            statement.setInt(1, evento);
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) throw new IllegalArgumentException("Vincula la reserva del lugar al evento antes de cancelar.");
            }
        }
    }

    private void actualizar(Connection connection, String sql, int... valores) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i=0; i<valores.length; i++) statement.setInt(i+1,valores[i]);
            statement.executeUpdate();
        }
    }

    public List<String> listarAvisos(int evento, int organizador) {
        List<String> avisos = new ArrayList<>();
        try (Connection connection=conectar(); PreparedStatement statement=connection.prepareStatement(
                "SELECT n.mensaje FROM Notificacion_interna n JOIN Evento e ON e.id_evento=n.id_evento "
                + "WHERE n.id_evento=? AND e.id_organizador=? ORDER BY n.id_notificacion")) {
            statement.setInt(1,evento); statement.setInt(2,organizador);
            try (ResultSet result=statement.executeQuery()) { while(result.next()) avisos.add(result.getString(1)); }
            return avisos;
        } catch (SQLException e) { throw new IllegalStateException("No se pudieron cargar los avisos. Vuelve a intentarlo.",e); }
    }
}
