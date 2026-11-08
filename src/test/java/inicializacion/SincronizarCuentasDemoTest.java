package inicializacion;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import persistencia.DatabaseConnection;
import static org.junit.jupiter.api.Assertions.*;

class SincronizarCuentasDemoTest {

    private String nuevaBase() {
        return "jdbc:h2:mem:demo_" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1";
    }

    private int contar(Connection conexion, String sql) throws SQLException {
        try (Statement statement = conexion.createStatement();
                ResultSet resultado = statement.executeQuery(sql)) {
            assertTrue(resultado.next());
            return resultado.getInt(1);
        }
    }

    private long contadorIdentidad(Connection conexion, String tabla, String columna)
            throws SQLException {
        try (PreparedStatement consulta = conexion.prepareStatement(
                "SELECT IDENTITY_BASE FROM INFORMATION_SCHEMA.COLUMNS "
                + "WHERE TABLE_SCHEMA = 'PUBLIC' AND TABLE_NAME = ? AND COLUMN_NAME = ?")) {
            consulta.setString(1, tabla.toUpperCase());
            consulta.setString(2, columna.toUpperCase());
            try (ResultSet resultado = consulta.executeQuery()) {
                assertTrue(resultado.next());
                return resultado.getLong(1);
            }
        }
    }

    @Test
    void baseNuevaConservaLasDoceCuentasYCompletaSusRelaciones() throws Exception {
        try (Connection conexion = DatabaseConnection.getConnection(nuevaBase())) {
            assertTrue(CargarDatosEjemplo.cargarSiEstaVacia(conexion));
            assertEquals(0, SincronizarCuentasDemo.sincronizar(conexion));

            assertEquals(12, contar(conexion, "SELECT COUNT(*) FROM Usuario"));
            assertEquals(4, contar(conexion, "SELECT COUNT(*) FROM Rol"));
            assertEquals(12, contar(conexion, """
                    SELECT COUNT(*)
                    FROM Usuario u
                    JOIN Usuario_rol ur ON ur.id_usuario = u.id_usuario
                    JOIN Rol r ON r.id_rol = ur.id_rol
                    WHERE u.tipo_usuario = r.nombre
                    """));
            assertEquals(3, contar(conexion, "SELECT COUNT(*) FROM Organizador"));
            assertEquals(3, contar(conexion, "SELECT COUNT(*) FROM Proveedor"));
            assertEquals(12, contar(conexion,
                    "SELECT COUNT(*) FROM Usuario WHERE contraseña = '1234'"));

            assertEquals(0, SincronizarCuentasDemo.sincronizar(conexion));
            assertEquals(12, contar(conexion, "SELECT COUNT(*) FROM Usuario"));
            assertEquals(12, contar(conexion, "SELECT COUNT(*) FROM Usuario_rol"));
        }
    }

