package repositories;

import persistencia.DatabaseConnection;
import entities.finanzas.CompromisoFinanciero;
import entities.finanzas.MovimientoFinanciero;
import entities.finanzas.SaldoContratacion;
import entities.finanzas.SolicitudPago;
import entities.finanzas.TipoPago;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class RepositoryMovimientoFinanciero {

    /**
     * CU06: contrataciones que comprometen el presupuesto de un evento.
     * - Cuentan todas excepto las CANCELADA (una FINALIZADA ya se pagó).
     * - La solicitud de la propuesta debe ser del mismo evento que la contratación,
     *   para que un registro mal relacionado no afecte el presupuesto de otro evento.
     * - El valor es el acordado en la contratación; si no existe, el de la propuesta aceptada.
     *   No se usa el precio de catálogo ni se asume $0: si ninguno existe, valor queda null.
     */
    public List<CompromisoFinanciero> listarCompromisosPorEvento(int idEvento) {
        List<CompromisoFinanciero> compromisos = new ArrayList<>();
        String sql = """
                SELECT c.id_contratacion,
                       COALESCE(s.nombre, pr.descripcion) AS nombre,
                       COALESCE(s.categoria, 'Otros') AS categoria,
                       COALESCE(c.valor_acordado, pr.valor) AS valor,
                       c.estado
                FROM Contratacion c
                JOIN Propuesta pr ON pr.id_propuesta = c.id_propuesta
                JOIN Solicitud_Ejecucion se ON se.id_solicitud = pr.id_solicitud
                                           AND se.id_evento = c.id_evento
                LEFT JOIN Servicio s ON s.id_servicio = se.id_servicio
                WHERE c.id_evento = ?
                  AND UPPER(COALESCE(c.estado, '')) <> 'CANCELADA'
                ORDER BY c.id_contratacion
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, idEvento);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    BigDecimal valor = rs.getBigDecimal("valor");
                    compromisos.add(new CompromisoFinanciero(
                            rs.getInt("id_contratacion"),
                            rs.getString("nombre"),
                            rs.getString("categoria"),
                            valor,
                            rs.getString("estado")));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("No se pudieron consultar las contrataciones del evento.", e);
        }

        return compromisos;
    }

    /**
     * CU-10: contrataciones del evento con lo pagado y su pago programado más antiguo.
     * - Mismas contrataciones que cuenta CU06 (no CANCELADA, solicitud del mismo evento).
     * - pagado suma los EGRESO REGISTRADO de la contratación.
     * - El pago programado es el EGRESO PENDIENTE más antiguo; si no hay, queda null.
     */
    public List<SaldoContratacion> listarSaldosPorEvento(int idEvento) {
        List<SaldoContratacion> saldos = new ArrayList<>();
        String sql = """
                SELECT c.id_contratacion, c.id_propuesta, c.fecha_contratacion,
                       COALESCE(s.nombre, pr.descripcion) AS servicio,
                       COALESCE(NULLIF(pv.empresa, ''), pv.nombre) AS proveedor,
                       COALESCE(c.valor_acordado, pr.valor) AS valor,
                       (SELECT COALESCE(SUM(m.valor), 0) FROM Movimiento_financiero m
                        WHERE m.id_contratacion = c.id_contratacion AND m.id_evento = c.id_evento
                          AND UPPER(TRIM(m.tipo)) = 'EGRESO'
                          AND UPPER(TRIM(m.estado)) = 'REGISTRADO') AS pagado,
                       prog.id_movimiento AS id_programado,
                       prog.valor AS valor_programado
                FROM Contratacion c
                JOIN Propuesta pr ON pr.id_propuesta = c.id_propuesta
                JOIN Solicitud_Ejecucion se ON se.id_solicitud = pr.id_solicitud
                                           AND se.id_evento = c.id_evento
                LEFT JOIN Servicio s ON s.id_servicio = se.id_servicio
                LEFT JOIN Proveedor pv ON pv.id_proveedor = se.id_proveedor
                LEFT JOIN Movimiento_financiero prog ON prog.id_movimiento = (
                        SELECT m.id_movimiento FROM Movimiento_financiero m
                        WHERE m.id_contratacion = c.id_contratacion AND m.id_evento = c.id_evento
                          AND UPPER(TRIM(m.tipo)) = 'EGRESO'
                          AND UPPER(TRIM(m.estado)) = 'PENDIENTE' AND m.valor IS NOT NULL
                        ORDER BY m.fecha, m.id_movimiento LIMIT 1)
                WHERE c.id_evento = ?
                  AND UPPER(COALESCE(c.estado, '')) <> 'CANCELADA'
                ORDER BY c.id_contratacion
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, idEvento);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Timestamp fecha = rs.getTimestamp("fecha_contratacion");
                    saldos.add(new SaldoContratacion(
                            rs.getInt("id_contratacion"),
                            rs.getInt("id_propuesta"),
                            rs.getString("servicio"),
                            rs.getString("proveedor"),
                            fecha != null ? fecha.toLocalDateTime() : null,
                            rs.getBigDecimal("valor"),
                            rs.getBigDecimal("pagado"),
                            rs.getObject("id_programado", Integer.class),
                            rs.getBigDecimal("valor_programado")));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("No se pudieron consultar los saldos de las contrataciones.", e);
        }

        return saldos;
    }

    /**
     * CU-10 (paso 11): guarda el pago como EGRESO REGISTRADO en una sola transacción.
     * Antes de guardarlo bloquea la contratación y vuelve a calcular su saldo, para que dos
     * pagos simultáneos no superen el valor acordado.
     * - Pago nuevo: inserta un movimiento.
     * - Pago programado: pasa ese movimiento PENDIENTE a REGISTRADO, sin crear otro.
     * Devuelve el id del movimiento registrado.
     */
    public int registrarPago(SolicitudPago pago) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                verificarSaldoActual(conn, pago);
                int idMovimiento = pago.esPagoProgramado()
                        ? registrarPagoProgramado(conn, pago)
                        : insertarPago(conn, pago);
                conn.commit();
                return idMovimiento;
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException("No se pudo registrar el pago. Vuelve a intentarlo.", e);
        }
    }

    /** Vuelve a validar el evento, la contratación y el saldo dentro de la transacción. */
    private void verificarSaldoActual(Connection conn, SolicitudPago pago) throws SQLException {
        int idContratacion = pago.getContratacion().getIdContratacion();
        String evento = "SELECT estado FROM Evento WHERE id_evento = ?";
        try (PreparedStatement st = conn.prepareStatement(evento)) {
            st.setInt(1, pago.getIdEvento());
            try (ResultSet rs = st.executeQuery()) {
                if (!rs.next() || "CANCELADO".equalsIgnoreCase(texto(rs.getString("estado")))) {
                    throw new IllegalStateException("El evento fue cancelado; no se pueden registrar pagos.");
                }
            }
        }
        // FOR UPDATE bloquea la contratación hasta el commit o el rollback.
        String bloqueo = "SELECT estado FROM Contratacion WHERE id_contratacion = ? AND id_evento = ? FOR UPDATE";
        try (PreparedStatement st = conn.prepareStatement(bloqueo)) {
            st.setInt(1, idContratacion);
            st.setInt(2, pago.getIdEvento());
            try (ResultSet rs = st.executeQuery()) {
                if (!rs.next() || "CANCELADA".equalsIgnoreCase(texto(rs.getString("estado")))) {
                    throw new IllegalStateException("La contratación fue cancelada; no se puede registrar el pago.");
                }
            }
        }
        String saldo = """
                SELECT COALESCE(c.valor_acordado, pr.valor) AS valor,
                       (SELECT COALESCE(SUM(m.valor), 0) FROM Movimiento_financiero m
                        WHERE m.id_contratacion = c.id_contratacion AND m.id_evento = c.id_evento
                          AND UPPER(TRIM(m.tipo)) = 'EGRESO'
                          AND UPPER(TRIM(m.estado)) = 'REGISTRADO') AS pagado
                FROM Contratacion c
                JOIN Propuesta pr ON pr.id_propuesta = c.id_propuesta
                WHERE c.id_contratacion = ?
                """;
        try (PreparedStatement st = conn.prepareStatement(saldo)) {
            st.setInt(1, idContratacion);
            try (ResultSet rs = st.executeQuery()) {
                rs.next();
                BigDecimal valor = rs.getBigDecimal("valor");
                BigDecimal pagado = rs.getBigDecimal("pagado");
                if (valor == null) {
                    throw new IllegalStateException("La contratación no tiene valor acordado.");
                }
                BigDecimal saldoActual = valor.subtract(pagado);
                boolean anticipoConPagos = pago.getTipo() == TipoPago.ANTICIPO && pagado.signum() > 0;
                boolean finalDistinto = pago.getTipo() == TipoPago.PAGO_FINAL
                        && pago.getValor().compareTo(saldoActual) != 0;
                if (pago.getValor().compareTo(saldoActual) > 0 || anticipoConPagos || finalDistinto) {
                    throw new IllegalStateException(
                            "El saldo de la contratación cambió mientras confirmabas. Revisa el pago.");
                }
            }
        }
    }

    private int insertarPago(Connection conn, SolicitudPago pago) throws SQLException {
        String sql = "INSERT INTO Movimiento_financiero "
                + "(id_propuesta, id_evento, id_contratacion, valor, fecha, descripcion, tipo, estado) "
                + "VALUES (?, ?, ?, ?, ?, ?, 'EGRESO', 'REGISTRADO')";
        try (PreparedStatement st = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            st.setInt(1, pago.getContratacion().getIdPropuesta());
            st.setInt(2, pago.getIdEvento());
            st.setInt(3, pago.getContratacion().getIdContratacion());
            st.setBigDecimal(4, pago.getValor());
            st.setTimestamp(5, Timestamp.valueOf(fechaPago(pago.getFecha())));
            st.setString(6, pago.getDescripcion());
            st.executeUpdate();
            try (ResultSet keys = st.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    /** Pasa el pago PENDIENTE a REGISTRADO con el valor, la fecha y la descripción confirmados. */
    private int registrarPagoProgramado(Connection conn, SolicitudPago pago) throws SQLException {
        String sql = "UPDATE Movimiento_financiero SET estado = 'REGISTRADO', valor = ?, fecha = ?, descripcion = ? "
                + "WHERE id_movimiento = ? AND id_contratacion = ? AND id_evento = ? "
                + "AND UPPER(TRIM(tipo)) = 'EGRESO' AND UPPER(TRIM(estado)) = 'PENDIENTE'";
        try (PreparedStatement st = conn.prepareStatement(sql)) {
            st.setBigDecimal(1, pago.getValor());
            st.setTimestamp(2, Timestamp.valueOf(fechaPago(pago.getFecha())));
            st.setString(3, pago.getDescripcion());
            st.setInt(4, pago.getIdPagoProgramado());
            st.setInt(5, pago.getContratacion().getIdContratacion());
            st.setInt(6, pago.getIdEvento());
            if (st.executeUpdate() != 1) {
                throw new IllegalStateException("El pago programado ya no está pendiente. Revisa el pago.");
            }
            return pago.getIdPagoProgramado();
        }
    }

    /** Un pago de hoy guarda la hora actual; uno de otro día, el inicio de ese día. */
    private static LocalDateTime fechaPago(LocalDate fecha) {
        return LocalDate.now().equals(fecha) ? LocalDateTime.now() : fecha.atStartOfDay();
    }

    private static String texto(String valor) {
        return valor == null ? "" : valor.trim();
    }

    /**
     * Consulta todos los movimientos financieros asociados a un evento desde la base de datos H2.
     */
    public List<MovimientoFinanciero> listarMovimientosPorEvento(int idEvento) {
        List<MovimientoFinanciero> lista = new ArrayList<>();
        String sql = "SELECT * FROM Movimiento_financiero WHERE id_evento = ? ORDER BY fecha DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, idEvento);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapearMovimiento(rs));
                }
            }
        } catch (SQLException e) {
            // Se propaga el error para que la vista lo muestre en vez de pintar una lista vacía.
            throw new RuntimeException("No se pudieron consultar los movimientos financieros.", e);
        }

        return lista;
    }

    // Busca un movimiento financiero específico por su ID.
    public MovimientoFinanciero buscarMovimiento(int idMovimiento) {
        String sql = "SELECT * FROM Movimiento_financiero WHERE id_movimiento = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, idMovimiento);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapearMovimiento(rs);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("No se pudo consultar el movimiento financiero.", e);
        }

        return null;
    }

    /**
     * Método auxiliar para convertir una fila del ResultSet a un objeto MovimientoFinanciero.
     */
    private MovimientoFinanciero mapearMovimiento(ResultSet rs) throws SQLException {
        MovimientoFinanciero mov = new MovimientoFinanciero();
        mov.setIdMovimiento(rs.getInt("id_movimiento"));
        mov.setIdPropuesta(rs.getInt("id_propuesta"));
        mov.setIdEvento(rs.getInt("id_evento"));
        mov.setIdContratacion(rs.getInt("id_contratacion"));
        mov.setValor(rs.getBigDecimal("valor"));
        Timestamp fecha = rs.getTimestamp("fecha");
        mov.setFecha(fecha != null ? fecha.toLocalDateTime() : null);
        mov.setDescripcion(rs.getString("descripcion"));
        mov.setTipo(rs.getString("tipo"));
        mov.setEstado(rs.getString("estado"));
        return mov;
    }
}