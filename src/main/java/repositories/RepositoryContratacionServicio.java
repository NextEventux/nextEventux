package repositories;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import config.DatabaseConnection;
import entities.ConfirmacionContratacion;
import entities.DisponibilidadContratacion;
import entities.PresupuestoContratacion;
import entities.ResumenContratacionServicio;

/**
 * CU-13: contratación de un servicio a partir de la propuesta seleccionada en CU-12.
 *
 * No confundir con {@link RepositoryContratacion}, que pertenece a CU-26
 * (solicitudes para contratar organizadores). Aquí se trabaja sobre la tabla
 * Contratacion, que es la de los servicios de proveedores.
 */
public class RepositoryContratacionServicio {

    private static final String ESTADO_CONTRATACION = "ACTIVA";
    private static final String DISPONIBILIDAD_RESERVADA = "RESERVADA";

    private final RepositorySolicitudServicio repositoryPresupuesto
            = new RepositorySolicitudServicio();

    /** Datos leídos de la base para validar y contratar una propuesta. */
    private record Datos(
            int idPropuesta,
            int numeroVersion,
            BigDecimal valor,
            String alcance,
            String condiciones,
            String estadoPropuesta,
            boolean vigente,
            int idSolicitud,
            String estadoSolicitud,
            int idNecesidad,
            String estadoNecesidad,
            int idPropuestaSeleccionada,
            String necesidad,
            int idEvento,
            String nombreEvento,
            LocalDateTime inicioEvento,
            LocalDateTime finEvento,
            String estadoEvento,
            BigDecimal presupuesto,
            int idServicio,
            String nombreServicio,
            String estadoServicio,
            int idProveedor,
            String nombreProveedor,
            String estadoProveedor,
            int idUsuarioProveedor,
            String estadoUsuarioProveedor) {
    }

    /** Horario encontrado en la disponibilidad del proveedor. */
    private record Reserva(
            int idDisponibilidad,
            LocalDate fecha,
            LocalTime inicio,
            LocalTime fin) {
    }

    private record ResultadoDisponibilidad(
            Reserva reserva,
            DisponibilidadContratacion detalle) {
    }

    // ------------------------------------------------------------------
    // Pasos 1 y 2: recuperar la versión exacta de la propuesta
    // ------------------------------------------------------------------

