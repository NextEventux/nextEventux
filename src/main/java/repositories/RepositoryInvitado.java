package repositories;

import config.DatabaseConnection;
import entities.Invitacion;
import entities.Invitado;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Acceso a la tabla Invitado y consultas de cupo para el CU-17. */
public class RepositoryInvitado {
    private static final String COLUMNAS =
            "id_invitado, id_invitacion, nombre, telefono, direccion, estado, correo";
    private static final String INVITACION_ACTIVA =
            "(i.estado_respuesta IS NULL OR UPPER(i.estado_respuesta) <> '" + Invitacion.ESTADO_REVOCADA + "')";

    private final String url;

    public RepositoryInvitado() {
        this(null);
    }

    // Permite utilizar una base aislada en las pruebas.
    public RepositoryInvitado(String url) {
        this.url = url;
    }

    private Connection conectar() throws SQLException {
        return url == null ? DatabaseConnection.getConnection() : DatabaseConnection.getConnection(url);
    }

    public void guardarInvitado(Invitado invitado) {
        String sql = "INSERT INTO Invitado (id_invitacion, nombre, telefono, direccion, estado, correo) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection connection = conectar();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, invitado.getIdInvitacion());
            statement.setString(2, invitado.getNombre());
            statement.setString(3, invitado.getTelefono());
            statement.setString(4, invitado.getDireccion());
            statement.setString(5, invitado.getEstado());
            statement.setString(6, invitado.getCorreo());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) throw new SQLException("No se generó el identificador del invitado.");
                invitado.setIdInvitado(keys.getInt(1));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo guardar el invitado. Revisa la conexión con la base de datos.", e);
        }
    }

    public Invitado buscarInvitado(int idInvitado) {
        try (Connection connection = conectar();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT " + COLUMNAS + " FROM Invitado WHERE id_invitado = ?")) {
            statement.setInt(1, idInvitado);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? convertir(result) : null;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo consultar el invitado. Revisa la conexión con la base de datos.", e);
        }
    }

    public List<Invitado> listarInvitados(int idInvitacion) {
        List<Invitado> invitados = new ArrayList<>();
        try (Connection connection = conectar();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT " + COLUMNAS + " FROM Invitado WHERE id_invitacion = ? ORDER BY id_invitado")) {
            statement.setInt(1, idInvitacion);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) invitados.add(convertir(result));
            }
            return invitados;
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudieron consultar los invitados. Revisa la conexión con la base de datos.", e);
        }
    }

    public void eliminarInvitado(int idInvitado) {
        try (Connection connection = conectar();
             PreparedStatement statement = connection.prepareStatement(
                     "DELETE FROM Invitado WHERE id_invitado = ?")) {
            statement.setInt(1, idInvitado);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo quitar el invitado. Revisa la conexión con la base de datos.", e);
        }
    }

    /** Acompañantes, necesidades especiales o reservas de regalos que dependen del invitado. */
    public boolean tieneRegistrosAsociados(int idInvitado) {
        String sql = "SELECT (SELECT COUNT(*) FROM Acompanante WHERE id_invitado = ?) "
                + "+ (SELECT COUNT(*) FROM Necesidad_Especial WHERE id_invitado = ?) "
                + "+ (SELECT COUNT(*) FROM Reserva_regalo WHERE id_invitado = ?)";
        try (Connection connection = conectar();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 1; i <= 3; i++) statement.setInt(i, idInvitado);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() && result.getLong(1) > 0;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudieron revisar los datos del invitado. Revisa la conexión con la base de datos.", e);
        }
    }

    /** R40: el correo ya está en una invitación activa del mismo evento. */
    public boolean existeCorreoEnEvento(int idEvento, String correo) {
        String sql = "SELECT COUNT(*) FROM Invitado v JOIN Invitacion i ON i.id_invitacion = v.id_invitacion "
                + "WHERE i.id_evento = ? AND " + INVITACION_ACTIVA + " AND LOWER(TRIM(v.correo)) = LOWER(TRIM(?))";
        try (Connection connection = conectar();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, idEvento);
            statement.setString(2, correo);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() && result.getLong(1) > 0;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo validar el invitado. Revisa la conexión con la base de datos.", e);
        }
    }

    /** Invitados y acompañantes de las invitaciones activas de un evento en un lugar. */
    public int contarAsistentes(int idEvento, int idLugar) {
        String filtro = " JOIN Invitacion i ON i.id_invitacion = v.id_invitacion "
                + "WHERE i.id_evento = ? AND i.id_lugar = ? AND " + INVITACION_ACTIVA;
        String sql = "SELECT (SELECT COUNT(*) FROM Invitado v" + filtro + ") + "
                + "(SELECT COUNT(*) FROM Acompanante a JOIN Invitado v ON v.id_invitado = a.id_invitado" + filtro + ")";
        try (Connection connection = conectar();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, idEvento);
            statement.setInt(2, idLugar);
            statement.setInt(3, idEvento);
            statement.setInt(4, idLugar);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? result.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo calcular el aforo. Revisa la conexión con la base de datos.", e);
        }
    }

    /** Asistentes (invitados + acompañantes) de cada invitación de un evento. */
    public Map<Integer, Integer> contarAsistentesPorInvitacion(int idEvento) {
        String invitados = "SELECT v.id_invitacion, COUNT(*) FROM Invitado v "
                + "JOIN Invitacion i ON i.id_invitacion = v.id_invitacion WHERE i.id_evento = ? GROUP BY v.id_invitacion";
        String acompanantes = "SELECT v.id_invitacion, COUNT(*) FROM Acompanante a "
                + "JOIN Invitado v ON v.id_invitado = a.id_invitado "
                + "JOIN Invitacion i ON i.id_invitacion = v.id_invitacion WHERE i.id_evento = ? GROUP BY v.id_invitacion";
        Map<Integer, Integer> conteo = new HashMap<>();
        try (Connection connection = conectar()) {
            for (String sql : new String[]{invitados, acompanantes}) {
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    statement.setInt(1, idEvento);
                    try (ResultSet result = statement.executeQuery()) {
                        while (result.next()) conteo.merge(result.getInt(1), result.getInt(2), Integer::sum);
                    }
                }
            }
            return conteo;
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudieron contar los invitados. Revisa la conexión con la base de datos.", e);
        }
    }

    private Invitado convertir(ResultSet result) throws SQLException {
        return new Invitado(result.getInt("id_invitado"), result.getInt("id_invitacion"), result.getString("nombre"),
                result.getString("telefono"), result.getString("direccion"), result.getString("estado"),
                result.getString("correo"));
    }
}
