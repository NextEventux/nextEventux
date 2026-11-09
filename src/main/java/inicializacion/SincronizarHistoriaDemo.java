package inicializacion;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class SincronizarHistoriaDemo {

    public record Resultado(
            int necesidadesCreadas,
            int solicitudesCreadas,
            int propuestasCreadas,
            int contratacionesCreadas) {
    }

    private record EventoDemo(String clave, String organizador) {
    }

    private record NecesidadDemo(
            String clave,
            String evento,
            String descripcion,
            String estado,
            String propuestaSeleccionada) {
    }

    private record SolicitudDemo(
            String clave,
            String evento,
            String proveedor,
            String servicio,
            String necesidad,
            LocalDateTime fecha,
            String estado,
            String motivoRechazo,
            LocalDateTime fechaRespuesta,
            String costoComprometido) {
    }

    private record PropuestaDemo(
            String clave,
            String solicitud,
            int version,
            String valor,
            String descripcion,
            String estado,
            String condiciones,
            LocalDateTime fechaEnvio,
            boolean vigente,
            String materialApoyo) {
    }

    private record ContratacionDemo(
            String clave,
            String propuesta,
            String evento,
            LocalDateTime fecha,
            String estado,
            String condiciones,
            String valorAcordado,
            int versionPropuesta,
            String alcancePropuesta) {
    }

    private record Asociacion(String tipo, String clave, Integer id, boolean registrada) {
    }

    private record NecesidadExistente(Integer propuestaSeleccionada) {
    }

    private record NecesidadValidada(
            Asociacion asociacion,
            NecesidadExistente existente) {
    }

    private static final String NECESIDAD = "NECESIDAD";
    private static final String SOLICITUD = "SOLICITUD";
    private static final String PROPUESTA = "PROPUESTA";
    private static final String CONTRATACION = "CONTRATACION";

    private static final List<EventoDemo> EVENTOS = List.of(
            new EventoDemo("boda-laura", "laura.gomez"),
            new EventoDemo("baby-shower-valentina", "carlos.rodriguez"),
            new EventoDemo("quince-sofia", "valentina"),
            new EventoDemo("graduacion-medicina", "valentina"));

    private static final List<NecesidadDemo> NECESIDADES = List.of(
            new NecesidadDemo("necesidad-1", "quince-sofia",
                    "Decoración temática de unicornios", "ABIERTA", null),
            new NecesidadDemo("necesidad-2", "quince-sofia",
                    "Catering para 70 personas", "ABIERTA", null),
            new NecesidadDemo("necesidad-3", "quince-sofia",
                    "Fotografía del evento", "ABIERTA", "propuesta-6"),
            new NecesidadDemo("necesidad-4", "graduacion-medicina",
                    "Fotografía y video de la ceremonia", "ABIERTA", null),
            new NecesidadDemo("necesidad-5", "graduacion-medicina",
                    "Mesa de postres", "ABIERTA", null),
            new NecesidadDemo("necesidad-6", "graduacion-medicina",
                    "Decoración de mesas", "ABIERTA", null),
            new NecesidadDemo("necesidad-7", "quince-sofia",
                    "Cóctel de bienvenida", "ABIERTA", null),
            new NecesidadDemo("necesidad-8", "baby-shower-valentina",
                    "Fotografía del baby shower", "ABIERTA", null));

    private static final List<SolicitudDemo> SOLICITUDES = List.of(
            solicitud("solicitud-1", "boda-laura", "decoraciones.arcoiris",
                    "decoracion-floral", null, "2026-10-08T10:00:00",
                    "CONTRATADA", null, "2026-10-08T12:00:00", null),
            solicitud("solicitud-2", "boda-laura", "sabores.eventos",
                    "catering-basico", null, "2026-10-08T11:30:00",
                    "CONTRATADA", null, "2026-10-08T13:00:00", null),
            solicitud("solicitud-3", "baby-shower-valentina", "decoraciones.arcoiris",
                    "decoracion-tematica", null, "2026-10-09T09:00:00",
                    "EN_NEGOCIACION", null, "2026-10-09T15:00:00", "900000.00"),
            solicitud("solicitud-4", "quince-sofia", "decoraciones.arcoiris",
                    "decoracion-tematica", "necesidad-1", "2026-11-01T09:00:00",
                    "PENDIENTE", null, null, "900000.00"),
            solicitud("solicitud-5", "quince-sofia", "sabores.eventos",
                    "catering-gourmet", "necesidad-2", "2026-11-01T09:30:00",
                    "EN_NEGOCIACION", null, "2026-11-01T14:00:00", "1900000.00"),
            solicitud("solicitud-6", "quince-sofia", "flash",
                    "cobertura-fotografica", "necesidad-3", "2026-11-01T10:00:00",
                    "EN_NEGOCIACION", null, "2026-11-01T12:00:00", "1500000.00"),
            solicitud("solicitud-7", "graduacion-medicina", "flash",
                    "video-highlights", "necesidad-4", "2026-11-01T10:30:00",
                    "EN_NEGOCIACION", null, "2026-11-01T16:00:00", "950000.00"),
            solicitud("solicitud-8", "quince-sofia", "sabores.eventos",
                    "catering-basico", "necesidad-2", "2026-11-01T11:00:00",
                    "RECHAZADA",
                    "El catering básico atiende hasta 50 personas; te recomendamos el catering gourmet.",
                    "2026-11-01T13:00:00", "1800000.00"),
            solicitud("solicitud-9", "graduacion-medicina", "decoraciones.arcoiris",
                    "arreglos-mesa", "necesidad-6", "2026-11-01T11:30:00",
                    "PENDIENTE", null, null, "450000.00"),
            solicitud("solicitud-10", "quince-sofia", "decoraciones.arcoiris",
                    "arreglos-mesa", "necesidad-1", "2026-11-01T12:00:00",
                    "PROPUESTA_RECHAZADA", null, "2026-11-01T17:00:00", "450000.00"),
            solicitud("solicitud-11", "quince-sofia", "sabores.eventos",
                    "coctel-bienvenida", "necesidad-7", "2026-11-02T09:00:00",
                    "PENDIENTE", null, null, "520000.00"),
            solicitud("solicitud-12", "baby-shower-valentina", "flash",
                    "cobertura-fotografica", "necesidad-8", "2026-11-02T09:30:00",
                    "PENDIENTE", null, null, "1500000.00"),
            solicitud("solicitud-13", "graduacion-medicina", "flash",
                    "cobertura-fotografica", "necesidad-4", "2026-11-02T10:00:00",
                    "RECHAZADA", "Ya tenemos otro evento ese día a esa hora.",
                    "2026-11-02T12:00:00", "1500000.00"),
            solicitud("solicitud-14", "quince-sofia", "flash",
                    "video-highlights", "necesidad-3", "2026-11-02T10:30:00",
                    "EN_NEGOCIACION", null, "2026-11-02T15:00:00", "950000.00"),
            solicitud("solicitud-15", "graduacion-medicina", "sabores.eventos",
                    "mesa-postres", "necesidad-5", "2026-11-02T11:00:00",
                    "EN_NEGOCIACION", null, "2026-11-02T16:00:00", "650000.00"));

    private static final List<PropuestaDemo> PROPUESTAS = List.of(
            propuesta("propuesta-1", "solicitud-1", 1, "1800000.00",
                    "Decoración completa para la boda", "CONTRATADA",
                    "Montaje terminado tres horas antes del evento", "2026-10-08T12:00:00",
                    true, null),
            propuesta("propuesta-2", "solicitud-2", 1, "2200000.00",
                    "Servicio de catering y postres", "CONTRATADA",
                    "Servicio de alimentación para 120 personas", "2026-10-08T13:00:00",
                    true, null),
            propuesta("propuesta-3", "solicitud-3", 1, "900000.00",
                    "Decoración temática para baby shower", "ENVIADA",
                    "Anticipo del 50% para reservar la fecha", "2026-10-09T15:00:00",
                    true, null),
            propuesta("propuesta-4", "solicitud-5", 1, "1900000.00",
                    "Menú de tres tiempos para 70 personas con 4 meseros y montaje de buffet",
                    "ENVIADA", "Anticipo del 40% y saldo al finalizar el evento",
                    "2026-11-01T14:00:00", true, "https://ejemplo.com/menu-gourmet.pdf"),
            propuesta("propuesta-5", "solicitud-6", 1, "1500000.00",
                    "Cobertura fotográfica de 10 horas con galería digital", "ENVIADA",
                    "Entrega de la galería en 15 días", "2026-11-01T12:00:00",
                    false, "https://ejemplo.com/portafolio-fotografia.pdf"),
            propuesta("propuesta-6", "solicitud-6", 2, "1350000.00",
                    "Cobertura fotográfica de 8 horas con galería digital y 300 fotos editadas",
                    "SELECCIONADA", "Entrega de la galería en 15 días. Anticipo del 30%",
                    "2026-11-02T10:00:00", true,
                    "https://ejemplo.com/portafolio-fotografia.pdf"),
            propuesta("propuesta-7", "solicitud-7", 1, "950000.00",
                    "Video highlights de cinco minutos de la ceremonia", "ENVIADA",
                    "Entrega en 20 días en formato digital", "2026-11-01T16:00:00",
                    true, null),
            propuesta("propuesta-8", "solicitud-10", 1, "450000.00",
                    "Centros de mesa con flores de temporada", "RECHAZADA",
                    "Montaje dos horas antes del evento", "2026-11-01T17:00:00",
                    false, null),
            propuesta("propuesta-9", "solicitud-14", 1, "800000.00",
                    "Video highlights de tres minutos para redes sociales", "ENVIADA",
                    "Entrega en 10 días", "2026-11-02T15:00:00", true, null),
            propuesta("propuesta-10", "solicitud-15", 1, "700000.00",
                    "Mesa de postres para 90 personas con seis variedades", "ENVIADA",
                    "Entrega y montaje incluidos", "2026-11-02T16:00:00", true, null));

    private static final List<ContratacionDemo> CONTRATACIONES = List.of(
            new ContratacionDemo("contratacion-1", "propuesta-1", "boda-laura",
                    LocalDateTime.parse("2026-10-10T14:00:00"), "ACTIVA",
                    "Montaje terminado tres horas antes del evento", "1800000.00",
                    1, "Decoración completa para la boda"),
            new ContratacionDemo("contratacion-2", "propuesta-2", "boda-laura",
                    LocalDateTime.parse("2026-10-10T15:00:00"), "ACTIVA",
                    "Servicio de alimentación para 120 personas", "2200000.00",
                    1, "Servicio de catering y postres"));

    private SincronizarHistoriaDemo() {
    }

    public static synchronized Resultado sincronizar(Connection conexion) throws SQLException {
        Map<String, Integer> eventos = validarCatalogo(conexion, EVENTOS);
        Map<String, Integer> servicios = validarServicios(conexion);
        Set<String> idsUsados = new HashSet<>();

        Map<String, Asociacion> necesidades = new LinkedHashMap<>();
        Map<String, NecesidadExistente> necesidadesExistentes = new HashMap<>();
        for (NecesidadDemo necesidad : NECESIDADES) {
            NecesidadValidada validada = validarNecesidad(
                    conexion, necesidad, eventos.get(necesidad.evento()), idsUsados);
            Asociacion asociacion = validada.asociacion();
            necesidades.put(necesidad.clave(), asociacion);
            if (validada.existente() != null) {
                necesidadesExistentes.put(necesidad.clave(), validada.existente());
            }
        }

        Map<String, Integer> proveedores = new HashMap<>();
        Map<String, Asociacion> solicitudes = new LinkedHashMap<>();
        for (SolicitudDemo solicitud : SOLICITUDES) {
            int idServicio = exigirId(servicios, solicitud.servicio(), "servicio");
            Integer perfil = proveedores.get(solicitud.proveedor());
            if (perfil == null) {
                perfil = buscarPerfil(conexion, solicitud.proveedor(), "PROVEEDOR",
                        "Proveedor", "id_proveedor");
                proveedores.put(solicitud.proveedor(), perfil);
            }
            int idProveedor = perfil;
            validarPropietarioServicio(conexion, idServicio, idProveedor, solicitud.clave());
            Integer idNecesidad = solicitud.necesidad() == null
                    ? null
                    : id(necesidades.get(solicitud.necesidad()));
            Asociacion asociacion = validarSolicitud(conexion, solicitud,
                    exigirId(eventos, solicitud.evento(), "evento"),
                    idServicio, idProveedor, idNecesidad, idsUsados);
            solicitudes.put(solicitud.clave(), asociacion);
        }
        validarIdentidadNecesidadesAdoptadas(necesidades, solicitudes);

        Map<String, Asociacion> propuestas = new LinkedHashMap<>();
        for (PropuestaDemo propuesta : PROPUESTAS) {
            Integer idSolicitud = id(solicitudes.get(propuesta.solicitud()));
            propuestas.put(propuesta.clave(),
                    validarPropuesta(conexion, propuesta, idSolicitud, idsUsados));
        }

        Map<String, Asociacion> contrataciones = new LinkedHashMap<>();
        for (ContratacionDemo contratacion : CONTRATACIONES) {
            Integer idPropuesta = id(propuestas.get(contratacion.propuesta()));
            int idEvento = exigirId(eventos, contratacion.evento(), "evento");
            Asociacion asociacion = validarContratacion(
                    conexion, contratacion, idPropuesta, idEvento, idsUsados);
            contrataciones.put(contratacion.clave(), asociacion);
        }

        validarSeleccionNecesidades(
                conexion, necesidades, necesidadesExistentes, propuestas);

        boolean autoCommit = conexion.getAutoCommit();
        Savepoint savepoint = null;
        if (autoCommit) {
            SincronizarCuentasDemo.actualizarContador(
                    conexion, "Necesidad_Servicio_Evento", "id_necesidad_servicio");
            SincronizarCuentasDemo.actualizarContador(
                    conexion, "Solicitud_Ejecucion", "id_solicitud");
            SincronizarCuentasDemo.actualizarContador(
                    conexion, "Propuesta", "id_propuesta");
            SincronizarCuentasDemo.actualizarContador(
                    conexion, "Contratacion", "id_contratacion");
            conexion.setAutoCommit(false);
        } else {
            SincronizarCuentasDemo.validarContador(
                    conexion, "Necesidad_Servicio_Evento", "id_necesidad_servicio");
            SincronizarCuentasDemo.validarContador(
                    conexion, "Solicitud_Ejecucion", "id_solicitud");
            SincronizarCuentasDemo.validarContador(
                    conexion, "Propuesta", "id_propuesta");
            SincronizarCuentasDemo.validarContador(
                    conexion, "Contratacion", "id_contratacion");
            savepoint = conexion.setSavepoint();
        }

        try {
            int necesidadesCreadas = sincronizarNecesidades(
                    conexion, necesidades, eventos);
            int solicitudesCreadas = sincronizarSolicitudes(
                    conexion, solicitudes, SOLICITUDES, eventos, servicios,
                    necesidades, proveedores);
            int propuestasCreadas = sincronizarPropuestas(
                    conexion, propuestas, solicitudes);
            int contratacionesCreadas = sincronizarContrataciones(
                    conexion, contrataciones, propuestas, eventos);

            completarValoresDerivados(conexion, solicitudes, propuestas,
                    contrataciones, necesidades, necesidadesExistentes);

            if (autoCommit) {
                conexion.commit();
            } else {
                conexion.releaseSavepoint(savepoint);
            }
            return new Resultado(necesidadesCreadas, solicitudesCreadas,
                    propuestasCreadas, contratacionesCreadas);
        } catch (SQLException | RuntimeException error) {
            try {
                if (autoCommit) {
                    conexion.rollback();
                } else {
                    conexion.rollback(savepoint);
                }
            } catch (SQLException rollback) {
                error.addSuppressed(rollback);
            }
            throw error;
        } finally {
            if (autoCommit) {
                conexion.setAutoCommit(true);
            }
        }
    }

    private static Map<String, Integer> validarCatalogo(
            Connection conexion,
            List<EventoDemo> ejemplos) throws SQLException {
        Map<String, Integer> ids = new LinkedHashMap<>();
        Map<String, Integer> organizadores = new HashMap<>();
        for (EventoDemo evento : ejemplos) {
            Integer organizador = organizadores.get(evento.organizador());
            if (organizador == null) {
                organizador = buscarPerfil(conexion, evento.organizador(), "ORGANIZADOR",
                        "Organizador", "id_organizador");
                organizadores.put(evento.organizador(), organizador);
            }
            int idOrganizador = organizador;
            Integer id = buscarRegistro(conexion, "EVENTO", evento.clave());
            if (id == null || !existeId(conexion, "Evento", "id_evento", id)) {
                throw conflicto("asociación de evento ausente o eliminada", evento.clave());
            }
            validarNoReutilizado(conexion, "EVENTO", evento.clave(), id);
            try (PreparedStatement consulta = conexion.prepareStatement(
                    "SELECT id_organizador FROM Evento WHERE id_evento = ?")) {
                consulta.setInt(1, id);
                try (ResultSet resultado = consulta.executeQuery()) {
                    if (!resultado.next() || resultado.getInt(1) != idOrganizador) {
                        throw conflicto("organizador de la asociación de evento", evento.clave());
                    }
                }
            }
            ids.put(evento.clave(), id);
        }
        return ids;
    }

    private static Map<String, Integer> validarServicios(Connection conexion)
            throws SQLException {
        Map<String, Integer> ids = new LinkedHashMap<>();
        for (SolicitudDemo solicitud : SOLICITUDES) {
            if (ids.containsKey(solicitud.servicio())) {
                continue;
            }
            Integer id = buscarRegistro(conexion, "SERVICIO", solicitud.servicio());
            if (id == null || !existeId(conexion, "Servicio", "id_servicio", id)) {
                throw conflicto("asociación de servicio ausente o eliminada",
                        solicitud.servicio());
            }
            validarNoReutilizado(conexion, "SERVICIO", solicitud.servicio(), id);
            ids.put(solicitud.servicio(), id);
        }
        return ids;
    }

    private static NecesidadValidada validarNecesidad(
            Connection conexion,
            NecesidadDemo necesidad,
            int idEvento,
            Set<String> idsUsados) throws SQLException {
        Integer id = buscarRegistro(conexion, NECESIDAD, necesidad.clave());
        boolean registrada = id != null;
        NecesidadExistente existente = null;
        if (registrada) {
            if (!existeId(conexion, "Necesidad_Servicio_Evento",
                    "id_necesidad_servicio", id)) {
                throw conflicto("asociación de necesidad eliminada", necesidad.clave());
            }
            validarNoReutilizado(conexion, NECESIDAD, necesidad.clave(), id);
            existente = validarEventoNecesidad(conexion, id, idEvento, necesidad.clave());
        } else {
            List<Integer> candidatos = buscarNecesidades(
                    conexion, idEvento, necesidad.descripcion(), false);
            if (candidatos.size() > 1) {
                throw conflicto("necesidad ambigua", necesidad.clave());
            }
            if (!candidatos.isEmpty()) {
                id = candidatos.get(0);
                validarNoReutilizado(conexion, NECESIDAD, necesidad.clave(), id);
                existente = validarEventoNecesidad(conexion, id, idEvento, necesidad.clave());
            } else if (!buscarNecesidades(conexion, idEvento,
                    necesidad.descripcion(), true).isEmpty()) {
                throw conflicto("necesidad con identidad similar", necesidad.clave());
            }
        }
        validarIdNoReutilizadoEnLote(idsUsados, NECESIDAD, necesidad.clave(), id);
        if (existente != null) {
            return new NecesidadValidada(
                    new Asociacion(NECESIDAD, necesidad.clave(), id, registrada),
                    existente);
        }
        return new NecesidadValidada(
                new Asociacion(NECESIDAD, necesidad.clave(), id, false), null);
    }

    private static void validarIdentidadNecesidadesAdoptadas(
            Map<String, Asociacion> necesidades,
            Map<String, Asociacion> solicitudes) throws SQLException {
        for (NecesidadDemo necesidad : NECESIDADES) {
            Asociacion asociacion = necesidades.get(necesidad.clave());
            if (asociacion.id() == null || asociacion.registrada()) {
                continue;
            }
            boolean respaldadaPorSolicitud = SOLICITUDES.stream()
                    .filter(solicitud -> necesidad.clave().equals(solicitud.necesidad()))
                    .map(solicitud -> solicitudes.get(solicitud.clave()))
                    .anyMatch(solicitud -> solicitud != null && solicitud.id() != null);
            if (!respaldadaPorSolicitud) {
                throw conflicto("necesidad sin relaciones que confirmen su identidad",
                        necesidad.clave());
            }
        }
    }

    private static NecesidadExistente validarEventoNecesidad(
            Connection conexion,
            int id,
            int idEvento,
            String clave) throws SQLException {
        try (PreparedStatement consulta = conexion.prepareStatement(
                "SELECT id_evento, id_propuesta_seleccionada "
                + "FROM Necesidad_Servicio_Evento WHERE id_necesidad_servicio = ?")) {
            consulta.setInt(1, id);
            try (ResultSet resultado = consulta.executeQuery()) {
                if (!resultado.next() || resultado.getInt("id_evento") != idEvento) {
                    throw conflicto("evento de la asociación de necesidad", clave);
                }
                int seleccionada = resultado.getInt("id_propuesta_seleccionada");
                boolean sinSeleccion = resultado.wasNull();
                return new NecesidadExistente(sinSeleccion ? null : seleccionada);
            }
        }
    }

    private static List<Integer> buscarNecesidades(
            Connection conexion,
            int idEvento,
            String descripcion,
            boolean normalizada) throws SQLException {
        String sql = normalizada
                ? "SELECT id_necesidad_servicio FROM Necesidad_Servicio_Evento "
                        + "WHERE id_evento = ? AND UPPER(TRIM(descripcion)) = UPPER(TRIM(?))"
                : "SELECT id_necesidad_servicio FROM Necesidad_Servicio_Evento "
                        + "WHERE id_evento = ? AND descripcion = ?";
        try (PreparedStatement consulta = conexion.prepareStatement(sql)) {
            consulta.setInt(1, idEvento);
            consulta.setString(2, descripcion);
            return leerIds(consulta);
        }
    }

    private static Asociacion validarSolicitud(
            Connection conexion,
            SolicitudDemo solicitud,
            int idEvento,
            int idServicio,
            int idProveedor,
            Integer idNecesidad,
            Set<String> idsUsados) throws SQLException {
        Integer id = buscarRegistro(conexion, SOLICITUD, solicitud.clave());
        boolean registrada = id != null;
        if (registrada) {
            if (!existeId(conexion, "Solicitud_Ejecucion", "id_solicitud", id)) {
                throw conflicto("asociación de solicitud eliminada", solicitud.clave());
            }
            validarNoReutilizado(conexion, SOLICITUD, solicitud.clave(), id);
            validarSolicitudRelacion(conexion, id, solicitud,
                    idEvento, idServicio, idProveedor, idNecesidad);
        } else if (solicitud.necesidad() != null && idNecesidad == null) {
            if (!buscarSolicitudesPorIdentidad(conexion, solicitud,
                    idEvento, idServicio, idProveedor, null, true).isEmpty()) {
                throw conflicto("solicitud con necesidad no identificada", solicitud.clave());
            }
        } else {
            List<Integer> candidatos = buscarSolicitudesPorIdentidad(
                    conexion, solicitud, idEvento, idServicio, idProveedor,
                    idNecesidad, true);
            if (candidatos.size() > 1) {
                throw conflicto("solicitud ambigua", solicitud.clave());
            }
            if (!candidatos.isEmpty()) {
                id = candidatos.get(0);
                validarNoReutilizado(conexion, SOLICITUD, solicitud.clave(), id);
                validarSolicitudRelacion(conexion, id, solicitud,
                        idEvento, idServicio, idProveedor, idNecesidad);
            } else if (!buscarSolicitudesPorIdentidad(conexion, solicitud,
                    idEvento, idServicio, idProveedor, idNecesidad, false).isEmpty()) {
                throw conflicto("solicitud con identidad ambigua", solicitud.clave());
            }
        }
        validarIdNoReutilizadoEnLote(idsUsados, SOLICITUD, solicitud.clave(), id);
        return new Asociacion(SOLICITUD, solicitud.clave(), id, registrada);
    }

    private static void validarSolicitudRelacion(
            Connection conexion,
            int id,
            SolicitudDemo solicitud,
            int idEvento,
            int idServicio,
            int idProveedor,
            Integer idNecesidad) throws SQLException {
        try (PreparedStatement consulta = conexion.prepareStatement(
                "SELECT id_evento, id_proveedor, id_servicio, id_necesidad_servicio "
                + "FROM Solicitud_Ejecucion WHERE id_solicitud = ?")) {
            consulta.setInt(1, id);
            try (ResultSet resultado = consulta.executeQuery()) {
                if (!resultado.next()
                        || resultado.getInt("id_evento") != idEvento
                        || resultado.getInt("id_proveedor") != idProveedor
                        || resultado.getInt("id_servicio") != idServicio
                        || !igualNullable(idNecesidad,
                                nullableInt(resultado, "id_necesidad_servicio"))) {
                    throw conflicto("relaciones de la asociación de solicitud",
                            solicitud.clave());
                }
            }
        }
    }

    private static List<Integer> buscarSolicitudesPorIdentidad(
            Connection conexion,
            SolicitudDemo solicitud,
            int idEvento,
            int idServicio,
            int idProveedor,
            Integer idNecesidad,
            boolean omitirFechaSolicitud) throws SQLException {
        String necesidad = solicitud.necesidad() != null && idNecesidad == null
                ? ""
                : idNecesidad == null ? " AND id_necesidad_servicio IS NULL"
                        : " AND id_necesidad_servicio = ?";
        String fecha = omitirFechaSolicitud ? "" : " AND fecha_solicitud = ?";
        String sql = "SELECT id_solicitud FROM Solicitud_Ejecucion "
                + "WHERE id_evento = ? AND id_proveedor = ? AND id_servicio = ?"
                + necesidad + fecha;
        try (PreparedStatement consulta = conexion.prepareStatement(sql)) {
            int indice = 1;
            consulta.setInt(indice++, idEvento);
            consulta.setInt(indice++, idProveedor);
            consulta.setInt(indice++, idServicio);
            if (!necesidad.isEmpty() && idNecesidad != null) {
                consulta.setInt(indice++, idNecesidad);
            }
            if (!omitirFechaSolicitud) {
                consulta.setTimestamp(indice, Timestamp.valueOf(solicitud.fecha()));
            }
            return leerIds(consulta);
        }
    }

    private static Asociacion validarPropuesta(
            Connection conexion,
            PropuestaDemo propuesta,
            Integer idSolicitud,
            Set<String> idsUsados) throws SQLException {
        Integer id = buscarRegistro(conexion, PROPUESTA, propuesta.clave());
        boolean registrada = id != null;
        if (registrada) {
            if (!existeId(conexion, "Propuesta", "id_propuesta", id)) {
                throw conflicto("asociación de propuesta eliminada", propuesta.clave());
            }
            validarNoReutilizado(conexion, PROPUESTA, propuesta.clave(), id);
            validarPropuestaRelacion(conexion, id, propuesta, idSolicitud);
        } else if (idSolicitud != null) {
            List<Integer> candidatos = buscarPropuestas(
                    conexion, idSolicitud, propuesta.version(), propuesta.fechaEnvio());
            if (candidatos.size() > 1) {
                throw conflicto("propuesta ambigua", propuesta.clave());
            }
            if (!candidatos.isEmpty()) {
                id = candidatos.get(0);
                validarNoReutilizado(conexion, PROPUESTA, propuesta.clave(), id);
                validarPropuestaRelacion(conexion, id, propuesta, idSolicitud);
            } else if (!buscarPropuestas(conexion, idSolicitud, propuesta.version()).isEmpty()) {
                throw conflicto("propuesta con identidad ambigua", propuesta.clave());
            }
        }
        validarIdNoReutilizadoEnLote(idsUsados, PROPUESTA, propuesta.clave(), id);
        return new Asociacion(PROPUESTA, propuesta.clave(), id, registrada);
    }

    private static List<Integer> buscarPropuestas(
            Connection conexion,
            int idSolicitud,
            int version,
            LocalDateTime fecha) throws SQLException {
        try (PreparedStatement consulta = conexion.prepareStatement(
                "SELECT id_propuesta FROM Propuesta WHERE id_solicitud = ? "
                + "AND numero_version = ? AND fecha_envio = ?")) {
            consulta.setInt(1, idSolicitud);
            consulta.setInt(2, version);
            consulta.setTimestamp(3, Timestamp.valueOf(fecha));
            return leerIds(consulta);
        }
    }

    private static List<Integer> buscarPropuestas(
            Connection conexion,
            int idSolicitud,
            int version) throws SQLException {
        try (PreparedStatement consulta = conexion.prepareStatement(
                "SELECT id_propuesta FROM Propuesta WHERE id_solicitud = ? "
                + "AND numero_version = ?")) {
            consulta.setInt(1, idSolicitud);
            consulta.setInt(2, version);
            return leerIds(consulta);
        }
    }

    private static void validarPropuestaRelacion(
            Connection conexion,
            int id,
            PropuestaDemo propuesta,
            Integer idSolicitud) throws SQLException {
        if (idSolicitud == null) {
            throw conflicto("solicitud de la propuesta no identificada", propuesta.clave());
        }
        try (PreparedStatement consulta = conexion.prepareStatement(
                "SELECT id_solicitud, numero_version FROM Propuesta WHERE id_propuesta = ?")) {
            consulta.setInt(1, id);
            try (ResultSet resultado = consulta.executeQuery()) {
                if (!resultado.next()
                        || resultado.getInt("id_solicitud") != idSolicitud
                        || resultado.getInt("numero_version") != propuesta.version()) {
                    throw conflicto("relaciones de la asociación de propuesta",
                            propuesta.clave());
                }
            }
        }
    }

    private static Asociacion validarContratacion(
            Connection conexion,
            ContratacionDemo contratacion,
            Integer idPropuesta,
            int idEvento,
            Set<String> idsUsados) throws SQLException {
        Integer id = buscarRegistro(conexion, CONTRATACION, contratacion.clave());
        boolean registrada = id != null;
        if (registrada) {
            if (!existeId(conexion, "Contratacion", "id_contratacion", id)) {
                throw conflicto("asociación de contratación eliminada", contratacion.clave());
            }
            validarNoReutilizado(conexion, CONTRATACION, contratacion.clave(), id);
            validarContratacionRelacion(
                    conexion, id, contratacion, idPropuesta, idEvento, false);
        } else if (idPropuesta != null) {
            List<Integer> candidatos = buscarContrataciones(conexion, idPropuesta);
            if (candidatos.size() > 1) {
                throw conflicto("contratación ambigua", contratacion.clave());
            }
            if (!candidatos.isEmpty()) {
                id = candidatos.get(0);
                validarNoReutilizado(conexion, CONTRATACION, contratacion.clave(), id);
                validarContratacionRelacion(
                        conexion, id, contratacion, idPropuesta, idEvento, true);
            }
        }
        validarIdNoReutilizadoEnLote(idsUsados, CONTRATACION, contratacion.clave(), id);
        return new Asociacion(CONTRATACION, contratacion.clave(), id, registrada);
    }

    private static List<Integer> buscarContrataciones(
            Connection conexion,
            int idPropuesta) throws SQLException {
        try (PreparedStatement consulta = conexion.prepareStatement(
                "SELECT id_contratacion FROM Contratacion WHERE id_propuesta = ?")) {
            consulta.setInt(1, idPropuesta);
            return leerIds(consulta);
        }
    }

    private static void validarContratacionRelacion(
            Connection conexion,
            int id,
            ContratacionDemo contratacion,
            Integer idPropuesta,
            int idEvento,
            boolean validarFecha) throws SQLException {
        if (idPropuesta == null) {
            throw conflicto("propuesta de la contratación no identificada",
                    contratacion.clave());
        }
        try (PreparedStatement consulta = conexion.prepareStatement(
                "SELECT id_propuesta, id_evento, fecha_contratacion, id_necesidad_servicio, "
                + "numero_version_propuesta FROM Contratacion WHERE id_contratacion = ?")) {
            consulta.setInt(1, id);
            try (ResultSet resultado = consulta.executeQuery()) {
                if (!resultado.next()) {
                    throw conflicto("relaciones de la asociación de contratación",
                            contratacion.clave());
                }
                int idPropuestaExistente = resultado.getInt("id_propuesta");
                int idEventoExistente = resultado.getInt("id_evento");
                Integer idNecesidadExistente
                        = nullableInt(resultado, "id_necesidad_servicio");
                Integer version = nullableInt(resultado, "numero_version_propuesta");
                if (idPropuestaExistente != idPropuesta
                        || idEventoExistente != idEvento
                        || idNecesidadExistente != null
                        || (version != null && version != contratacion.versionPropuesta())) {
                    throw conflicto("relaciones de la asociación de contratación",
                            contratacion.clave());
                }
                Timestamp fecha = resultado.getTimestamp("fecha_contratacion");
                if (validarFecha && (fecha == null
                        || !fecha.equals(Timestamp.valueOf(contratacion.fecha())))) {
                    throw conflicto("fecha de identidad de contratación",
                            contratacion.clave());
                }
            }
        }
    }

    private static void validarSeleccionNecesidades(
            Connection conexion,
            Map<String, Asociacion> necesidades,
            Map<String, NecesidadExistente> existentes,
            Map<String, Asociacion> propuestas) throws SQLException {
        for (NecesidadDemo necesidad : NECESIDADES) {
            Integer idNecesidad = id(necesidades.get(necesidad.clave()));
            NecesidadExistente existente = existentes.get(necesidad.clave());
            if (existente == null || existente.propuestaSeleccionada() == null) {
                if ("necesidad-3".equals(necesidad.clave()) && idNecesidad != null) {
                    throw conflicto("propuesta seleccionada ausente",
                            necesidad.clave());
                }
                continue;
            }
            if (necesidad.propuestaSeleccionada() != null) {
                Integer idEsperado = id(propuestas.get(necesidad.propuestaSeleccionada()));
                if (idEsperado == null
                        || existente.propuestaSeleccionada().intValue() != idEsperado) {
                    throw conflicto("propuesta seleccionada incompatible",
                            necesidad.clave());
                }
            } else {
                validarPropuestaPerteneceNecesidad(
                        conexion, idNecesidad, existente.propuestaSeleccionada());
            }
        }
    }

    private static void validarPropuestaPerteneceNecesidad(
            Connection conexion,
            Integer idNecesidad,
            int idPropuesta) throws SQLException {
        if (idNecesidad == null) {
            throw conflicto("necesidad de la propuesta seleccionada ausente",
                    Integer.toString(idPropuesta));
        }
        try (PreparedStatement consulta = conexion.prepareStatement(
                "SELECT s.id_necesidad_servicio FROM Propuesta p "
                + "JOIN Solicitud_Ejecucion s ON s.id_solicitud = p.id_solicitud "
                + "WHERE p.id_propuesta = ?")) {
            consulta.setInt(1, idPropuesta);
            try (ResultSet resultado = consulta.executeQuery()) {
                if (!resultado.next()) {
                    throw conflicto("propuesta seleccionada de otra necesidad",
                            Integer.toString(idPropuesta));
                }
                Integer idNecesidadPropuesta
                        = nullableInt(resultado, "id_necesidad_servicio");
                if (idNecesidadPropuesta == null
                        || !idNecesidadPropuesta.equals(idNecesidad)) {
                    throw conflicto("propuesta seleccionada de otra necesidad",
                            Integer.toString(idPropuesta));
                }
            }
        }
    }

    private static int sincronizarNecesidades(
            Connection conexion,
            Map<String, Asociacion> asociaciones,
            Map<String, Integer> eventos) throws SQLException {
        int creadas = 0;
        for (NecesidadDemo necesidad : NECESIDADES) {
            Asociacion asociacion = asociaciones.get(necesidad.clave());
            int id = asociacion.id() == null
                    ? insertarNecesidad(conexion, necesidad,
                            exigirId(eventos, necesidad.evento(), "evento"))
                    : asociacion.id();
            if (asociacion.id() == null) {
                asociaciones.put(necesidad.clave(), new Asociacion(
                        asociacion.tipo(), asociacion.clave(), id, false));
                creadas++;
            }
            registrar(conexion, asociacion, id);
        }
        return creadas;
    }

    private static int sincronizarSolicitudes(
            Connection conexion,
            Map<String, Asociacion> asociaciones,
            List<SolicitudDemo> solicitudes,
            Map<String, Integer> eventos,
            Map<String, Integer> servicios,
            Map<String, Asociacion> necesidades,
            Map<String, Integer> proveedores) throws SQLException {
        int creadas = 0;
        for (SolicitudDemo solicitud : solicitudes) {
            Asociacion asociacion = asociaciones.get(solicitud.clave());
            Integer idNecesidad = solicitud.necesidad() == null
                    ? null : id(necesidades.get(solicitud.necesidad()));
            int id = asociacion.id() == null
                    ? insertarSolicitud(conexion, solicitud,
                            exigirId(eventos, solicitud.evento(), "evento"),
                            exigirId(servicios, solicitud.servicio(), "servicio"),
                            exigirId(proveedores, solicitud.proveedor(), "proveedor"),
                            idNecesidad)
                    : asociacion.id();
            if (asociacion.id() == null) {
                asociaciones.put(solicitud.clave(), new Asociacion(
                        asociacion.tipo(), asociacion.clave(), id, false));
                creadas++;
            }
            registrar(conexion, asociacion, id);
        }
        return creadas;
    }

    private static int sincronizarPropuestas(
            Connection conexion,
            Map<String, Asociacion> asociaciones,
            Map<String, Asociacion> solicitudes) throws SQLException {
        int creadas = 0;
        for (PropuestaDemo propuesta : PROPUESTAS) {
            Asociacion asociacion = asociaciones.get(propuesta.clave());
            int id = asociacion.id() == null
                    ? insertarPropuesta(conexion, propuesta,
                            exigirAsociacionId(solicitudes, propuesta.solicitud(), "solicitud"))
                    : asociacion.id();
            if (asociacion.id() == null) {
                asociaciones.put(propuesta.clave(), new Asociacion(
                        asociacion.tipo(), asociacion.clave(), id, false));
                creadas++;
            }
            registrar(conexion, asociacion, id);
        }
        return creadas;
    }

    private static int sincronizarContrataciones(
            Connection conexion,
            Map<String, Asociacion> asociaciones,
            Map<String, Asociacion> propuestas,
            Map<String, Integer> eventos) throws SQLException {
        int creadas = 0;
        for (ContratacionDemo contratacion : CONTRATACIONES) {
            Asociacion asociacion = asociaciones.get(contratacion.clave());
            int id = asociacion.id() == null
                    ? insertarContratacion(conexion, contratacion,
                            exigirAsociacionId(propuestas, contratacion.propuesta(), "propuesta"),
                            exigirId(eventos, contratacion.evento(), "evento"))
                    : asociacion.id();
            if (asociacion.id() == null) {
                asociaciones.put(contratacion.clave(), new Asociacion(
                        asociacion.tipo(), asociacion.clave(), id, false));
                creadas++;
            }
            registrar(conexion, asociacion, id);
        }
        return creadas;
    }

    private static void completarValoresDerivados(
            Connection conexion,
            Map<String, Asociacion> solicitudes,
            Map<String, Asociacion> propuestas,
            Map<String, Asociacion> contrataciones,
            Map<String, Asociacion> necesidades,
            Map<String, NecesidadExistente> necesidadesExistentes) throws SQLException {
        for (SolicitudDemo solicitud : SOLICITUDES) {
            if (solicitud.costoComprometido() != null) {
                try (PreparedStatement actualizar = conexion.prepareStatement(
                        "UPDATE Solicitud_Ejecucion "
                        + "SET costo_comprometido = COALESCE(costo_comprometido, ?) "
                        + "WHERE id_solicitud = ?")) {
                    actualizar.setBigDecimal(1, new BigDecimal(solicitud.costoComprometido()));
                    actualizar.setInt(2, exigirAsociacionId(
                            solicitudes, solicitud.clave(), "solicitud"));
                    actualizar.executeUpdate();
                }
            }
        }
        for (ContratacionDemo contratacion : CONTRATACIONES) {
            try (PreparedStatement actualizar = conexion.prepareStatement(
                    "UPDATE Contratacion SET "
                    + "numero_version_propuesta = COALESCE(numero_version_propuesta, ?), "
                    + "alcance_propuesta = COALESCE(alcance_propuesta, ?) "
                    + "WHERE id_contratacion = ?")) {
                actualizar.setInt(1, contratacion.versionPropuesta());
                actualizar.setString(2, contratacion.alcancePropuesta());
                actualizar.setInt(3, exigirAsociacionId(
                        contrataciones, contratacion.clave(), "contratación"));
                actualizar.executeUpdate();
            }
        }
        NecesidadDemo seleccionada = NECESIDADES.stream()
                .filter(n -> "necesidad-3".equals(n.clave()))
                .findFirst().orElseThrow();
        Asociacion asociacionNecesidad = necesidades.get(seleccionada.clave());
        if (asociacionNecesidad.id() != null) {
            Integer idPropuesta = id(propuestas.get(seleccionada.propuestaSeleccionada()));
            if (idPropuesta == null) {
                throw conflicto("propuesta seleccionada no creada", seleccionada.clave());
            }
            if (!necesidadesExistentes.containsKey(seleccionada.clave())) {
                try (PreparedStatement actualizar = conexion.prepareStatement(
                        "UPDATE Necesidad_Servicio_Evento "
                        + "SET estado = 'SELECCIONADA', id_propuesta_seleccionada = ? "
                        + "WHERE id_necesidad_servicio = ? "
                        + "AND id_propuesta_seleccionada IS NULL")) {
                    actualizar.setInt(1, idPropuesta);
                    actualizar.setInt(2, asociacionNecesidad.id());
                    if (actualizar.executeUpdate() != 1) {
                        throw conflicto("no se pudo establecer la propuesta seleccionada",
                                seleccionada.clave());
                    }
                }
            }
        }
    }

    private static int insertarNecesidad(
            Connection conexion,
            NecesidadDemo necesidad,
            int idEvento) throws SQLException {
        try (PreparedStatement insertar = conexion.prepareStatement(
                "INSERT INTO Necesidad_Servicio_Evento "
                + "(id_evento, descripcion, estado) VALUES (?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            insertar.setInt(1, idEvento);
            insertar.setString(2, necesidad.descripcion());
            insertar.setString(3, necesidad.estado());
            insertar.executeUpdate();
            return claveGenerada(insertar, necesidad.clave());
        }
    }

    private static int insertarSolicitud(
            Connection conexion,
            SolicitudDemo solicitud,
            int idEvento,
            int idServicio,
            int idProveedor,
            Integer idNecesidad) throws SQLException {
        try (PreparedStatement insertar = conexion.prepareStatement(
                "INSERT INTO Solicitud_Ejecucion "
                + "(id_proveedor, id_evento, id_servicio, id_necesidad_servicio, "
                + "fecha_solicitud, estado, motivo_rechazo, fecha_respuesta, "
                + "costo_comprometido) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            insertar.setInt(1, idProveedor);
            insertar.setInt(2, idEvento);
            insertar.setInt(3, idServicio);
            setNullableInt(insertar, 4, idNecesidad);
            insertar.setTimestamp(5, Timestamp.valueOf(solicitud.fecha()));
            insertar.setString(6, solicitud.estado());
            insertar.setString(7, solicitud.motivoRechazo());
            setNullableTimestamp(insertar, 8, solicitud.fechaRespuesta());
            if (solicitud.costoComprometido() == null) {
                insertar.setNull(9, Types.DECIMAL);
            } else {
                insertar.setBigDecimal(9, new BigDecimal(solicitud.costoComprometido()));
            }
            insertar.executeUpdate();
            return claveGenerada(insertar, solicitud.clave());
        }
    }

    private static int insertarPropuesta(
            Connection conexion,
            PropuestaDemo propuesta,
            int idSolicitud) throws SQLException {
        try (PreparedStatement insertar = conexion.prepareStatement(
                "INSERT INTO Propuesta "
                + "(id_solicitud, valor, descripcion, estado, numero_version, "
                + "condiciones, fecha_envio, vigente, material_apoyo) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            insertar.setInt(1, idSolicitud);
            insertar.setBigDecimal(2, new BigDecimal(propuesta.valor()));
            insertar.setString(3, propuesta.descripcion());
            insertar.setString(4, propuesta.estado());
            insertar.setInt(5, propuesta.version());
            insertar.setString(6, propuesta.condiciones());
            insertar.setTimestamp(7, Timestamp.valueOf(propuesta.fechaEnvio()));
            insertar.setBoolean(8, propuesta.vigente());
            insertar.setString(9, propuesta.materialApoyo());
            insertar.executeUpdate();
            return claveGenerada(insertar, propuesta.clave());
        }
    }

    private static int insertarContratacion(
            Connection conexion,
            ContratacionDemo contratacion,
            int idPropuesta,
            int idEvento) throws SQLException {
        try (PreparedStatement insertar = conexion.prepareStatement(
                "INSERT INTO Contratacion "
                + "(id_propuesta, id_evento, fecha_contratacion, estado, condiciones, "
                + "valor_acordado, numero_version_propuesta, alcance_propuesta) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            insertar.setInt(1, idPropuesta);
            insertar.setInt(2, idEvento);
            insertar.setTimestamp(3, Timestamp.valueOf(contratacion.fecha()));
            insertar.setString(4, contratacion.estado());
            insertar.setString(5, contratacion.condiciones());
            insertar.setBigDecimal(6, new BigDecimal(contratacion.valorAcordado()));
            insertar.setInt(7, contratacion.versionPropuesta());
            insertar.setString(8, contratacion.alcancePropuesta());
            insertar.executeUpdate();
            return claveGenerada(insertar, contratacion.clave());
        }
    }

    private static void registrar(Connection conexion, Asociacion asociacion, int id)
            throws SQLException {
        if (asociacion.registrada()) {
            return;
        }
        try (PreparedStatement insertar = conexion.prepareStatement(
                "INSERT INTO Registro_demo_catalogo (tipo, clave, id_registro) "
                + "VALUES (?, ?, ?)")) {
            insertar.setString(1, asociacion.tipo());
            insertar.setString(2, asociacion.clave());
            insertar.setInt(3, id);
            insertar.executeUpdate();
        }
    }

    private static Integer buscarRegistro(Connection conexion, String tipo, String clave)
            throws SQLException {
        try (PreparedStatement consulta = conexion.prepareStatement(
                "SELECT id_registro FROM Registro_demo_catalogo "
                + "WHERE tipo = ? AND clave = ?")) {
            consulta.setString(1, tipo);
            consulta.setString(2, clave);
            try (ResultSet resultado = consulta.executeQuery()) {
                if (!resultado.next()) {
                    return null;
                }
                int id = resultado.getInt(1);
                if (resultado.next()) {
                    throw conflicto("procedencia duplicada", clave);
                }
                return id;
            }
        }
    }

    private static void validarNoReutilizado(
            Connection conexion,
            String tipo,
            String clave,
            int id) throws SQLException {
        try (PreparedStatement consulta = conexion.prepareStatement(
                "SELECT clave FROM Registro_demo_catalogo "
                + "WHERE tipo = ? AND id_registro = ? AND clave <> ?")) {
            consulta.setString(1, tipo);
            consulta.setInt(2, id);
            consulta.setString(3, clave);
            try (ResultSet resultado = consulta.executeQuery()) {
                if (resultado.next()) {
                    throw conflicto("registro reutilizado por otro ejemplo", clave);
                }
            }
        }
    }

    private static void validarIdNoReutilizadoEnLote(
            Set<String> idsUsados,
            String tipo,
            String clave,
            Integer id) throws SQLException {
        if (id == null) {
            return;
        }
        String identidad = tipo + ":" + id;
        if (!idsUsados.add(identidad)) {
            throw conflicto("registro asignado a más de un ejemplo", clave);
        }
    }

    private static int buscarPerfil(
            Connection conexion,
            String usuario,
            String tipo,
            String tabla,
            String columna) throws SQLException {
        String sql = "SELECT p." + columna + " FROM Usuario u JOIN " + tabla
                + " p ON p.id_usuario = u.id_usuario "
                + "WHERE u.usuario = ? AND u.tipo_usuario = ?";
        try (PreparedStatement consulta = conexion.prepareStatement(sql)) {
            consulta.setString(1, usuario);
            consulta.setString(2, tipo);
            try (ResultSet resultado = consulta.executeQuery()) {
                if (!resultado.next()) {
                    throw conflicto("perfil oficial ausente", usuario);
                }
                int id = resultado.getInt(1);
                if (resultado.next()) {
                    throw conflicto("perfil oficial duplicado", usuario);
                }
                return id;
            }
        }
    }

    private static void validarPropietarioServicio(
            Connection conexion,
            int idServicio,
            int idProveedor,
            String clave) throws SQLException {
        try (PreparedStatement consulta = conexion.prepareStatement(
                "SELECT id_proveedor FROM Servicio WHERE id_servicio = ?")) {
            consulta.setInt(1, idServicio);
            try (ResultSet resultado = consulta.executeQuery()) {
                if (!resultado.next() || resultado.getInt(1) != idProveedor) {
                    throw conflicto("proveedor de la asociación de servicio", clave);
                }
            }
        }
    }

    private static boolean existeId(
            Connection conexion,
            String tabla,
            String columna,
            int id) throws SQLException {
        String sql = "SELECT 1 FROM " + tabla + " WHERE " + columna + " = ?";
        try (PreparedStatement consulta = conexion.prepareStatement(sql)) {
            consulta.setInt(1, id);
            try (ResultSet resultado = consulta.executeQuery()) {
                return resultado.next();
            }
        }
    }

    private static List<Integer> leerIds(PreparedStatement consulta) throws SQLException {
        List<Integer> ids = new ArrayList<>();
        try (ResultSet resultado = consulta.executeQuery()) {
            while (resultado.next()) {
                ids.add(resultado.getInt(1));
            }
        }
        return ids;
    }

    private static Integer nullableInt(ResultSet resultado, String columna) throws SQLException {
        int valor = resultado.getInt(columna);
        return resultado.wasNull() ? null : valor;
    }

    private static boolean igualNullable(Integer primero, Integer segundo) {
        return primero == null ? segundo == null : primero.equals(segundo);
    }

    private static Integer id(Asociacion asociacion) {
        return asociacion == null ? null : asociacion.id();
    }

    private static int exigirId(Map<String, Integer> ids, String clave, String tipo)
            throws SQLException {
        Integer id = ids.get(clave);
        if (id == null) {
            throw conflicto(tipo + " no resuelto", clave);
        }
        return id;
    }

    private static int exigirAsociacionId(
            Map<String, Asociacion> asociaciones,
            String clave,
            String tipo)
            throws SQLException {
        Integer id = id(asociaciones.get(clave));
        if (id == null) {
            throw conflicto(tipo + " no resuelto", clave);
        }
        return id;
    }

    private static int claveGenerada(PreparedStatement insertar, String clave)
            throws SQLException {
        try (ResultSet claves = insertar.getGeneratedKeys()) {
            if (!claves.next()) {
                throw new SQLException("No se generó el identificador de " + clave);
            }
            return claves.getInt(1);
        }
    }

    private static void setNullableInt(
            PreparedStatement statement,
            int indice,
            Integer valor) throws SQLException {
        if (valor == null) {
            statement.setNull(indice, Types.INTEGER);
        } else {
            statement.setInt(indice, valor);
        }
    }

    private static void setNullableTimestamp(
            PreparedStatement statement,
            int indice,
            LocalDateTime valor) throws SQLException {
        if (valor == null) {
            statement.setNull(indice, Types.TIMESTAMP);
        } else {
            statement.setTimestamp(indice, Timestamp.valueOf(valor));
        }
    }

    private static SQLException conflicto(String tipo, String clave) {
        return new SQLException("Conflicto de identidad para " + tipo + ": " + clave
                + ". No se modificaron ni duplicaron registros.");
    }

    private static SolicitudDemo solicitud(
            String clave,
            String evento,
            String proveedor,
            String servicio,
            String necesidad,
            String fecha,
            String estado,
            String motivo,
            String fechaRespuesta,
            String costo) {
        return new SolicitudDemo(clave, evento, proveedor, servicio, necesidad,
                LocalDateTime.parse(fecha), estado, motivo,
                fechaRespuesta == null ? null : LocalDateTime.parse(fechaRespuesta), costo);
    }

    private static PropuestaDemo propuesta(
            String clave,
            String solicitud,
            int version,
            String valor,
            String descripcion,
            String estado,
            String condiciones,
            String fecha,
            boolean vigente,
            String materialApoyo) {
        return new PropuestaDemo(clave, solicitud, version, valor, descripcion, estado,
                condiciones, LocalDateTime.parse(fecha), vigente, materialApoyo);
    }

}
