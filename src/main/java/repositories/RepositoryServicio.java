package repositories;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import config.DatabaseConnection;
import entities.Servicio;

// Se encarga de guardar y consultar los servicios en H2
public class RepositoryServicio {

    public void guardarServicio(Servicio servicio) {

        String sql = """
                INSERT INTO Servicio
                (id_proveedor, nombre, descripcion, precio, categoria, estado,
                 tipo, cantidad_maxima_personas, ciudad_zona, anticipacion_minima)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection connection = DatabaseConnection.getConnection(); PreparedStatement statement = connection.prepareStatement(
                sql,
                Statement.RETURN_GENERATED_KEYS)) {

            statement.setInt(1, servicio.getIdProveedor());
            statement.setString(2, servicio.getNombre());
            statement.setString(3, servicio.getDescripcion());
            statement.setDouble(4, servicio.getPrecio());
            statement.setString(5, servicio.getCategoria());
            statement.setString(6, servicio.getEstado());
            statement.setString(7, servicio.getTipo());

            if (servicio.getCantidadMaximaPersonas() == null) {
                statement.setNull(8, java.sql.Types.INTEGER);
            } else {
                statement.setInt(8, servicio.getCantidadMaximaPersonas());
            }

            statement.setString(9, servicio.getCiudadZona());
            statement.setInt(10, servicio.getAnticipacionMinima());

            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    servicio.setIdServicio(keys.getInt(1));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("No se pudo guardar el servicio.", e);
        }
    }

    public Servicio buscarServicio(int idServicio) {

        String sql = """
                SELECT *
                FROM Servicio
                WHERE id_servicio = ?
                """;

        try (Connection connection = DatabaseConnection.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, idServicio);

            try (ResultSet result = statement.executeQuery()) {

                if (result.next()) {
                    return construirServicio(result);
                }
            }

            return null;

        } catch (SQLException e) {
            throw new RuntimeException("No se pudo consultar el servicio.", e);
        }
    }

    public List<Servicio> listarServiciosProveedor(int idProveedor) {

        List<Servicio> servicios = new ArrayList<>();

        String sql = """
                SELECT *
                FROM Servicio
                WHERE id_proveedor = ?
                ORDER BY id_servicio
                """;

        try (Connection connection = DatabaseConnection.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, idProveedor);

            try (ResultSet result = statement.executeQuery()) {

                while (result.next()) {
                    servicios.add(construirServicio(result));
                }
            }

            return servicios;

        } catch (SQLException e) {
            throw new RuntimeException("No se pudieron consultar los servicios.", e);
        }
    }

    public void actualizarServicio(Servicio servicio) {

        String sql = """
                UPDATE Servicio
                SET id_proveedor = ?,
                    nombre = ?,
                    descripcion = ?,
                    precio = ?,
                    categoria = ?,
                    estado = ?,
                    tipo = ?,
                    cantidad_maxima_personas = ?,
                    ciudad_zona = ?,
                    anticipacion_minima = ?
                WHERE id_servicio = ?
                """;

        try (Connection connection = DatabaseConnection.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, servicio.getIdProveedor());
            statement.setString(2, servicio.getNombre());
            statement.setString(3, servicio.getDescripcion());
            statement.setDouble(4, servicio.getPrecio());
            statement.setString(5, servicio.getCategoria());
            statement.setString(6, servicio.getEstado());
            statement.setString(7, servicio.getTipo());

            if (servicio.getCantidadMaximaPersonas() == null) {
                statement.setNull(8, java.sql.Types.INTEGER);
            } else {
                statement.setInt(8, servicio.getCantidadMaximaPersonas());
            }

            statement.setString(9, servicio.getCiudadZona());
            statement.setInt(10, servicio.getAnticipacionMinima());
            statement.setInt(11, servicio.getIdServicio());

            statement.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("No se pudo actualizar el servicio.", e);
        }
    }

    public void actualizarEstado(int idServicio, String estado) {

        String sql = """
                UPDATE Servicio
                SET estado = ?
                WHERE id_servicio = ?
                """;

        try (Connection connection = DatabaseConnection.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, estado);
            statement.setInt(2, idServicio);
            statement.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("No se pudo cambiar el estado del servicio.", e);
        }
    }

    public boolean tieneSolicitudesPendientes(int idServicio) {

        String sql = """
                SELECT COUNT(*)
                FROM Solicitud_Ejecucion
                WHERE id_servicio = ?
                  AND UPPER(estado) = 'PENDIENTE'
                """;

        try (Connection connection = DatabaseConnection.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, idServicio);

            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getInt(1) > 0;
            }

        } catch (SQLException e) {
            throw new RuntimeException(
                    "No se pudieron consultar las solicitudes del servicio.",
                    e);
        }
    }

    public List<Servicio> listarServiciosDisponibles() {

        List<Servicio> servicios
                = new ArrayList<>();

        String sql = """
            SELECT *
            FROM Servicio
            WHERE UPPER(estado) = 'DISPONIBLE'
            ORDER BY nombre
            """;

        try (Connection connection
                = DatabaseConnection.getConnection(); PreparedStatement statement
                = connection.prepareStatement(sql); ResultSet result
                = statement.executeQuery()) {

            while (result.next()) {
                servicios.add(
                        construirServicio(result));
            }

            return servicios;

        } catch (SQLException e) {

            throw new RuntimeException(
                    "No se pudieron consultar los servicios publicados.",
                    e);
        }
    }

    private Servicio construirServicio(ResultSet result) throws SQLException {

        Servicio servicio = new Servicio();

        servicio.setIdServicio(result.getInt("id_servicio"));
        servicio.setIdProveedor(result.getInt("id_proveedor"));
        servicio.setNombre(result.getString("nombre"));
        servicio.setDescripcion(result.getString("descripcion"));
        servicio.setPrecio(result.getDouble("precio"));
        servicio.setCategoria(result.getString("categoria"));
        servicio.setEstado(result.getString("estado"));
        servicio.setTipo(result.getString("tipo"));

        int cantidad = result.getInt("cantidad_maxima_personas");

        if (result.wasNull()) {
            servicio.setCantidadMaximaPersonas(null);
        } else {
            servicio.setCantidadMaximaPersonas(cantidad);
        }

        servicio.setCiudadZona(result.getString("ciudad_zona"));
        servicio.setAnticipacionMinima(
                result.getInt("anticipacion_minima"));

        return servicio;
    }

    /**
     * Servicios contratados para un evento (CU06).
     * Recorre Contratacion -> Propuesta -> Solicitud_Ejecucion -> Servicio, así solo
     * aparece lo que realmente se contrató para ESTE evento. El precio que se devuelve
     * es el valor acordado en la contratación, que es lo que pesa en el presupuesto.
     */
    public List<Servicio> listarServiciosPorEvento(int idEvento) {
        List<Servicio> servicios = new ArrayList<>();

        String sql = """
                SELECT s.id_servicio,
                       se.id_proveedor,
                       COALESCE(s.nombre, pr.descripcion) AS nombre,
                       COALESCE(s.descripcion, pr.descripcion) AS descripcion,
                       COALESCE(c.valor_acordado, pr.valor, s.precio, 0) AS precio,
                       COALESCE(s.categoria, 'Otros') AS categoria,
                       c.estado
                FROM Contratacion c
                JOIN Propuesta pr ON pr.id_propuesta = c.id_propuesta
                JOIN Solicitud_Ejecucion se ON se.id_solicitud = pr.id_solicitud
                LEFT JOIN Servicio s ON s.id_servicio = se.id_servicio
                WHERE c.id_evento = ?
                  AND UPPER(c.estado) = 'ACTIVA'
                ORDER BY c.id_contratacion
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, idEvento);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Servicio servicio = new Servicio();
                    servicio.setIdServicio(rs.getInt("id_servicio"));
                    servicio.setIdProveedor(rs.getInt("id_proveedor"));
                    servicio.setNombre(rs.getString("nombre"));
                    servicio.setDescripcion(rs.getString("descripcion"));
                    servicio.setPrecio(rs.getDouble("precio"));
                    servicio.setCategoria(rs.getString("categoria"));
                    servicio.setEstado(rs.getString("estado"));
                    servicios.add(servicio);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("No se pudieron consultar los servicios del evento.", e);
        }

        return servicios;
    }
}