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

class SincronizarHistoriaDemoTest {

    private String nuevaBase() {
        return "jdbc:h2:mem:historia_" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1";
    }

    private Connection baseConCatalogo() throws Exception {
        Connection conexion = DatabaseConnection.getConnection(nuevaBase());
        SincronizarCuentasDemo.sincronizar(conexion);
        SincronizarCatalogoDemo.sincronizar(conexion);
        return conexion;
    }

    private Connection baseConSeed() throws Exception {
        Connection conexion = DatabaseConnection.getConnection(nuevaBase());
        CargarDatosEjemplo.cargar(conexion);
        SincronizarCuentasDemo.sincronizar(conexion);
        SincronizarCatalogoDemo.sincronizar(conexion);
        return conexion;
    }

    private int contar(Connection conexion, String sql) throws SQLException {
        try (Statement statement = conexion.createStatement();
                ResultSet resultado = statement.executeQuery(sql)) {
            assertTrue(resultado.next());
            return resultado.getInt(1);
        }
    }

    private int idCatalogo(Connection conexion, String tipo, String clave) throws SQLException {
        try (PreparedStatement consulta = conexion.prepareStatement(
                "SELECT id_registro FROM Registro_demo_catalogo WHERE tipo = ? AND clave = ?")) {
            consulta.setString(1, tipo);
            consulta.setString(2, clave);
            try (ResultSet resultado = consulta.executeQuery()) {
                assertTrue(resultado.next());
                return resultado.getInt(1);
            }
        }
    }

    private long identityBase(Connection conexion, String tabla, String columna)
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
    void baseNuevaReconoceLaHistoriaDelSeedSinDuplicar() throws Exception {
        try (Connection conexion = baseConSeed()) {
            SincronizarHistoriaDemo.Resultado resultado
                    = SincronizarHistoriaDemo.sincronizar(conexion);

            assertEquals(new SincronizarHistoriaDemo.Resultado(0, 0, 0, 0), resultado);
            assertEquals(8, contar(conexion,
                    "SELECT COUNT(*) FROM Necesidad_Servicio_Evento"));
            assertEquals(15, contar(conexion, "SELECT COUNT(*) FROM Solicitud_Ejecucion"));
            assertEquals(10, contar(conexion, "SELECT COUNT(*) FROM Propuesta"));
            assertEquals(2, contar(conexion, "SELECT COUNT(*) FROM Contratacion"));
            assertEquals(15, contar(conexion, """
                    SELECT COUNT(*) FROM Solicitud_Ejecucion s
                    JOIN Servicio v ON v.id_servicio = s.id_servicio
                    WHERE s.id_proveedor = v.id_proveedor
                    """));
            assertEquals(13, contar(conexion, """
                    SELECT COUNT(*) FROM Solicitud_Ejecucion
                    WHERE costo_comprometido IS NOT NULL
                    """));
            assertEquals(8, contar(conexion,
                    "SELECT COUNT(*) FROM Registro_demo_catalogo WHERE tipo = 'NECESIDAD'"));
            assertEquals(15, contar(conexion,
                    "SELECT COUNT(*) FROM Registro_demo_catalogo WHERE tipo = 'SOLICITUD'"));
            assertEquals(10, contar(conexion,
                    "SELECT COUNT(*) FROM Registro_demo_catalogo WHERE tipo = 'PROPUESTA'"));
            assertEquals(2, contar(conexion,
                    "SELECT COUNT(*) FROM Registro_demo_catalogo WHERE tipo = 'CONTRATACION'"));
            assertEquals(1, contar(conexion, """
                    SELECT COUNT(*)
                    FROM Necesidad_Servicio_Evento n
                    JOIN Propuesta p ON p.id_propuesta = n.id_propuesta_seleccionada
                    JOIN Solicitud_Ejecucion s ON s.id_solicitud = p.id_solicitud
                    WHERE n.id_necesidad_servicio = 3
                      AND p.id_propuesta = 6 AND p.numero_version = 2
                      AND s.id_necesidad_servicio = n.id_necesidad_servicio
                    """));
            assertEquals(2, contar(conexion, """
                    SELECT COUNT(*) FROM Solicitud_Ejecucion
                    WHERE id_evento = 1 AND estado = 'CONTRATADA'
                    """));
            assertEquals(2, contar(conexion, """
                    SELECT COUNT(*) FROM Contratacion c
                    JOIN Propuesta p ON p.id_propuesta = c.id_propuesta
                    JOIN Solicitud_Ejecucion s ON s.id_solicitud = p.id_solicitud
                    WHERE c.id_evento = 1 AND c.estado = 'ACTIVA'
                      AND s.id_evento = c.id_evento
                      AND c.numero_version_propuesta = p.numero_version
                      AND c.alcance_propuesta = p.descripcion
                    """));

            resultado = SincronizarHistoriaDemo.sincronizar(conexion);
            assertEquals(new SincronizarHistoriaDemo.Resultado(0, 0, 0, 0), resultado);
            assertEquals(8, contar(conexion,
                    "SELECT COUNT(*) FROM Necesidad_Servicio_Evento"));
            assertEquals(15, contar(conexion, "SELECT COUNT(*) FROM Solicitud_Ejecucion"));
            assertEquals(10, contar(conexion, "SELECT COUNT(*) FROM Propuesta"));
            assertEquals(2, contar(conexion, "SELECT COUNT(*) FROM Contratacion"));
            assertEquals(4, contar(conexion, "SELECT COUNT(*) FROM Movimiento_financiero"));
        }
    }

