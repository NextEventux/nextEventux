
package repositories;

import config.DatabaseConnection;
import entities.Organizador;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Repositorio de organizadores.
 * Permite consultar perfiles para CU-026 y mantiene operaciones
 * de consulta y actualizacion requeridas por el diagrama.
 */
public class RepositoryOrganizador {

    private final String url;

    public RepositoryOrganizador() {
        this(null);
    }

    /**
     * Permite utilizar una base de datos aislada en las pruebas.
     */
    public RepositoryOrganizador(String url) {
        this.url = url;
    }

    private Connection conectar() throws SQLException {
        return url == null
                ? DatabaseConnection.getConnection()
                : DatabaseConnection.getConnection(url);
    }

    private static final String CONSULTA_BASE =
            "SELECT o.id_organizador, u.nombre, o.empresa, o.ciudad, "
            + "o.especialidad, o.portafolio, o.tarifa_base, "
            + "COALESCE(AVG(r.calificacion), 0) AS calificacion_promedio, "
            + "COUNT(r.id_resena_organizador) AS total_resenas "
            + "FROM Organizador o "
            + "JOIN Usuario u ON u.id_usuario = o.id_usuario "
            + "LEFT JOIN Resena_Organizador r "
            + "ON r.id_organizador = o.id_organizador ";

