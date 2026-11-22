package inicializacion;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class SincronizarCatalogoDemo {

    public record Resultado(int eventosCreados, int serviciosCreados) {
    }

    private record EventoDemo(
            String clave,
            String organizador,
            String nombre,
            String descripcion,
            LocalDateTime fechaInicio,
            LocalDateTime fechaFin,
            String ubicacion,
            String estado,
            LocalDateTime fechaCreacion,
            String tipo,
            String presupuesto,
            int invitados) {
    }

    private record ServicioDemo(
            String clave,
            String proveedor,
            String nombre,
            String descripcion,
            String precio,
            String categoria,
            String estado,
            String tipo,
            int cantidadMaximaPersonas,
            String ciudadZona,
            int anticipacionMinima) {
    }

    private record RegistroValidado(
            String tipo,
            String clave,
            Integer idExistente) {
    }

    private static final String EVENTO = "EVENTO";
    private static final String SERVICIO = "SERVICIO";

    private static final List<EventoDemo> EVENTOS = List.of(
            new EventoDemo("boda-laura", "laura.gomez", "Boda Laura y Daniel",
                    "Celebración de matrimonio", LocalDateTime.parse("2026-11-15T15:00:00"),
                    LocalDateTime.parse("2026-11-15T23:00:00"), "Salón Primavera", "PLANEADO",
                    LocalDateTime.parse("2026-10-06T09:00:00"), "Boda", "15000000.00", 120),
            new EventoDemo("baby-shower-valentina", "carlos.rodriguez",
                    "Baby Shower Valentina", "Celebración de bienvenida para bebé",
                    LocalDateTime.parse("2026-12-05T14:00:00"),
                    LocalDateTime.parse("2026-12-05T19:00:00"), "Jardín Los Pinos",
                    "PLANEADO", LocalDateTime.parse("2026-10-07T10:00:00"),
                    "Baby shower", "5000000.00", 60),
            new EventoDemo("quince-sofia", "valentina", "Cumpleaños 15 de Sofía",
                    "Fiesta de quince años con temática de unicornios",
                    LocalDateTime.parse("2027-02-20T17:00:00"),
                    LocalDateTime.parse("2027-02-20T23:00:00"), "Salón Mirador", "PLANEADO",
                    LocalDateTime.parse("2026-10-20T10:00:00"), "Cumpleaños", "12000000.00", 70),
            new EventoDemo("graduacion-medicina", "valentina", "Graduación de Medicina",
                    "Celebración de grado con cena y brindis",
                    LocalDateTime.parse("2027-03-13T15:00:00"),
                    LocalDateTime.parse("2027-03-13T22:00:00"), "Terraza Chapinero", "PLANEADO",
                    LocalDateTime.parse("2026-10-22T11:00:00"), "Graduación", "9000000.00", 90));

    private static final List<ServicioDemo> SERVICIOS = List.of(
            new ServicioDemo("decoracion-floral", "decoraciones.arcoiris",
                    "Decoración floral", "Decoración floral para eventos sociales",
                    "1200000.00", "DECORACION", "DISPONIBLE", "Decoración", 300, "Bogotá", 7),
            new ServicioDemo("decoracion-tematica", "decoraciones.arcoiris",
                    "Decoración temática", "Decoración personalizada según temática",
                    "900000.00", "DECORACION", "DISPONIBLE", "Decoración", 200, "Bogotá", 10),
            new ServicioDemo("catering-basico", "sabores.eventos",
                    "Catering básico", "Servicio de alimentación para eventos",
                    "1800000.00", "ALIMENTACION", "DISPONIBLE", "Catering", 100, "Bogotá", 7),
            new ServicioDemo("mesa-postres", "sabores.eventos",
                    "Mesa de postres", "Postres variados para celebraciones",
                    "650000.00", "ALIMENTACION", "DISPONIBLE", "Postres", 150, "Bogotá", 5),
            new ServicioDemo("cobertura-fotografica", "flash", "Cobertura fotográfica completa",
                    "Fotógrafo profesional durante todo el evento, con galería digital",
                    "1500000.00", "FOTOGRAFIA", "DISPONIBLE", "Fotografía", 300, "Bogotá", 7),
            new ServicioDemo("video-highlights", "flash", "Video highlights del evento",
                    "Video resumen de cinco minutos con los mejores momentos",
                    "950000.00", "FOTOGRAFIA", "DISPONIBLE", "Video", 300, "Bogotá", 10),
            new ServicioDemo("sesion-retratos", "flash", "Sesión de retratos",
                    "Sesión de retratos familiares antes del evento",
                    "400000.00", "FOTOGRAFIA", "BORRADOR", "Fotografía", 20, "Bogotá", 3),
            new ServicioDemo("album-premium", "flash", "Álbum impreso premium",
                    "Álbum de 40 páginas con las mejores fotos",
                    "600000.00", "FOTOGRAFIA", "RETIRADO", "Fotografía", 1, "Bogotá", 15),
            new ServicioDemo("arreglos-mesa", "decoraciones.arcoiris", "Arreglos de mesa",
                    "Centros de mesa con flores de temporada",
                    "450000.00", "DECORACION", "DISPONIBLE", "Decoración", 200, "Bogotá", 5),
            new ServicioDemo("arco-globos", "decoraciones.arcoiris", "Arco de globos",
                    "Arco de globos para la entrada del salón",
                    "380000.00", "DECORACION", "BORRADOR", "Decoración", 300, "Bogotá", 3),
            new ServicioDemo("ambientacion-salon", "decoraciones.arcoiris",
                    "Ambientación de salón",
                    "Ambientación completa con iluminación y cortinas",
                    "1100000.00", "DECORACION", "DISPONIBLE", "Decoración", 250, "Bogotá", 14),
            new ServicioDemo("catering-gourmet", "sabores.eventos", "Catering gourmet",
                    "Menú de tres tiempos con meseros y montaje de buffet",
                    "1900000.00", "ALIMENTACION", "DISPONIBLE", "Catering", 150, "Bogotá", 10),
            new ServicioDemo("coctel-bienvenida", "sabores.eventos", "Cóctel de bienvenida",
                    "Cóctel sin alcohol con estación de bebidas",
                    "520000.00", "ALIMENTACION", "DISPONIBLE", "Bebidas", 150, "Bogotá", 5),
            new ServicioDemo("estacion-cafe", "sabores.eventos", "Estación de café",
                    "Café de origen y repostería para la sobremesa",
                    "300000.00", "ALIMENTACION", "BORRADOR", "Bebidas", 100, "Bogotá", 3),
            new ServicioDemo("cabina-fotos", "flash", "Cabina de fotos",
                    "Cabina instantánea con accesorios y fotos impresas",
                    "700000.00", "FOTOGRAFIA", "DISPONIBLE", "Fotografía", 150, "Bogotá", 7));

    private SincronizarCatalogoDemo() {
    }

    public static synchronized Resultado sincronizar(Connection conexion) throws SQLException {
        Map<String, Integer> organizadores = buscarPerfiles(conexion, "ORGANIZADOR", "Organizador",
                "id_organizador", List.of("laura.gomez", "carlos.rodriguez", "valentina"));
        Map<String, Integer> proveedores = buscarPerfiles(conexion, "PROVEEDOR", "Proveedor",
                "id_proveedor", List.of("decoraciones.arcoiris", "sabores.eventos", "flash"));

        List<RegistroValidado> registros = new ArrayList<>();
        for (EventoDemo evento : EVENTOS) {
            registros.add(validarEvento(conexion, evento, organizadores.get(evento.organizador())));
        }
        for (ServicioDemo servicio : SERVICIOS) {
            registros.add(validarServicio(conexion, servicio, proveedores.get(servicio.proveedor())));
        }

        boolean autoCommit = conexion.getAutoCommit();
        Savepoint savepoint = null;
        if (autoCommit) {
            SincronizarCuentasDemo.actualizarContador(conexion, "Evento", "id_evento");
            SincronizarCuentasDemo.actualizarContador(conexion, "Servicio", "id_servicio");
            conexion.setAutoCommit(false);
        } else {
            SincronizarCuentasDemo.validarContador(conexion, "Evento", "id_evento");
            SincronizarCuentasDemo.validarContador(conexion, "Servicio", "id_servicio");
            savepoint = conexion.setSavepoint();
        }

        try {
            int eventosCreados = 0;
            int serviciosCreados = 0;
            for (int i = 0; i < EVENTOS.size(); i++) {
                EventoDemo evento = EVENTOS.get(i);
                RegistroValidado registro = registros.get(i);
                int id = registro.idExistente() == null
                        ? insertarEvento(conexion, evento, organizadores.get(evento.organizador()))
                        : registro.idExistente();
                registrar(conexion, registro, id);
                if (registro.idExistente() == null) {
                    eventosCreados++;
                }
            }
            for (int i = 0; i < SERVICIOS.size(); i++) {
                ServicioDemo servicio = SERVICIOS.get(i);
                RegistroValidado registro = registros.get(EVENTOS.size() + i);
                int id = registro.idExistente() == null
                        ? insertarServicio(conexion, servicio, proveedores.get(servicio.proveedor()))
                        : registro.idExistente();
                registrar(conexion, registro, id);
                if (registro.idExistente() == null) {
                    serviciosCreados++;
                }
            }

            if (autoCommit) {
                conexion.commit();
            } else {
                conexion.releaseSavepoint(savepoint);
            }
            return new Resultado(eventosCreados, serviciosCreados);
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

    private static Map<String, Integer> buscarPerfiles(
            Connection conexion,
            String tipo,
            String tabla,
            String columnaId,
            List<String> usuarios) throws SQLException {
        Map<String, Integer> perfiles = new LinkedHashMap<>();
        String sql = "SELECT u.id_usuario, p." + columnaId + " FROM Usuario u "
                + "JOIN " + tabla + " p ON p.id_usuario = u.id_usuario "
                + "WHERE u.usuario = ? AND u.tipo_usuario = ?";
        try (PreparedStatement consulta = conexion.prepareStatement(sql)) {
            for (String usuario : usuarios) {
                consulta.setString(1, usuario);
                consulta.setString(2, tipo);
                try (ResultSet resultado = consulta.executeQuery()) {
                    if (!resultado.next()) {
                        throw new SQLException(
                                "No existe un perfil " + tipo + " para " + usuario);
                    }
                    int id = resultado.getInt(columnaId);
                    if (resultado.next()) {
                        throw new SQLException(
                                "Hay perfiles duplicados para la cuenta " + usuario);
                    }
                    perfiles.put(usuario, id);
                }
            }
        }
        return perfiles;
    }

    private static RegistroValidado validarEvento(
            Connection conexion,
            EventoDemo evento,
            int idOrganizador) throws SQLException {
        Integer registrado = buscarRegistro(conexion, EVENTO, evento.clave());
        if (registrado != null) {
            if (!existeEvento(conexion, registrado)) {
                throw conflicto("asociación de evento eliminado", evento.nombre());
            } else {
                validarPropietarioEvento(conexion, registrado, idOrganizador, evento.nombre());
                validarNoReutilizado(conexion, EVENTO, evento.clave(), registrado,
                        evento.nombre());
                return new RegistroValidado(EVENTO, evento.clave(), registrado);
            }
        }
        List<Integer> candidatos = buscarEventosIdenticos(conexion, evento, idOrganizador);
        if (candidatos.size() > 1) {
            throw conflicto("evento", evento.nombre());
        }
        if (!candidatos.isEmpty()) {
            registrado = candidatos.get(0);
            validarNoReutilizado(conexion, EVENTO, evento.clave(), registrado, evento.nombre());
        }
        if (registrado == null && existeEventoConMismoNombre(conexion, evento.nombre(), idOrganizador)) {
            throw conflicto("evento con el mismo organizador y nombre", evento.nombre());
        }
        return new RegistroValidado(EVENTO, evento.clave(), registrado);
    }

    private static RegistroValidado validarServicio(
            Connection conexion,
            ServicioDemo servicio,
            int idProveedor) throws SQLException {
        Integer registrado = buscarRegistro(conexion, SERVICIO, servicio.clave());
        if (registrado != null) {
            if (!existeServicio(conexion, registrado)) {
                throw conflicto("asociación de servicio eliminado", servicio.nombre());
            } else {
                validarPropietarioServicio(conexion, registrado, idProveedor, servicio.nombre());
                validarNoReutilizado(conexion, SERVICIO, servicio.clave(), registrado,
                        servicio.nombre());
                return new RegistroValidado(SERVICIO, servicio.clave(), registrado);
            }
        }
        List<Integer> candidatos = buscarServiciosIdenticos(conexion, servicio, idProveedor);
        if (candidatos.size() > 1) {
            throw conflicto("servicio", servicio.nombre());
        }
        if (!candidatos.isEmpty()) {
            registrado = candidatos.get(0);
            validarNoReutilizado(conexion, SERVICIO, servicio.clave(), registrado,
                    servicio.nombre());
        }
        if (registrado == null
                && existeServicioConMismoNombre(conexion, servicio.nombre(), idProveedor)) {
            throw conflicto("servicio del mismo proveedor y nombre", servicio.nombre());
        }
        return new RegistroValidado(SERVICIO, servicio.clave(), registrado);
    }

    private static List<Integer> buscarEventosIdenticos(
            Connection conexion,
            EventoDemo evento,
            int idOrganizador) throws SQLException {
        String sql = "SELECT id_evento FROM Evento WHERE id_organizador = ? "
                + "AND nombre = ? AND descripcion = ? AND tipo = ? AND ubicacion = ?";
        try (PreparedStatement consulta = conexion.prepareStatement(sql)) {
            consulta.setInt(1, idOrganizador);
            consulta.setString(2, evento.nombre());
            consulta.setString(3, evento.descripcion());
            consulta.setString(4, evento.tipo());
            consulta.setString(5, evento.ubicacion());
            return leerIds(consulta);
        }
    }

    private static List<Integer> buscarServiciosIdenticos(
            Connection conexion,
            ServicioDemo servicio,
            int idProveedor) throws SQLException {
        String sql = "SELECT id_servicio FROM Servicio WHERE id_proveedor = ? "
                + "AND nombre = ? AND descripcion = ? AND categoria = ? AND tipo = ?";
        try (PreparedStatement consulta = conexion.prepareStatement(sql)) {
            consulta.setInt(1, idProveedor);
            consulta.setString(2, servicio.nombre());
            consulta.setString(3, servicio.descripcion());
            consulta.setString(4, servicio.categoria());
            consulta.setString(5, servicio.tipo());
            return leerIds(consulta);
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

    private static boolean existeEventoConMismoNombre(
            Connection conexion,
            String nombre,
            int idOrganizador) throws SQLException {
        return existeNombre(conexion,
                "SELECT 1 FROM Evento WHERE id_organizador = ? AND UPPER(TRIM(nombre)) = UPPER(?)",
                idOrganizador, nombre);
    }

    private static boolean existeServicioConMismoNombre(
            Connection conexion,
            String nombre,
            int idProveedor) throws SQLException {
        return existeNombre(conexion,
                "SELECT 1 FROM Servicio WHERE id_proveedor = ? AND UPPER(TRIM(nombre)) = UPPER(?)",
                idProveedor, nombre);
    }

    private static boolean existeNombre(
            Connection conexion,
            String sql,
            int propietario,
            String nombre) throws SQLException {
        try (PreparedStatement consulta = conexion.prepareStatement(sql)) {
            consulta.setInt(1, propietario);
            consulta.setString(2, nombre);
            try (ResultSet resultado = consulta.executeQuery()) {
                return resultado.next();
            }
        }
    }

    private static Integer buscarRegistro(Connection conexion, String tipo, String clave)
            throws SQLException {
        try (PreparedStatement consulta = conexion.prepareStatement(
                "SELECT id_registro FROM Registro_demo_catalogo WHERE tipo = ? AND clave = ?")) {
            consulta.setString(1, tipo);
            consulta.setString(2, clave);
            try (ResultSet resultado = consulta.executeQuery()) {
                if (!resultado.next()) {
                    return null;
                }
                return resultado.getInt(1);
            }
        }
    }

    private static boolean existeEvento(Connection conexion, int id) throws SQLException {
        return existeId(conexion, "SELECT 1 FROM Evento WHERE id_evento = ?", id);
    }

    private static boolean existeServicio(Connection conexion, int id) throws SQLException {
        return existeId(conexion, "SELECT 1 FROM Servicio WHERE id_servicio = ?", id);
    }

    private static void validarPropietarioEvento(
            Connection conexion,
            int idEvento,
            int idOrganizadorEsperado,
            String nombre) throws SQLException {
        try (PreparedStatement consulta = conexion.prepareStatement(
                "SELECT id_organizador FROM Evento WHERE id_evento = ?")) {
            consulta.setInt(1, idEvento);
            try (ResultSet resultado = consulta.executeQuery()) {
                if (!resultado.next() || resultado.getInt(1) != idOrganizadorEsperado) {
                    throw conflicto("organizador de la asociación de evento", nombre);
                }
            }
        }
    }

    private static void validarPropietarioServicio(
            Connection conexion,
            int idServicio,
            int idProveedorEsperado,
            String nombre) throws SQLException {
        try (PreparedStatement consulta = conexion.prepareStatement(
                "SELECT id_proveedor FROM Servicio WHERE id_servicio = ?")) {
            consulta.setInt(1, idServicio);
            try (ResultSet resultado = consulta.executeQuery()) {
                if (!resultado.next() || resultado.getInt(1) != idProveedorEsperado) {
                    throw conflicto("proveedor de la asociación de servicio", nombre);
                }
            }
        }
    }

    private static void validarNoReutilizado(
            Connection conexion,
            String tipo,
            String claveEsperada,
            int idRegistro,
            String nombre) throws SQLException {
        try (PreparedStatement consulta = conexion.prepareStatement(
                "SELECT clave FROM Registro_demo_catalogo "
                + "WHERE tipo = ? AND id_registro = ? AND clave <> ?")) {
            consulta.setString(1, tipo);
            consulta.setInt(2, idRegistro);
            consulta.setString(3, claveEsperada);
            try (ResultSet resultado = consulta.executeQuery()) {
                if (resultado.next()) {
                    throw conflicto("asociación reutilizada por otro ejemplo", nombre);
                }
            }
        }
    }

    private static boolean existeId(Connection conexion, String sql, int id) throws SQLException {
        try (PreparedStatement consulta = conexion.prepareStatement(sql)) {
            consulta.setInt(1, id);
            try (ResultSet resultado = consulta.executeQuery()) {
                return resultado.next();
            }
        }
    }

    private static SQLException conflicto(String tipo, String nombre) {
        return new SQLException("Conflicto de identidad para " + tipo + ": " + nombre
                + ". No se modificaron ni duplicaron registros.");
    }

    private static int insertarEvento(
            Connection conexion,
            EventoDemo evento,
            int idOrganizador) throws SQLException {
        String sql = "INSERT INTO Evento "
                + "(id_organizador, nombre, descripcion, fecha_inicio, fecha_fin, ubicacion, "
                + "estado, fecha_creacion, tipo, presupuesto, invitados_iniciales) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement insertar = conexion.prepareStatement(
                sql, Statement.RETURN_GENERATED_KEYS)) {
            insertar.setInt(1, idOrganizador);
            insertar.setString(2, evento.nombre());
            insertar.setString(3, evento.descripcion());
            insertar.setTimestamp(4, Timestamp.valueOf(evento.fechaInicio()));
            insertar.setTimestamp(5, Timestamp.valueOf(evento.fechaFin()));
            insertar.setString(6, evento.ubicacion());
            insertar.setString(7, evento.estado());
            insertar.setTimestamp(8, Timestamp.valueOf(evento.fechaCreacion()));
            insertar.setString(9, evento.tipo());
            insertar.setBigDecimal(10, new BigDecimal(evento.presupuesto()));
            insertar.setInt(11, evento.invitados());
            insertar.executeUpdate();
            return claveGenerada(insertar, "evento " + evento.nombre());
        }
    }

    private static int insertarServicio(
            Connection conexion,
            ServicioDemo servicio,
            int idProveedor) throws SQLException {
        String sql = "INSERT INTO Servicio "
                + "(id_proveedor, nombre, descripcion, precio, categoria, estado, tipo, "
                + "cantidad_maxima_personas, ciudad_zona, anticipacion_minima) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement insertar = conexion.prepareStatement(
                sql, Statement.RETURN_GENERATED_KEYS)) {
            insertar.setInt(1, idProveedor);
            insertar.setString(2, servicio.nombre());
            insertar.setString(3, servicio.descripcion());
            insertar.setBigDecimal(4, new BigDecimal(servicio.precio()));
            insertar.setString(5, servicio.categoria());
            insertar.setString(6, servicio.estado());
            insertar.setString(7, servicio.tipo());
            insertar.setInt(8, servicio.cantidadMaximaPersonas());
            insertar.setString(9, servicio.ciudadZona());
            insertar.setInt(10, servicio.anticipacionMinima());
            insertar.executeUpdate();
            return claveGenerada(insertar, "servicio " + servicio.nombre());
        }
    }

    private static int claveGenerada(PreparedStatement insertar, String entidad)
            throws SQLException {
        try (ResultSet claves = insertar.getGeneratedKeys()) {
            if (!claves.next()) {
                throw new SQLException("No se generó el identificador de " + entidad);
            }
            return claves.getInt(1);
        }
    }

    private static void registrar(
            Connection conexion,
            RegistroValidado registro,
            int id) throws SQLException {
        if (registro.idExistente() != null
                && registro.idExistente() == id
                && buscarRegistro(conexion, registro.tipo(), registro.clave()) != null) {
            return;
        }
        try (PreparedStatement guardar = conexion.prepareStatement(
                "MERGE INTO Registro_demo_catalogo (tipo, clave, id_registro) "
                + "KEY (tipo, clave) VALUES (?, ?, ?)")) {
            guardar.setString(1, registro.tipo());
            guardar.setString(2, registro.clave());
            guardar.setInt(3, id);
            guardar.executeUpdate();
        }
    }
}
