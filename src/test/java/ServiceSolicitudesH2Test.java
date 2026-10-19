
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import config.DatabaseConnection;
import entities.SolicitudEjecucion;
import repositories.RepositorySolicitudEjecucion;
import services.ServiceSolicitudes;

public class ServiceSolicitudesH2Test {

    private static final String URL
            = "jdbc:h2:mem:cu12solicitudes;DB_CLOSE_DELAY=-1";

    private ServiceSolicitudes service;

    @BeforeEach
    void preparar() throws Exception {

        System.setProperty("nexteventux.db.url", URL);

        // La conexion inicializa el esquema y las migraciones de CU12
        try (Connection connection = DatabaseConnection.getConnection(); Statement statement = connection.createStatement()) {

            // Estos registros solo se usan para probar las solicitudes
            statement.execute("SET REFERENTIAL_INTEGRITY FALSE");

            statement.executeUpdate("""
                    INSERT INTO Solicitud_Ejecucion
                    (id_solicitud, id_proveedor, id_evento,
                     id_servicio, fecha_solicitud, estado)
                    VALUES
                    (9001, 9001, 1, 1,
                     TIMESTAMP '2026-10-18 10:00:00', 'PENDIENTE'),
                    (9002, 9002, 1, 2,
                     TIMESTAMP '2026-10-18 11:00:00', 'PENDIENTE'),
                    (9003, 9001, 2, 1,
                     TIMESTAMP '2026-10-18 12:00:00', 'RESPONDIDA')
                    """);

            statement.execute("SET REFERENTIAL_INTEGRITY TRUE");
        }

        service = new ServiceSolicitudes(
                new RepositorySolicitudEjecucion());
    }

    @AfterEach
    void limpiar() throws Exception {

        try {
            // Conexion directa para limpiar sin ejecutar las migraciones
            try (Connection connection
                    = DriverManager.getConnection(URL, "sa", ""); Statement statement = connection.createStatement()) {

                statement.execute("DROP ALL OBJECTS");
            }
        } finally {
            System.clearProperty("nexteventux.db.url");
        }
    }

    @Test
    void proveedorSoloConsultaSusSolicitudes() {

        List<SolicitudEjecucion> solicitudes
                = service.listarSolicitudesProveedor(9001);

        assertEquals(2, solicitudes.size());

        for (SolicitudEjecucion solicitud : solicitudes) {
            assertEquals(9001, solicitud.getIdProveedor());
        }
    }

    @Test
    void proveedorPuedeRechazarSolicitudPendiente() throws Exception {

        service.rechazarSolicitud(
                9001,
                9001,
                "No tengo disponibilidad para el evento.");

        try (Connection connection = DatabaseConnection.getConnection(); Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery("""
                     SELECT estado, motivo_rechazo, fecha_respuesta
                     FROM Solicitud_Ejecucion
                     WHERE id_solicitud = 9001
                     """)) {

            assertTrue(result.next());
            assertEquals("RECHAZADA", result.getString("estado"));
            assertEquals(
                    "No tengo disponibilidad para el evento.",
                    result.getString("motivo_rechazo"));
            assertNotNull(result.getTimestamp("fecha_respuesta"));
        }
    }

    @Test
    void proveedorNoPuedeRechazarSolicitudAjena() {

        assertThrows(
                IllegalStateException.class,
                () -> service.rechazarSolicitud(
                        9001,
                        9002,
                        "Intento de rechazo ajeno."));

        List<SolicitudEjecucion> solicitudes
                = service.listarSolicitudesProveedor(9002);

        assertEquals("PENDIENTE", solicitudes.get(0).getEstado());
    }

    @Test
    void noSePuedeRechazarUnaSolicitudYaRespondida() {

        assertThrows(
                IllegalStateException.class,
                () -> service.rechazarSolicitud(
                        9001,
                        9003,
                        "Intento de modificar una respuesta anterior."));
    }

    @Test
    void noSePuedeRechazarDosVeces() {

        service.rechazarSolicitud(
                9001,
                9001,
                "Primera respuesta.");

        assertThrows(
                IllegalStateException.class,
                () -> service.rechazarSolicitud(
                        9001,
                        9001,
                        "Segunda respuesta."));
    }

    @Test
    void motivoDeRechazoEsObligatorio() {

        assertThrows(
                IllegalArgumentException.class,
                () -> service.rechazarSolicitud(9001, 9001, null));

        assertThrows(
                IllegalArgumentException.class,
                () -> service.rechazarSolicitud(9001, 9001, "   "));
    }

