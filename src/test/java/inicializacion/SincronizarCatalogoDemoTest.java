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

class SincronizarCatalogoDemoTest {

    private String nuevaBase() {
        return "jdbc:h2:mem:catalogo_" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1";
    }

    private Connection baseConCuentas() throws Exception {
        Connection conexion = DatabaseConnection.getConnection(nuevaBase());
        SincronizarCuentasDemo.sincronizar(conexion);
        return conexion;
    }

    private int contar(Connection conexion, String sql) throws SQLException {
        try (Statement statement = conexion.createStatement();
                ResultSet resultado = statement.executeQuery(sql)) {
            assertTrue(resultado.next());
            return resultado.getInt(1);
        }
    }

    private int idPerfil(Connection conexion, String tabla, String columna, String usuario)
            throws SQLException {
        String sql = "SELECT p." + columna + " FROM " + tabla + " p "
                + "JOIN Usuario u ON u.id_usuario = p.id_usuario WHERE u.usuario = ?";
        try (PreparedStatement consulta = conexion.prepareStatement(sql)) {
            consulta.setString(1, usuario);
            try (ResultSet resultado = consulta.executeQuery()) {
                assertTrue(resultado.next());
                int id = resultado.getInt(1);
                assertFalse(resultado.next());
                return id;
            }
        }
    }

    private void registrarAsociacion(
            Connection conexion,
            String tipo,
            String clave,
            int idRegistro) throws SQLException {
        try (PreparedStatement insertar = conexion.prepareStatement(
                "INSERT INTO Registro_demo_catalogo (tipo, clave, id_registro) "
                + "VALUES (?, ?, ?)")) {
            insertar.setString(1, tipo);
            insertar.setString(2, clave);
            insertar.setInt(3, idRegistro);
            insertar.executeUpdate();
        }
    }

    @Test
    void baseNuevaReconoceCatalogoDelSeedYEsIdempotente() throws Exception {
        try (Connection conexion = DatabaseConnection.getConnection(nuevaBase())) {
            assertTrue(CargarDatosEjemplo.cargarSiEstaVacia(conexion));
            assertEquals(0, SincronizarCuentasDemo.sincronizar(conexion));

            var resultado = SincronizarCatalogoDemo.sincronizar(conexion);
            assertEquals(0, resultado.eventosCreados());
            assertEquals(0, resultado.serviciosCreados());
            assertEquals(4, contar(conexion, "SELECT COUNT(*) FROM Evento"));
            assertEquals(15, contar(conexion, "SELECT COUNT(*) FROM Servicio"));
            assertEquals(19, contar(conexion, "SELECT COUNT(*) FROM Registro_demo_catalogo"));

            assertEquals(1, contar(conexion, """
                    SELECT COUNT(*) FROM Evento e
                    JOIN Organizador o ON o.id_organizador = e.id_organizador
                    JOIN Usuario u ON u.id_usuario = o.id_usuario
                    WHERE e.nombre = 'Baby Shower Valentina'
                      AND u.usuario = 'carlos.rodriguez'
                    """));
            assertEquals(5, idPerfil(conexion, "Proveedor", "id_proveedor", "flash"));
            assertEquals(5, contar(conexion, """
                    SELECT COUNT(*) FROM Servicio s
                    JOIN Proveedor p ON p.id_proveedor = s.id_proveedor
                    JOIN Usuario u ON u.id_usuario = p.id_usuario
                    WHERE u.usuario = 'flash'
                    """));

            resultado = SincronizarCatalogoDemo.sincronizar(conexion);
            assertEquals(0, resultado.eventosCreados());
            assertEquals(0, resultado.serviciosCreados());
            assertEquals(4, contar(conexion, "SELECT COUNT(*) FROM Evento"));
            assertEquals(15, contar(conexion, "SELECT COUNT(*) FROM Servicio"));
        }
    }

