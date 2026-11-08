package inicializacion;

import entities.contrataciones.DisponibilidadContratacion;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import persistencia.DatabaseConnection;
import repositories.solicitudes.RepositoryContratacionServicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SincronizarDisponibilidadDemoTest {

    private static final String TIPO = "DISPONIBILIDAD";
    private static final String CLAVE = "fotografia-quince-sofia-2027-02-20";

    private String url;
    private String urlAnterior;

    @BeforeEach
    void prepararBase() {
        urlAnterior = System.getProperty("nexteventux.db.url");
        url = "jdbc:h2:mem:disponibilidad_demo_" + UUID.randomUUID()
                + ";DB_CLOSE_DELAY=-1";
        System.setProperty("nexteventux.db.url", url);
    }

    @AfterEach
    void restaurarConfiguracion() {
        if (urlAnterior == null) {
            System.clearProperty("nexteventux.db.url");
        } else {
            System.setProperty("nexteventux.db.url", urlAnterior);
        }
    }

    @Test
    void reconoceHorarioDeBaseNuevaConSeedSinDuplicarNiAdoptarProcedencia()
            throws Exception {
        try (Connection conexion = DatabaseConnection.getConnection(url)) {
            CargarDatosEjemplo.cargar(conexion);
            SincronizarCuentasDemo.sincronizar(conexion);
            SincronizarCatalogoDemo.sincronizar(conexion);

            var resultado = SincronizarDisponibilidadDemo.sincronizar(conexion);

            assertFalse(resultado.creada());
            assertNull(resultado.advertencia());
            assertEquals(11, contar(conexion,
                    "SELECT COUNT(*) FROM Disponibilidad_Servicio"));
            assertEquals(0, contar(conexion,
                    "SELECT COUNT(*) FROM Registro_demo_catalogo WHERE tipo = '"
                    + TIPO + "'"));

            SincronizarDisponibilidadDemo.sincronizar(conexion);
            assertEquals(11, contar(conexion,
                    "SELECT COUNT(*) FROM Disponibilidad_Servicio"));
            assertEquals(0, contar(conexion,
                    "SELECT COUNT(*) FROM Registro_demo_catalogo WHERE tipo = '"
                    + TIPO + "'"));
        }
    }

    @Test
    void agregaHorarioEnBaseExistenteConIdsRealesYProcedencia() throws Exception {
        prepararCatalogoSinSeed();

        try (Connection conexion = DatabaseConnection.getConnection(url)) {
            var resultado = SincronizarDisponibilidadDemo.sincronizar(conexion);

            assertTrue(resultado.creada());
            assertNull(resultado.advertencia());
            assertEquals(1, contar(conexion,
                    "SELECT COUNT(*) FROM Disponibilidad_Servicio"));
            assertEquals(1, contar(conexion, """
                    SELECT COUNT(*)
                    FROM Disponibilidad_Servicio d
                    JOIN Registro_demo_catalogo r
                      ON r.id_registro = d.id_disponibilidad
                    JOIN Registro_demo_catalogo s
                      ON s.tipo = 'SERVICIO'
                     AND s.clave = 'cobertura-fotografica'
                    WHERE r.tipo = 'DISPONIBILIDAD'
                      AND r.clave = 'fotografia-quince-sofia-2027-02-20'
                      AND d.id_servicio = s.id_registro
                      AND d.fecha = DATE '2027-02-20'
                      AND d.hora_inicio = TIME '14:00:00'
                      AND d.hora_fin = TIME '23:59:00'
                      AND d.estado = 'DISPONIBLE'
                    """));
        }
    }

    @Test
    void conservaHorarioExactoSinProcedenciaYNoLoAdopta() throws Exception {
        prepararCatalogoSinSeed();

        try (Connection conexion = DatabaseConnection.getConnection(url)) {
            int idServicio = idServicioOficial(conexion);
            insertarDisponibilidad(conexion, null, idServicio,
                    Date.valueOf("2027-02-20"), "14:00:00", "23:59:00", "RESERVADA");

            var resultado = SincronizarDisponibilidadDemo.sincronizar(conexion);

            assertFalse(resultado.creada());
            assertNull(resultado.advertencia());
            assertEquals(1, contar(conexion,
                    "SELECT COUNT(*) FROM Disponibilidad_Servicio"));
            assertEquals(0, contar(conexion,
                    "SELECT COUNT(*) FROM Registro_demo_catalogo WHERE tipo = '"
                    + TIPO + "'"));
            assertEquals(1, contar(conexion, """
                    SELECT COUNT(*) FROM Disponibilidad_Servicio
                    WHERE hora_inicio = TIME '14:00:00'
                      AND hora_fin = TIME '23:59:00'
                      AND estado = 'RESERVADA'
                    """));
        }
    }

    @Test
    void detectaProcedenciaInesperadaEnHorarioExactoSinModificarlo()
            throws Exception {
        prepararCatalogoSinSeed();

        try (Connection conexion = DatabaseConnection.getConnection(url)) {
            int idServicio = idServicioOficial(conexion);
            insertarDisponibilidad(conexion, null, idServicio,
                    Date.valueOf("2027-02-20"), "14:00:00", "23:59:00", "RESERVADA");
            int idDisponibilidad = entero(conexion,
                    "SELECT id_disponibilidad FROM Disponibilidad_Servicio");
            ejecutar(conexion, """
                    INSERT INTO Registro_demo_catalogo (tipo, clave, id_registro)
                    VALUES ('DISPONIBILIDAD', 'clave-inesperada', %d)
                    """.formatted(idDisponibilidad));

            SQLException error = assertThrows(SQLException.class,
                    () -> SincronizarDisponibilidadDemo.sincronizar(conexion));

            assertTrue(error.getMessage().contains("procedencia inesperada"));
            assertEquals(1, contar(conexion,
                    "SELECT COUNT(*) FROM Disponibilidad_Servicio"));
            assertEquals(1, contar(conexion, """
                    SELECT COUNT(*) FROM Registro_demo_catalogo
                    WHERE tipo = 'DISPONIBILIDAD'
                      AND clave = 'clave-inesperada'
                    """));
            assertEquals("RESERVADA", texto(conexion,
                    "SELECT estado FROM Disponibilidad_Servicio "
                    + "WHERE id_disponibilidad = " + idDisponibilidad));
        }
    }

    @Test
    void informaSolapamientoSinCambiosYPermiteContinuarLaInicializacion()
            throws Exception {
        prepararCuentasYCatalogo();

        try (Connection conexion = DatabaseConnection.getConnection(url)) {
            insertarDisponibilidad(conexion, null, idServicioOficial(conexion),
                    Date.valueOf("2027-02-20"), "12:00:00", "18:00:00", "DISPONIBLE");

            var resultado = SincronizarDisponibilidadDemo.sincronizar(conexion);

            assertFalse(resultado.creada());
            assertNotNull(resultado.advertencia());
            assertTrue(resultado.advertencia().contains("solapa"));
            assertEquals(1, contar(conexion,
                    "SELECT COUNT(*) FROM Disponibilidad_Servicio"));
            assertEquals(0, contar(conexion,
                    "SELECT COUNT(*) FROM Registro_demo_catalogo WHERE tipo = '"
                    + TIPO + "'"));

            SincronizarHistoriaDemo.sincronizar(conexion);
            assertEquals(15, contar(conexion,
                    "SELECT COUNT(*) FROM Solicitud_Ejecucion"));
        }
    }

    @Test
    void reparaContadorSinReutilizarIdsYConservaDisponibilidadPersonal()
            throws Exception {
        prepararCatalogoSinSeed();

        try (Connection conexion = DatabaseConnection.getConnection(url)) {
            int idServicio = idServicioOficial(conexion);
            insertarDisponibilidad(conexion, 900, idServicio,
                    Date.valueOf("2027-02-21"), "09:00:00", "10:00:00", "DISPONIBLE");

            var resultado = SincronizarDisponibilidadDemo.sincronizar(conexion);

            assertTrue(resultado.creada());
            assertEquals(2, contar(conexion,
                    "SELECT COUNT(*) FROM Disponibilidad_Servicio"));
            assertEquals(1, contar(conexion, """
                    SELECT COUNT(*) FROM Disponibilidad_Servicio
                    WHERE id_disponibilidad = 900
                      AND fecha = DATE '2027-02-21'
                      AND hora_inicio = TIME '09:00:00'
                      AND hora_fin = TIME '10:00:00'
                    """));
            assertTrue(entero(conexion, """
                    SELECT id_registro FROM Registro_demo_catalogo
                    WHERE tipo = 'DISPONIBILIDAD'
                      AND clave = 'fotografia-quince-sofia-2027-02-20'
                    """) > 900);
        }
    }

    @Test
    void ejecucionRepetidaConProcedenciaValidaNoCambiaEstadoReservado()
            throws Exception {
        prepararCatalogoSinSeed();

        try (Connection conexion = DatabaseConnection.getConnection(url)) {
            SincronizarDisponibilidadDemo.sincronizar(conexion);
            int idDisponibilidad = entero(conexion, """
                    SELECT id_registro FROM Registro_demo_catalogo
                    WHERE tipo = 'DISPONIBILIDAD' AND clave = '%s'
                    """.formatted(CLAVE));
            ejecutar(conexion, "UPDATE Disponibilidad_Servicio "
                    + "SET estado = 'RESERVADA' WHERE id_disponibilidad = "
                    + idDisponibilidad);

            var resultado = SincronizarDisponibilidadDemo.sincronizar(conexion);

            assertFalse(resultado.creada());
            assertNull(resultado.advertencia());
            assertEquals(1, contar(conexion,
                    "SELECT COUNT(*) FROM Disponibilidad_Servicio"));
            assertEquals(1, contar(conexion,
                    "SELECT COUNT(*) FROM Registro_demo_catalogo "
                    + "WHERE tipo = 'DISPONIBILIDAD' AND clave = '" + CLAVE + "'"));
            assertEquals("RESERVADA", texto(conexion,
                    "SELECT estado FROM Disponibilidad_Servicio "
                    + "WHERE id_disponibilidad = " + idDisponibilidad));
        }
    }

    @Test
    void fallaSinCambiosSiLaProcedenciaApuntaAUnaDisponibilidadAlterada()
            throws Exception {
        prepararCatalogoSinSeed();

        try (Connection conexion = DatabaseConnection.getConnection(url)) {
            SincronizarDisponibilidadDemo.sincronizar(conexion);
            int idDisponibilidad = entero(conexion, """
                    SELECT id_registro FROM Registro_demo_catalogo
                    WHERE tipo = 'DISPONIBILIDAD' AND clave = '%s'
                    """.formatted(CLAVE));
            ejecutar(conexion, "UPDATE Disponibilidad_Servicio "
                    + "SET hora_inicio = TIME '15:00:00' "
                    + "WHERE id_disponibilidad = " + idDisponibilidad);

            SQLException error = assertThrows(SQLException.class,
                    () -> SincronizarDisponibilidadDemo.sincronizar(conexion));

            assertTrue(error.getMessage().contains("procedencia"));
            assertEquals(1, contar(conexion,
                    "SELECT COUNT(*) FROM Disponibilidad_Servicio"));
            assertEquals(1, contar(conexion,
                    "SELECT COUNT(*) FROM Registro_demo_catalogo "
                    + "WHERE tipo = 'DISPONIBILIDAD' AND clave = '" + CLAVE + "'"));
            assertEquals("15:00:00", texto(conexion,
                    "SELECT CAST(hora_inicio AS VARCHAR) "
                    + "FROM Disponibilidad_Servicio WHERE id_disponibilidad = "
                    + idDisponibilidad));
        }
    }

    @Test
    void fallaSiLaProcedenciaQuedoHuerfana() throws Exception {
        prepararCatalogoSinSeed();

        try (Connection conexion = DatabaseConnection.getConnection(url)) {
            SincronizarDisponibilidadDemo.sincronizar(conexion);
            int idDisponibilidad = entero(conexion, """
                    SELECT id_registro FROM Registro_demo_catalogo
                    WHERE tipo = 'DISPONIBILIDAD' AND clave = '%s'
                    """.formatted(CLAVE));
            ejecutar(conexion, "DELETE FROM Disponibilidad_Servicio "
                    + "WHERE id_disponibilidad = " + idDisponibilidad);

            SQLException error = assertThrows(SQLException.class,
                    () -> SincronizarDisponibilidadDemo.sincronizar(conexion));

            assertTrue(error.getMessage().contains("huérfana"));
            assertEquals(0, contar(conexion,
                    "SELECT COUNT(*) FROM Disponibilidad_Servicio"));
            assertEquals(1, contar(conexion,
                    "SELECT COUNT(*) FROM Registro_demo_catalogo "
                    + "WHERE tipo = 'DISPONIBILIDAD' AND clave = '" + CLAVE + "'"));
        }
    }

    @Test
    void rollbackEvitaDisponibilidadSinProcedenciaSiFallaElRegistro()
            throws Exception {
        prepararCatalogoSinSeed();

        try (Connection conexion = DatabaseConnection.getConnection(url);
                Statement sentencia = conexion.createStatement()) {
            sentencia.execute("""
                    ALTER TABLE Registro_demo_catalogo
                    ADD CONSTRAINT no_disponibilidad_demo
                    CHECK (tipo <> 'DISPONIBILIDAD')
                    """);

            assertThrows(SQLException.class,
                    () -> SincronizarDisponibilidadDemo.sincronizar(conexion));

            assertEquals(0, contar(conexion,
                    "SELECT COUNT(*) FROM Disponibilidad_Servicio"));
            assertEquals(0, contar(conexion,
                    "SELECT COUNT(*) FROM Registro_demo_catalogo "
                    + "WHERE tipo = 'DISPONIBILIDAD'"));
        }
    }

    @Test
    void horarioSincronizadoPermiteLaValidacionDeCalendarioSinOmitirOtrasReglas()
            throws Exception {
        prepararCatalogoSinSeed();
        int idOrganizador;
        int idPropuesta;

        try (Connection conexion = DatabaseConnection.getConnection(url)) {
            SincronizarHistoriaDemo.sincronizar(conexion);
            idOrganizador = idOrganizadorOficial(conexion);
            idPropuesta = idRegistro(conexion, "PROPUESTA", "propuesta-6");
            int contratacionesAntes = contar(conexion,
                    "SELECT COUNT(*) FROM Contratacion");

            RepositoryContratacionServicio repositorio
                    = new RepositoryContratacionServicio();
            DisponibilidadContratacion antes
                    = repositorio.verificarDisponibilidad(idOrganizador, idPropuesta);
            assertFalse(antes.disponible());

            SincronizarDisponibilidadDemo.sincronizar(conexion);

            DisponibilidadContratacion despues
                    = repositorio.verificarDisponibilidad(idOrganizador, idPropuesta);
            assertTrue(despues.disponible(), despues.mensaje());
            assertEquals(contratacionesAntes, contar(conexion,
                    "SELECT COUNT(*) FROM Contratacion"));

            ejecutar(conexion, "UPDATE Servicio SET estado = 'RETIRADO' "
                    + "WHERE id_servicio = "
                    + idServicioOficial(conexion));
            IllegalStateException error = assertThrows(IllegalStateException.class,
                    () -> repositorio.verificarDisponibilidad(idOrganizador, idPropuesta));
            assertTrue(error.getMessage().contains("servicio ya no está disponible"));
            assertEquals(contratacionesAntes, contar(conexion,
                    "SELECT COUNT(*) FROM Contratacion"));
        }
    }

    private void prepararCatalogoSinSeed() throws Exception {
        prepararCuentasYCatalogo();
        try (Connection conexion = DatabaseConnection.getConnection(url)) {
            SincronizarHistoriaDemo.sincronizar(conexion);
        }
    }

    private void prepararCuentasYCatalogo() throws Exception {
        try (Connection conexion = DatabaseConnection.getConnection(url)) {
            SincronizarCuentasDemo.sincronizar(conexion);
            SincronizarCatalogoDemo.sincronizar(conexion);
        }
    }

    private int idServicioOficial(Connection conexion) throws SQLException {
        return entero(conexion, """
                SELECT id_registro FROM Registro_demo_catalogo
                WHERE tipo = 'SERVICIO' AND clave = 'cobertura-fotografica'
                """);
    }

    private int idOrganizadorOficial(Connection conexion) throws SQLException {
        return entero(conexion, """
                SELECT o.id_organizador
                FROM Organizador o
                JOIN Usuario u ON u.id_usuario = o.id_usuario
                WHERE u.usuario = 'valentina'
                """);
    }

    private int idRegistro(Connection conexion, String tipo, String clave)
            throws SQLException {
        try (PreparedStatement consulta = conexion.prepareStatement("""
                SELECT id_registro FROM Registro_demo_catalogo
                WHERE tipo = ? AND clave = ?
                """)) {
            consulta.setString(1, tipo);
            consulta.setString(2, clave);
            try (ResultSet resultado = consulta.executeQuery()) {
                assertTrue(resultado.next());
                return resultado.getInt(1);
            }
        }
    }

    private void insertarDisponibilidad(
            Connection conexion,
            Integer id,
            int idServicio,
            Date fecha,
            String inicio,
            String fin,
            String estado) throws SQLException {
        String columnas = id == null
                ? "(id_servicio, fecha, hora_inicio, hora_fin, estado)"
                : "(id_disponibilidad, id_servicio, fecha, hora_inicio, hora_fin, estado)";
        String valores = id == null ? "(?, ?, ?, ?, ?)" : "(?, ?, ?, ?, ?, ?)";
        try (PreparedStatement insertar = conexion.prepareStatement(
                "INSERT INTO Disponibilidad_Servicio " + columnas + " VALUES " + valores)) {
            int indice = 1;
            if (id != null) {
                insertar.setInt(indice++, id);
            }
            insertar.setInt(indice++, idServicio);
            insertar.setDate(indice++, fecha);
            insertar.setTime(indice++, java.sql.Time.valueOf(inicio));
            insertar.setTime(indice++, java.sql.Time.valueOf(fin));
            insertar.setString(indice, estado);
            insertar.executeUpdate();
        }
    }

    private int contar(Connection conexion, String sql) throws SQLException {
        return entero(conexion, sql);
    }

    private int entero(Connection conexion, String sql) throws SQLException {
        try (Statement sentencia = conexion.createStatement();
                ResultSet resultado = sentencia.executeQuery(sql)) {
            assertTrue(resultado.next());
            return resultado.getInt(1);
        }
    }

    private String texto(Connection conexion, String sql) throws SQLException {
        try (Statement sentencia = conexion.createStatement();
                ResultSet resultado = sentencia.executeQuery(sql)) {
            assertTrue(resultado.next());
            return resultado.getString(1);
        }
    }

    private void ejecutar(Connection conexion, String sql) throws SQLException {
        try (Statement sentencia = conexion.createStatement()) {
            sentencia.executeUpdate(sql);
        }
    }
}