    @Test
    void baseExistenteAgregaSoloFaltantesConIdsGeneradosYConservaDatos() throws Exception {
        try (Connection conexion = DatabaseConnection.getConnection(nuevaBase());
                Statement statement = conexion.createStatement()) {
            statement.executeUpdate("""
                    INSERT INTO Usuario
                    (id_usuario, nombre, usuario, contraseña, estado,
                     fecha_registro, acepta_marketing, tipo_usuario)
                    VALUES
                    (1, 'Cuenta personal', 'personal', 'clave-personal',
                     'ACTIVO', CURRENT_TIMESTAMP, FALSE, 'CLIENTE'),
                    (50, 'Laura Gómez', 'laura.gomez', 'clave-cambiada',
                     'ACTIVO', CURRENT_TIMESTAMP, TRUE, 'ORGANIZADOR'),
                    (51, 'Decoraciones Arcoíris', 'decoraciones.arcoiris',
                     'clave-propia', 'ACTIVO', CURRENT_TIMESTAMP, TRUE, 'PROVEEDOR')
                    """);
            statement.executeUpdate("""
                    INSERT INTO Rol (id_rol, nombre, descripcion, estado)
                    VALUES (1, 'PERSONAL', 'Rol personal', 'ACTIVO')
                    """);
            statement.executeUpdate(
                    "INSERT INTO Usuario_rol (id_usuario_rol, id_usuario, id_rol) "
                    + "VALUES (1, 1, 1)");
            statement.executeUpdate("""
                    INSERT INTO Organizador
                    (id_organizador, id_usuario, empresa, telefono, estado,
                     ciudad, especialidad, portafolio, tarifa_base, estado_verificacion)
                    VALUES
                    (1, 50, 'Empresa personal', '3000000000', 'ACTIVO',
                     'Cali', 'Especialidad propia', 'Portafolio propio',
                     123.45, 'PENDIENTE')
                    """);

            assertEquals(10, SincronizarCuentasDemo.sincronizar(conexion));

            assertEquals(13, contar(conexion, "SELECT COUNT(*) FROM Usuario"));
            assertEquals(1, contar(conexion,
                    "SELECT COUNT(*) FROM Usuario WHERE id_usuario = 1 AND usuario = 'personal' "
                    + "AND contraseña = 'clave-personal'"));
            assertEquals(1, contar(conexion,
                    "SELECT COUNT(*) FROM Usuario WHERE usuario = 'laura.gomez' "
                    + "AND id_usuario = 50 AND contraseña = 'clave-cambiada'"));
            assertEquals(1, contar(conexion,
                    "SELECT COUNT(*) FROM Usuario WHERE usuario = 'decoraciones.arcoiris' "
                    + "AND id_usuario = 51 AND contraseña = 'clave-propia'"));
            assertEquals(1, contar(conexion,
                    "SELECT COUNT(*) FROM Organizador WHERE id_usuario = 50 "
                    + "AND empresa = 'Empresa personal' AND ciudad = 'Cali' "
                    + "AND tarifa_base = 123.45"));
            assertEquals(3, contar(conexion, "SELECT COUNT(*) FROM Organizador"));
            assertEquals(3, contar(conexion, "SELECT COUNT(*) FROM Proveedor"));
            assertEquals(12, contar(conexion, """
                    SELECT COUNT(*)
                    FROM Usuario u
                    JOIN Usuario_rol ur ON ur.id_usuario = u.id_usuario
                    JOIN Rol r ON r.id_rol = ur.id_rol
                    WHERE u.tipo_usuario = r.nombre
                    """));
            assertEquals(0, contar(conexion, "SELECT COUNT(*) FROM Evento"));
            assertEquals(0, contar(conexion, "SELECT COUNT(*) FROM Servicio"));

            assertEquals(0, SincronizarCuentasDemo.sincronizar(conexion));
            assertEquals(13, contar(conexion, "SELECT COUNT(*) FROM Usuario"));
            assertEquals(13, contar(conexion, "SELECT COUNT(*) FROM Usuario_rol"));
        }
    }

    @Test
    void fallaSinCambiosSiUnNombreOficialYaPerteneceAOtroTipo() throws Exception {
        try (Connection conexion = DatabaseConnection.getConnection(nuevaBase());
                Statement statement = conexion.createStatement()) {
            statement.executeUpdate("""
                    INSERT INTO Usuario
                    (nombre, usuario, contraseña, estado, fecha_registro,
                     acepta_marketing, tipo_usuario)
                    VALUES ('Cuenta conflictiva', 'laura.gomez', 'clave',
                            'ACTIVO', CURRENT_TIMESTAMP, FALSE, 'PROVEEDOR')
                    """);

            assertThrows(SQLException.class,
                    () -> SincronizarCuentasDemo.sincronizar(conexion));
            assertEquals(1, contar(conexion, "SELECT COUNT(*) FROM Usuario"));
            assertEquals(0, contar(conexion, "SELECT COUNT(*) FROM Rol"));
        }
    }

    @Test
    void fallaSiUnNombreOficialSoloExisteConMayusculasODiferenciasDeEspacios()
            throws Exception {
        for (String usuario : new String[]{"LAURA.GOMEZ", " laura.gomez "}) {
            try (Connection conexion = DatabaseConnection.getConnection(nuevaBase());
                    Statement statement = conexion.createStatement()) {
                statement.executeUpdate("""
                        INSERT INTO Usuario
                        (id_usuario, nombre, usuario, contraseña, estado, fecha_registro,
                         acepta_marketing, tipo_usuario)
                        VALUES (100, 'Laura personal', '%s', 'clave-propia',
                                'ACTIVO', CURRENT_TIMESTAMP, FALSE, 'ORGANIZADOR')
                        """.formatted(usuario));
                long contadorAntes = contadorIdentidad(conexion, "Usuario", "id_usuario");

                assertThrows(SQLException.class,
                        () -> SincronizarCuentasDemo.sincronizar(conexion));
                assertEquals(1, contar(conexion, "SELECT COUNT(*) FROM Usuario"));
                assertEquals(0, contar(conexion, "SELECT COUNT(*) FROM Rol"));
                assertEquals(contadorAntes,
                        contadorIdentidad(conexion, "Usuario", "id_usuario"));
            }
        }
    }

