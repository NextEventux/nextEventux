
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import config.CargarDatosEjemplo;
import config.DatabaseConnection;

/** Datos de demostración de CU-11: cada proveedor tiene servicios en varios estados. */
public class ServiciosDemostracionH2Test {

    private String url;

    @BeforeEach
    void cargarDatos() throws Exception {
        url = "jdbc:h2:mem:servicios_" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1";
        System.setProperty("nexteventux.db.url", url);

        try (Connection connection = DatabaseConnection.getConnection(url)) {
            CargarDatosEjemplo.cargar(connection);
        }
    }

    @AfterEach
    void limpiar() throws Exception {
        try (Connection connection = DriverManager.getConnection(url, "sa", "");
                Statement statement = connection.createStatement()) {
            statement.execute("SHUTDOWN");
        } finally {
            System.clearProperty("nexteventux.db.url");
        }
    }

    @Test
    void cadaProveedorTieneCincoServicios() throws Exception {
        for (int proveedor : List.of(1, 2, 5)) {

            assertEquals(5, contar("SELECT COUNT(*) FROM Servicio "
                    + "WHERE id_proveedor = " + proveedor), "proveedor " + proveedor);
        }
    }

    @Test
    void losServiciosCubrenLosEstadosDeCu11() throws Exception {
        for (String estado : List.of("DISPONIBLE", "BORRADOR", "RETIRADO")) {

            assertTrue(contar("SELECT COUNT(*) FROM Servicio "
                    + "WHERE estado = '" + estado + "'") > 0, estado);
        }
    }

    @Test
    void soloLosServiciosPublicadosTienenHorarios() throws Exception {
        assertEquals(0, contar("SELECT COUNT(*) FROM Servicio s "
                + "WHERE s.estado <> 'DISPONIBLE' AND EXISTS (SELECT 1 "
                + "FROM Disponibilidad_Servicio d "
                + "WHERE d.id_servicio = s.id_servicio)"));
    }

    @Test
    void hayPreguntasRespondidasYPendientes() throws Exception {
        assertTrue(contar("SELECT COUNT(*) FROM Pregunta_Servicio "
                + "WHERE estado = 'RESPONDIDA'") > 0);
        assertTrue(contar("SELECT COUNT(*) FROM Pregunta_Servicio "
                + "WHERE estado = 'PENDIENTE'") > 0);
    }

    private int contar(String sql) throws Exception {
        try (Connection connection = DatabaseConnection.getConnection(url);
                Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery(sql)) {

            assertTrue(result.next());
            return result.getInt(1);
        }
    }
}