    @Test
    void creaRegistroDeCatalogoEnUnaBaseAntiguaYaInicializada() throws Exception {
        String url = nuevaBase();
        try (Connection conexion = DatabaseConnection.getConnection(url);
                Statement statement = conexion.createStatement()) {
            statement.execute("DROP TABLE Registro_demo_catalogo");
            statement.executeUpdate("""
                    DELETE FROM Esquema_version
                    WHERE nombre = '015_catalogo_demostracion.sql'
                    """);
        }

        try (Connection conexion = DatabaseConnection.getConnection(url)) {
            assertEquals(1, contar(conexion, """
                    SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES
                    WHERE TABLE_SCHEMA = 'PUBLIC'
                      AND TABLE_NAME = 'REGISTRO_DEMO_CATALOGO'
                    """));
            assertEquals(1, contar(conexion, """
                    SELECT COUNT(*) FROM Esquema_version
                    WHERE nombre = '015_catalogo_demostracion.sql'
                    """));
            assertEquals(0, contar(conexion, "SELECT COUNT(*) FROM Evento"));
            assertEquals(0, contar(conexion, "SELECT COUNT(*) FROM Servicio"));
        }

        try (Connection conexion = DatabaseConnection.getConnectionParaSincronizarDemo(url)) {
            assertEquals(1, contar(conexion, """
                    SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES
                    WHERE TABLE_SCHEMA = 'PUBLIC'
                      AND TABLE_NAME = 'REGISTRO_DEMO_CATALOGO'
                    """));
            assertEquals(0, contar(conexion, "SELECT COUNT(*) FROM Usuario"));
        }
    }

    @Test
    void conservaCambiosDeIdentidadEnRegistrosYaVinculados() throws Exception {
        try (Connection conexion = DatabaseConnection.getConnection(nuevaBase());
                Statement statement = conexion.createStatement()) {
            CargarDatosEjemplo.cargar(conexion);
            SincronizarCuentasDemo.sincronizar(conexion);
            assertEquals(0, SincronizarCatalogoDemo.sincronizar(conexion).eventosCreados());

            statement.executeUpdate("""
                    UPDATE Evento
                    SET nombre = 'Boda editada por el equipo',
                        descripcion = 'Descripción local',
                        fecha_inicio = TIMESTAMP '2028-01-01 15:00:00',
                        presupuesto = 7654321.00,
                        estado = 'FINALIZADO'
                    WHERE id_evento = 1
                    """);
            statement.executeUpdate("""
                    UPDATE Servicio
                    SET nombre = 'Servicio fotográfico editado',
                        descripcion = 'Descripción local del equipo',
                        precio = 876543.00,
                        estado = 'RETIRADO'
                    WHERE id_servicio = 5
                    """);

            var resultado = SincronizarCatalogoDemo.sincronizar(conexion);
            assertEquals(0, resultado.eventosCreados());
            assertEquals(0, resultado.serviciosCreados());
            assertEquals(4, contar(conexion, "SELECT COUNT(*) FROM Evento"));
            assertEquals(15, contar(conexion, "SELECT COUNT(*) FROM Servicio"));
            assertEquals(1, contar(conexion, """
                    SELECT COUNT(*) FROM Evento
                    WHERE id_evento = 1 AND nombre = 'Boda editada por el equipo'
                      AND descripcion = 'Descripción local'
                      AND fecha_inicio = TIMESTAMP '2028-01-01 15:00:00'
                      AND presupuesto = 7654321.00 AND estado = 'FINALIZADO'
                    """));
            assertEquals(1, contar(conexion, """
                    SELECT COUNT(*) FROM Servicio
                    WHERE id_servicio = 5 AND nombre = 'Servicio fotográfico editado'
                      AND descripcion = 'Descripción local del equipo'
                      AND precio = 876543.00 AND estado = 'RETIRADO'
                    """));
        }
    }

