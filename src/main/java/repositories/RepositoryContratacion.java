package repositories;

import config.DatabaseConnection;
import entities.Contratacion;
import services.OrganizadorNoDisponibleException;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Acceso a datos para el caso de uso CU-026.
 * Las solicitudes de organizadores se almacenan separadas
 * de las contrataciones de servicios de proveedores.
 */
public class RepositoryContratacion {

    /** E1: no se guardó nada y el cliente puede volver a intentarlo. */
    public static final String ERROR_REGISTRO =
            "No fue posible registrar la solicitud de contratación. "
            + "No se guardaron cambios. Puedes intentarlo nuevamente.";

    /** Ya existe la misma solicitud pendiente (cliente, organizador y fecha). */
    public static final String SOLICITUD_DUPLICADA =
            "Ya enviaste a este organizador una solicitud para esa fecha "
            + "y sigue pendiente. Consulta su estado en «Mis solicitudes».";

    /** La solicitud ya fue aceptada o rechazada y no puede procesarse otra vez. */
    public static final String SOLICITUD_YA_RESPONDIDA =
            "Esta solicitud ya fue respondida y no puede procesarse nuevamente.";

    /** La solicitud no existe o no está dirigida al organizador de la sesión. */
    public static final String SOLICITUD_NO_ENCONTRADA =
            "La solicitud seleccionada no existe o no está dirigida a tu cuenta.";

    /** La cuenta del organizador no está activa (por ejemplo, suspendida). */
    public static final String ORGANIZADOR_NO_ACTIVO =
            "Tu cuenta de organizador no está activa. No es posible responder la solicitud.";

    /** E1 de la respuesta: no se guardó nada y se puede reintentar. */
    public static final String ERROR_RESPUESTA =
            "No fue posible registrar la respuesta. La solicitud conserva su estado. "
            + "Puedes intentarlo nuevamente.";

    private static final String CONSULTA_SOLICITUDES =
            "SELECT s.*, u.nombre AS nombre_organizador, c.nombre AS nombre_cliente "
            + "FROM Solicitud_Contratacion_Organizador s "
            + "JOIN Organizador o ON o.id_organizador = s.id_organizador "
            + "JOIN Usuario u ON u.id_usuario = o.id_usuario "
            + "JOIN Usuario c ON c.id_usuario = s.id_cliente ";

    private final String url;

    public RepositoryContratacion() {
        this(null);
    }

    public RepositoryContratacion(String url) {
        this.url = url;
    }

    private Connection conectar() throws SQLException {
        return url == null
                ? DatabaseConnection.getConnection()
                : DatabaseConnection.getConnection(url);
    }