    /**
     * Busca un organizador por su identificador.
     */
    public Organizador buscarOrganizador(int idOrganizador) {

        String sql = CONSULTA_BASE
                + "WHERE o.id_organizador = ? "
                + "GROUP BY o.id_organizador, u.nombre, o.empresa, "
                + "o.ciudad, o.especialidad, o.portafolio, o.tarifa_base";

        try (Connection connection = conectar();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, idOrganizador);

            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? convertir(result) : null;
            }

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "No se pudo consultar el organizador.", e);
        }
    }

    /**
     * Comprueba que el organizador este activo y verificado.
     */
    public boolean organizadorActivoVerificado(int idOrganizador) {

        String sql =
                "SELECT COUNT(*) "
                + "FROM Organizador o "
                + "JOIN Usuario u ON u.id_usuario = o.id_usuario "
                + "WHERE o.id_organizador = ? "
                + "AND UPPER(o.estado) = 'ACTIVO' "
                + "AND UPPER(o.estado_verificacion) = 'VERIFICADO' "
                + "AND UPPER(u.estado) = 'ACTIVO' "
                + "AND UPPER(u.tipo_usuario) = 'ORGANIZADOR'";

        try (Connection connection = conectar();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, idOrganizador);

            try (ResultSet result = statement.executeQuery()) {
                return result.next() && result.getInt(1) > 0;
            }

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "No se pudo verificar el estado del organizador.", e);
        }
    }

    /**
     * Busca organizadores activos y verificados que coincidan
     * con los criterios ingresados por el cliente.
     *
     * Filtros admitidos:
     * ciudad, tipoEvento, presupuestoEstimado y fechaAproximada.
     */
    public List<Organizador> consultarOrganizadoresDisponibles(
            Map<String, Object> filtros) {

        StringBuilder sql = new StringBuilder(CONSULTA_BASE);

        sql.append(
                "WHERE UPPER(u.estado) = 'ACTIVO' "
                + "AND UPPER(u.tipo_usuario) = 'ORGANIZADOR' "
                + "AND UPPER(o.estado) = 'ACTIVO' "
                + "AND UPPER(o.estado_verificacion) = 'VERIFICADO' ");

        List<Object> parametros = new ArrayList<>();

        if (filtros != null) {
            agregarFiltroCiudad(sql, parametros, filtros);
            agregarFiltroTipoEvento(sql, parametros, filtros);
            agregarFiltroPresupuesto(sql, parametros, filtros);
            agregarFiltroFecha(sql, parametros, filtros);
        }

        sql.append(
                "GROUP BY o.id_organizador, u.nombre, o.empresa, "
                + "o.ciudad, o.especialidad, o.portafolio, o.tarifa_base "
                + "ORDER BY calificacion_promedio DESC, "
                + "total_resenas DESC, u.nombre ASC");

        try (Connection connection = conectar();
             PreparedStatement statement =
                     connection.prepareStatement(sql.toString())) {

            asignarParametros(statement, parametros);

            try (ResultSet result = statement.executeQuery()) {
                List<Organizador> organizadores = new ArrayList<>();

                while (result.next()) {
                    organizadores.add(convertir(result));
                }

                return organizadores;
            }

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "No se pudieron consultar los organizadores disponibles.",
                    e);
        }
    }

    /**
     * Lista los organizadores pendientes de verificacion.
     */
    public List<Organizador> listarOrganizadoresPendientes() {

        String sql = CONSULTA_BASE
                + "WHERE UPPER(o.estado_verificacion) = 'PENDIENTE' "
                + "GROUP BY o.id_organizador, u.nombre, o.empresa, "
                + "o.ciudad, o.especialidad, o.portafolio, o.tarifa_base "
                + "ORDER BY u.nombre";

        try (Connection connection = conectar();
             PreparedStatement statement =
                     connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {

            List<Organizador> organizadores = new ArrayList<>();

            while (result.next()) {
                organizadores.add(convertir(result));
            }

            return organizadores;

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "No se pudieron consultar los organizadores pendientes.",
                    e);
        }
    }

    /**
     * Actualiza el nombre y la informacion de perfil de un organizador.
     */
    public void actualizarOrganizador(
            int idOrganizador, Organizador organizador) {

        if (idOrganizador <= 0) {
            throw new IllegalArgumentException(
                    "El identificador del organizador no es valido.");
        }

        if (organizador == null
                || organizador.nombre() == null
                || organizador.nombre().isBlank()) {

            throw new IllegalArgumentException(
                    "Los datos y el nombre del organizador son obligatorios.");
        }

        try (Connection connection = conectar()) {

            connection.setAutoCommit(false);

            try {
                String actualizarUsuario =
                        "UPDATE Usuario SET nombre = ? "
                        + "WHERE id_usuario = ("
                        + "SELECT id_usuario FROM Organizador "
                        + "WHERE id_organizador = ?)";

                try (PreparedStatement statement =
                             connection.prepareStatement(actualizarUsuario)) {

                    statement.setString(1, organizador.nombre().trim());
                    statement.setInt(2, idOrganizador);

                    if (statement.executeUpdate() != 1) {
                        throw new IllegalArgumentException(
                                "El organizador seleccionado no existe.");
                    }
                }

                String actualizarPerfil =
                        "UPDATE Organizador SET empresa = ?, ciudad = ?, "
                        + "especialidad = ?, portafolio = ?, tarifa_base = ? "
                        + "WHERE id_organizador = ?";

                try (PreparedStatement statement =
                             connection.prepareStatement(actualizarPerfil)) {

                    statement.setString(1, organizador.empresa());
                    statement.setString(2, organizador.ciudad());
                    statement.setString(3, organizador.especialidad());
                    statement.setString(4, organizador.portafolio());
                    statement.setBigDecimal(5, organizador.tarifaBase());
                    statement.setInt(6, idOrganizador);

                    if (statement.executeUpdate() != 1) {
                        throw new IllegalArgumentException(
                                "No se pudo actualizar el perfil del organizador.");
                    }
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
            throw new IllegalStateException(
                    "No se pudo actualizar el organizador.", e);
        }
    }

    /**
     * Comprueba el estado y la disponibilidad del organizador
     * para la fecha del evento.
     */
    public boolean verificarDisponibilidadOrganizador(
            int idOrganizador, java.time.LocalDate fecha) {

        if (idOrganizador <= 0) {
            throw new IllegalArgumentException(
                    "El identificador del organizador no es valido.");
        }

        if (fecha == null) {
            throw new IllegalArgumentException(
                    "La fecha aproximada del evento es obligatoria.");
        }

        String sql =
                "SELECT COUNT(*) "
                + "FROM Organizador o "
                + "JOIN Usuario u ON u.id_usuario = o.id_usuario "
                + "WHERE o.id_organizador = ? "
                + "AND UPPER(u.estado) = 'ACTIVO' "
                + "AND UPPER(u.tipo_usuario) = 'ORGANIZADOR' "
                + "AND UPPER(o.estado) = 'ACTIVO' "
                + "AND UPPER(o.estado_verificacion) = 'VERIFICADO' "
                + "AND NOT EXISTS ("
                + "SELECT 1 FROM Evento e "
                + "WHERE e.id_organizador = o.id_organizador "
                + "AND CAST(e.fecha_inicio AS DATE) <= ? "
                + "AND CAST(e.fecha_fin AS DATE) >= ? "
                + "AND UPPER(e.estado) <> 'CANCELADO')";

        try (Connection connection = conectar();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, idOrganizador);
            statement.setDate(2, Date.valueOf(fecha));
            statement.setDate(3, Date.valueOf(fecha));

            try (ResultSet result = statement.executeQuery()) {
                return result.next() && result.getInt(1) > 0;
            }

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "No se pudo verificar la disponibilidad del organizador.",
                    e);
        }
    }

    private void agregarFiltroCiudad(
            StringBuilder sql,
            List<Object> parametros,
            Map<String, Object> filtros) {

        Object ciudad = filtros.get("ciudad");

        if (ciudad == null || ciudad.toString().isBlank()) {
            return;
        }

        sql.append("AND LOWER(o.ciudad) = LOWER(?) ");
        parametros.add(ciudad.toString().trim());
    }

    private void agregarFiltroTipoEvento(
            StringBuilder sql,
            List<Object> parametros,
            Map<String, Object> filtros) {

        Object tipoEvento = filtros.get("tipoEvento");

        if (tipoEvento == null || tipoEvento.toString().isBlank()) {
            return;
        }

        sql.append("AND LOWER(o.especialidad) LIKE LOWER(?) ");
        parametros.add("%" + tipoEvento.toString().trim() + "%");
    }

    private void agregarFiltroPresupuesto(
            StringBuilder sql,
            List<Object> parametros,
            Map<String, Object> filtros) {

        Object presupuesto = filtros.get("presupuestoEstimado");

        if (presupuesto == null) {
            return;
        }

        if (!(presupuesto instanceof Number)) {
            throw new IllegalArgumentException(
                    "El presupuesto debe ser un valor numerico.");
        }

        BigDecimal valor = new BigDecimal(presupuesto.toString());

        if (valor.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "El presupuesto no puede ser negativo.");
        }

        sql.append("AND o.tarifa_base <= ? ");
        parametros.add(valor);
    }

    private void agregarFiltroFecha(
            StringBuilder sql,
            List<Object> parametros,
            Map<String, Object> filtros) {

        Object fecha = filtros.get("fechaAproximada");

        if (fecha == null) {
            return;
        }

        if (!(fecha instanceof java.time.LocalDate)) {
            throw new IllegalArgumentException(
                    "La fecha aproximada debe ser LocalDate.");
        }

        sql.append(
                "AND NOT EXISTS ("
                + "SELECT 1 FROM Evento e "
                + "WHERE e.id_organizador = o.id_organizador "
                + "AND CAST(e.fecha_inicio AS DATE) <= ? "
                + "AND CAST(e.fecha_fin AS DATE) >= ? "
                + "AND UPPER(e.estado) <> 'CANCELADO') ");

        Date fechaSQL = Date.valueOf((java.time.LocalDate) fecha);

        parametros.add(fechaSQL);
        parametros.add(fechaSQL);
    }

    private void asignarParametros(
            PreparedStatement statement,
            List<Object> parametros) throws SQLException {

        for (int i = 0; i < parametros.size(); i++) {
            statement.setObject(i + 1, parametros.get(i));
        }
    }

    private Organizador convertir(ResultSet result) throws SQLException {

        BigDecimal tarifa = result.getBigDecimal("tarifa_base");
        BigDecimal promedio = result.getBigDecimal("calificacion_promedio");

        return new Organizador(
                result.getInt("id_organizador"),
                result.getString("nombre"),
                result.getString("empresa"),
                result.getString("ciudad"),
                result.getString("especialidad"),
                result.getString("portafolio"),
                tarifa == null ? BigDecimal.ZERO : tarifa,
                promedio == null ? BigDecimal.ZERO : promedio,
                result.getInt("total_resenas")
        );
    }
}