    @Test
    void baseExistenteAgregaHistoriaFaltanteYConservaIdsGenerados() throws Exception {
        try (Connection conexion = baseConCatalogo()) {
            assertEquals(new SincronizarHistoriaDemo.Resultado(8, 15, 10, 2),
                    SincronizarHistoriaDemo.sincronizar(conexion));
            assertEquals(8, contar(conexion,
                    "SELECT COUNT(*) FROM Necesidad_Servicio_Evento"));
            assertEquals(15, contar(conexion, "SELECT COUNT(*) FROM Solicitud_Ejecucion"));
            assertEquals(10, contar(conexion, "SELECT COUNT(*) FROM Propuesta"));
            assertEquals(2, contar(conexion, "SELECT COUNT(*) FROM Contratacion"));
            assertEquals(0, contar(conexion, "SELECT COUNT(*) FROM Movimiento_financiero"));

            int organizadorCarlos = contar(conexion, """
                    SELECT COUNT(*)
                    FROM Evento e
                    JOIN Organizador o ON o.id_organizador = e.id_organizador
                    JOIN Usuario u ON u.id_usuario = o.id_usuario
                    WHERE e.id_evento = 2 AND u.usuario = 'carlos.rodriguez'
                    """);
            assertEquals(1, organizadorCarlos);

            int idFlash = idCatalogo(conexion, "SERVICIO", "cobertura-fotografica");
            int idProveedorFlash = contar(conexion, """
                    SELECT p.id_proveedor FROM Proveedor p
                    JOIN Usuario u ON u.id_usuario = p.id_usuario
                    WHERE u.usuario = 'flash'
                    """);
            assertEquals(1, contar(conexion, """
                    SELECT COUNT(*) FROM Solicitud_Ejecucion
                    WHERE id_servicio = %d AND id_proveedor = %d
                      AND id_necesidad_servicio = %d
                    """.formatted(idFlash, idProveedorFlash,
                    idCatalogo(conexion, "NECESIDAD", "necesidad-3"))));
            assertEquals(new SincronizarHistoriaDemo.Resultado(0, 0, 0, 0),
                    SincronizarHistoriaDemo.sincronizar(conexion));
        }
    }