    public ResumenContratacionServicio consultarResumen(
            int idOrganizador,
            int idPropuesta) {

        try (Connection connection = DatabaseConnection.getConnection()) {

            Datos datos = leer(connection, idOrganizador, idPropuesta);
            validarContratable(connection, datos);

            return new ResumenContratacionServicio(
                    datos.idPropuesta(),
                    datos.numeroVersion(),
                    datos.valor(),
                    datos.alcance(),
                    datos.condiciones(),
                    datos.idSolicitud(),
                    datos.idNecesidad(),
                    datos.necesidad(),
                    datos.idEvento(),
                    datos.nombreEvento(),
                    datos.inicioEvento(),
                    datos.finEvento(),
                    datos.idServicio(),
                    datos.nombreServicio(),
                    datos.idProveedor(),
                    datos.nombreProveedor());

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "No se pudo consultar la propuesta seleccionada.", e);
        }
    }

    // ------------------------------------------------------------------
    // Paso 3: disponibilidad del proveedor
    // ------------------------------------------------------------------

    public DisponibilidadContratacion verificarDisponibilidad(
            int idOrganizador,
            int idPropuesta) {

        try (Connection connection = DatabaseConnection.getConnection()) {

            Datos datos = leer(connection, idOrganizador, idPropuesta);
            validarContratable(connection, datos);

            return buscarDisponibilidad(connection, datos, false).detalle();

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "No se pudo verificar la disponibilidad del proveedor.", e);
        }
    }

    // ------------------------------------------------------------------
    // Pasos 4 y 5: presupuesto del evento (Gestión Financiera)
    // ------------------------------------------------------------------

    public PresupuestoContratacion verificarPresupuesto(
            int idOrganizador,
            int idPropuesta) {

        try (Connection connection = DatabaseConnection.getConnection()) {

            Datos datos = leer(connection, idOrganizador, idPropuesta);
            validarContratable(connection, datos);

            return calcularPresupuesto(connection, datos);

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "No se pudo comprobar el presupuesto del evento.", e);
        }
    }

    // ------------------------------------------------------------------
    // Pasos 7 y 8: formalizar la contratación
    // ------------------------------------------------------------------

    /**
     * Formaliza la contratación en una sola transacción. Todo se verifica de
     * nuevo con el evento y la necesidad bloqueados, porque entre la revisión
     * del organizador y la confirmación otra operación pudo ocupar el horario
     * o comprometer el presupuesto.
     */
    public ConfirmacionContratacion contratar(
            int idOrganizador,
            int idPropuesta) {

        try (Connection connection = DatabaseConnection.getConnection()) {

            connection.setAutoCommit(false);

            try {
                Datos previo = leer(connection, idOrganizador, idPropuesta);

                // Mismo orden de bloqueo que CU-12: evento y luego necesidad
                bloquear(connection,
                        "SELECT id_evento FROM Evento WHERE id_evento = ? FOR UPDATE",
                        previo.idEvento());

                if (previo.idNecesidad() > 0) {
                    bloquear(connection,
                            "SELECT id_necesidad_servicio FROM Necesidad_Servicio_Evento "
                            + "WHERE id_necesidad_servicio = ? FOR UPDATE",
                            previo.idNecesidad());
                }

                Datos datos = leer(connection, idOrganizador, idPropuesta);
                validarContratable(connection, datos);

                // A2: el proveedor ya no está disponible
                ResultadoDisponibilidad disponibilidad
                        = buscarDisponibilidad(connection, datos, true);

                if (!disponibilidad.detalle().disponible()) {
                    throw new IllegalStateException(
                            disponibilidad.detalle().mensaje());
                }

                // A1: el presupuesto no alcanza
                PresupuestoContratacion presupuesto
                        = calcularPresupuesto(connection, datos);

                if (!presupuesto.suficiente()) {
                    throw new IllegalArgumentException(presupuesto.mensaje());
                }

                Reserva reserva = disponibilidad.reserva();

                int idContratacion = insertarContratacion(
                        connection, datos, reserva);

                reservarHorario(connection, reserva);
                actualizarEstados(connection, datos);
                notificar(connection, datos);

                connection.commit();

                return new ConfirmacionContratacion(
                        idContratacion,
                        datos.idPropuesta(),
                        datos.numeroVersion(),
                        datos.valor(),
                        datos.alcance(),
                        datos.condiciones(),
                        datos.nombreEvento(),
                        datos.nombreServicio(),
                        datos.nombreProveedor(),
                        reserva.fecha(),
                        reserva.inicio(),
                        reserva.fin(),
                        presupuesto.disponibleDespues(),
                        LocalDateTime.now());

            } catch (SQLException | RuntimeException e) {

                try {
                    connection.rollback();
                } catch (SQLException errorRollback) {
                    e.addSuppressed(errorRollback);
                }

                throw e;
            }

        } catch (SQLException e) {

            // Violación de las restricciones únicas por propuesta o necesidad
            if ("23505".equals(e.getSQLState())) {
                throw new IllegalStateException(
                        "Esta necesidad ya está cubierta con una contratación.", e);
            }

            throw new IllegalStateException(
                    "No se pudo formalizar la contratación. No se reservó el "
                    + "horario ni se comprometió presupuesto; vuelve a intentarlo.",
                    e);
        }
    }

    // ------------------------------------------------------------------
    // Lectura y validaciones
    // ------------------------------------------------------------------

    private Datos leer(
            Connection connection,
            int idOrganizador,
            int idPropuesta) throws SQLException {

        String sql = """
                SELECT p.id_propuesta, p.numero_version, p.valor,
                       p.descripcion, p.condiciones,
                       p.estado AS estado_propuesta, p.vigente,
                       s.id_solicitud, s.estado AS estado_solicitud,
                       s.id_necesidad_servicio, s.id_servicio,
                       n.estado AS estado_necesidad,
                       n.id_propuesta_seleccionada,
                       n.descripcion AS necesidad,
                       e.id_evento, e.nombre AS evento,
                       e.fecha_inicio, e.fecha_fin,
                       e.estado AS estado_evento, e.presupuesto,
                       v.nombre AS servicio, v.estado AS estado_servicio,
                       s.id_proveedor, pr.nombre AS proveedor,
                       pr.estado AS estado_proveedor,
                       pr.id_usuario AS usuario_proveedor,
                       u.estado AS estado_usuario
                FROM Propuesta p
                JOIN Solicitud_Ejecucion s
                  ON s.id_solicitud = p.id_solicitud
                JOIN Evento e
                  ON e.id_evento = s.id_evento
                LEFT JOIN Necesidad_Servicio_Evento n
                  ON n.id_necesidad_servicio = s.id_necesidad_servicio
                LEFT JOIN Servicio v
                  ON v.id_servicio = s.id_servicio
                LEFT JOIN Proveedor pr
                  ON pr.id_proveedor = s.id_proveedor
                LEFT JOIN Usuario u
                  ON u.id_usuario = pr.id_usuario
                WHERE p.id_propuesta = ?
                  AND e.id_organizador = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, idPropuesta);
            statement.setInt(2, idOrganizador);

            try (ResultSet r = statement.executeQuery()) {

                if (!r.next()) {
                    throw new IllegalStateException(
                            "La propuesta no existe o no pertenece a tus eventos.");
                }

                Timestamp inicio = r.getTimestamp("fecha_inicio");
                Timestamp fin = r.getTimestamp("fecha_fin");

                return new Datos(
                        r.getInt("id_propuesta"),
                        r.getInt("numero_version"),
                        r.getBigDecimal("valor"),
                        r.getString("descripcion"),
                        r.getString("condiciones"),
                        r.getString("estado_propuesta"),
                        r.getBoolean("vigente"),
                        r.getInt("id_solicitud"),
                        r.getString("estado_solicitud"),
                        r.getInt("id_necesidad_servicio"),
                        r.getString("estado_necesidad"),
                        r.getInt("id_propuesta_seleccionada"),
                        r.getString("necesidad"),
                        r.getInt("id_evento"),
                        r.getString("evento"),
                        inicio == null ? null : inicio.toLocalDateTime(),
                        fin == null ? null : fin.toLocalDateTime(),
                        r.getString("estado_evento"),
                        r.getBigDecimal("presupuesto"),
                        r.getInt("id_servicio"),
                        r.getString("servicio"),
                        r.getString("estado_servicio"),
                        r.getInt("id_proveedor"),
                        r.getString("proveedor"),
                        r.getString("estado_proveedor"),
                        r.getInt("usuario_proveedor"),
                        r.getString("estado_usuario"));
            }
        }
    }

    /** Precondiciones de CU-13 y excepciones E1 y E2. */
    private void validarContratable(
            Connection connection,
            Datos d) throws SQLException {

        if (d.idNecesidad() <= 0) {
            throw new IllegalStateException(
                    "La propuesta no tiene una necesidad asociada.");
        }

        // E1: la necesidad ya está cubierta
        if ("CONTRATADA".equalsIgnoreCase(d.estadoNecesidad())
                || "CONTRATADA".equalsIgnoreCase(d.estadoPropuesta())
                || existeContratacion(connection, d.idNecesidad())) {

            throw new IllegalStateException(
                    "Esta necesidad ya está cubierta con una contratación.");
        }

        // E2: solo se formaliza la propuesta seleccionada y vigente
        if (!d.vigente()
                || d.idPropuestaSeleccionada() != d.idPropuesta()
                || !"EN_NEGOCIACION".equalsIgnoreCase(d.estadoSolicitud())) {

            throw new IllegalStateException(
                    "Solo se puede contratar la propuesta que está "
                    + "seleccionada para contratación.");
        }

        if (d.estadoEvento() == null
                || "CANCELADO".equalsIgnoreCase(d.estadoEvento())
                || "FINALIZADO".equalsIgnoreCase(d.estadoEvento())
                || (d.finEvento() != null
                && d.finEvento().isBefore(LocalDateTime.now()))) {

            throw new IllegalStateException(
                    "El evento ya no está vigente.");
        }

        if (d.idServicio() <= 0 || d.nombreServicio() == null) {
            throw new IllegalStateException(
                    "La solicitud no está asociada a un servicio publicado.");
        }

        if (!"DISPONIBLE".equalsIgnoreCase(d.estadoServicio())) {
            throw new IllegalStateException(
                    "El servicio ya no está disponible.");
        }

        boolean proveedorVerificado
                = d.estadoProveedor() != null
                && ("APROBADO".equalsIgnoreCase(d.estadoProveedor())
                || "VERIFICADO".equalsIgnoreCase(d.estadoProveedor()))
                && "ACTIVO".equalsIgnoreCase(d.estadoUsuarioProveedor());

        if (!proveedorVerificado) {
            throw new IllegalStateException(
                    "El proveedor ya no está verificado.");
        }

        if (d.valor() == null || d.valor().signum() <= 0) {
            throw new IllegalStateException(
                    "La propuesta no tiene un valor válido.");
        }

        if (d.presupuesto() == null) {
            throw new IllegalStateException(
                    "El evento no tiene un presupuesto configurado en Gestión Financiera.");
        }
    }

    private boolean existeContratacion(
            Connection connection,
            int idNecesidad) throws SQLException {

        String sql = """
                SELECT COUNT(*)
                FROM Contratacion c
                LEFT JOIN Propuesta p
                  ON p.id_propuesta = c.id_propuesta
                LEFT JOIN Solicitud_Ejecucion s
                  ON s.id_solicitud = p.id_solicitud
                WHERE (c.id_necesidad_servicio = ?
                       OR s.id_necesidad_servicio = ?)
                  AND UPPER(COALESCE(c.estado, '')) <> 'CANCELADA'
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, idNecesidad);
            statement.setInt(2, idNecesidad);

            try (ResultSet r = statement.executeQuery()) {
                r.next();
                return r.getInt(1) > 0;
            }
        }
    }

    // ------------------------------------------------------------------
    // Disponibilidad
    // ------------------------------------------------------------------

    /**
     * Busca en la disponibilidad del proveedor un horario que cubra el
     * evento completo. La contratación reserva ese horario por completo.
     */
    private ResultadoDisponibilidad buscarDisponibilidad(
            Connection connection,
            Datos d,
            boolean bloquear) throws SQLException {

        LocalDateTime inicio = d.inicioEvento();
        LocalDateTime fin = d.finEvento();

        if (inicio == null || fin == null) {
            return sinDisponibilidad(null, null, null,
                    "El evento no tiene fecha y horario definidos.");
        }

        LocalDate dia = inicio.toLocalDate();
        LocalTime horaInicio = inicio.toLocalTime();
        LocalTime horaFin = fin.toLocalTime();

        if (!fin.toLocalDate().equals(dia)) {
            return sinDisponibilidad(dia, horaInicio, horaFin,
                    "El evento abarca más de un día y la disponibilidad del "
                    + "proveedor se registra por día. Ajusta las fechas del "
                    + "evento o acuerda otra modalidad con el proveedor.");
        }

        if (!horaFin.isAfter(horaInicio)) {
            return sinDisponibilidad(dia, horaInicio, horaFin,
                    "El horario del evento no es válido.");
        }

        String sql = """
                SELECT id_disponibilidad, hora_inicio, hora_fin
                FROM Disponibilidad_Servicio
                WHERE id_servicio = ?
                  AND fecha = ?
                  AND UPPER(estado) = 'DISPONIBLE'
                  AND hora_inicio <= ?
                  AND hora_fin >= ?
                ORDER BY hora_inicio DESC, hora_fin ASC
                LIMIT 1
                """ + (bloquear ? " FOR UPDATE" : "");

        Reserva reserva = null;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, d.idServicio());
            statement.setDate(2, Date.valueOf(dia));
            statement.setTime(3, Time.valueOf(horaInicio));
            statement.setTime(4, Time.valueOf(horaFin));

            try (ResultSet r = statement.executeQuery()) {

                if (r.next()) {
                    reserva = new Reserva(
                            r.getInt("id_disponibilidad"),
                            dia,
                            r.getTime("hora_inicio").toLocalTime(),
                            r.getTime("hora_fin").toLocalTime());
                }
            }
        }

        if (reserva == null) {
            return sinDisponibilidad(dia, horaInicio, horaFin,
                    "El proveedor ya no tiene disponible el " + dia
                    + " de " + horaInicio + " a " + horaFin + ".");
        }

        if (existeCruceContratacion(connection, d.idServicio(), reserva)) {
            return sinDisponibilidad(dia, horaInicio, horaFin,
                    "El proveedor ya tiene una contratación confirmada que "
                    + "se cruza con el horario del evento.");
        }

        return new ResultadoDisponibilidad(
                reserva,
                new DisponibilidadContratacion(
                        true, dia, horaInicio, horaFin,
                        reserva.inicio(), reserva.fin(),
                        "El proveedor está disponible el " + dia + " de "
                        + reserva.inicio() + " a " + reserva.fin() + "."));
    }

    private ResultadoDisponibilidad sinDisponibilidad(
            LocalDate dia,
            LocalTime inicio,
            LocalTime fin,
            String mensaje) {

        return new ResultadoDisponibilidad(
                null,
                new DisponibilidadContratacion(
                        false, dia, inicio, fin, null, null, mensaje));
    }

    private boolean existeCruceContratacion(
            Connection connection,
            int idServicio,
            Reserva reserva) throws SQLException {

        String sql = """
                SELECT COUNT(*)
                FROM Contratacion c
                JOIN Propuesta p
                  ON p.id_propuesta = c.id_propuesta
                JOIN Solicitud_Ejecucion s
                  ON s.id_solicitud = p.id_solicitud
                WHERE s.id_servicio = ?
                  AND UPPER(c.estado) IN ('ACTIVA', 'CONFIRMADA')
                  AND c.fecha_servicio = ?
                  AND ? < c.hora_fin
                  AND ? > c.hora_inicio
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, idServicio);
            statement.setDate(2, Date.valueOf(reserva.fecha()));
            statement.setTime(3, Time.valueOf(reserva.inicio()));
            statement.setTime(4, Time.valueOf(reserva.fin()));

            try (ResultSet r = statement.executeQuery()) {
                r.next();
                return r.getInt(1) > 0;
            }
        }
    }

    // ------------------------------------------------------------------
    // Presupuesto
    // ------------------------------------------------------------------

    private PresupuestoContratacion calcularPresupuesto(
            Connection connection,
            Datos d) throws SQLException {

        // Misma regla de CU-01 y CU-06; se excluye la necesidad que se va a cubrir
        BigDecimal comprometido
                = repositoryPresupuesto.comprometidoSinNecesidad(
                        connection, d.idEvento(), d.idNecesidad());

        BigDecimal disponibleDespues
                = d.presupuesto().subtract(comprometido).subtract(d.valor());

        boolean suficiente = disponibleDespues.signum() >= 0;

        BigDecimal faltante = suficiente
                ? BigDecimal.ZERO
                : disponibleDespues.negate();

        String mensaje = suficiente
                ? "El presupuesto alcanza. Después de contratar quedarían "
                + cop(disponibleDespues) + " disponibles."
                : "Presupuesto insuficiente. Disponible: "
                + cop(d.presupuesto().subtract(comprometido))
                + "; faltan " + cop(faltante)
                + " para contratar por " + cop(d.valor())
                + ". Ajusta el presupuesto en Gestión Financiera o "
                + "selecciona otra propuesta.";

        return new PresupuestoContratacion(
                d.presupuesto(),
                comprometido,
                d.valor(),
                disponibleDespues,
                suficiente,
                faltante,
                mensaje);
    }

    // ------------------------------------------------------------------
    // Escritura
    // ------------------------------------------------------------------

    private void bloquear(
            Connection connection,
            String sql,
            int id) throws SQLException {

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            statement.executeQuery().close();
        }
    }

    private int insertarContratacion(
            Connection connection,
            Datos d,
            Reserva reserva) throws SQLException {

        String sql = """
                INSERT INTO Contratacion
                (id_propuesta, id_evento, fecha_contratacion, estado,
                 condiciones, valor_acordado, fecha_servicio,
                 hora_inicio, hora_fin, id_disponibilidad,
                 id_necesidad_servicio, numero_version_propuesta,
                 alcance_propuesta)
                VALUES (?, ?, CURRENT_TIMESTAMP, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement statement = connection.prepareStatement(
                sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {

            statement.setInt(1, d.idPropuesta());
            statement.setInt(2, d.idEvento());
            statement.setString(3, ESTADO_CONTRATACION);
            statement.setString(4, d.condiciones());
            statement.setBigDecimal(5, d.valor());
            statement.setDate(6, Date.valueOf(reserva.fecha()));
            statement.setTime(7, Time.valueOf(reserva.inicio()));
            statement.setTime(8, Time.valueOf(reserva.fin()));
            statement.setInt(9, reserva.idDisponibilidad());
            statement.setInt(10, d.idNecesidad());
            statement.setInt(11, d.numeroVersion());
            statement.setString(12, d.alcance());

            statement.executeUpdate();

            try (ResultSet claves = statement.getGeneratedKeys()) {

                if (!claves.next()) {
                    throw new IllegalStateException(
                            "No se pudo obtener el identificador de la contratación.");
                }

                return claves.getInt(1);
            }
        }
    }

    // Solo al formalizar se reserva la fecha y el horario del proveedor
    private void reservarHorario(
            Connection connection,
            Reserva reserva) throws SQLException {

        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE Disponibilidad_Servicio SET estado = ? "
                + "WHERE id_disponibilidad = ? "
                + "AND UPPER(estado) = 'DISPONIBLE'")) {

            statement.setString(1, DISPONIBILIDAD_RESERVADA);
            statement.setInt(2, reserva.idDisponibilidad());

            if (statement.executeUpdate() != 1) {
                throw new IllegalStateException(
                        "El proveedor ya no tiene disponible ese horario.");
            }
        }
    }

    private void actualizarEstados(
            Connection connection,
            Datos d) throws SQLException {

        // Propuesta contratada; las demás de la necesidad conservan su historial
        ejecutar(connection,
                "UPDATE Propuesta SET estado = 'CONTRATADA' "
                + "WHERE id_propuesta = ?",
                d.idPropuesta());

        ejecutar(connection,
                "UPDATE Propuesta SET estado = 'NO_SELECCIONADA' "
                + "WHERE id_solicitud IN (SELECT id_solicitud "
                + "FROM Solicitud_Ejecucion "
                + "WHERE id_necesidad_servicio = ? AND id_solicitud <> ? "
                + "AND UPPER(estado) IN ('PENDIENTE', 'EN_NEGOCIACION')) "
                + "AND UPPER(estado) <> 'RECHAZADA'",
                d.idNecesidad(), d.idSolicitud());

        ejecutar(connection,
                "UPDATE Solicitud_Ejecucion SET estado = 'NO_SELECCIONADA' "
                + "WHERE id_necesidad_servicio = ? AND id_solicitud <> ? "
                + "AND UPPER(estado) IN ('PENDIENTE', 'EN_NEGOCIACION')",
                d.idNecesidad(), d.idSolicitud());

        ejecutar(connection,
                "UPDATE Solicitud_Ejecucion SET estado = 'CONTRATADA' "
                + "WHERE id_solicitud = ?",
                d.idSolicitud());

        ejecutar(connection,
                "UPDATE Necesidad_Servicio_Evento SET estado = 'CONTRATADA' "
                + "WHERE id_necesidad_servicio = ?",
                d.idNecesidad());
    }

    private void notificar(
            Connection connection,
            Datos d) throws SQLException {

        // Proveedor contratado
        insertarAviso(connection, d.idUsuarioProveedor(), d.idEvento(),
                "Tu propuesta #" + d.idPropuesta() + " (versión "
                + d.numeroVersion() + ") fue contratada para el evento "
                + d.nombreEvento() + ". Tu horario quedó reservado.");

        // Proveedores cuyas propuestas quedaron como no seleccionadas
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT DISTINCT pr.id_usuario "
                + "FROM Solicitud_Ejecucion s "
                + "JOIN Proveedor pr ON pr.id_proveedor = s.id_proveedor "
                + "WHERE s.id_necesidad_servicio = ? AND s.id_solicitud <> ? "
                + "AND UPPER(s.estado) = 'NO_SELECCIONADA'")) {

            statement.setInt(1, d.idNecesidad());
            statement.setInt(2, d.idSolicitud());

            try (ResultSet r = statement.executeQuery()) {

                while (r.next()) {
                    insertarAviso(connection, r.getInt(1), d.idEvento(),
                            "El organizador contrató otra propuesta para la "
                            + "necesidad del evento " + d.nombreEvento()
                            + ". Tu propuesta quedó como no seleccionada.");
                }
            }
        }
    }

    private void insertarAviso(
            Connection connection,
            int idUsuario,
            int idEvento,
            String mensaje) throws SQLException {

        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO Notificacion_interna "
                + "(id_usuario, id_evento, mensaje) VALUES (?, ?, ?)")) {

            statement.setInt(1, idUsuario);
            statement.setInt(2, idEvento);
            statement.setString(3, mensaje);
            statement.executeUpdate();
        }
    }

    private void ejecutar(
            Connection connection,
            String sql,
            int... valores) throws SQLException {

        try (PreparedStatement statement = connection.prepareStatement(sql)) {

            for (int i = 0; i < valores.length; i++) {
                statement.setInt(i + 1, valores[i]);
            }

            statement.executeUpdate();
        }
    }

    /** Formato de pesos colombianos para mensajes: $ 8.600.000 COP. */
    private static String cop(BigDecimal valor) {
        java.text.DecimalFormat formato = new java.text.DecimalFormat(
                "#,##0.##",
                java.text.DecimalFormatSymbols.getInstance(
                        java.util.Locale.forLanguageTag("es-CO")));
        return "$ " + formato.format(valor) + " COP";
    }
}
