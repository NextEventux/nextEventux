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
import java.util.LinkedHashMap;
import java.util.Map;

public final class SincronizarCuentasDemo {

    private record PerfilOrganizador(
            String empresa,
            String telefono,
            String ciudad,
            String especialidad,
            String portafolio,
            String tarifaBase) {
    }

    private record PerfilProveedor(
            String nombre,
            String empresa,
            String correo,
            String telefono,
            String direccion,
            String estado) {
    }

    private record Cuenta(
            String nombre,
            String usuario,
            String tipo,
            boolean marketing,
            LocalDateTime fechaRegistro,
            PerfilOrganizador organizador,
            PerfilProveedor proveedor) {
    }

    private record CuentaExistente(int idUsuario, String tipo) {
    }

    private record EstadoValidado(
            Map<String, CuentaExistente> cuentas,
            Map<String, Integer> roles) {
    }

    private record ContadorIdentidad(long siguiente, long maximo) {
    }

    private static final Cuenta[] CUENTAS = {
        new Cuenta("Laura Gómez", "laura.gomez", "ORGANIZADOR", true,
                LocalDateTime.parse("2026-10-01T09:00:00"),
                new PerfilOrganizador("Eventos Laura", "3001234567", "Bogota",
                        "Bodas y eventos sociales",
                        "Organizacion de bodas y celebraciones sociales", "1500000.00"),
                null),
        new Cuenta("Carlos Rodríguez", "carlos.rodriguez", "ORGANIZADOR", true,
                LocalDateTime.parse("2026-10-02T10:30:00"),
                new PerfilOrganizador("Celebraciones CR", "3017654321", "Bogota",
                        "Celebraciones familiares",
                        "Organizacion de reuniones y celebraciones familiares", "1000000.00"),
                null),
        new Cuenta("Valentina Flores", "valentina", "ORGANIZADOR", false,
                LocalDateTime.parse("2026-10-03T08:00:00"),
                new PerfilOrganizador("Momentos Valentina", "3005550101", "Bogota",
                        "Eventos sociales",
                        "Organizacion de eventos sociales y familiares", "900000.00"),
                null),
        new Cuenta("Decoraciones Arcoíris", "decoraciones.arcoiris", "PROVEEDOR", true,
                LocalDateTime.parse("2026-10-03T11:00:00"), null,
                new PerfilProveedor("Mariana López", "Decoraciones Arcoíris",
                        "contacto@arcoiris.com", "3101234567", "Carrera 15 #80-20", "APROBADO")),
        new Cuenta("Sabores & Eventos", "sabores.eventos", "PROVEEDOR", false,
                LocalDateTime.parse("2026-10-03T14:00:00"), null,
                new PerfilProveedor("Andrés Torres", "Sabores & Eventos",
                        "contacto@saboreseventos.com", "3117654321", "Calle 90 #20-30", "APROBADO")),
        new Cuenta("Flash Studio Fotografía", "flash", "PROVEEDOR", false,
                LocalDateTime.parse("2026-10-04T09:00:00"), null,
                new PerfilProveedor("Flash Studio Fotografía", "Flash Studio Fotografía",
                        "flash@ejemplo.com", "3005550203", "Bogotá", "VERIFICADO")),
        new Cuenta("Ana Martínez", "ana.martinez", "CLIENTE", true,
                LocalDateTime.parse("2026-10-04T08:30:00"), null, null),
        new Cuenta("Diego Pérez", "diego.perez", "CLIENTE", false,
                LocalDateTime.parse("2026-10-05T16:00:00"), null, null),
        new Cuenta("Paula Ríos", "paula.rios", "CLIENTE", false,
                LocalDateTime.parse("2026-10-05T17:00:00"), null, null),
        new Cuenta("Camila Reyes", "camila", "ADMINISTRADOR", false,
                LocalDateTime.parse("2026-10-06T09:00:00"), null, null),
        new Cuenta("Mateo Castillo", "mateo", "ADMINISTRADOR", false,
                LocalDateTime.parse("2026-10-06T09:10:00"), null, null),
        new Cuenta("Shirly Montenegro", "shirly", "ADMINISTRADOR", false,
                LocalDateTime.parse("2026-10-06T09:20:00"), null, null)
    };

    private static final Map<String, String> ROLES = Map.of(
            "ADMINISTRADOR", "Administrador general del sistema",
            "ORGANIZADOR", "Usuario encargado de gestionar eventos",
            "PROVEEDOR", "Usuario que ofrece servicios para eventos",
            "CLIENTE", "Usuario que solicita y participa en la gestión de eventos");

    private SincronizarCuentasDemo() {
    }

