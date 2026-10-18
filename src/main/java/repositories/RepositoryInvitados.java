package repositories;

import config.DatabaseConnection;
import entities.InvitacionDTO;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class RepositoryInvitados
{
    public void guardarInvitacion(InvitacionDTO datos) throws SQLException
    {
        String sql = "INSERT INTO Invitacion " + "(id_evento, id_lugar, fecha_envio, mensaje, estado_respuesta) " + "VALUES (?, ?, ?, ?, ?)";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql))
        {
            statement.setInt(1, datos.getIdEvento());
            statement.setInt(2, datos.getIdLugar());
            statement.setTimestamp(3, Timestamp.valueOf(datos.getFechaEnvio()));
            statement.setString(4, datos.getMensaje());
            statement.setString(5, datos.getEstadoRespuesta());

            statement.executeUpdate();
        }
    }

    public InvitacionDTO buscarInvitacion(int idInvitacion) throws SQLException
    {
        String sql = "SELECT id_invitacion, id_evento, id_lugar, " + "fecha_envio, mensaje, estado_respuesta " + "FROM Invitacion WHERE id_invitacion = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql))
        {
            statement.setInt(1, idInvitacion);

            try (ResultSet result = statement.executeQuery())
            {
                if (result.next())
                {
                    return convertirInvitacion(result);
                }
            }
        }

        return null;
    }

    public void actualizarInvitacion(InvitacionDTO invitacion) throws SQLException
    {
        String sql = "UPDATE Invitacion SET " +
                     "id_evento = ?, " +
                     "id_lugar = ?, " +
                     "fecha_envio = ?, " +
                     "mensaje = ?, " +
                     "estado_respuesta = ? " +
                     "WHERE id_invitacion = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql))
        {
            statement.setInt(1, invitacion.getIdEvento());
            statement.setInt(2, invitacion.getIdLugar());
            statement.setTimestamp(3, Timestamp.valueOf(invitacion.getFechaEnvio()));
            statement.setString(4, invitacion.getMensaje());
            statement.setString(5, invitacion.getEstadoRespuesta());
            statement.setInt(6, invitacion.getIdInvitacion());

            statement.executeUpdate();
        }
    }

    public void eliminarInvitacion(int id) throws SQLException
    {
        String sql = "DELETE FROM Invitacion WHERE id_invitacion = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql))
        {
            statement.setInt(1, id);
            statement.executeUpdate();
        }
    }

    public List<InvitacionDTO> listarInvitaciones(int idEvento) throws SQLException
    {
        List<InvitacionDTO> invitaciones = new ArrayList<>();

        String sql = "SELECT id_invitacion, id_evento, id_lugar, " +
                     "fecha_envio, mensaje, estado_respuesta " +
                     "FROM Invitacion WHERE id_evento = ? " +
                     "ORDER BY id_invitacion";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql))
        {
            statement.setInt(1, idEvento);

            try (ResultSet result = statement.executeQuery())
            {
                while (result.next())
                {
                    invitaciones.add(convertirInvitacion(result));
                }
            }
        }

        return invitaciones;
    }

    private InvitacionDTO convertirInvitacion(ResultSet result)
            throws SQLException
    {
        Timestamp fecha = result.getTimestamp("fecha_envio");

        return new InvitacionDTO(
                result.getInt("id_invitacion"),
                result.getInt("id_evento"),
                result.getInt("id_lugar"),
                fecha.toLocalDateTime(),
                result.getString("mensaje"),
                result.getString("estado_respuesta")
        );
    }
}