    @Test
    void fallaSinElegirArbitrariamenteEntreUsuariosDuplicados() throws Exception {
        try (Connection conexion = DatabaseConnection.getConnection(nuevaBase());
                Statement statement = conexion.createStatement()) {
            statement.executeUpdate("""
                    INSERT INTO Usuario
                    (id_usuario, nombre, usuario, contraseña, estado,
                     fecha_registro, acepta_marketing, tipo_usuario)
                    VALUES
                    (1, 'Laura uno', 'laura.gomez', 'clave1',
                     'ACTIVO', CURRENT_TIMESTAMP, TRUE, 'ORGANIZADOR'),
                    (2, 'Laura dos', ' LAURA.GOMEZ ', 'clave2',
                     'ACTIVO', CURRENT_TIMESTAMP, TRUE, 'ORGANIZADOR')
                    """);

            assertThrows(SQLException.class,
                    () -> SincronizarCuentasDemo.sincronizar(conexion));
            assertEquals(2, contar(conexion, "SELECT COUNT(*) FROM Usuario"));
            assertEquals(0, contar(conexion, "SELECT COUNT(*) FROM Rol"));
        }
    }

    @Test
    void fallaSiElPerfilExistenteEsIncompatibleSinInsertarRegistrosParciales()
            throws Exception {
        try (Connection conexion = DatabaseConnection.getConnection(nuevaBase());
                Statement statement = conexion.createStatement()) {
            statement.executeUpdate("""
                    INSERT INTO Usuario
                    (nombre, usuario, contraseña, estado, fecha_registro,
                     acepta_marketing, tipo_usuario)
                    VALUES ('Laura Gómez', 'laura.gomez', 'clave',
                            'ACTIVO', CURRENT_TIMESTAMP, TRUE, 'ORGANIZADOR')
                    """);
            statement.executeUpdate("""
                    INSERT INTO Proveedor (id_usuario, nombre, estado)
                    VALUES (1, 'Perfil incompatible', 'APROBADO')
                    """);

            assertThrows(SQLException.class,
                    () -> SincronizarCuentasDemo.sincronizar(conexion));
            assertEquals(1, contar(conexion, "SELECT COUNT(*) FROM Usuario"));
            assertEquals(0, contar(conexion, "SELECT COUNT(*) FROM Rol"));
            assertEquals(1, contar(conexion, "SELECT COUNT(*) FROM Proveedor"));
            assertEquals(0, contar(conexion, "SELECT COUNT(*) FROM Usuario_rol"));
        }
    }

    @Test
    void fallaSiLaCuentaTienePerfilesDuplicadosSinInsertarRegistrosParciales()
            throws Exception {
        try (Connection conexion = DatabaseConnection.getConnection(nuevaBase());
                Statement statement = conexion.createStatement()) {
            statement.executeUpdate("""
                    INSERT INTO Usuario
                    (nombre, usuario, contraseña, estado, fecha_registro,
                     acepta_marketing, tipo_usuario)
                    VALUES ('Flash', 'flash', 'clave',
                            'ACTIVO', CURRENT_TIMESTAMP, TRUE, 'PROVEEDOR')
                    """);
            statement.executeUpdate("""
                    INSERT INTO Proveedor (id_usuario, nombre, estado)
                    VALUES (1, 'Perfil uno', 'APROBADO'), (1, 'Perfil dos', 'APROBADO')
                    """);

            assertThrows(SQLException.class,
                    () -> SincronizarCuentasDemo.sincronizar(conexion));
            assertEquals(1, contar(conexion, "SELECT COUNT(*) FROM Usuario"));
            assertEquals(2, contar(conexion,
                    "SELECT COUNT(*) FROM Proveedor WHERE id_usuario = 1"));
            assertEquals(0, contar(conexion, "SELECT COUNT(*) FROM Rol"));
            assertEquals(0, contar(conexion, "SELECT COUNT(*) FROM Usuario_rol"));
        }
    }