    public static synchronized int sincronizar(Connection conexion) throws SQLException {
        boolean autoCommit = conexion.getAutoCommit();
        EstadoValidado estado = validarEstado(conexion);
        Savepoint savepoint = null;
        if (autoCommit) {
            // H2 may commit DDL implicitly; repair only after all conflicts are validated.
            actualizarContadores(conexion);
            conexion.setAutoCommit(false);
        } else {
            validarContadores(conexion);
            savepoint = conexion.setSavepoint();
        }

        try {
            Map<String, CuentaExistente> cuentasExistentes = estado.cuentas();
            Map<String, Integer> roles = estado.roles();

            for (Map.Entry<String, Integer> rol : roles.entrySet()) {
                if (rol.getValue() == null) {
                    roles.put(rol.getKey(), insertarRol(conexion, rol.getKey()));
                }
            }

            int creadas = 0;
            for (Cuenta cuenta : CUENTAS) {
                CuentaExistente existente = cuentasExistentes.get(cuenta.usuario());
                int idUsuario;
                if (existente == null) {
                    idUsuario = insertarCuenta(conexion, cuenta);
                    creadas++;
                } else {
                    idUsuario = existente.idUsuario();
                }

                asegurarRelacionRol(conexion, idUsuario, roles.get(cuenta.tipo()));
                asegurarPerfil(conexion, cuenta, idUsuario);
            }

            if (autoCommit) {
                conexion.commit();
            } else {
                conexion.releaseSavepoint(savepoint);
            }
            return creadas;
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

    private static EstadoValidado validarEstado(Connection conexion) throws SQLException {
        Map<String, CuentaExistente> cuentasExistentes = new LinkedHashMap<>();
        for (Cuenta cuenta : CUENTAS) {
            CuentaExistente existente = buscarCuenta(conexion, cuenta);
            cuentasExistentes.put(cuenta.usuario(), existente);
            if (existente != null) {
                validarPerfil(conexion, cuenta, existente.idUsuario());
            }
        }

        Map<String, Integer> roles = new LinkedHashMap<>();
        for (String tipo : ROLES.keySet()) {
            roles.put(tipo, buscarRol(conexion, tipo));
        }

        for (Cuenta cuenta : CUENTAS) {
            CuentaExistente existente = cuentasExistentes.get(cuenta.usuario());
            Integer idRol = roles.get(cuenta.tipo());
            if (existente != null && idRol != null) {
                validarRelacionRol(conexion, existente.idUsuario(), idRol);
            }
        }
        return new EstadoValidado(cuentasExistentes, roles);
    }

    private static void actualizarContadores(Connection conexion) throws SQLException {
        if (!conexion.getAutoCommit()) {
            throw new SQLException(
                    "No se reparan contadores de identidad con una transacción abierta.");
        }
        actualizarContador(conexion, "Usuario", "id_usuario");
        actualizarContador(conexion, "Rol", "id_rol");
        actualizarContador(conexion, "Usuario_rol", "id_usuario_rol");
        actualizarContador(conexion, "Organizador", "id_organizador");
        actualizarContador(conexion, "Proveedor", "id_proveedor");
    }

    private static void validarContadores(Connection conexion) throws SQLException {
        validarContador(conexion, "Usuario", "id_usuario");
        validarContador(conexion, "Rol", "id_rol");
        validarContador(conexion, "Usuario_rol", "id_usuario_rol");
        validarContador(conexion, "Organizador", "id_organizador");
        validarContador(conexion, "Proveedor", "id_proveedor");
    }

    static void actualizarContador(
            Connection conexion,
            String tabla,
            String columna) throws SQLException {
        ContadorIdentidad contador = leerContador(conexion, tabla, columna);
        if (contador.siguiente() <= contador.maximo()) {
            if (contador.maximo() >= Integer.MAX_VALUE) {
                throw new SQLException(
                        "No quedan identificadores disponibles en " + tabla + "." + columna);
            }
            try (Statement actualizar = conexion.createStatement()) {
                actualizar.execute("ALTER TABLE " + tabla + " ALTER COLUMN "
                        + columna + " RESTART WITH " + (contador.maximo() + 1));
            }
        }
    }

    static void validarContador(
            Connection conexion,
            String tabla,
            String columna) throws SQLException {
        ContadorIdentidad contador = leerContador(conexion, tabla, columna);
        if (contador.siguiente() <= contador.maximo()) {
            throw new SQLException(
                    "El contador de " + tabla + "." + columna
                    + " está desactualizado. La conexión tiene una transacción abierta; "
                    + "la sincronización no modificó la transacción del llamador.");
        }
    }

    private static ContadorIdentidad leerContador(
            Connection conexion,
            String tabla,
            String columna) throws SQLException {
        String sql = "SELECT IDENTITY_BASE, (SELECT COALESCE(MAX(" + columna + "), 0) FROM "
                + tabla + ") FROM INFORMATION_SCHEMA.COLUMNS "
                + "WHERE TABLE_SCHEMA = 'PUBLIC' AND TABLE_NAME = ? AND COLUMN_NAME = ?";
        boolean contadorNulo;
        try (PreparedStatement consulta = conexion.prepareStatement(sql)) {
            consulta.setString(1, tabla.toUpperCase());
            consulta.setString(2, columna.toUpperCase());
            try (ResultSet resultado = consulta.executeQuery()) {
                if (!resultado.next()) {
                    throw new SQLException(
                            "No se encontró el contador de " + tabla + "." + columna);
                }
                long siguiente = resultado.getLong(1);
                contadorNulo = resultado.wasNull();
                long maximo = resultado.getLong(2);
                if (contadorNulo) {
                    throw new SQLException(
                        "No se pudo leer el contador de " + tabla + "." + columna);
                }
                return new ContadorIdentidad(siguiente, maximo);
            }
        }
    }

    private static CuentaExistente buscarCuenta(Connection conexion, Cuenta cuenta)
            throws SQLException {
        try (PreparedStatement consulta = conexion.prepareStatement(
                "SELECT id_usuario, usuario, tipo_usuario FROM Usuario "
                + "WHERE UPPER(TRIM(usuario)) = UPPER(?)")) {
            consulta.setString(1, cuenta.usuario());
            try (ResultSet resultado = consulta.executeQuery()) {
                if (!resultado.next()) {
                    return null;
                }

                CuentaExistente existente = new CuentaExistente(
                        resultado.getInt("id_usuario"),
                        resultado.getString("tipo_usuario"));
                String usuarioExistente = resultado.getString("usuario");
                if (resultado.next()) {
                    throw new SQLException(
                            "Nombre de usuario duplicado o con variantes: " + cuenta.usuario());
                }
                if (!cuenta.usuario().equals(usuarioExistente)) {
                    throw new SQLException(
                            "Conflicto de identidad: el usuario oficial "
                            + cuenta.usuario() + " existe con otro uso de mayúsculas o espacios.");
                }
                if (!cuenta.tipo().equalsIgnoreCase(existente.tipo())) {
                    throw new SQLException(
                            "Conflicto de tipo de usuario: " + cuenta.usuario());
                }
                return existente;
            }
        }
    }

    private static Integer buscarRol(Connection conexion, String tipo) throws SQLException {
        try (PreparedStatement consulta = conexion.prepareStatement(
                "SELECT id_rol, estado FROM Rol WHERE UPPER(TRIM(nombre)) = UPPER(?)")) {
            consulta.setString(1, tipo);
            try (ResultSet resultado = consulta.executeQuery()) {
                if (!resultado.next()) {
                    return null;
                }
                int id = resultado.getInt("id_rol");
                String estado = resultado.getString("estado");
                if (resultado.next()) {
                    throw new SQLException("Nombre de rol duplicado: " + tipo);
                }
                if (!"ACTIVO".equalsIgnoreCase(estado)) {
                    throw new SQLException("El rol de demostración no está activo: " + tipo);
                }
                return id;
            }
        }
    }

    private static int insertarRol(Connection conexion, String tipo) throws SQLException {
        try (PreparedStatement insertar = conexion.prepareStatement(
                "INSERT INTO Rol (nombre, descripcion, estado) VALUES (?, ?, 'ACTIVO')",
                Statement.RETURN_GENERATED_KEYS)) {
            insertar.setString(1, tipo);
            insertar.setString(2, ROLES.get(tipo));
            insertar.executeUpdate();
            try (ResultSet claves = insertar.getGeneratedKeys()) {
                if (!claves.next()) {
                    throw new SQLException("No se generó el identificador del rol " + tipo);
                }
                return claves.getInt(1);
            }
        }
    }

    private static int insertarCuenta(Connection conexion, Cuenta cuenta) throws SQLException {
        try (PreparedStatement insertar = conexion.prepareStatement(
                "INSERT INTO Usuario "
                + "(nombre, usuario, contraseña, estado, fecha_registro, "
                + "acepta_marketing, tipo_usuario) "
                + "VALUES (?, ?, '1234', 'ACTIVO', ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            insertar.setString(1, cuenta.nombre());
            insertar.setString(2, cuenta.usuario());
            insertar.setTimestamp(3, Timestamp.valueOf(cuenta.fechaRegistro()));
            insertar.setBoolean(4, cuenta.marketing());
            insertar.setString(5, cuenta.tipo());
            insertar.executeUpdate();
            try (ResultSet claves = insertar.getGeneratedKeys()) {
                if (!claves.next()) {
                    throw new SQLException(
                            "No se generó el identificador de " + cuenta.usuario());
                }
                return claves.getInt(1);
            }
        }
    }

    private static void validarPerfil(
            Connection conexion,
            Cuenta cuenta,
            int idUsuario) throws SQLException {
        int organizadores = contarPerfil(conexion, "Organizador", idUsuario);
        int proveedores = contarPerfil(conexion, "Proveedor", idUsuario);
        if (organizadores > 1 || proveedores > 1) {
            throw new SQLException("Perfiles duplicados para la cuenta " + cuenta.usuario());
        }

        boolean tipoIncompatible = switch (cuenta.tipo()) {
            case "ORGANIZADOR" -> proveedores > 0;
            case "PROVEEDOR" -> organizadores > 0;
            default -> organizadores > 0 || proveedores > 0;
        };
        if (tipoIncompatible) {
            throw new SQLException("Perfil incompatible con la cuenta " + cuenta.usuario());
        }
    }

    private static int contarPerfil(Connection conexion, String tabla, int idUsuario)
            throws SQLException {
        try (PreparedStatement consulta = conexion.prepareStatement(
                "SELECT COUNT(*) FROM " + tabla + " WHERE id_usuario = ?")) {
            consulta.setInt(1, idUsuario);
            try (ResultSet resultado = consulta.executeQuery()) {
                resultado.next();
                return resultado.getInt(1);
            }
        }
    }

    private static void validarRelacionRol(
            Connection conexion,
            int idUsuario,
            int idRol) throws SQLException {
        if (contarRelacionRol(conexion, idUsuario, idRol) > 1) {
            throw new SQLException(
                    "Relación Usuario_rol duplicada para el usuario " + idUsuario);
        }
    }

    private static int contarRelacionRol(Connection conexion, int idUsuario, int idRol)
            throws SQLException {
        try (PreparedStatement consulta = conexion.prepareStatement(
                "SELECT COUNT(*) FROM Usuario_rol WHERE id_usuario = ? AND id_rol = ?")) {
            consulta.setInt(1, idUsuario);
            consulta.setInt(2, idRol);
            try (ResultSet resultado = consulta.executeQuery()) {
                resultado.next();
                return resultado.getInt(1);
            }
        }
    }

    private static void asegurarRelacionRol(
            Connection conexion,
            int idUsuario,
            int idRol) throws SQLException {
        if (contarRelacionRol(conexion, idUsuario, idRol) == 0) {
            try (PreparedStatement insertar = conexion.prepareStatement(
                    "INSERT INTO Usuario_rol (id_usuario, id_rol) VALUES (?, ?)")) {
                insertar.setInt(1, idUsuario);
                insertar.setInt(2, idRol);
                insertar.executeUpdate();
            }
        }
    }

    private static void asegurarPerfil(Connection conexion, Cuenta cuenta, int idUsuario)
            throws SQLException {
        if (cuenta.organizador() != null
                && contarPerfil(conexion, "Organizador", idUsuario) == 0) {
            PerfilOrganizador perfil = cuenta.organizador();
            try (PreparedStatement insertar = conexion.prepareStatement(
                    "INSERT INTO Organizador "
                    + "(id_usuario, empresa, telefono, estado, ciudad, especialidad, "
                    + "portafolio, tarifa_base, estado_verificacion) "
                    + "VALUES (?, ?, ?, 'ACTIVO', ?, ?, ?, ?, 'VERIFICADO')")) {
                insertar.setInt(1, idUsuario);
                insertar.setString(2, perfil.empresa());
                insertar.setString(3, perfil.telefono());
                insertar.setString(4, perfil.ciudad());
                insertar.setString(5, perfil.especialidad());
                insertar.setString(6, perfil.portafolio());
                insertar.setBigDecimal(7, new BigDecimal(perfil.tarifaBase()));
                insertar.executeUpdate();
            }
        } else if (cuenta.proveedor() != null
                && contarPerfil(conexion, "Proveedor", idUsuario) == 0) {
            PerfilProveedor perfil = cuenta.proveedor();
            try (PreparedStatement insertar = conexion.prepareStatement(
                    "INSERT INTO Proveedor "
                    + "(id_usuario, nombre, empresa, correo, telefono, direccion, estado) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?)")) {
                insertar.setInt(1, idUsuario);
                insertar.setString(2, perfil.nombre());
                insertar.setString(3, perfil.empresa());
                insertar.setString(4, perfil.correo());
                insertar.setString(5, perfil.telefono());
                insertar.setString(6, perfil.direccion());
                insertar.setString(7, perfil.estado());
                insertar.executeUpdate();
            }
        }
    }
}
