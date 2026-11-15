package inicializacion;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import persistencia.DatabaseConnection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SincronizarPreferenciasClientesDemoTest {

    private String nuevaBase() {
        return "jdbc:h2:mem:preferencias_demo_" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1";
    }

    @Test
    void seedTieneDatosNecesariosSinCrearSegmentosManualmente() throws Exception {
        try (Connection conexion = DatabaseConnection.getConnection(nuevaBase())) {
            assertTrue(CargarDatosEjemplo.cargarSiEstaVacia(conexion));

            assertEquals(3, contar(conexion,
                    "SELECT COUNT(*) FROM Usuario WHERE tipo_usuario = 'CLIENTE' "
                    + "AND pais IS NOT NULL AND ciudad IS NOT NULL"));
            assertEquals(3, contar(conexion, "SELECT COUNT(*) FROM Busqueda_evento"));
            assertEquals(0, contar(conexion, "SELECT COUNT(*) FROM Segmento_cliente"));
            assertEquals(0, SincronizarPreferenciasClientesDemo.sincronizar(conexion));
        }
    }

    @Test
    void sincronizaPorUsuarioPreservaDatosExistentesYEsIdempotente() throws Exception {
        try (Connection conexion = DatabaseConnection.getConnection(nuevaBase());
                Statement sentencia = conexion.createStatement()) {
            sentencia.executeUpdate("""
                    INSERT INTO Usuario
                    (id_usuario, nombre, usuario, contraseña, estado, fecha_registro,
                     acepta_marketing, tipo_usuario, pais, ciudad)
                    VALUES
                    (50, 'Ana Martínez', 'ana.martinez', 'clave-propia', 'ACTIVO',
                     CURRENT_TIMESTAMP, FALSE, 'CLIENTE', 'Chile', 'Cali'),
                    (60, 'Diego Pérez', 'diego.perez', '1234', 'ACTIVO',
                     CURRENT_TIMESTAMP, FALSE, 'CLIENTE', NULL, NULL),
                    (70, 'Paula Ríos', 'paula.rios', '1234', 'ACTIVO',
                     CURRENT_TIMESTAMP, FALSE, 'CLIENTE', 'Colombia', NULL)
                    """);

            SincronizarCuentasDemo.sincronizar(conexion);
            try (PreparedStatement solicitud = conexion.prepareStatement(
                    "INSERT INTO Solicitud_Contratacion_Organizador "
                    + "(id_cliente, id_organizador, tipo_evento, ciudad, fecha_aproximada, "
                    + "presupuesto_estimado, estado) "
                    + "VALUES (50, ?, 'Boda', 'Cali', DATE '2026-12-01', 100000, 'PENDIENTE')")) {
                solicitud.setInt(1, idOrganizador(conexion, "laura.gomez"));
                solicitud.executeUpdate();
            }
            assertTrue(SincronizarPreferenciasClientesDemo.sincronizar(conexion) > 0);

            assertEquals(50, idUsuario(conexion, "ana.martinez"));
            assertEquals("Chile", datoUsuario(conexion, "pais", "ana.martinez"));
            assertEquals("Cali", datoUsuario(conexion, "ciudad", "ana.martinez"));
            assertFalse(aceptaMarketing(conexion, "ana.martinez"));
            assertEquals(0, contar(conexion,
                    "SELECT COUNT(*) FROM Busqueda_evento WHERE id_usuario = 50 AND tipo = 'Boda'"));
            assertEquals("Colombia", datoUsuario(conexion, "pais", "diego.perez"));
            assertEquals("Bogotá", datoUsuario(conexion, "ciudad", "diego.perez"));
            assertEquals("Medellín", datoUsuario(conexion, "ciudad", "paula.rios"));
            assertEquals(1, contar(conexion,
                    "SELECT COUNT(*) FROM Solicitud_Contratacion_Organizador "
                    + "WHERE id_cliente = 50 AND tipo_evento = 'Boda'"));

            int preferenciasAntes = contar(conexion, "SELECT COUNT(*) FROM Busqueda_evento");
            assertEquals(0, SincronizarPreferenciasClientesDemo.sincronizar(conexion));
            assertEquals(preferenciasAntes, contar(conexion, "SELECT COUNT(*) FROM Busqueda_evento"));
            assertEquals(0, contar(conexion, "SELECT COUNT(*) FROM Segmento_cliente"));
        }
    }

    private int contar(Connection conexion, String sql) throws SQLException {
        try (Statement sentencia = conexion.createStatement();
                ResultSet resultado = sentencia.executeQuery(sql)) {
            assertTrue(resultado.next());
            return resultado.getInt(1);
        }
    }

    private int idUsuario(Connection conexion, String usuario) throws SQLException {
        try (PreparedStatement consulta = conexion.prepareStatement(
                "SELECT id_usuario FROM Usuario WHERE usuario = ?")) {
            consulta.setString(1, usuario);
            try (ResultSet resultado = consulta.executeQuery()) {
                assertTrue(resultado.next());
                return resultado.getInt(1);
            }
        }
    }

    private int idOrganizador(Connection conexion, String usuario) throws SQLException {
        try (PreparedStatement consulta = conexion.prepareStatement(
                "SELECT id_organizador FROM Organizador "
                + "WHERE id_usuario = (SELECT id_usuario FROM Usuario WHERE usuario = ?)")) {
            consulta.setString(1, usuario);
            try (ResultSet resultado = consulta.executeQuery()) {
                assertTrue(resultado.next());
                return resultado.getInt(1);
            }
        }
    }

    private String datoUsuario(Connection conexion, String columna, String usuario)
            throws SQLException {
        try (PreparedStatement consulta = conexion.prepareStatement(
                "SELECT " + columna + " FROM Usuario WHERE usuario = ?")) {
            consulta.setString(1, usuario);
            try (ResultSet resultado = consulta.executeQuery()) {
                assertTrue(resultado.next());
                return resultado.getString(1);
            }
        }
    }

    private boolean aceptaMarketing(Connection conexion, String usuario) throws SQLException {
        try (PreparedStatement consulta = conexion.prepareStatement(
                "SELECT acepta_marketing FROM Usuario WHERE usuario = ?")) {
            consulta.setString(1, usuario);
            try (ResultSet resultado = consulta.executeQuery()) {
                assertTrue(resultado.next());
                return resultado.getBoolean(1);
            }
        }
    }
}