    @Test
    void conservaEdicionesDeSolicitudesPropuestasNecesidadesYContrataciones()
            throws Exception {
        try (Connection conexion = baseConSeed()) {
            SincronizarHistoriaDemo.sincronizar(conexion);
            try (Statement statement = conexion.createStatement()) {
                statement.executeUpdate("""
                        UPDATE Solicitud_Ejecucion
                        SET estado = 'PENDIENTE', costo_comprometido = 777777.00,
                            fecha_solicitud = TIMESTAMP '2028-01-01 10:00:00'
                        WHERE id_solicitud = 6
                        """);
                statement.executeUpdate("""
                        UPDATE Propuesta
                        SET valor = 1234567.00, estado = 'ENVIADA',
                            fecha_envio = TIMESTAMP '2028-01-02 10:00:00'
                        WHERE id_propuesta = 6
                        """);
                statement.executeUpdate("""
                        UPDATE Contratacion
                        SET valor_acordado = 1234567.00,
                            fecha_contratacion = TIMESTAMP '2028-01-03 10:00:00'
                        WHERE id_contratacion = 1
                        """);
                statement.executeUpdate("""
                        UPDATE Necesidad_Servicio_Evento
                        SET descripcion = 'Fotografía editada'
                        WHERE id_necesidad_servicio = 3
                        """);
            }

            assertEquals(new SincronizarHistoriaDemo.Resultado(0, 0, 0, 0),
                    SincronizarHistoriaDemo.sincronizar(conexion));
            assertEquals(1, contar(conexion, """
                    SELECT COUNT(*) FROM Solicitud_Ejecucion
                    WHERE id_solicitud = 6 AND estado = 'PENDIENTE'
                      AND costo_comprometido = 777777.00
                      AND fecha_solicitud = TIMESTAMP '2028-01-01 10:00:00'
                    """));
            assertEquals(1, contar(conexion, """
                    SELECT COUNT(*) FROM Propuesta
                    WHERE id_propuesta = 6 AND valor = 1234567.00
                      AND estado = 'ENVIADA'
                      AND fecha_envio = TIMESTAMP '2028-01-02 10:00:00'
                    """));
            assertEquals(1, contar(conexion, """
                    SELECT COUNT(*) FROM Contratacion
                    WHERE id_contratacion = 1 AND valor_acordado = 1234567.00
                      AND fecha_contratacion = TIMESTAMP '2028-01-03 10:00:00'
                    """));
            assertEquals(1, contar(conexion, """
                    SELECT COUNT(*) FROM Necesidad_Servicio_Evento
                    WHERE id_necesidad_servicio = 3
                      AND descripcion = 'Fotografía editada'
                    """));
        }
    }

    @Test
    void IDsPersonalesOcupadosNoSeReutilizanNiSeSobrescriben() throws Exception {
        try (Connection conexion = baseConCatalogo();
                Statement statement = conexion.createStatement()) {
            int eventId = idCatalogo(conexion, "EVENTO", "boda-laura");
            int serviceId = idCatalogo(conexion, "SERVICIO", "decoracion-floral");
            int providerId = contar(conexion, """
                    SELECT p.id_proveedor FROM Proveedor p
                    JOIN Usuario u ON u.id_usuario = p.id_usuario
                    WHERE u.usuario = 'decoraciones.arcoiris'
                    """);
            statement.executeUpdate("""
                    INSERT INTO Necesidad_Servicio_Evento
                    (id_necesidad_servicio, id_evento, descripcion, estado)
                    VALUES (100, %d, 'Necesidad personal', 'ABIERTA')
                    """.formatted(eventId));
            statement.executeUpdate("""
                    INSERT INTO Solicitud_Ejecucion
                    (id_solicitud, id_proveedor, id_evento, id_servicio,
                     id_necesidad_servicio, fecha_solicitud, estado)
                    VALUES (100, %d, %d, %d, 100,
                            TIMESTAMP '2025-01-01 10:00:00', 'PENDIENTE')
                    """.formatted(providerId, eventId, serviceId));
            statement.executeUpdate("""
                    INSERT INTO Propuesta
                    (id_propuesta, id_solicitud, valor, descripcion, estado,
                     numero_version, condiciones, fecha_envio, vigente)
                    VALUES (100, 100, 10.00, 'Propuesta personal', 'ENVIADA',
                            1, 'Condiciones personales',
                            TIMESTAMP '2025-01-02 10:00:00', TRUE)
                    """);
            statement.executeUpdate("""
                    INSERT INTO Contratacion
                    (id_contratacion, id_propuesta, id_evento, fecha_contratacion,
                     estado, condiciones, valor_acordado)
                    VALUES (100, 100, %d, TIMESTAMP '2025-01-03 10:00:00',
                            'ACTIVA', 'Contrato personal', 10.00)
                    """.formatted(eventId));
            statement.execute("ALTER TABLE Necesidad_Servicio_Evento "
                    + "ALTER COLUMN id_necesidad_servicio RESTART WITH 1");
            statement.execute("ALTER TABLE Solicitud_Ejecucion "
                    + "ALTER COLUMN id_solicitud RESTART WITH 1");
            statement.execute("ALTER TABLE Propuesta "
                    + "ALTER COLUMN id_propuesta RESTART WITH 1");
            statement.execute("ALTER TABLE Contratacion "
                    + "ALTER COLUMN id_contratacion RESTART WITH 1");

            SincronizarHistoriaDemo.sincronizar(conexion);
            assertTrue(contar(conexion, """
                    SELECT MIN(id_necesidad_servicio) FROM Necesidad_Servicio_Evento
                    WHERE id_necesidad_servicio <> 100
                    """) > 100);
            assertTrue(contar(conexion,
                    "SELECT MIN(id_solicitud) FROM Solicitud_Ejecucion "
                    + "WHERE id_solicitud <> 100") > 100);
            assertTrue(contar(conexion,
                    "SELECT MIN(id_propuesta) FROM Propuesta WHERE id_propuesta <> 100") > 100);
            assertTrue(contar(conexion,
                    "SELECT MIN(id_contratacion) FROM Contratacion "
                    + "WHERE id_contratacion <> 100") > 100);
            assertEquals(1, contar(conexion, """
                    SELECT COUNT(*) FROM Necesidad_Servicio_Evento
                    WHERE id_necesidad_servicio = 100
                      AND descripcion = 'Necesidad personal'
                    """));
            assertEquals(1, contar(conexion, """
                    SELECT COUNT(*) FROM Contratacion
                    WHERE id_contratacion = 100 AND valor_acordado = 10.00
                    """));
        }
    }

