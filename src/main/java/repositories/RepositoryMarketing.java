package repositories;

import config.DatabaseConnection;
import entities.ClienteMarketing;
import entities.SolicitudEvento;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;  
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class RepositoryMarketing {
    private static final String TIPO_CLIENTE = "CLIENTE";
    private static final String ESTADO_ACTIVO = "ACTIVO";

    private final String url;

    public RepositoryMarketing() {
        this(null);
    }

    // Permite utilizar una base aislada en las pruebas.
    public RepositoryMarketing(String url) {
        this.url = url;
    }

    private Connection conectar() throws SQLException {
        return url == null ? DatabaseConnection.getConnection() : DatabaseConnection.getConnection(url);
    }

    public List<ClienteMarketing> listarClientesActivos() {
        String sql = "SELECT id_usuario, nombre, pais, ciudad, fecha_nacimiento, acepta_marketing "
                + "FROM Usuario WHERE tipo_usuario = ? AND estado = ? ORDER BY id_usuario";
        List<ClienteMarketing> clientes = new ArrayList<>();
        try (Connection connection = conectar();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, TIPO_CLIENTE);
            statement.setString(2, ESTADO_ACTIVO);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    clientes.add(new ClienteMarketing(
                            result.getInt("id_usuario"),
                            result.getString("nombre"),
                            result.getString("pais"),
                            result.getString("ciudad"),
                            result.getObject("fecha_nacimiento", LocalDate.class),
                            result.getBoolean("acepta_marketing")));
                }
            }
            return clientes;
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudieron consultar los clientes. Revisa la conexión con la base de datos.", e);
        }
    }

    public List<SolicitudEvento> contarSolicitudesPorTipo() {
        String sql = "SELECT id_usuario, tipo, COUNT(*) AS cantidad FROM ("
                + "SELECT id_usuario, tipo FROM Busqueda_evento "
                + "UNION ALL "
                + "SELECT id_cliente AS id_usuario, tipo FROM Evento "
                + "WHERE id_cliente IS NOT NULL AND tipo IS NOT NULL"
                + ") AS solicitudes GROUP BY id_usuario, tipo ORDER BY id_usuario, tipo";
        List<SolicitudEvento> solicitudes = new ArrayList<>();
        try (Connection connection = conectar();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                solicitudes.add(new SolicitudEvento(
                        result.getInt("id_usuario"),
                        result.getString("tipo"),
                        result.getInt("cantidad")));
            }
            return solicitudes;
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudieron consultar las solicitudes de eventos. Revisa la conexión con la base de datos.", e);
        }
    }
}