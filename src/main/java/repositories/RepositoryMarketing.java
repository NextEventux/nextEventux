package repositories;

import persistencia.DatabaseConnection;
import entities.Campana;
import entities.ClienteMarketing;
import entities.SolicitudEvento;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import entities.Segmentacion;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class RepositoryMarketing {
    private static final String TIPO_CLIENTE = "CLIENTE";
    private static final String TIPO_ADMINISTRADOR = "ADMINISTRADOR";
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
        String sql = "SELECT u.id_usuario, u.nombre, "
                + "COALESCE(NULLIF(u.pais, ''), 'No especificado') AS pais, "
                + "COALESCE(NULLIF(TRIM(u.ciudad), ''), ("
                + "SELECT NULLIF(TRIM(s.ciudad), '') FROM Solicitud_Contratacion_Organizador s "
                + "WHERE s.id_cliente = u.id_usuario "
                + "ORDER BY s.fecha_solicitud DESC, s.id_solicitud DESC LIMIT 1"
                + ")) AS ciudad, u.acepta_marketing "
                + "FROM Usuario u WHERE u.tipo_usuario = ? AND u.estado = ? ORDER BY u.id_usuario";
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
                + " UNION ALL "
                + "SELECT id_cliente AS id_usuario, tipo_evento AS tipo "
                + "FROM Solicitud_Contratacion_Organizador "
                + "WHERE tipo_evento IS NOT NULL"
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
     public boolean existeAdministradorActivo(int idUsuario) {
        String sql = "SELECT COUNT(*) FROM Usuario WHERE id_usuario = ? AND tipo_usuario = ? AND estado = ?";
        try (Connection connection = conectar();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, idUsuario);
            statement.setString(2, TIPO_ADMINISTRADOR);
            statement.setString(3, ESTADO_ACTIVO);
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo verificar el administrador. Revisa la conexión con la base de datos.", e);
        }
    }

    public List<Segmentacion> reemplazarSegmentosAutomaticos(List<Segmentacion> segmentos) {
        try (Connection connection = conectar()) {
            connection.setAutoCommit(false);
            try {
                desactivarSegmentosAutomaticos(connection);
                for (Segmentacion segmento : segmentos) {
                    guardarSegmento(connection, segmento);
                }
                connection.commit();
                return segmentos;
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudieron guardar los segmentos. Revisa la conexión con la base de datos.", e);
        }
    }

    private void desactivarSegmentosAutomaticos(Connection connection) throws SQLException {
        String sql = "UPDATE Segmentacion SET estados = ? WHERE estados = ? AND criterios LIKE ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, Segmentacion.ESTADO_INACTIVA);
            statement.setString(2, Segmentacion.ESTADO_ACTIVA);
            statement.setString(3, Segmentacion.PREFIJO_CRITERIOS_AUTOMATICOS + "%");
            statement.executeUpdate();
        }
    }

    private void guardarSegmento(Connection connection, Segmentacion segmento) throws SQLException {
        String sqlSegmento = "INSERT INTO Segmentacion (id_usuario, nombre, criterios, descripcion, estados) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sqlSegmento, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, segmento.getIdUsuario());
            statement.setString(2, segmento.getNombre());
            statement.setString(3, segmento.getCriterios());
            statement.setString(4, segmento.getDescripcion());
            statement.setString(5, segmento.getEstado());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("No se generó el identificador del segmento.");
                }
                segmento.setIdSegmento(keys.getInt(1));
            }
        }

        String sqlClientes = "INSERT INTO Segmento_cliente (id_segmento, id_usuario) VALUES (?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sqlClientes)) {
            for (int idCliente : segmento.getIdsClientes()) {
                statement.setInt(1, segmento.getIdSegmento());
                statement.setInt(2, idCliente);
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }
    public List<Segmentacion> listarSegmentosActivos() {
        String sql = "SELECT id_segmento, id_usuario, nombre, criterios, descripcion, estados " + "FROM Segmentacion WHERE estados = ? ORDER BY id_segmento";
        List<Segmentacion> segmentos = new ArrayList<>();
        try (Connection connection = conectar();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, Segmentacion.ESTADO_ACTIVA);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    int idSegmento = result.getInt("id_segmento");
                    segmentos.add(new Segmentacion(
                            idSegmento,
                            result.getInt("id_usuario"),
                            result.getString("nombre"),
                            result.getString("criterios"),
                            result.getString("descripcion"),
                            result.getString("estados"),
                            listarIdsClientes(connection, idSegmento)));
                }
            }
            return segmentos;
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudieron consultar los segmentos. Revisa la conexión con la base de datos.", e);
        }
    }

    private List<Integer> listarIdsClientes(Connection connection, int idSegmento) throws SQLException {
        String sql = "SELECT id_usuario FROM Segmento_cliente WHERE id_segmento = ? ORDER BY id_usuario";
        List<Integer> ids = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, idSegmento);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    ids.add(result.getInt("id_usuario"));
                }
            }
        }
        return ids;
    }

    // ---------- CU-22: campañas de marketing ----------

    private static final String ESTADO_ENVIO = "ENVIADO";
    private static final String SELECT_CAMPANA = "SELECT c.id_campana, c.id_usuario, c.id_segmento, c.nombre, "
            + "c.tipo_oferta, c.fecha_inicio, c.fecha_fin, c.presupuesto_maximo, c.estado, "
            + "c.visualizaciones, c.conversiones, s.nombre AS nombre_segmento "
            + "FROM Campana c JOIN Segmentacion s ON s.id_segmento = c.id_segmento ";

    // Busca un segmento por su identificador, esté activo o no; devuelve null si no existe.
    public Segmentacion buscarSegmento(int idSegmento) {
        String sql = "SELECT id_segmento, id_usuario, nombre, criterios, descripcion, estados "
                + "FROM Segmentacion WHERE id_segmento = ?";
        try (Connection connection = conectar();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, idSegmento);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return null;
                }
                return new Segmentacion(
                        result.getInt("id_segmento"),
                        result.getInt("id_usuario"),
                        result.getString("nombre"),
                        result.getString("criterios"),
                        result.getString("descripcion"),
                        result.getString("estados"),
                        listarIdsClientes(connection, idSegmento));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo consultar el segmento. Revisa la conexión con la base de datos.", e);
        }
    }

    // Presupuesto de marketing que todavía no está comprometido en campañas.
    public BigDecimal obtenerPresupuestoDisponible() {
        String sql = "SELECT disponible FROM Presupuesto_marketing WHERE id_presupuesto = 1";
        try (Connection connection = conectar();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            return result.next() ? result.getBigDecimal(1) : BigDecimal.ZERO;
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo consultar el presupuesto de marketing. Revisa la conexión con la base de datos.", e);
        }
    }

    /*
     * Guarda la campaña y descuenta su presupuesto del disponible en una sola transacción.
     * Devuelve false, sin guardar nada, si el presupuesto disponible ya no alcanza.
     */
    public boolean guardarCampanaComprometiendoPresupuesto(Campana campana) {
        String descontar = "UPDATE Presupuesto_marketing SET disponible = disponible - ? "
                + "WHERE id_presupuesto = 1 AND disponible >= ?";
        try (Connection connection = conectar()) {
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement(descontar)) {
                statement.setBigDecimal(1, campana.getPresupuestoMaximo());
                statement.setBigDecimal(2, campana.getPresupuestoMaximo());
                if (statement.executeUpdate() == 0) {
                    connection.rollback();
                    return false;
                }
                insertarCampana(connection, campana);
                connection.commit();
                return true;
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo guardar la campaña. Revisa la conexión con la base de datos.", e);
        }
    }

    private void insertarCampana(Connection connection, Campana campana) throws SQLException {
        String sql = "INSERT INTO Campana (id_usuario, id_segmento, nombre, tipo_oferta, fecha_inicio, "
                + "fecha_fin, presupuesto_maximo, estado, visualizaciones, conversiones) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, campana.getIdUsuario());
            statement.setInt(2, campana.getIdSegmento());
            statement.setString(3, campana.getNombre());
            statement.setString(4, campana.getTipoOferta());
            statement.setTimestamp(5, Timestamp.valueOf(campana.getFechaInicio().atStartOfDay()));
            statement.setTimestamp(6, Timestamp.valueOf(campana.getFechaFin().atStartOfDay()));
            statement.setBigDecimal(7, campana.getPresupuestoMaximo());
            statement.setString(8, campana.getEstado());
            statement.setLong(9, campana.getVisualizaciones());
            statement.setLong(10, campana.getConversiones());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("No se generó el identificador de la campaña.");
                }
                campana.setIdCampana(keys.getInt(1));
            }
        }
    }

    // Busca una campaña de segmento por su identificador; devuelve null si no existe.
    public Campana buscarCampana(int idCampana) {
        String sql = SELECT_CAMPANA + "WHERE c.id_campana = ?";
        try (Connection connection = conectar();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, idCampana);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? mapearCampana(result) : null;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo consultar la campaña. Revisa la conexión con la base de datos.", e);
        }
    }

    // Lista las campañas dirigidas a un segmento; las campañas de proveedores no se incluyen.
    public List<Campana> listarCampanas() {
        String sql = SELECT_CAMPANA + "ORDER BY c.id_campana";
        List<Campana> campanas = new ArrayList<>();
        try (Connection connection = conectar();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                campanas.add(mapearCampana(result));
            }
            return campanas;
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudieron consultar las campañas. Revisa la conexión con la base de datos.", e);
        }
    }

    /*
     * Pasa la campaña de PROGRAMADA a ACTIVA y registra un envío para cada cliente del segmento
     * que acepta marketing, todo en una transacción. Devuelve false si no estaba PROGRAMADA.
     */
    public boolean activarYDistribuirCampana(int idCampana) {
        String activar = "UPDATE Campana SET estado = ? WHERE id_campana = ? AND estado = ?";
        String distribuir = "INSERT INTO Envio_Campana (id_campana, id_usuario, fecha_solicitud, estado) "
                + "SELECT c.id_campana, sc.id_usuario, CURRENT_TIMESTAMP, ? "
                + "FROM Campana c "
                + "JOIN Segmento_cliente sc ON sc.id_segmento = c.id_segmento "
                + "JOIN Usuario u ON u.id_usuario = sc.id_usuario "
                + "WHERE c.id_campana = ? AND u.acepta_marketing = TRUE";
        try (Connection connection = conectar()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement statement = connection.prepareStatement(activar)) {
                    statement.setString(1, Campana.ESTADO_ACTIVA);
                    statement.setInt(2, idCampana);
                    statement.setString(3, Campana.ESTADO_PROGRAMADA);
                    if (statement.executeUpdate() == 0) {
                        connection.rollback();
                        return false;
                    }
                }
                try (PreparedStatement statement = connection.prepareStatement(distribuir)) {
                    statement.setString(1, ESTADO_ENVIO);
                    statement.setInt(2, idCampana);
                    statement.executeUpdate();
                }
                connection.commit();
                return true;
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo activar la campaña. Revisa la conexión con la base de datos.", e);
        }
    }

    // Cambia el estado solo si la campaña está en el estado esperado; devuelve true si lo cambió.
    public boolean cambiarEstadoCampana(int idCampana, String estadoEsperado, String estadoNuevo) {
        String sql = "UPDATE Campana SET estado = ? WHERE id_campana = ? AND estado = ?";
        try (Connection connection = conectar();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, estadoNuevo);
            statement.setInt(2, idCampana);
            statement.setString(3, estadoEsperado);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo cambiar el estado de la campaña. Revisa la conexión con la base de datos.", e);
        }
    }

    // Suma una visualización a una campaña ACTIVA; devuelve false si no lo está.
    public boolean incrementarVisualizaciones(int idCampana) {
        return incrementarMetrica("visualizaciones", idCampana);
    }

    // Suma una conversión a una campaña ACTIVA; devuelve false si no lo está.
    public boolean incrementarConversiones(int idCampana) {
        return incrementarMetrica("conversiones", idCampana);
    }

    private boolean incrementarMetrica(String columna, int idCampana) {
        String sql = "UPDATE Campana SET " + columna + " = " + columna + " + 1 "
                + "WHERE id_campana = ? AND estado = ?";
        try (Connection connection = conectar();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, idCampana);
            statement.setString(2, Campana.ESTADO_ACTIVA);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo registrar la métrica de la campaña. Revisa la conexión con la base de datos.", e);
        }
    }

    private Campana mapearCampana(ResultSet result) throws SQLException {
        return new Campana(
                result.getInt("id_campana"),
                result.getInt("id_usuario"),
                result.getInt("id_segmento"),
                result.getString("nombre"),
                result.getString("tipo_oferta"),
                result.getTimestamp("fecha_inicio").toLocalDateTime().toLocalDate(),
                result.getTimestamp("fecha_fin").toLocalDateTime().toLocalDate(),
                result.getBigDecimal("presupuesto_maximo"),
                result.getString("estado"),
                result.getLong("visualizaciones"),
                result.getLong("conversiones"),
                result.getString("nombre_segmento"));
    }
}