    @Test
    void fallaAnteNecesidadesDuplicadasSinCrearHistoriaParcial() throws Exception {
        try (Connection conexion = baseConCatalogo();
                Statement statement = conexion.createStatement()) {
            int eventId = idCatalogo(conexion, "EVENTO", "quince-sofia");
            statement.executeUpdate("""
                    INSERT INTO Necesidad_Servicio_Evento
                    (id_evento, descripcion, estado)
                    VALUES (%d, 'Decoración temática de unicornios', 'ABIERTA')
                    """.formatted(eventId));
            statement.executeUpdate("""
                    INSERT INTO Necesidad_Servicio_Evento
                    (id_evento, descripcion, estado)
                    VALUES (%d, 'Decoración temática de unicornios', 'ABIERTA')
                    """.formatted(eventId));

            assertThrows(SQLException.class,
                    () -> SincronizarHistoriaDemo.sincronizar(conexion));
            assertEquals(2, contar(conexion, """
                    SELECT COUNT(*) FROM Necesidad_Servicio_Evento
                    WHERE descripcion = 'Decoración temática de unicornios'
                    """));
            assertEquals(0, contar(conexion, "SELECT COUNT(*) FROM Solicitud_Ejecucion"));
            assertEquals(19, contar(conexion, "SELECT COUNT(*) FROM Registro_demo_catalogo"));
        }
    }

    @Test
    void noAdoptaUnaNecesidadPorDescripcionSinEvidenciaDeSusRelaciones()
            throws Exception {
        try (Connection conexion = baseConCatalogo();
                Statement statement = conexion.createStatement()) {
            int eventId = idCatalogo(conexion, "EVENTO", "quince-sofia");
            statement.executeUpdate("""
                    INSERT INTO Necesidad_Servicio_Evento
                    (id_evento, descripcion, estado)
                    VALUES (%d, 'Decoración temática de unicornios', 'ABIERTA')
                    """.formatted(eventId));

            assertThrows(SQLException.class,
                    () -> SincronizarHistoriaDemo.sincronizar(conexion));
            assertEquals(1, contar(conexion,
                    "SELECT COUNT(*) FROM Necesidad_Servicio_Evento"));
            assertEquals(0, contar(conexion, "SELECT COUNT(*) FROM Solicitud_Ejecucion"));
            assertEquals(19, contar(conexion, "SELECT COUNT(*) FROM Registro_demo_catalogo"));
        }
    }