    @Test
    void baseExistenteAdoptaEjemplosEditadosYAgregaSoloLosFaltantes() throws Exception {
        try (Connection conexion = baseConCuentas();
                Statement statement = conexion.createStatement()) {
            int laura = idPerfil(conexion, "Organizador", "id_organizador", "laura.gomez");
            int flash = idPerfil(conexion, "Proveedor", "id_proveedor", "flash");
            statement.executeUpdate("""
                    INSERT INTO Evento
                    (id_evento, id_organizador, nombre, descripcion, fecha_inicio, fecha_fin,
                     ubicacion, estado, fecha_creacion, tipo, presupuesto, invitados_iniciales)
                    VALUES (1, %d, 'Boda Laura y Daniel', 'Celebración de matrimonio',
                            TIMESTAMP '2028-01-01 15:00:00', TIMESTAMP '2028-01-01 23:00:00',
                            'Salón Primavera', 'FINALIZADO', CURRENT_TIMESTAMP,
                            'Boda', 123456.00, 25)
                    """.formatted(laura));
            statement.executeUpdate("""
                    INSERT INTO Servicio
                    (id_servicio, id_proveedor, nombre, descripcion, precio, categoria, estado,
                     tipo, cantidad_maxima_personas, ciudad_zona, anticipacion_minima)
                    VALUES (5, %d, 'Cobertura fotográfica completa',
                            'Fotógrafo profesional durante todo el evento, con galería digital',
                            987654.00, 'FOTOGRAFIA', 'RETIRADO', 'Fotografía', 300, 'Bogotá', 7)
                    """.formatted(flash));

            var resultado = SincronizarCatalogoDemo.sincronizar(conexion);
            assertEquals(3, resultado.eventosCreados());
            assertEquals(14, resultado.serviciosCreados());
            assertEquals(4, contar(conexion, "SELECT COUNT(*) FROM Evento"));
            assertEquals(15, contar(conexion, "SELECT COUNT(*) FROM Servicio"));
            assertEquals(1, contar(conexion, """
                    SELECT COUNT(*) FROM Evento
                    WHERE id_evento = 1 AND estado = 'FINALIZADO'
                      AND presupuesto = 123456.00 AND invitados_iniciales = 25
                    """));
            assertEquals(1, contar(conexion, """
                    SELECT COUNT(*) FROM Servicio
                    WHERE id_servicio = 5 AND precio = 987654.00 AND estado = 'RETIRADO'
                    """));
            assertEquals(19, contar(conexion, "SELECT COUNT(*) FROM Registro_demo_catalogo"));

            resultado = SincronizarCatalogoDemo.sincronizar(conexion);
            assertEquals(0, resultado.eventosCreados());
            assertEquals(0, resultado.serviciosCreados());
            assertEquals(4, contar(conexion, "SELECT COUNT(*) FROM Evento"));
            assertEquals(15, contar(conexion, "SELECT COUNT(*) FROM Servicio"));
        }
    }

    @Test
    void agregaCatalogoConIdsAutogeneradosAunqueLosIdsDelSeedEstanOcupados()
            throws Exception {
        try (Connection conexion = baseConCuentas();
                Statement statement = conexion.createStatement()) {
            int organizador = idPerfil(conexion, "Organizador", "id_organizador", "laura.gomez");
            int proveedor = idPerfil(conexion, "Proveedor", "id_proveedor", "flash");
            statement.executeUpdate("""
                    INSERT INTO Evento
                    (id_evento, id_organizador, nombre, fecha_inicio, fecha_fin,
                     ubicacion, estado, fecha_creacion)
                    VALUES (1, %d, 'Evento personal', CURRENT_TIMESTAMP,
                            DATEADD('HOUR', 1, CURRENT_TIMESTAMP), 'Cali',
                            'PLANEADO', CURRENT_TIMESTAMP)
                    """.formatted(organizador));
            statement.executeUpdate("""
                    INSERT INTO Servicio
                    (id_servicio, id_proveedor, nombre, precio, estado)
                    VALUES (5, %d, 'Servicio personal', 100.00, 'DISPONIBLE')
                    """.formatted(proveedor));

            var resultado = SincronizarCatalogoDemo.sincronizar(conexion);
            assertEquals(4, resultado.eventosCreados());
            assertEquals(15, resultado.serviciosCreados());
            assertEquals(1, contar(conexion,
                    "SELECT COUNT(*) FROM Evento WHERE id_evento = 1 AND nombre = 'Evento personal'"));
            assertEquals(1, contar(conexion,
                    "SELECT COUNT(*) FROM Servicio WHERE id_servicio = 5 AND nombre = 'Servicio personal'"));
            assertEquals(4, contar(conexion, "SELECT COUNT(*) FROM Evento WHERE id_evento > 1"));
            assertEquals(15, contar(conexion, "SELECT COUNT(*) FROM Servicio WHERE id_servicio > 5"));
            assertEquals(5, contar(conexion, "SELECT COUNT(*) FROM Evento"));
            assertEquals(16, contar(conexion, "SELECT COUNT(*) FROM Servicio"));
        }
    }