    @Test
    void organizadorPuedeCrearSolicitudParaSuEvento() throws Exception {

        try (Connection connection = DatabaseConnection.getConnection(); Statement statement = connection.createStatement()) {

            statement.execute("SET REFERENTIAL_INTEGRITY FALSE");

            statement.executeUpdate("""
                    INSERT INTO Organizador
                    (id_organizador, id_usuario, empresa, telefono, estado)
                    VALUES
                    (9001, 9001, 'Organizador de prueba',
                     '3000000000', 'ACTIVO')
                    """);

            statement.executeUpdate("""
                    INSERT INTO Proveedor
                    (id_proveedor, id_usuario, nombre, empresa,
                     correo, telefono, direccion, estado)
                    VALUES
                    (9001, 9002, 'Proveedor de prueba',
                     'Servicios de prueba', 'cu12@ejemplo.com',
                     '3000000001', 'Bogota', 'APROBADO')
                    """);

            statement.executeUpdate("""
                    INSERT INTO Evento
                    (id_evento, id_organizador, nombre,
                     fecha_inicio, fecha_fin, estado, fecha_creacion)
                    VALUES
                    (9001, 9001, 'Evento de prueba',
                     TIMESTAMP '2026-11-20 10:00:00',
                     TIMESTAMP '2026-11-20 18:00:00',
                     'PLANEADO',
                     CURRENT_TIMESTAMP)
                    """);

            statement.executeUpdate("""
                    INSERT INTO Servicio
                    (id_servicio, id_proveedor, nombre, estado)
                    VALUES
                    (9001, 9001, 'Fotografia de prueba', 'DISPONIBLE')
                    """);

            statement.execute("SET REFERENTIAL_INTEGRITY TRUE");
        }

        int idSolicitud = service.crearSolicitud(
                9001,
                9001,
                9001,
                "Necesito fotografia para mi evento.");

        assertTrue(idSolicitud > 0);

        try (Connection connection = DatabaseConnection.getConnection(); java.sql.PreparedStatement statement
                = connection.prepareStatement("""
                             SELECT s.estado,
                                    s.id_evento,
                                    s.id_servicio,
                                    n.descripcion
                             FROM Solicitud_Ejecucion s
                             JOIN Necesidad_Servicio_Evento n
                               ON n.id_necesidad_servicio =
                                  s.id_necesidad_servicio
                             WHERE s.id_solicitud = ?
                             """)) {

            statement.setInt(1, idSolicitud);

            try (ResultSet result = statement.executeQuery()) {

                assertTrue(result.next());
                assertEquals("PENDIENTE", result.getString("estado"));
                assertEquals(9001, result.getInt("id_evento"));
                assertEquals(9001, result.getInt("id_servicio"));
                assertEquals(
                        "Necesito fotografia para mi evento.",
                        result.getString("descripcion"));
            }
        }
    }

    @Test
    void soloUnaPropuestaPuedeEstarSeleccionadaPorNecesidad()
            throws Exception {

        try (Connection connection = DatabaseConnection.getConnection(); Statement statement = connection.createStatement()) {

            statement.execute("SET REFERENTIAL_INTEGRITY FALSE");

            statement.executeUpdate("""
                    INSERT INTO Organizador
                    (id_organizador, id_usuario, empresa, telefono, estado)
                    VALUES
                    (9001, 9001, 'Organizador de prueba',
                     '3000000000', 'ACTIVO')
                    """);

            statement.executeUpdate("""
                    INSERT INTO Evento
                    (id_evento, id_organizador, nombre,
                     fecha_inicio, fecha_fin, estado, fecha_creacion)
                    VALUES
                    (9001, 9001, 'Evento de prueba',
                     TIMESTAMP '2026-11-20 10:00:00',
                     TIMESTAMP '2026-11-20 18:00:00',
                     'PLANEADO', CURRENT_TIMESTAMP)
                    """);

            statement.executeUpdate("""
                    INSERT INTO Necesidad_Servicio_Evento
                    (id_necesidad_servicio, id_evento, descripcion, estado)
                    VALUES
                    (9001, 9001, 'Necesito fotografia', 'ABIERTA')
                    """);

            statement.executeUpdate("""
                    UPDATE Solicitud_Ejecucion
                    SET id_evento = 9001,
                        id_necesidad_servicio = 9001,
                        estado = 'EN_NEGOCIACION'
                    WHERE id_solicitud = 9001
                    """);

            statement.executeUpdate("""
                    INSERT INTO Solicitud_Ejecucion
                    (id_solicitud, id_proveedor, id_evento,
                     id_servicio, id_necesidad_servicio,
                     fecha_solicitud, estado)
                    VALUES
                    (9100, 9002, 9001, 2, 9001,
                     CURRENT_TIMESTAMP, 'EN_NEGOCIACION')
                    """);

            statement.executeUpdate("""
                    INSERT INTO Propuesta
                    (id_propuesta, id_solicitud, numero_version,
                     valor, descripcion, condiciones,
                     fecha_envio, vigente, estado)
                    VALUES
                    (9101, 9001, 1, 800000,
                     'Primera propuesta', 'Condiciones A',
                     CURRENT_TIMESTAMP, TRUE, 'ENVIADA'),
                    (9102, 9100, 1, 900000,
                     'Segunda propuesta', 'Condiciones B',
                     CURRENT_TIMESTAMP, TRUE, 'ENVIADA')
                    """);

            statement.execute("SET REFERENTIAL_INTEGRITY TRUE");
        }

        service.seleccionarPropuesta(9001, 9101);

        // El organizador puede cambiar de seleccion antes de contratar
        service.seleccionarPropuesta(9001, 9102);

        try (Connection connection = DatabaseConnection.getConnection(); Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery("""
                     SELECT id_propuesta_seleccionada
                     FROM Necesidad_Servicio_Evento
                     WHERE id_necesidad_servicio = 9001
                     """)) {

            assertTrue(result.next());
            assertEquals(9102, result.getInt(1));
        }

        // Un organizador diferente no puede seleccionar la propuesta
        assertThrows(
                IllegalStateException.class,
                () -> service.seleccionarPropuesta(9002, 9101));
        // No se puede rechazar la propuesta seleccionada
        assertThrows(
                IllegalStateException.class,
                () -> service.rechazarPropuesta(9001, 9102));

        // Si no esta seleccionada, el organizador puede rechazarla
        service.rechazarPropuesta(9001, 9101);

        try (Connection connection = DatabaseConnection.getConnection(); Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery("""
                     SELECT estado, vigente
                     FROM Propuesta
                     WHERE id_propuesta = 9101
                     """)) {

            assertTrue(result.next());
            assertEquals("RECHAZADA", result.getString("estado"));
            assertFalse(result.getBoolean("vigente"));
        }
    }
}