    @Test
    void fallaAnteSolicitudesDuplicadasConLaMismaIdentidad() throws Exception {
        try (Connection conexion = baseConCatalogo();
                Statement statement = conexion.createStatement()) {
            int eventId = idCatalogo(conexion, "EVENTO", "boda-laura");
            int serviceId = idCatalogo(conexion, "SERVICIO", "decoracion-floral");
            int providerId = contar(conexion, """
                    SELECT p.id_proveedor FROM Proveedor p
                    JOIN Usuario u ON u.id_usuario = p.id_usuario
                    WHERE u.usuario = 'decoraciones.arcoiris'
                    """);
            statement.executeUpdate("""
                    INSERT INTO Solicitud_Ejecucion
                    (id_proveedor, id_evento, id_servicio, fecha_solicitud, estado)
                    VALUES (%d, %d, %d, TIMESTAMP '2026-10-08 10:00:00', 'PENDIENTE')
                    """.formatted(providerId, eventId, serviceId));
            statement.executeUpdate("""
                    INSERT INTO Solicitud_Ejecucion
                    (id_proveedor, id_evento, id_servicio, fecha_solicitud, estado)
                    VALUES (%d, %d, %d, TIMESTAMP '2026-10-08 10:00:00', 'PENDIENTE')
                    """.formatted(providerId, eventId, serviceId));

            assertThrows(SQLException.class,
                    () -> SincronizarHistoriaDemo.sincronizar(conexion));
            assertEquals(2, contar(conexion,
                    "SELECT COUNT(*) FROM Solicitud_Ejecucion"));
            assertEquals(0, contar(conexion,
                    "SELECT COUNT(*) FROM Necesidad_Servicio_Evento"));
            assertEquals(19, contar(conexion,
                    "SELECT COUNT(*) FROM Registro_demo_catalogo"));
        }
    }

    @Test
    void conflictosDePropietarioDeEventoOServicioFallanAntesDeInsertar()
            throws Exception {
        try (Connection conexion = baseConCatalogo();
                Statement statement = conexion.createStatement()) {
            int idQuince = idCatalogo(conexion, "EVENTO", "quince-sofia");
            int organizadorLaura = contar(conexion, """
                    SELECT o.id_organizador FROM Organizador o
                    JOIN Usuario u ON u.id_usuario = o.id_usuario
                    WHERE u.usuario = 'laura.gomez'
                    """);
            statement.executeUpdate("UPDATE Evento SET id_organizador = "
                    + organizadorLaura + " WHERE id_evento = " + idQuince);

            assertThrows(SQLException.class,
                    () -> SincronizarHistoriaDemo.sincronizar(conexion));
            assertEquals(0, contar(conexion, "SELECT COUNT(*) FROM Necesidad_Servicio_Evento"));
        }

        try (Connection conexion = baseConCatalogo();
                Statement statement = conexion.createStatement()) {
            int idServicio = idCatalogo(conexion, "SERVICIO", "cobertura-fotografica");
            int proveedorSabores = contar(conexion, """
                    SELECT p.id_proveedor FROM Proveedor p
                    JOIN Usuario u ON u.id_usuario = p.id_usuario
                    WHERE u.usuario = 'sabores.eventos'
                    """);
            statement.executeUpdate("UPDATE Servicio SET id_proveedor = "
                    + proveedorSabores + " WHERE id_servicio = " + idServicio);

            assertThrows(SQLException.class,
                    () -> SincronizarHistoriaDemo.sincronizar(conexion));
            assertEquals(0, contar(conexion, "SELECT COUNT(*) FROM Necesidad_Servicio_Evento"));
        }
    }

    @Test
    void asociacionARegistroEliminadoFallaSinInsercionesParciales() throws Exception {
        try (Connection conexion = baseConCatalogo();
                PreparedStatement asociar = conexion.prepareStatement(
                        "INSERT INTO Registro_demo_catalogo (tipo, clave, id_registro) "
                        + "VALUES ('SOLICITUD', 'solicitud-1', 999)")) {
            asociar.executeUpdate();
            assertThrows(SQLException.class,
                    () -> SincronizarHistoriaDemo.sincronizar(conexion));
            assertEquals(0, contar(conexion, "SELECT COUNT(*) FROM Necesidad_Servicio_Evento"));
            assertEquals(19, contar(conexion, """
                    SELECT COUNT(*) FROM Registro_demo_catalogo WHERE tipo <> 'SOLICITUD'
                    """));
        }
    }