    @Test
    void detectaHomonimoIncompatibleAntesDeInsertarORepararContadores() throws Exception {
        try (Connection conexion = baseConCuentas();
                Statement statement = conexion.createStatement()) {
            int valentina = idPerfil(conexion, "Organizador", "id_organizador", "valentina");
            statement.executeUpdate("""
                    INSERT INTO Evento
                    (id_evento, id_organizador, nombre, descripcion, fecha_inicio, fecha_fin,
                     ubicacion, estado, fecha_creacion, tipo, presupuesto, invitados_iniciales)
                    VALUES (100, %d, 'Graduación de Medicina', 'Evento personal distinto',
                            CURRENT_TIMESTAMP, DATEADD('HOUR', 1, CURRENT_TIMESTAMP),
                            'Cali', 'PLANEADO', CURRENT_TIMESTAMP, 'Celebración', 1000, 5)
                    """.formatted(valentina));
            long contador = contadorIdentidad(conexion, "Evento", "id_evento");

            assertThrows(SQLException.class,
                    () -> SincronizarCatalogoDemo.sincronizar(conexion));
            assertEquals(contador, contadorIdentidad(conexion, "Evento", "id_evento"));
            assertEquals(1, contar(conexion, "SELECT COUNT(*) FROM Evento"));
            assertEquals(0, contar(conexion, "SELECT COUNT(*) FROM Servicio"));
            assertEquals(0, contar(conexion, "SELECT COUNT(*) FROM Registro_demo_catalogo"));
        }
    }

    @Test
    void detectaHomonimoIncompatibleDeServicioAntesDeInsertarEventos() throws Exception {
        try (Connection conexion = baseConCuentas();
                Statement statement = conexion.createStatement()) {
            int flash = idPerfil(conexion, "Proveedor", "id_proveedor", "flash");
            statement.executeUpdate("""
                    INSERT INTO Servicio
                    (id_servicio, id_proveedor, nombre, descripcion, precio, categoria, estado,
                     tipo, cantidad_maxima_personas, ciudad_zona, anticipacion_minima)
                    VALUES (100, %d, 'Cabina de fotos', 'Servicio personal distinto',
                            100.00, 'FOTOGRAFIA', 'DISPONIBLE', 'Fotografía', 10, 'Cali', 1)
                    """.formatted(flash));

            assertThrows(SQLException.class,
                    () -> SincronizarCatalogoDemo.sincronizar(conexion));
            assertEquals(0, contar(conexion, "SELECT COUNT(*) FROM Evento"));
            assertEquals(1, contar(conexion, "SELECT COUNT(*) FROM Servicio"));
            assertEquals(0, contar(conexion, "SELECT COUNT(*) FROM Registro_demo_catalogo"));
        }
    }

    @Test
    void fallaSiUnaAsociacionDeEventoApuntaAEventoDeOtroOrganizador() throws Exception {
        try (Connection conexion = baseConCuentas();
                Statement statement = conexion.createStatement()) {
            int carlos = idPerfil(conexion, "Organizador", "id_organizador", "carlos.rodriguez");
            statement.executeUpdate("""
                    INSERT INTO Evento
                    (id_evento, id_organizador, nombre, fecha_inicio, fecha_fin,
                     ubicacion, estado, fecha_creacion)
                    VALUES (100, %d, 'Evento local', CURRENT_TIMESTAMP,
                            DATEADD('HOUR', 1, CURRENT_TIMESTAMP), 'Cali',
                            'PLANEADO', CURRENT_TIMESTAMP)
                    """.formatted(carlos));
            registrarAsociacion(conexion, "EVENTO", "boda-laura", 100);

            assertThrows(SQLException.class,
                    () -> SincronizarCatalogoDemo.sincronizar(conexion));
            assertEquals(1, contar(conexion, "SELECT COUNT(*) FROM Evento"));
            assertEquals(0, contar(conexion, "SELECT COUNT(*) FROM Servicio"));
            assertEquals(1, contar(conexion, "SELECT COUNT(*) FROM Registro_demo_catalogo"));
        }
    }