    @Test
    void fallaSiLaRelacionUsuarioRolEstaDuplicadaSinInsertarRegistrosParciales()
            throws Exception {
        try (Connection conexion = DatabaseConnection.getConnection(nuevaBase());
                Statement statement = conexion.createStatement()) {
            statement.executeUpdate("""
                    INSERT INTO Usuario
                    (nombre, usuario, contraseña, estado, fecha_registro,
                     acepta_marketing, tipo_usuario)
                    VALUES ('Laura Gómez', 'laura.gomez', 'clave',
                            'ACTIVO', CURRENT_TIMESTAMP, TRUE, 'ORGANIZADOR')
                    """);
            statement.executeUpdate("""
                    INSERT INTO Rol (nombre, descripcion, estado)
                    VALUES ('ORGANIZADOR', 'Rol oficial', 'ACTIVO')
                    """);
            statement.executeUpdate("""
                    INSERT INTO Usuario_rol (id_usuario, id_rol)
                    VALUES (1, 1), (1, 1)
                    """);

            assertThrows(SQLException.class,
                    () -> SincronizarCuentasDemo.sincronizar(conexion));
            assertEquals(1, contar(conexion, "SELECT COUNT(*) FROM Usuario"));
            assertEquals(1, contar(conexion, "SELECT COUNT(*) FROM Rol"));
            assertEquals(2, contar(conexion, "SELECT COUNT(*) FROM Usuario_rol"));
            assertEquals(0, contar(conexion, "SELECT COUNT(*) FROM Proveedor"));
        }
    }

    @Test
    void fallaSinCambiosSiUnRolOficialEstaDuplicado() throws Exception {
        try (Connection conexion = DatabaseConnection.getConnection(nuevaBase());
                Statement statement = conexion.createStatement()) {
            statement.executeUpdate("""
                    INSERT INTO Rol (id_rol, nombre, descripcion, estado)
                    VALUES
                    (1, 'ORGANIZADOR', 'Rol uno', 'ACTIVO'),
                    (2, 'organizador', 'Rol dos', 'ACTIVO')
                    """);

            assertThrows(SQLException.class,
                    () -> SincronizarCuentasDemo.sincronizar(conexion));
            assertEquals(0, contar(conexion, "SELECT COUNT(*) FROM Usuario"));
            assertEquals(2, contar(conexion, "SELECT COUNT(*) FROM Rol"));
        }
    }

    @Test
    void conservaLaTransaccionAbiertaDelLlamador() throws Exception {
        try (Connection conexion = DatabaseConnection.getConnection(nuevaBase());
                Statement statement = conexion.createStatement()) {
            conexion.setAutoCommit(false);
            statement.executeUpdate("""
                    INSERT INTO Usuario
                    (nombre, usuario, contraseña, estado, fecha_registro,
                     acepta_marketing, tipo_usuario)
                    VALUES ('Cuenta personal', 'personal', 'clave',
                            'ACTIVO', CURRENT_TIMESTAMP, FALSE, 'CLIENTE')
                    """);

            assertEquals(12, SincronizarCuentasDemo.sincronizar(conexion));
            assertFalse(conexion.getAutoCommit());
            assertEquals(13, contar(conexion, "SELECT COUNT(*) FROM Usuario"));
            conexion.rollback();
            assertFalse(conexion.getAutoCommit());
            assertEquals(0, contar(conexion, "SELECT COUNT(*) FROM Usuario"));
        }
    }

