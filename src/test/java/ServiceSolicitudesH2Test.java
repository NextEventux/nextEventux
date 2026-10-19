import config.DatabaseConnection;
import entities.SolicitudEjecucion;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import repositories.RepositorySolicitudEjecucion;
import services.ServiceSolicitudes;

import static org.junit.jupiter.api.Assertions.*;

public class ServiceSolicitudesH2Test {

    private static final String URL =
            "jdbc:h2:mem:cu12solicitudes;DB_CLOSE_DELAY=-1";

    private ServiceSolicitudes service;

    @BeforeEach
    void preparar() throws Exception {

        System.setProperty("nexteventux.db.url", URL);

        // La conexion inicializa el esquema y las migraciones de CU12
        try (Connection connection = DatabaseConnection.getConnection();
             Statement statement = connection.createStatement()) {

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
            try (Connection connection =
                         DriverManager.getConnection(URL, "sa", "");
                 Statement statement = connection.createStatement()) {

                statement.execute("DROP ALL OBJECTS");
            }
        } finally {
            System.clearProperty("nexteventux.db.url");
        }
    }

    @Test
    void proveedorSoloConsultaSusSolicitudes() {

        List<SolicitudEjecucion> solicitudes =
                service.listarSolicitudesProveedor(9001);

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

        try (Connection connection = DatabaseConnection.getConnection();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("""
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

        List<SolicitudEjecucion> solicitudes =
                service.listarSolicitudesProveedor(9002);

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
    void motivoDeRechazoNoPuedeSuperar500Caracteres() {

        String motivo = "A".repeat(501);

        assertThrows(
                IllegalArgumentException.class,
                () -> service.rechazarSolicitud(9001, 9001, motivo));
    }
}