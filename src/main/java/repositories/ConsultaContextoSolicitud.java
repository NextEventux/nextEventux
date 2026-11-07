package repositories;

import entities.eventos.Evento;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

import entities.ContextoSolicitud;

/**
 * Fragmentos de SQL compartidos para leer el contexto de una solicitud (CU-12).
 * Se usan sobre una consulta cuya tabla principal es
 * {@code Solicitud_Ejecucion s}. Los JOIN son opcionales para no ocultar
 * solicitudes cuyos datos relacionados falten.
 */
final class ConsultaContextoSolicitud {

    static final String COLUMNAS = """
            ev.nombre AS ctx_evento,
            ev.fecha_inicio AS ctx_inicio,
            ev.fecha_fin AS ctx_fin,
            ev.ubicacion AS ctx_ubicacion,
            ev.invitados_iniciales AS ctx_personas,
            sv.nombre AS ctx_servicio,
            uo.nombre AS ctx_organizador,
            pr.nombre AS ctx_proveedor,
            nec.descripcion AS ctx_necesidad
            """;

    static final String JOINS = """
            LEFT JOIN Evento ev
              ON ev.id_evento = s.id_evento
            LEFT JOIN Organizador org
              ON org.id_organizador = ev.id_organizador
            LEFT JOIN Usuario uo
              ON uo.id_usuario = org.id_usuario
            LEFT JOIN Servicio sv
              ON sv.id_servicio = s.id_servicio
            LEFT JOIN Proveedor pr
              ON pr.id_proveedor = s.id_proveedor
            LEFT JOIN Necesidad_Servicio_Evento nec
              ON nec.id_necesidad_servicio = s.id_necesidad_servicio
            """;

    private ConsultaContextoSolicitud() {
    }

    static ContextoSolicitud leer(ResultSet result) throws SQLException {
        int personas = result.getInt("ctx_personas");
        boolean sinPersonas = result.wasNull();

        return new ContextoSolicitud(
                result.getString("ctx_evento"),
                fecha(result.getTimestamp("ctx_inicio")),
                fecha(result.getTimestamp("ctx_fin")),
                result.getString("ctx_ubicacion"),
                sinPersonas ? null : personas,
                result.getString("ctx_servicio"),
                result.getString("ctx_organizador"),
                result.getString("ctx_proveedor"),
                result.getString("ctx_necesidad"));
    }

    private static LocalDateTime fecha(Timestamp valor) {
        return valor == null ? null : valor.toLocalDateTime();
    }
}