    @Test
    void noReparaUnaSecuenciaDesactualizadaDentroDeLaTransaccionDelLlamador()
            throws Exception {
        String url = nuevaBase();
        try (Connection conexion = DatabaseConnection.getConnection(url);
                Statement statement = conexion.createStatement()) {
            statement.executeUpdate("""
                    INSERT INTO Usuario
                    (id_usuario, nombre, usuario, contraseña, estado, fecha_registro,
                     acepta_marketing, tipo_usuario)
                    VALUES (100, 'Cuenta existente', 'existente', 'clave',
                            'ACTIVO', CURRENT_TIMESTAMP, FALSE, 'CLIENTE')
                    """);
            long contadorAntes = contadorIdentidad(conexion, "Usuario", "id_usuario");
            conexion.setAutoCommit(false);
            statement.executeUpdate("""
                    INSERT INTO Usuario
                    (id_usuario, nombre, usuario, contraseña, estado, fecha_registro,
                     acepta_marketing, tipo_usuario)
                    VALUES (101, 'Cuenta no confirmada', 'sin.confirmar', 'clave',
                            'ACTIVO', CURRENT_TIMESTAMP, FALSE, 'CLIENTE')
                    """);

            assertThrows(SQLException.class,
                    () -> SincronizarCuentasDemo.sincronizar(conexion));
            assertFalse(conexion.getAutoCommit());
            assertEquals(contadorAntes,
                    contadorIdentidad(conexion, "Usuario", "id_usuario"));
            assertEquals(2, contar(conexion, "SELECT COUNT(*) FROM Usuario"));
            conexion.rollback();
            assertEquals(1, contar(conexion, "SELECT COUNT(*) FROM Usuario"));
        }
    }

    @Test
    void validaConflictosAntesDeRepararContadoresDesactualizados() throws Exception {
        try (Connection conexion = DatabaseConnection.getConnection(nuevaBase());
                Statement statement = conexion.createStatement()) {
            statement.executeUpdate("""
                    INSERT INTO Usuario
                    (id_usuario, nombre, usuario, contraseña, estado, fecha_registro,
                     acepta_marketing, tipo_usuario)
                    VALUES (100, 'Laura personal', ' LAURA.GOMEZ ', 'clave-propia',
                            'ACTIVO', CURRENT_TIMESTAMP, FALSE, 'ORGANIZADOR')
                    """);
            long contadorAntes = contadorIdentidad(conexion, "Usuario", "id_usuario");

            assertThrows(SQLException.class,
                    () -> SincronizarCuentasDemo.sincronizar(conexion));
            assertEquals(contadorAntes,
                    contadorIdentidad(conexion, "Usuario", "id_usuario"));
            assertEquals(0, contar(conexion, "SELECT COUNT(*) FROM Rol"));
        }
    }

    @Test
    void revierteInsercionesParcialesSiFallaUnaInsercionDuranteLaSincronizacion()
            throws Exception {
        try (Connection conexion = DatabaseConnection.getConnection(nuevaBase());
                Statement statement = conexion.createStatement()) {
            statement.execute("ALTER TABLE Usuario ADD CONSTRAINT impedir_paula "
                    + "CHECK (usuario <> 'paula.rios')");

            assertThrows(SQLException.class,
                    () -> SincronizarCuentasDemo.sincronizar(conexion));
            assertEquals(0, contar(conexion, "SELECT COUNT(*) FROM Usuario"));
            assertEquals(0, contar(conexion, "SELECT COUNT(*) FROM Rol"));
            assertEquals(0, contar(conexion, "SELECT COUNT(*) FROM Usuario_rol"));
            assertEquals(0, contar(conexion, "SELECT COUNT(*) FROM Organizador"));
            assertEquals(0, contar(conexion, "SELECT COUNT(*) FROM Proveedor"));
        }
    }

    @Test
    void puedeReinicializarUnaBaseExistenteSinDuplicarRegistros() throws Exception {
        try (Connection conexion = DatabaseConnection.getConnection(nuevaBase());
                Statement statement = conexion.createStatement()) {
            statement.executeUpdate("""
                    INSERT INTO Usuario
                    (nombre, usuario, contraseña, estado, fecha_registro,
                     acepta_marketing, tipo_usuario)
                    VALUES ('Cuenta personal', 'personal', 'clave',
                            'ACTIVO', CURRENT_TIMESTAMP, FALSE, 'CLIENTE')
                    """);

            assertFalse(CargarDatosEjemplo.cargarSiEstaVacia(conexion));
            assertEquals(12, SincronizarCuentasDemo.sincronizar(conexion));
            assertEquals(0, SincronizarCuentasDemo.sincronizar(conexion));
            assertEquals(13, contar(conexion, "SELECT COUNT(*) FROM Usuario"));
            assertEquals(12, contar(conexion, "SELECT COUNT(*) FROM Usuario_rol"));
            assertEquals(3, contar(conexion, "SELECT COUNT(*) FROM Organizador"));
            assertEquals(3, contar(conexion, "SELECT COUNT(*) FROM Proveedor"));
        }
    }
}