    @Test
    void detectaEliminacionDeUnaContratacionYaVinculada() throws Exception {
        try (Connection conexion = baseConCatalogo();
                Statement statement = conexion.createStatement()) {
            SincronizarHistoriaDemo.sincronizar(conexion);
            int idContratacion = idCatalogo(conexion, "CONTRATACION", "contratacion-1");
            statement.executeUpdate(
                    "DELETE FROM Contratacion WHERE id_contratacion = " + idContratacion);

            assertThrows(SQLException.class,
                    () -> SincronizarHistoriaDemo.sincronizar(conexion));
            assertEquals(8, contar(conexion,
                    "SELECT COUNT(*) FROM Necesidad_Servicio_Evento"));
            assertEquals(15, contar(conexion, "SELECT COUNT(*) FROM Solicitud_Ejecucion"));
            assertEquals(10, contar(conexion, "SELECT COUNT(*) FROM Propuesta"));
            assertEquals(1, contar(conexion, "SELECT COUNT(*) FROM Contratacion"));
        }
    }

    @Test
    void conflictoSeDetectaAntesDeRepararContadores() throws Exception {
        try (Connection conexion = baseConCatalogo();
                Statement statement = conexion.createStatement()) {
            statement.executeUpdate("""
                    INSERT INTO Registro_demo_catalogo (tipo, clave, id_registro)
                    VALUES ('SOLICITUD', 'solicitud-1', 999)
                    """);
            statement.execute("ALTER TABLE Necesidad_Servicio_Evento "
                    + "ALTER COLUMN id_necesidad_servicio RESTART WITH 1");
            assertEquals(1, identityBase(
                    conexion, "NECESIDAD_SERVICIO_EVENTO", "ID_NECESIDAD_SERVICIO"));

            assertThrows(SQLException.class,
                    () -> SincronizarHistoriaDemo.sincronizar(conexion));
            assertEquals(1, identityBase(
                    conexion, "NECESIDAD_SERVICIO_EVENTO", "ID_NECESIDAD_SERVICIO"));
            assertEquals(0, contar(conexion, "SELECT COUNT(*) FROM Necesidad_Servicio_Evento"));
        }
    }

    @Test
    void transaccionAbiertaConSecuenciaDesactualizadaNoSeModifica()
            throws Exception {
        try (Connection conexion = baseConCatalogo();
                Statement statement = conexion.createStatement()) {
            int eventId = idCatalogo(conexion, "EVENTO", "boda-laura");
            statement.executeUpdate("""
                    INSERT INTO Necesidad_Servicio_Evento
                    (id_necesidad_servicio, id_evento, descripcion, estado)
                    VALUES (100, %d, 'Necesidad personal', 'ABIERTA')
                    """.formatted(eventId));
            statement.execute("ALTER TABLE Necesidad_Servicio_Evento "
                    + "ALTER COLUMN id_necesidad_servicio RESTART WITH 1");
            conexion.setAutoCommit(false);
            statement.executeUpdate(
                    "UPDATE Evento SET presupuesto = 7654321 WHERE id_evento = " + eventId);

            assertThrows(SQLException.class,
                    () -> SincronizarHistoriaDemo.sincronizar(conexion));
            assertFalse(conexion.getAutoCommit());
            assertEquals(7654321,
                    contar(conexion, "SELECT CAST(presupuesto AS INT) FROM Evento "
                            + "WHERE id_evento = " + eventId));
            assertEquals(0, contar(conexion, "SELECT COUNT(*) FROM Solicitud_Ejecucion"));
            conexion.rollback();
        }
    }

    @Test
    void errorDuranteLaInsercionRevierteTodasLasFilasYProcedencias()
            throws Exception {
        try (Connection conexion = baseConCatalogo();
                Statement statement = conexion.createStatement()) {
            statement.execute("CREATE UNIQUE INDEX uq_prueba_historia "
                    + "ON Solicitud_Ejecucion(id_evento, id_proveedor)");

            assertThrows(SQLException.class,
                    () -> SincronizarHistoriaDemo.sincronizar(conexion));
            assertEquals(0, contar(conexion, "SELECT COUNT(*) FROM Necesidad_Servicio_Evento"));
            assertEquals(0, contar(conexion, "SELECT COUNT(*) FROM Solicitud_Ejecucion"));
            assertEquals(0, contar(conexion, "SELECT COUNT(*) FROM Propuesta"));
            assertEquals(0, contar(conexion, "SELECT COUNT(*) FROM Contratacion"));
            assertEquals(19, contar(conexion, "SELECT COUNT(*) FROM Registro_demo_catalogo"));
            assertTrue(conexion.getAutoCommit());
        }
    }
}
