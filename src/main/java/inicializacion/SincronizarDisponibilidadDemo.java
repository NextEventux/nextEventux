package inicializacion;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Time;

public final class SincronizarDisponibilidadDemo {

    public record Resultado(boolean creada, String advertencia) {
    }

    private static final String TIPO = "DISPONIBILIDAD";
    private static final String CLAVE = "fotografia-quince-sofia-2027-02-20";
    private static final String CLAVE_SERVICIO = "cobertura-fotografica";
    private static final Date FECHA = Date.valueOf("2027-02-20");
    private static final Time HORA_INICIO = Time.valueOf("14:00:00");
    private static final Time HORA_FIN = Time.valueOf("23:59:00");

    private SincronizarDisponibilidadDemo() {
    }

    public static synchronized Resultado sincronizar(Connection conexion) throws SQLException {
        int idServicio = buscarServicioOficial(conexion);
        Integer idRegistrado = buscarRegistro(conexion);
        if (idRegistrado != null) {
            validarDisponibilidadRegistrada(conexion, idRegistrado, idServicio);
            return new Resultado(false, null);
        }

        if (existeDisponibilidadExacta(conexion, idServicio)) {
            return new Resultado(false, null);
        }

        if (existeDisponibilidadSolapada(conexion, idServicio)) {
            return new Resultado(false,
                    "No se agregó el horario oficial de fotografía del cumpleaños de Sofía "
                    + "porque se solapa con una disponibilidad existente. Se conservaron "
                    + "todos los horarios y la inicialización continuará.");
        }

        boolean autoCommit = conexion.getAutoCommit();
        Savepoint savepoint = null;
        if (autoCommit) {
            SincronizarCuentasDemo.actualizarContador(
                    conexion, "Disponibilidad_Servicio", "id_disponibilidad");
            conexion.setAutoCommit(false);
        } else {
            SincronizarCuentasDemo.validarContador(
                    conexion, "Disponibilidad_Servicio", "id_disponibilidad");
            savepoint = conexion.setSavepoint();
        }

        try {
            int idDisponibilidad = insertarDisponibilidad(conexion, idServicio);
            registrar(conexion, idDisponibilidad);
            if (autoCommit) {
                conexion.commit();
            } else {
                conexion.releaseSavepoint(savepoint);
            }
            return new Resultado(true, null);
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

    private static int buscarServicioOficial(Connection conexion) throws SQLException {
        String sql = """
                SELECT s.id_servicio
                FROM Registro_demo_catalogo r
                JOIN Servicio s ON s.id_servicio = r.id_registro
                JOIN Proveedor p ON p.id_proveedor = s.id_proveedor
                JOIN Usuario u ON u.id_usuario = p.id_usuario
                WHERE r.tipo = 'SERVICIO'
                  AND r.clave = ?
                  AND u.usuario = 'flash'
                  AND u.tipo_usuario = 'PROVEEDOR'
                """;
        try (PreparedStatement consulta = conexion.prepareStatement(sql)) {
            consulta.setString(1, CLAVE_SERVICIO);
            try (ResultSet resultado = consulta.executeQuery()) {
                if (!resultado.next()) {
                    throw new SQLException(
                            "No se encontró la asociación válida del servicio oficial "
                            + "Cobertura fotográfica.");
                }
                int id = resultado.getInt(1);
                if (resultado.next()) {
                    throw new SQLException(
                            "La asociación del servicio oficial Cobertura fotográfica "
                            + "no es única.");
                }
                return id;
            }
        }
    }

    private static Integer buscarRegistro(Connection conexion) throws SQLException {
        try (PreparedStatement consulta = conexion.prepareStatement(
                "SELECT id_registro FROM Registro_demo_catalogo "
                + "WHERE tipo = ? AND clave = ?")) {
            consulta.setString(1, TIPO);
            consulta.setString(2, CLAVE);
            try (ResultSet resultado = consulta.executeQuery()) {
                if (!resultado.next()) {
                    return null;
                }
                int id = resultado.getInt(1);
                if (resultado.next()) {
                    throw new SQLException(
                            "La procedencia de la disponibilidad oficial está duplicada.");
                }
                return id;
            }
        }
    }

    private static void validarDisponibilidadRegistrada(
            Connection conexion,
            int idDisponibilidad,
            int idServicio) throws SQLException {
        try (PreparedStatement consulta = conexion.prepareStatement("""
                SELECT id_servicio, fecha, hora_inicio, hora_fin
                FROM Disponibilidad_Servicio
                WHERE id_disponibilidad = ?
                """)) {
            consulta.setInt(1, idDisponibilidad);
            try (ResultSet resultado = consulta.executeQuery()) {
                if (!resultado.next()) {
                    throw new SQLException(
                            "La procedencia de la disponibilidad oficial está huérfana: "
                            + "la fila asociada ya no existe.");
                }
                boolean coincide = resultado.getInt("id_servicio") == idServicio
                        && FECHA.equals(resultado.getDate("fecha"))
                        && HORA_INICIO.equals(resultado.getTime("hora_inicio"))
                        && HORA_FIN.equals(resultado.getTime("hora_fin"));
                if (!coincide || resultado.next()) {
                    throw new SQLException(
                            "La procedencia de la disponibilidad oficial apunta a datos "
                            + "alterados o inesperados. No se modificó ningún horario.");
                }
            }
        }
    }

    private static boolean existeDisponibilidadExacta(
            Connection conexion,
            int idServicio) throws SQLException {
        try (PreparedStatement consulta = conexion.prepareStatement("""
                SELECT id_disponibilidad
                FROM Disponibilidad_Servicio
                WHERE id_servicio = ? AND fecha = ?
                  AND hora_inicio = ? AND hora_fin = ?
                """)) {
            consulta.setInt(1, idServicio);
            consulta.setDate(2, FECHA);
            consulta.setTime(3, HORA_INICIO);
            consulta.setTime(4, HORA_FIN);
            try (ResultSet resultado = consulta.executeQuery()) {
                if (!resultado.next()) {
                    return false;
                }
                if (resultado.next()) {
                    throw new SQLException(
                            "Hay disponibilidades duplicadas para el horario oficial "
                            + "de fotografía.");
                }
                return true;
            }
        }
    }

    private static boolean existeDisponibilidadSolapada(
            Connection conexion,
            int idServicio) throws SQLException {
        try (PreparedStatement consulta = conexion.prepareStatement("""
                SELECT 1
                FROM Disponibilidad_Servicio
                WHERE id_servicio = ? AND fecha = ?
                  AND hora_inicio < ? AND hora_fin > ?
                """)) {
            consulta.setInt(1, idServicio);
            consulta.setDate(2, FECHA);
            consulta.setTime(3, HORA_FIN);
            consulta.setTime(4, HORA_INICIO);
            try (ResultSet resultado = consulta.executeQuery()) {
                return resultado.next();
            }
        }
    }

    private static int insertarDisponibilidad(
            Connection conexion,
            int idServicio) throws SQLException {
        try (PreparedStatement insertar = conexion.prepareStatement("""
                INSERT INTO Disponibilidad_Servicio
                    (id_servicio, fecha, hora_inicio, hora_fin, estado)
                VALUES (?, ?, ?, ?, 'DISPONIBLE')
                """, Statement.RETURN_GENERATED_KEYS)) {
            insertar.setInt(1, idServicio);
            insertar.setDate(2, FECHA);
            insertar.setTime(3, HORA_INICIO);
            insertar.setTime(4, HORA_FIN);
            insertar.executeUpdate();
            try (ResultSet claves = insertar.getGeneratedKeys()) {
                if (!claves.next()) {
                    throw new SQLException(
                            "No se generó el identificador de la disponibilidad oficial.");
                }
                return claves.getInt(1);
            }
        }
    }

    private static void registrar(Connection conexion, int idDisponibilidad)
            throws SQLException {
        try (PreparedStatement insertar = conexion.prepareStatement("""
                INSERT INTO Registro_demo_catalogo (tipo, clave, id_registro)
                VALUES (?, ?, ?)
                """)) {
            insertar.setString(1, TIPO);
            insertar.setString(2, CLAVE);
            insertar.setInt(3, idDisponibilidad);
            insertar.executeUpdate();
        }
    }
}
