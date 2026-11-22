package inicializacion;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Statement;
import java.util.List;

public final class SincronizarPreferenciasClientesDemo {

    private record ClienteDemo(String usuario, String pais, String ciudad, String tipoEvento) {
    }

    private record CuentaExistente(int idUsuario, String estado) {
    }

    private static final List<ClienteDemo> CLIENTES = List.of(
            new ClienteDemo("ana.martinez", "Colombia", "Bogotá", "Boda"),
            new ClienteDemo("diego.perez", "Colombia", "Bogotá", "Boda"),
            new ClienteDemo("paula.rios", "Colombia", "Medellín", "Quinceaños"));

    private SincronizarPreferenciasClientesDemo() {
    }

    public static synchronized int sincronizar(Connection conexion) throws SQLException {
        boolean autoCommit = conexion.getAutoCommit();
        Savepoint savepoint = null;
        if (autoCommit) {
            conexion.setAutoCommit(false);
        } else {
            savepoint = conexion.setSavepoint();
        }

        int cambios = 0;
        try {
            for (ClienteDemo cliente : CLIENTES) {
                CuentaExistente cuenta = buscarCliente(conexion, cliente.usuario());
                if (cuenta == null) {
                    throw new SQLException(
                            "No se encontró la cuenta demo de cliente " + cliente.usuario());
                }
                if (!"ACTIVO".equalsIgnoreCase(cuenta.estado())) {
                    continue;
                }
                cambios += completarUbicacion(conexion, cuenta.idUsuario(), cliente);
                cambios += asegurarPreferencia(conexion, cuenta.idUsuario(), cliente.tipoEvento());
            }
            if (autoCommit) {
                conexion.commit();
            } else {
                conexion.releaseSavepoint(savepoint);
            }
            return cambios;
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

    private static CuentaExistente buscarCliente(Connection conexion, String usuario)
            throws SQLException {
        String sql = "SELECT id_usuario, estado FROM Usuario "
                + "WHERE usuario = ? AND tipo_usuario = 'CLIENTE'";
        try (PreparedStatement consulta = conexion.prepareStatement(sql)) {
            consulta.setString(1, usuario);
            try (ResultSet resultado = consulta.executeQuery()) {
                if (!resultado.next()) {
                    return null;
                }
                CuentaExistente cuenta = new CuentaExistente(
                        resultado.getInt("id_usuario"), resultado.getString("estado"));
                if (resultado.next()) {
                    throw new SQLException("Hay más de una cuenta demo para " + usuario);
                }
                return cuenta;
            }
        }
    }

    private static int completarUbicacion(
            Connection conexion, int idUsuario, ClienteDemo cliente) throws SQLException {
        String paisActual;
        String ciudadActual;
        try (PreparedStatement consulta = conexion.prepareStatement(
                "SELECT pais, ciudad FROM Usuario WHERE id_usuario = ?")) {
            consulta.setInt(1, idUsuario);
            try (ResultSet resultado = consulta.executeQuery()) {
                if (!resultado.next()) {
                    throw new SQLException("No existe el cliente " + idUsuario);
                }
                paisActual = resultado.getString("pais");
                ciudadActual = resultado.getString("ciudad");
            }
        }

        boolean completarPais = vacio(paisActual);
        boolean completarCiudad = vacio(ciudadActual)
                && !tieneCiudadEnSolicitudMasReciente(conexion, idUsuario);
        if (!completarPais && !completarCiudad) {
            return 0;
        }

        String sql = "UPDATE Usuario SET "
                + "pais = CASE WHEN pais IS NULL OR TRIM(pais) = '' THEN ? ELSE pais END, "
                + "ciudad = CASE WHEN ciudad IS NULL OR TRIM(ciudad) = '' THEN ? ELSE ciudad END "
                + "WHERE id_usuario = ?";
        try (PreparedStatement actualizar = conexion.prepareStatement(sql)) {
            actualizar.setString(1, cliente.pais());
            actualizar.setString(2, completarCiudad ? cliente.ciudad() : ciudadActual);
            actualizar.setInt(3, idUsuario);
            return actualizar.executeUpdate();
        }
    }

    private static boolean tieneCiudadEnSolicitudMasReciente(
            Connection conexion, int idUsuario) throws SQLException {
        String sql = "SELECT NULLIF(TRIM(ciudad), '') FROM Solicitud_Contratacion_Organizador "
                + "WHERE id_cliente = ? ORDER BY fecha_solicitud DESC, id_solicitud DESC LIMIT 1";
        try (PreparedStatement consulta = conexion.prepareStatement(sql)) {
            consulta.setInt(1, idUsuario);
            try (ResultSet resultado = consulta.executeQuery()) {
                return resultado.next() && resultado.getString(1) != null;
            }
        }
    }

    private static int asegurarPreferencia(
            Connection conexion, int idUsuario, String tipoEvento) throws SQLException {
        String buscar = "SELECT 1 FROM ("
                + "SELECT tipo FROM Busqueda_evento WHERE id_usuario = ? "
                + "UNION ALL SELECT tipo FROM Evento WHERE id_cliente = ? "
                + "UNION ALL SELECT tipo_evento AS tipo "
                + "FROM Solicitud_Contratacion_Organizador WHERE id_cliente = ?"
                + ") preferencias WHERE UPPER(TRIM(tipo)) = UPPER(?) LIMIT 1";
        try (PreparedStatement consulta = conexion.prepareStatement(buscar)) {
            consulta.setInt(1, idUsuario);
            consulta.setInt(2, idUsuario);
            consulta.setInt(3, idUsuario);
            consulta.setString(4, tipoEvento);
            try (ResultSet resultado = consulta.executeQuery()) {
                if (resultado.next()) {
                    return 0;
                }
            }
        }
        String insertar = "INSERT INTO Busqueda_evento (id_usuario, tipo, fecha_busqueda) "
                + "VALUES (?, ?, CURRENT_TIMESTAMP)";
        try (PreparedStatement sentencia = conexion.prepareStatement(insertar)) {
            sentencia.setInt(1, idUsuario);
            sentencia.setString(2, tipoEvento);
            return sentencia.executeUpdate();
        }
    }

    private static boolean vacio(String valor) {
        return valor == null || valor.isBlank();
    }
}