    /**
     * Registra la solicitud y notifica al organizador en una sola
     * transacción. Dentro de ella se vuelven a comprobar el cliente,
     * el estado del organizador (E2), su disponibilidad para la fecha
     * y que no exista la misma solicitud pendiente. Si algo falla no
     * queda ningún cambio (E1).
     */
    public void guardarContratacion(Contratacion contratacion) {

        validarDatos(contratacion);

        try (Connection connection = conectar()) {
            connection.setAutoCommit(false);

            try {
                // Bloquear primero al cliente serializa sus envíos simultáneos.
                String nombreCliente = validarCliente(
                        connection, contratacion.getIdCliente());

                validarOrganizadorDisponible(
                        connection, contratacion.getIdOrganizador());

                validarDisponibilidadFecha(connection, contratacion);
                validarSinDuplicado(connection, contratacion);

                int idGenerado = insertarSolicitud(connection, contratacion);

                notificar(
                        connection,
                        contratacion.getIdOrganizador(),
                        mensajeNotificacion(nombreCliente, contratacion));

                contratacion.setIdContratacion(idGenerado);
                contratacion.setEstado("PENDIENTE");

                cargarFechaSolicitud(connection, contratacion);

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
            throw new IllegalStateException(ERROR_REGISTRO, e);
        }
    }

    /**
     * Consulta una solicitud por su identificador.
     */
    public Contratacion buscarContratacion(int idContratacion) {

        String sql = CONSULTA_SOLICITUDES + "WHERE s.id_solicitud = ?";

        try (Connection connection = conectar();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, idContratacion);

            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? convertir(result) : null;
            }

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "No se pudo consultar la solicitud de contratación.", e);
        }
    }

    /**
     * Consulta las solicitudes registradas por un cliente.
     */
    public List<Contratacion> listarContrataciones(int idCliente) {

        if (idCliente <= 0) {
            throw new IllegalArgumentException(
                    "El identificador del cliente no es valido.");
        }

        String sql = CONSULTA_SOLICITUDES
                + "WHERE s.id_cliente = ? "
                + "ORDER BY s.fecha_solicitud DESC, s.id_solicitud DESC";

        try (Connection connection = conectar();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, idCliente);

            try (ResultSet result = statement.executeQuery()) {
                List<Contratacion> lista = new ArrayList<>();

                while (result.next()) {
                    lista.add(convertir(result));
                }

                return lista;
            }

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "No se pudieron consultar las solicitudes del cliente.", e);
        }
    }

    /**
     * Permite filtrar las solicitudes por cliente, organizador,
     * estado, ciudad o tipo de evento.
     *
     * Claves admitidas: idCliente, idOrganizador, estado,
     * ciudad y tipoEvento.
     */
    public List<Contratacion> listarContrataciones(
            Map<String, Object> filtros) {

        StringBuilder sql = new StringBuilder(
                CONSULTA_SOLICITUDES + "WHERE 1 = 1");

        List<Object> parametros = new ArrayList<>();

        if (filtros != null) {

            agregarFiltroNumerico(
                    sql, parametros, filtros, "idCliente", "s.id_cliente");

            agregarFiltroNumerico(
                    sql, parametros, filtros,
                    "idOrganizador", "s.id_organizador");

            agregarFiltroTexto(
                    sql, parametros, filtros, "estado", "s.estado");

            agregarFiltroTexto(
                    sql, parametros, filtros, "ciudad", "s.ciudad");

            agregarFiltroTexto(
                    sql, parametros, filtros,
                    "tipoEvento", "s.tipo_evento");
        }

        sql.append(" ORDER BY s.fecha_solicitud DESC, s.id_solicitud DESC");

        try (Connection connection = conectar();
             PreparedStatement statement =
                     connection.prepareStatement(sql.toString())) {

            for (int i = 0; i < parametros.size(); i++) {
                statement.setObject(i + 1, parametros.get(i));
            }

            try (ResultSet result = statement.executeQuery()) {
                List<Contratacion> lista = new ArrayList<>();

                while (result.next()) {
                    lista.add(convertir(result));
                }

                return lista;
            }

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "No se pudieron consultar las solicitudes de contratación.", e);
        }
    }

    // ------------------------------------------------------------------
    // Ampliación de CU-026: el organizador responde las solicitudes
    // ------------------------------------------------------------------

    /**
     * Solicitudes dirigidas al organizador de la cuenta indicada: primero
     * las pendientes y luego las ya respondidas, de la más reciente a la
     * más antigua.
     */
    public List<Contratacion> listarSolicitudesRecibidas(int idUsuarioOrganizador) {

        if (idUsuarioOrganizador <= 0) {
            throw new IllegalArgumentException(
                    "El identificador del organizador no es valido.");
        }

        String sql = CONSULTA_SOLICITUDES
                + "WHERE o.id_usuario = ? "
                + "ORDER BY CASE WHEN UPPER(s.estado) = 'PENDIENTE' THEN 0 ELSE 1 END, "
                + "s.fecha_solicitud DESC, s.id_solicitud DESC";

        try (Connection connection = conectar();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, idUsuarioOrganizador);

            try (ResultSet result = statement.executeQuery()) {
                List<Contratacion> lista = new ArrayList<>();

                while (result.next()) {
                    lista.add(convertir(result));
                }

                return lista;
            }

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "No se pudieron consultar las solicitudes recibidas.", e);
        }
    }

    /**
     * Registra la decisión del organizador y notifica al cliente en una
     * sola transacción. Solo una solicitud PENDIENTE dirigida al organizador
     * de la cuenta puede pasar a ACEPTADA o RECHAZADA; si algo falla, la
     * solicitud conserva su estado y no queda notificación.
     *
     * @return la solicitud con su nuevo estado
     */
    public Contratacion responderSolicitud(
            int idSolicitud, int idUsuarioOrganizador, String nuevoEstado) {

        if (idSolicitud <= 0 || idUsuarioOrganizador <= 0) {
            throw new IllegalArgumentException(SOLICITUD_NO_ENCONTRADA);
        }

        if (!"ACEPTADA".equals(nuevoEstado) && !"RECHAZADA".equals(nuevoEstado)) {
            throw new IllegalArgumentException(
                    "La respuesta debe ser aceptar o rechazar la solicitud.");
        }

        try (Connection connection = conectar()) {
            connection.setAutoCommit(false);

            try {
                // Mismo orden que el envío del cliente: primero el organizador.
                OrganizadorResponde organizador =
                        validarOrganizadorResponde(connection, idUsuarioOrganizador);

                SolicitudBloqueada solicitud = bloquearSolicitud(
                        connection, idSolicitud, organizador.idOrganizador());

                if (!"PENDIENTE".equalsIgnoreCase(solicitud.estado())) {
                    throw new IllegalStateException(SOLICITUD_YA_RESPONDIDA);
                }

                actualizarEstado(connection, idSolicitud, nuevoEstado);

                insertarNotificacion(
                        connection,
                        solicitud.idCliente(),
                        mensajeRespuesta(organizador.nombre(), idSolicitud,
                                solicitud, nuevoEstado));

                Contratacion respondida = buscarContratacion(connection, idSolicitud);

                connection.commit();
                return respondida;

            } catch (SQLException | RuntimeException e) {
                try {
                    connection.rollback();
                } catch (SQLException rollback) {
                    e.addSuppressed(rollback);
                }

                throw e;
            }

        } catch (SQLException e) {
            throw new IllegalStateException(ERROR_RESPUESTA, e);
        }
    }

    private record OrganizadorResponde(int idOrganizador, String nombre) {
    }

    private record SolicitudBloqueada(
            int idCliente, String estado, String tipoEvento,
            String ciudad, java.time.LocalDate fechaAproximada) {
    }

    /**
     * Bloquea al organizador de la cuenta y comprueba que la cuenta y el
     * perfil sigan activos (una cuenta suspendida por CU-029 no responde).
     */
    private OrganizadorResponde validarOrganizadorResponde(
            Connection connection, int idUsuarioOrganizador) throws SQLException {

        String sql =
                "SELECT o.id_organizador, u.nombre, u.estado AS estado_usuario, "
                + "o.estado AS estado_organizador "
                + "FROM Organizador o "
                + "JOIN Usuario u ON u.id_usuario = o.id_usuario "
                + "WHERE o.id_usuario = ? "
                + "AND UPPER(u.tipo_usuario) = 'ORGANIZADOR' "
                + "FOR UPDATE";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, idUsuarioOrganizador);

            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new IllegalArgumentException(SOLICITUD_NO_ENCONTRADA);
                }

                if (!"ACTIVO".equalsIgnoreCase(result.getString("estado_usuario"))
                        || !"ACTIVO".equalsIgnoreCase(
                                result.getString("estado_organizador"))) {
                    throw new IllegalStateException(ORGANIZADOR_NO_ACTIVO);
                }

                return new OrganizadorResponde(
                        result.getInt("id_organizador"),
                        result.getString("nombre"));
            }
        }
    }

    /** Bloquea la solicitud solo si está dirigida a ese organizador. */
    private SolicitudBloqueada bloquearSolicitud(
            Connection connection, int idSolicitud, int idOrganizador)
            throws SQLException {

        String sql =
                "SELECT id_cliente, estado, tipo_evento, ciudad, fecha_aproximada "
                + "FROM Solicitud_Contratacion_Organizador "
                + "WHERE id_solicitud = ? AND id_organizador = ? "
                + "FOR UPDATE";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, idSolicitud);
            statement.setInt(2, idOrganizador);

            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new IllegalArgumentException(SOLICITUD_NO_ENCONTRADA);
                }

                Date fecha = result.getDate("fecha_aproximada");

                return new SolicitudBloqueada(
                        result.getInt("id_cliente"),
                        result.getString("estado"),
                        result.getString("tipo_evento"),
                        result.getString("ciudad"),
                        fecha == null ? null : fecha.toLocalDate());
            }
        }
    }

    private void actualizarEstado(
            Connection connection, int idSolicitud, String nuevoEstado)
            throws SQLException {

        String sql =
                "UPDATE Solicitud_Contratacion_Organizador SET estado = ? "
                + "WHERE id_solicitud = ? AND UPPER(estado) = 'PENDIENTE'";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, nuevoEstado);
            statement.setInt(2, idSolicitud);

            if (statement.executeUpdate() != 1) {
                throw new IllegalStateException(SOLICITUD_YA_RESPONDIDA);
            }
        }
    }

    private String mensajeRespuesta(
            String nombreOrganizador,
            int idSolicitud,
            SolicitudBloqueada solicitud,
            String nuevoEstado) {

        return nombreOrganizador
                + ("ACEPTADA".equals(nuevoEstado) ? " aceptó" : " rechazó")
                + " tu solicitud de contratación #" + idSolicitud
                + ". Tipo de evento: " + solicitud.tipoEvento()
                + ". Ciudad: " + solicitud.ciudad()
                + ". Fecha aproximada: " + solicitud.fechaAproximada()
                + ". Estado: " + ("ACEPTADA".equals(nuevoEstado) ? "Aceptada" : "Rechazada")
                + ". Consulta el detalle en «Mis solicitudes».";
    }

    private Contratacion buscarContratacion(Connection connection, int idSolicitud)
            throws SQLException {

        try (PreparedStatement statement = connection.prepareStatement(
                CONSULTA_SOLICITUDES + "WHERE s.id_solicitud = ?")) {

            statement.setInt(1, idSolicitud);

            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new SQLException("No se encontró la solicitud respondida.");
                }
                return convertir(result);
            }
        }
    }

    private void validarDatos(Contratacion contratacion) {

        if (contratacion == null) {
            throw new IllegalArgumentException(
                    "La solicitud de contratación es obligatoria.");
        }

        if (contratacion.getIdCliente() <= 0
                || contratacion.getIdOrganizador() <= 0) {

            throw new IllegalArgumentException(
                    "Selecciona un cliente y un organizador válidos.");
        }

        if (contratacion.getTipoEvento() == null
                || contratacion.getTipoEvento().isBlank()
                || contratacion.getCiudad() == null
                || contratacion.getCiudad().isBlank()
                || contratacion.getFechaAproximada() == null
                || contratacion.getPresupuestoEstimado() == null) {

            throw new IllegalArgumentException(
                    "Completa el tipo de evento, la ciudad, la fecha y el presupuesto.");
        }

        if (contratacion.getTipoEvento().trim().length() > 100
                || contratacion.getCiudad().trim().length() > 150
                || (contratacion.getCriteriosBusqueda() != null
                && contratacion.getCriteriosBusqueda().trim().length()
                        > RepositoryOrganizador.LONGITUD_MAXIMA_CRITERIOS)) {

            throw new IllegalArgumentException(
                    "Uno de los datos supera la longitud permitida.");
        }

        if (contratacion.getPresupuestoEstimado().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "El presupuesto estimado no puede ser negativo.");
        }
    }

    /**
     * Bloquea y comprueba la cuenta del cliente en la base de datos.
     * Una sesión abierta de una cuenta suspendida no puede registrar.
     *
     * @return el nombre del cliente, para la notificación
     */
    private String validarCliente(Connection connection, int idCliente)
            throws SQLException {

        String sql =
                "SELECT nombre, tipo_usuario, estado FROM Usuario "
                + "WHERE id_usuario = ? FOR UPDATE";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, idCliente);

            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new IllegalArgumentException(
                            "El cliente seleccionado no existe.");
                }

                if (!"CLIENTE".equalsIgnoreCase(
                        result.getString("tipo_usuario"))) {

                    throw new IllegalArgumentException(
                            "La solicitud debe estar asociada a un cliente.");
                }

                if (!"ACTIVO".equalsIgnoreCase(result.getString("estado"))) {
                    throw new IllegalArgumentException(
                            "Tu cuenta de cliente no está activa. "
                            + "No es posible enviar la solicitud.");
                }

                return result.getString("nombre");
            }
        }
    }

    /**
     * E2: bloquea al organizador y comprueba que siga activo y verificado.
     */
    private void validarOrganizadorDisponible(
            Connection connection, int idOrganizador) throws SQLException {

        String sql =
                "SELECT o.id_organizador "
                + "FROM Organizador o "
                + "JOIN Usuario u ON u.id_usuario = o.id_usuario "
                + "WHERE o.id_organizador = ? "
                + "AND UPPER(u.tipo_usuario) = 'ORGANIZADOR' "
                + "AND UPPER(u.estado) = 'ACTIVO' "
                + "AND UPPER(o.estado) = 'ACTIVO' "
                + "AND UPPER(o.estado_verificacion) = 'VERIFICADO' "
                + "FOR UPDATE";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, idOrganizador);

            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new OrganizadorNoDisponibleException(
                            OrganizadorNoDisponibleException.NO_ACTIVO_VERIFICADO);
                }
            }
        }
    }

    /**
     * El organizador no debe tener un evento propio (no cancelado) en la
     * fecha aproximada. Es la misma regla que aplica la búsqueda.
     */
    private void validarDisponibilidadFecha(
            Connection connection, Contratacion contratacion) throws SQLException {

        String sql =
                "SELECT COUNT(*) FROM Evento e "
                + "WHERE e.id_organizador = ? "
                + "AND CAST(e.fecha_inicio AS DATE) <= ? "
                + "AND CAST(e.fecha_fin AS DATE) >= ? "
                + "AND UPPER(e.estado) <> 'CANCELADO'";

        Date fecha = Date.valueOf(contratacion.getFechaAproximada());

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, contratacion.getIdOrganizador());
            statement.setDate(2, fecha);
            statement.setDate(3, fecha);

            try (ResultSet result = statement.executeQuery()) {
                if (result.next() && result.getInt(1) > 0) {
                    throw new OrganizadorNoDisponibleException(
                            OrganizadorNoDisponibleException.SIN_DISPONIBILIDAD_FECHA);
                }
            }
        }
    }

    /**
     * Evita registrar dos veces la misma solicitud pendiente, por ejemplo
     * al reintentar después de un error. Se ejecuta con el cliente ya
     * bloqueado, así que dos envíos simultáneos no pasan ambos.
     */
    private void validarSinDuplicado(
            Connection connection, Contratacion contratacion) throws SQLException {

        String sql =
                "SELECT COUNT(*) FROM Solicitud_Contratacion_Organizador "
                + "WHERE id_cliente = ? AND id_organizador = ? "
                + "AND fecha_aproximada = ? AND UPPER(estado) = 'PENDIENTE'";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, contratacion.getIdCliente());
            statement.setInt(2, contratacion.getIdOrganizador());
            statement.setDate(3, Date.valueOf(contratacion.getFechaAproximada()));

            try (ResultSet result = statement.executeQuery()) {
                if (result.next() && result.getInt(1) > 0) {
                    throw new IllegalStateException(SOLICITUD_DUPLICADA);
                }
            }
        }
    }

    private int insertarSolicitud(
            Connection connection, Contratacion contratacion) throws SQLException {

        String sql =
                "INSERT INTO Solicitud_Contratacion_Organizador "
                + "(id_cliente, id_organizador, tipo_evento, ciudad, "
                + "fecha_aproximada, presupuesto_estimado, "
                + "criterios_busqueda, estado) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, 'PENDIENTE')";

        try (PreparedStatement statement = connection.prepareStatement(
                sql, Statement.RETURN_GENERATED_KEYS)) {

            statement.setInt(1, contratacion.getIdCliente());
            statement.setInt(2, contratacion.getIdOrganizador());
            statement.setString(3, contratacion.getTipoEvento().trim());
            statement.setString(4, contratacion.getCiudad().trim());
            statement.setDate(
                    5, Date.valueOf(contratacion.getFechaAproximada()));
            statement.setBigDecimal(
                    6, contratacion.getPresupuestoEstimado());

            String criterios = contratacion.getCriteriosBusqueda();

            if (criterios == null || criterios.isBlank()) {
                statement.setNull(7, java.sql.Types.VARCHAR);
            } else {
                statement.setString(7, criterios.trim());
            }

            statement.executeUpdate();

            try (ResultSet generated = statement.getGeneratedKeys()) {
                if (!generated.next()) {
                    throw new SQLException(
                            "No se generó el identificador de la solicitud.");
                }

                return generated.getInt(1);
            }
        }
    }

    private String mensajeNotificacion(
            String nombreCliente, Contratacion contratacion) {

        return "Nueva solicitud de contratación de "
                + nombreCliente
                + ". Tipo de evento: "
                + contratacion.getTipoEvento().trim()
                + ". Ciudad: "
                + contratacion.getCiudad().trim()
                + ". Fecha aproximada: "
                + contratacion.getFechaAproximada()
                + ". Estado: Pendiente.";
    }

    /**
     * Notificación interna para la cuenta del organizador. Se guarda en
     * id_usuario, la columna que leen los avisos del organizador, y en
     * id_usuario_destinatario, que agregó la migración 011.
     */
    private void notificar(
            Connection connection,
            int idOrganizador,
            String mensaje) throws SQLException {

        String sql = "SELECT id_usuario FROM Organizador WHERE id_organizador = ?";

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, idOrganizador);

            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new SQLException(
                            "No se pudo generar la notificación "
                            + "para el organizador seleccionado.");
                }

                insertarNotificacion(connection, result.getInt(1), mensaje);
            }
        }
    }

    /**
     * Notificación interna para una cuenta: id_usuario es la columna que
     * leen los avisos existentes; id_usuario_destinatario la agregó la 011.
     */
    private void insertarNotificacion(
            Connection connection,
            int idUsuario,
            String mensaje) throws SQLException {

        String sql =
                "INSERT INTO Notificacion_interna "
                + "(id_usuario, id_usuario_destinatario, mensaje) "
                + "VALUES (?, ?, ?)";

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, idUsuario);
            statement.setInt(2, idUsuario);
            statement.setString(3, mensaje);

            if (statement.executeUpdate() != 1) {
                throw new SQLException("No se pudo generar la notificación.");
            }
        }
    }

    private void cargarFechaSolicitud(
            Connection connection, Contratacion contratacion)
            throws SQLException {

        String sql =
                "SELECT fecha_solicitud FROM Solicitud_Contratacion_Organizador "
                + "WHERE id_solicitud = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, contratacion.getIdContratacion());

            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    Timestamp fecha = result.getTimestamp("fecha_solicitud");

                    if (fecha != null) {
                        contratacion.setFechaSolicitud(fecha.toLocalDateTime());
                    }
                }
            }
        }
    }

    private Contratacion convertir(ResultSet result) throws SQLException {

        Contratacion contratacion = new Contratacion();

        contratacion.setIdContratacion(result.getInt("id_solicitud"));
        contratacion.setIdCliente(result.getInt("id_cliente"));
        contratacion.setIdOrganizador(result.getInt("id_organizador"));
        contratacion.setTipoEvento(result.getString("tipo_evento"));
        contratacion.setCiudad(result.getString("ciudad"));

        Date fecha = result.getDate("fecha_aproximada");
        if (fecha != null) {
            contratacion.setFechaAproximada(fecha.toLocalDate());
        }

        contratacion.setPresupuestoEstimado(
                result.getBigDecimal("presupuesto_estimado"));

        contratacion.setCriteriosBusqueda(
                result.getString("criterios_busqueda"));

        contratacion.setEstado(result.getString("estado"));

        Timestamp fechaSolicitud = result.getTimestamp("fecha_solicitud");
        if (fechaSolicitud != null) {
            contratacion.setFechaSolicitud(fechaSolicitud.toLocalDateTime());
        }

        contratacion.setNombreOrganizador(
                result.getString("nombre_organizador"));

        contratacion.setNombreCliente(
                result.getString("nombre_cliente"));

        return contratacion;
    }

    private void agregarFiltroNumerico(
            StringBuilder sql,
            List<Object> parametros,
            Map<String, Object> filtros,
            String clave,
            String columna) {

        Object valor = filtros.get(clave);

        if (valor == null) {
            return;
        }

        if (!(valor instanceof Number)) {
            throw new IllegalArgumentException(
                    "El filtro " + clave + " debe ser numerico.");
        }

        sql.append(" AND ").append(columna).append(" = ?");
        parametros.add(((Number) valor).intValue());
    }

    private void agregarFiltroTexto(
            StringBuilder sql,
            List<Object> parametros,
            Map<String, Object> filtros,
            String clave,
            String columna) {

        Object valor = filtros.get(clave);

        if (valor == null || valor.toString().isBlank()) {
            return;
        }

        sql.append(" AND LOWER(").append(columna).append(") LIKE LOWER(?)");
        parametros.add("%" + valor.toString().trim() + "%");
    }
}