    @Test
    void fallaSiUnaAsociacionDeServicioApuntaAServicioDeOtroProveedor() throws Exception {
        try (Connection conexion = baseConCuentas();
                Statement statement = conexion.createStatement()) {
            int sabores = idPerfil(conexion, "Proveedor", "id_proveedor", "sabores.eventos");
            statement.executeUpdate("""
                    INSERT INTO Servicio
                    (id_servicio, id_proveedor, nombre, precio, estado)
                    VALUES (100, %d, 'Servicio local', 100.00, 'DISPONIBLE')
                    """.formatted(sabores));
            registrarAsociacion(conexion, "SERVICIO", "cobertura-fotografica", 100);

            assertThrows(SQLException.class,
                    () -> SincronizarCatalogoDemo.sincronizar(conexion));
            assertEquals(0, contar(conexion, "SELECT COUNT(*) FROM Evento"));
            assertEquals(1, contar(conexion, "SELECT COUNT(*) FROM Servicio"));
            assertEquals(1, contar(conexion, "SELECT COUNT(*) FROM Registro_demo_catalogo"));
        }
    }

    @Test
    void fallaSiUnaAsociacionDeCatalogoApuntaAUnRegistroEliminado() throws Exception {
        try (Connection conexion = baseConCuentas()) {
            registrarAsociacion(conexion, "EVENTO", "boda-laura", 900);

            assertThrows(SQLException.class,
                    () -> SincronizarCatalogoDemo.sincronizar(conexion));
            assertEquals(0, contar(conexion, "SELECT COUNT(*) FROM Evento"));
            assertEquals(0, contar(conexion, "SELECT COUNT(*) FROM Servicio"));
            assertEquals(1, contar(conexion, "SELECT COUNT(*) FROM Registro_demo_catalogo"));
        }
    }

    @Test
    void fallaSiUnaAsociacionSeReutilizoParaOtroEjemplo() throws Exception {
        try (Connection conexion = baseConCuentas();
                Statement statement = conexion.createStatement()) {
            String restriccion = null;
            try (ResultSet resultado = statement.executeQuery(
                    "SELECT CONSTRAINT_NAME FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS "
                    + "WHERE TABLE_SCHEMA = 'PUBLIC' "
                    + "AND TABLE_NAME = 'REGISTRO_DEMO_CATALOGO' "
                    + "AND CONSTRAINT_TYPE = 'UNIQUE'")) {
                assertTrue(resultado.next());
                restriccion = resultado.getString(1);
                assertFalse(resultado.next());
            }
            statement.execute("ALTER TABLE Registro_demo_catalogo DROP CONSTRAINT \""
                    + restriccion.replace("\"", "\"\"") + "\"");

            int laura = idPerfil(conexion, "Organizador", "id_organizador", "laura.gomez");
            statement.executeUpdate("""
                    INSERT INTO Evento
                    (id_evento, id_organizador, nombre, fecha_inicio, fecha_fin,
                     ubicacion, estado, fecha_creacion)
                    VALUES (100, %d, 'Evento local', CURRENT_TIMESTAMP,
                            DATEADD('HOUR', 1, CURRENT_TIMESTAMP), 'Cali',
                            'PLANEADO', CURRENT_TIMESTAMP)
                    """.formatted(laura));
            registrarAsociacion(conexion, "EVENTO", "boda-laura", 100);
            registrarAsociacion(conexion, "EVENTO", "baby-shower-valentina", 100);

            assertThrows(SQLException.class,
                    () -> SincronizarCatalogoDemo.sincronizar(conexion));
            assertEquals(1, contar(conexion, "SELECT COUNT(*) FROM Evento"));
            assertEquals(0, contar(conexion, "SELECT COUNT(*) FROM Servicio"));
            assertEquals(2, contar(conexion, "SELECT COUNT(*) FROM Registro_demo_catalogo"));
        }
    }

