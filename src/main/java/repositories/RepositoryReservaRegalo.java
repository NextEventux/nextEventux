package repositories;

import config.DatabaseConnection;
import entities.Regalo;
import entities.ReservaRegalo;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;

/**
 * Acceso a la tabla Reserva_regalo (CU-20).
 * Reservar y cancelar también actualizan el estado del regalo, en la misma transacción.
 */
public class RepositoryReservaRegalo {
    private static final String ACTIVA = "UPPER(estado) = '" + ReservaRegalo.ESTADO_RESERVADO + "'";

    private final String url;

    public RepositoryReservaRegalo() {
        this(null);
    }

    // Permite utilizar una base aislada en las pruebas.
    public RepositoryReservaRegalo(String url) {
        this.url = url;
    }

    private Connection conectar() throws SQLException {
        return url == null ? DatabaseConnection.getConnection() : DatabaseConnection.getConnection(url);
    }

    /**
     * Registra la reserva solo si el regalo sigue libre (R43). El regalo se bloquea durante la
     * transacción para que dos invitados no lo reserven al mismo tiempo.
     *
     * @return false si otro invitado lo reservó primero.
     */
    public boolean registrarReserva(ReservaRegalo reserva) {
        try (Connection connection = conectar()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement bloqueo = connection.prepareStatement(
                        "SELECT id_regalo FROM Regalo WHERE id_regalo = ? FOR UPDATE")) {
                    bloqueo.setInt(1, reserva.getIdRegalo());
                    bloqueo.executeQuery().close();
                }
                if (contarActivas(connection, reserva.getIdRegalo()) > 0) {
                    connection.rollback();
                    return false;
                }
                try (PreparedStatement insertar = connection.prepareStatement(
                        "INSERT INTO Reserva_regalo (id_evento, id_invitado, id_regalo, fecha_reserva, estado) "
                                + "VALUES (?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
                    insertar.setInt(1, reserva.getIdEvento());
                    insertar.setInt(2, reserva.getIdInvitado());
                    insertar.setInt(3, reserva.getIdRegalo());
                    insertar.setTimestamp(4, Timestamp.valueOf(reserva.getFechaReserva()));
                    insertar.setString(5, reserva.getEstado());
                    insertar.executeUpdate();
                    try (ResultSet keys = insertar.getGeneratedKeys()) {
                        if (keys.next()) reserva.setIdReserva(keys.getInt(1));
                    }
                }
                actualizarEstadoRegalo(connection, reserva.getIdRegalo(), Regalo.ESTADO_RESERVADO);
                connection.commit();
                return true;
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo reservar el regalo. Revisa la conexión con la base de datos.", e);
        }
    }

    public ReservaRegalo buscarReservaActiva(int idRegalo, int idInvitado) {
        String sql = "SELECT id_reserva, id_evento, id_invitado, id_regalo, fecha_reserva, estado FROM Reserva_regalo "
                + "WHERE id_regalo = ? AND id_invitado = ? AND " + ACTIVA + " ORDER BY id_reserva FETCH FIRST 1 ROWS ONLY";
        try (Connection connection = conectar();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, idRegalo);
            statement.setInt(2, idInvitado);
            try (ResultSet r = statement.executeQuery()) {
                if (!r.next()) return null;
                return new ReservaRegalo(r.getInt("id_reserva"), r.getInt("id_evento"), r.getInt("id_invitado"),
                        r.getInt("id_regalo"), r.getTimestamp("fecha_reserva").toLocalDateTime(), r.getString("estado"));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo consultar la reserva. Revisa la conexión con la base de datos.", e);
        }
    }

    public int contarReservasActivas(int idRegalo) {
        try (Connection connection = conectar()) {
            return contarActivas(connection, idRegalo);
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo consultar la reserva. Revisa la conexión con la base de datos.", e);
        }
    }

    /** Cancela la reserva; el regalo vuelve a quedar DISPONIBLE si no le queda otra reserva activa. */
    public void cancelarReserva(ReservaRegalo reserva) {
        try (Connection connection = conectar()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement statement = connection.prepareStatement(
                        "UPDATE Reserva_regalo SET estado = ? WHERE id_reserva = ?")) {
                    statement.setString(1, ReservaRegalo.ESTADO_CANCELADO);
                    statement.setInt(2, reserva.getIdReserva());
                    statement.executeUpdate();
                }
                if (contarActivas(connection, reserva.getIdRegalo()) == 0) {
                    actualizarEstadoRegalo(connection, reserva.getIdRegalo(), Regalo.ESTADO_DISPONIBLE);
                }
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo cancelar la reserva. Revisa la conexión con la base de datos.", e);
        }
    }

    private int contarActivas(Connection connection, int idRegalo) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT COUNT(*) FROM Reserva_regalo WHERE id_regalo = ? AND " + ACTIVA)) {
            statement.setInt(1, idRegalo);
            try (ResultSet r = statement.executeQuery()) {
                r.next();
                return r.getInt(1);
            }
        }
    }

    private void actualizarEstadoRegalo(Connection connection, int idRegalo, String estado) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("UPDATE Regalo SET estado = ? WHERE id_regalo = ?")) {
            statement.setString(1, estado);
            statement.setInt(2, idRegalo);
            statement.executeUpdate();
        }
    }
}
