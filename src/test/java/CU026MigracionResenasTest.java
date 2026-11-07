import config.CargarDatosEjemplo;
import config.DatabaseConnection;
import controllers.ControllerAdministracion;
import entities.Organizador;
import entities.SesionUsuario;
import org.h2.tools.RunScript;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import repositories.RepositoryAutenticacion;
import repositories.RepositoryContratacion;
import repositories.RepositoryOrganizador;
import services.ServiceAdministracion;
import services.ServiceAutenticacion;

import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Migración 015 de CU-026: retira solo las reseñas de demostración que
 * crearon la migración 011 y las versiones de seed.sql, identificadas por
 * su firma exacta, sin tocar reseñas legítimas ni otros datos.
 *
 * La base parte del seed.sql real del proyecto. Las firmas de seed antiguas
 * se reproducen tal como estaban en el historial de git.
 */
public class CU026MigracionResenasTest {

    private static final String MIGRACION = "015_cu26_retirar_resenas_demostracion.sql";

    private String url;

    @BeforeEach
    void preparar() throws Exception {
        url = "jdbc:h2:mem:cu26_migracion_" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1";

        try (Connection connection = DatabaseConnection.getConnection(url)) {
            CargarDatosEjemplo.cargar(connection);
        }
    }

    @AfterEach
    void limpiar() throws Exception {
        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement()) {
            statement.execute("DROP ALL OBJECTS");
        }
        DatabaseConnection.reiniciarInicializacion();
    }

    @Test
    void m1_borraLasResenasQueInsertoLaMigracion011() throws Exception {
        // Se ejecuta el SQL real de la 011 sobre los organizadores del seed.
        ejecutarRecurso("/migrations/011_cu26_contratar_organizador.sql");
        assertEquals(2, contar("Resena_Organizador"));
        assertEquals(1, contar("Resena_Organizador WHERE fecha = TIMESTAMP '2026-10-10 10:00:00'"));

        reaplicarMigracion();

        assertEquals(0, contar("Resena_Organizador"));
    }

    @Test
    void m2_borraTodasLasFirmasDeLasVersionesDelSeed() throws Exception {
        agregarOrganizadoresDelSeedAntiguo();
        insertarResenasComoElSeed();
        assertEquals(6, contar("Resena_Organizador"));

        reaplicarMigracion();

        assertEquals(0, contar("Resena_Organizador"));
    }

    @Test
    void m3_conservaResenaLegitimaDelMismoOrganizadorYCliente() throws Exception {
        insertarResenasComoElSeed();
        sql("INSERT INTO Resena_Organizador (id_organizador, id_cliente, calificacion, comentario) "
                + "VALUES (1, 5, 5, 'Nos ayudo con todo el matrimonio.')");

        reaplicarMigracion();

        assertEquals(1, contar("Resena_Organizador"));
        assertEquals(1, contar("Resena_Organizador WHERE comentario = 'Nos ayudo con todo el matrimonio.'"));
    }

    @Test
    void m4_conservaFilasQueDifierenEnClienteCalificacionOFecha() throws Exception {
        // Mismo comentario que S-1 pero otro cliente u otra calificación.
        sql("INSERT INTO Resena_Organizador (id_organizador, id_cliente, calificacion, comentario) VALUES "
                + "(1, 6, 5, 'Buena organizacion y comunicacion durante el evento.'), "
                + "(1, 5, 4, 'Buena organizacion y comunicacion durante el evento.')");

        // Con el nombre de usuario del cliente cambiado, solo la fecha exacta de la 011 identifica M-1.
        sql("UPDATE Usuario SET usuario = 'ana.m' WHERE id_usuario = 5");
        sql("INSERT INTO Resena_Organizador (id_organizador, id_cliente, calificacion, comentario, fecha) VALUES "
                + "(1, 5, 5, 'Buena organizacion y comunicacion durante el evento.', TIMESTAMP '2026-10-10 10:00:00'), "
                + "(1, 5, 5, 'Buena organizacion y comunicacion durante el evento.', TIMESTAMP '2026-10-12 09:00:00')");

        reaplicarMigracion();

        assertEquals(3, contar("Resena_Organizador"));
        assertEquals(0, contar("Resena_Organizador WHERE fecha = TIMESTAMP '2026-10-10 10:00:00'"));
        assertEquals(1, contar("Resena_Organizador WHERE fecha = TIMESTAMP '2026-10-12 09:00:00'"));
    }

    @Test
    void m5_conservaSiElUsuarioDelOrganizadorODelClienteNoCoincide() throws Exception {
        insertarResenasComoElSeed();
        sql("UPDATE Usuario SET usuario = 'valentina.flores' WHERE id_usuario = 8");
        sql("UPDATE Usuario SET usuario = 'diego.p' WHERE id_usuario = 6");

        reaplicarMigracion();

        // Se conservan S-3 y S-6 (organizador valentina) y S-2 (cliente diego).
        assertEquals(3, contar("Resena_Organizador"));
        assertEquals(2, contar("Resena_Organizador WHERE id_organizador = 3"));
        assertEquals(1, contar("Resena_Organizador WHERE id_organizador = 2"));
    }

    @Test
    void m6_seRegistraUnaVezYNoSeVuelveAEjecutar() throws Exception {
        assertEquals(1, contar("Esquema_version WHERE nombre = '" + MIGRACION + "'"));

        insertarResenasComoElSeed();
        DatabaseConnection.reiniciarInicializacion();
        DatabaseConnection.getConnection(url).close();

        assertEquals(1, contar("Esquema_version WHERE nombre = '" + MIGRACION + "'"));
        assertEquals(4, contar("Resena_Organizador"));
    }

    @Test
    void m7_baseNuevaConElSeedQuedaSinResenasYConOrganizadoresVerificados() {
        assertEquals(0, contar("Resena_Organizador"));
        assertEquals(3, contar("Organizador WHERE estado_verificacion = 'VERIFICADO'"));

        // El seed real permite completar la búsqueda de CU-026.
        SesionUsuario cliente = new ServiceAutenticacion(new RepositoryAutenticacion(url))
                .iniciarSesion("ana.martinez", "1234");
        ControllerAdministracion controlador = new ControllerAdministracion(
                new ServiceAdministracion(new RepositoryContratacion(url), new RepositoryOrganizador(url)),
                cliente);

        Map<String, Object> filtros = new HashMap<>();
        filtros.put("tipoEvento", "Boda");
        filtros.put("ciudad", "Bogota");
        filtros.put("fechaAproximada", LocalDate.now().plusYears(1));
        filtros.put("presupuestoEstimado", new BigDecimal("2000000"));

        List<Organizador> resultado = controlador.consultarOrganizadores(filtros);

        assertEquals(1, resultado.size());
        assertEquals("Laura Gómez", resultado.get(0).nombre());
        assertTrue(controlador.consultarResenas(resultado.get(0).id()).isEmpty());
    }

    @Test
    void m8_noModificaUsuariosOrganizadoresNiSolicitudes() throws Exception {
        sql("INSERT INTO Solicitud_Contratacion_Organizador (id_cliente, id_organizador, tipo_evento, "
                + "ciudad, fecha_aproximada, presupuesto_estimado, estado) "
                + "VALUES (5, 1, 'Boda', 'Bogota', DATE '2027-06-01', 2000000, 'PENDIENTE')");
        insertarResenasComoElSeed();

        int usuarios = contar("Usuario");
        int organizadores = contar("Organizador");
        int solicitudes = contar("Solicitud_Contratacion_Organizador");
        int notificaciones = contar("Notificacion_interna");
        String perfiles = perfiles();

        reaplicarMigracion();

        assertEquals(usuarios, contar("Usuario"));
        assertEquals(organizadores, contar("Organizador"));
        assertEquals(solicitudes, contar("Solicitud_Contratacion_Organizador"));
        assertEquals(notificaciones, contar("Notificacion_interna"));
        assertEquals(perfiles, perfiles());
        assertEquals(0, contar("Resena_Organizador"));
    }

    // ------------------------------------------------------------------

    /** Simula una base que aún no tenía la 015 y la vuelve a abrir. */
    private void reaplicarMigracion() throws Exception {
        sql("DELETE FROM Esquema_version WHERE nombre = '" + MIGRACION + "'");
        DatabaseConnection.reiniciarInicializacion();
        DatabaseConnection.getConnection(url).close();
        assertEquals(1, contar("Esquema_version WHERE nombre = '" + MIGRACION + "'"));
    }

    /** Reseñas tal como las insertaban las versiones de seed.sql (sin fecha propia). */
    private void insertarResenasComoElSeed() throws Exception {
        sql("INSERT INTO Resena_Organizador (id_organizador, id_cliente, calificacion, comentario) VALUES "
                + "(1, 5, 5, 'Buena organizacion y comunicacion durante el evento.'), "
                + "(2, 6, 4, 'La celebracion estuvo bien organizada.'), "
                + "(3, 5, 5, 'Excelente atencion y organizacion.'), "
                + "(3, 7, 5, 'Una celebracion muy bien organizada.')");
    }

    /** Organizadores 4 y 5 de las versiones 7c893a8 a 53c5249 del seed y sus reseñas. */
    private void agregarOrganizadoresDelSeedAntiguo() throws Exception {
        sql("INSERT INTO Usuario (id_usuario, nombre, usuario, contraseña, estado, fecha_registro, "
                + "acepta_marketing, tipo_usuario) VALUES "
                + "(9, 'Santiago Rivera', 'santiago', '1234', 'ACTIVO', CURRENT_TIMESTAMP, FALSE, 'ORGANIZADOR'), "
                + "(10, 'Juliana Torres', 'juliana', '1234', 'ACTIVO', CURRENT_TIMESTAMP, FALSE, 'ORGANIZADOR')");
        sql("INSERT INTO Organizador (id_organizador, id_usuario, empresa, telefono, estado) VALUES "
                + "(4, 9, 'Eventos Rivera', '3005550102', 'ACTIVO'), "
                + "(5, 10, 'Sueños de Juliana', '3005550103', 'ACTIVO')");
        sql("INSERT INTO Resena_Organizador (id_organizador, id_cliente, calificacion, comentario) VALUES "
                + "(4, 6, 4, 'Buen servicio para eventos empresariales.'), "
                + "(5, 5, 5, 'Una celebracion muy bien organizada.')");
    }

    private String perfiles() throws Exception {
        StringBuilder texto = new StringBuilder();
        try (Connection connection = DatabaseConnection.getConnection(url);
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(
                     "SELECT id_organizador, ciudad, especialidad, tarifa_base, estado_verificacion "
                     + "FROM Organizador ORDER BY id_organizador")) {
            while (result.next()) {
                for (int i = 1; i <= 5; i++) {
                    texto.append(result.getString(i)).append('|');
                }
                texto.append('\n');
            }
        }
        return texto.toString();
    }

    private void ejecutarRecurso(String recurso) throws Exception {
        try (Connection connection = DatabaseConnection.getConnection(url);
             InputStreamReader reader = new InputStreamReader(
                     getClass().getResourceAsStream(recurso), StandardCharsets.UTF_8)) {
            RunScript.execute(connection, reader);
        }
    }

    private void sql(String sentencia) throws Exception {
        try (Connection connection = DatabaseConnection.getConnection(url);
             Statement statement = connection.createStatement()) {
            statement.execute(sentencia);
        }
    }

    private int contar(String tablaYCondicion) {
        try (Connection connection = DatabaseConnection.getConnection(url);
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM " + tablaYCondicion)) {
            result.next();
            return result.getInt(1);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