    @Test
    void aperturaParaSincronizarNoReparaContadoresAntesDeValidarConflictos()
            throws Exception {
        String url = nuevaBase();
        try (Connection conexion = DatabaseConnection.getConnection(url);
                Statement statement = conexion.createStatement()) {
            SincronizarCuentasDemo.sincronizar(conexion);
            int laura = idPerfil(conexion, "Organizador", "id_organizador", "laura.gomez");
            statement.executeUpdate("""
                    INSERT INTO Evento
                    (id_evento, id_organizador, nombre, descripcion, fecha_inicio, fecha_fin,
                     ubicacion, estado, fecha_creacion, tipo, presupuesto, invitados_iniciales)
                    VALUES (100, %d, 'Boda Laura y Daniel', 'Evento personal distinto',
                            CURRENT_TIMESTAMP, DATEADD('HOUR', 1, CURRENT_TIMESTAMP),
                            'Cali', 'PLANEADO', CURRENT_TIMESTAMP, 'Celebración', 1000, 5)
                    """.formatted(laura));
            statement.execute("ALTER TABLE Evento ALTER COLUMN id_evento RESTART WITH 1");
        }

        try (Connection conexion = DatabaseConnection.getConnectionParaSincronizarDemo(url)) {
            assertEquals(1, contadorIdentidad(conexion, "Evento", "id_evento"));
            assertThrows(SQLException.class,
                    () -> SincronizarCatalogoDemo.sincronizar(conexion));
            assertEquals(1, contadorIdentidad(conexion, "Evento", "id_evento"));
            assertEquals(1, contar(conexion, "SELECT COUNT(*) FROM Evento"));
            assertEquals(0, contar(conexion, "SELECT COUNT(*) FROM Servicio"));
        }
    }

    @Test
    void noAlteraTransaccionAbiertaSiElContadorEstaDesactualizado() throws Exception {
        try (Connection conexion = baseConCuentas();
                Statement statement = conexion.createStatement()) {
            int organizador = idPerfil(conexion, "Organizador", "id_organizador", "laura.gomez");
            statement.executeUpdate("""
                    INSERT INTO Evento
                    (id_evento, id_organizador, nombre, fecha_inicio, fecha_fin,
                     ubicacion, estado, fecha_creacion)
                    VALUES (100, %d, 'Evento local', CURRENT_TIMESTAMP,
                            DATEADD('HOUR', 1, CURRENT_TIMESTAMP), 'Cali',
                            'PLANEADO', CURRENT_TIMESTAMP)
                    """.formatted(organizador));
            conexion.setAutoCommit(false);
            statement.executeUpdate("""
                    INSERT INTO Evento
                    (id_evento, id_organizador, nombre, fecha_inicio, fecha_fin,
                     ubicacion, estado, fecha_creacion)
                    VALUES (101, %d, 'Evento no confirmado', CURRENT_TIMESTAMP,
                            DATEADD('HOUR', 1, CURRENT_TIMESTAMP), 'Cali',
                            'PLANEADO', CURRENT_TIMESTAMP)
                    """.formatted(organizador));

            assertThrows(SQLException.class,
                    () -> SincronizarCatalogoDemo.sincronizar(conexion));
            assertFalse(conexion.getAutoCommit());
            assertEquals(2, contar(conexion, "SELECT COUNT(*) FROM Evento"));
            assertEquals(0, contar(conexion, "SELECT COUNT(*) FROM Servicio"));
            assertEquals(0, contar(conexion, "SELECT COUNT(*) FROM Registro_demo_catalogo"));
            conexion.rollback();
            assertEquals(1, contar(conexion, "SELECT COUNT(*) FROM Evento"));
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
}
