
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import inicializacion.CargarDatosEjemplo;
import persistencia.DatabaseConnection;
import entities.contrataciones.ConfirmacionContratacion;
import entities.SesionUsuario;
import repositories.RepositoryAutenticacion;
import repositories.solicitudes.RepositoryContratacionServicio;
import repositories.solicitudes.RepositoryPropuestaServicio;
import repositories.solicitudes.RepositorySolicitudEjecucion;
import services.ServiceAutenticacion;
import services.solicitudes.ServiceContratacionServicio;
import services.solicitudes.ServiceSolicitudes;

/**
 * Comprueba que los datos de demostración cuentan una historia completa de
 * CU-12 y CU-13: cada proveedor recibe al menos cinco solicitudes, hay
 * solicitudes en todos los estados y la propuesta seleccionada se puede
 * contratar de principio a fin.
 */
public class HistoriaDemostracionProveedoresH2Test {

    private String url;

    @BeforeEach
    void cargarDatos() throws Exception {
        url = "jdbc:h2:mem:historia_" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1";
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
    void cadaProveedorTieneAlMenosCincoSolicitudes() throws Exception {
        for (int proveedor : List.of(1, 2, 5)) {

            assertTrue(contar("SELECT COUNT(*) FROM Solicitud_Ejecucion "
                    + "WHERE id_proveedor = " + proveedor) >= 5,
                    "solicitudes " + proveedor);
        }
    }

    @Test
    void lasSolicitudesCubrenLosEstadosDeCu12() throws Exception {
        for (String estado : List.of("PENDIENTE", "EN_NEGOCIACION",
                "RECHAZADA", "PROPUESTA_RECHAZADA", "CONTRATADA")) {

            assertTrue(contar("SELECT COUNT(*) FROM Solicitud_Ejecucion "
                    + "WHERE estado = '" + estado + "'") > 0, estado);
        }

        assertEquals(1, contar("SELECT COUNT(*) FROM Mensaje "
                + "WHERE id_propuesta IS NOT NULL"),
                "hay un ajuste vinculado a una versión");
    }

    @Test
    void elProveedorVeSolicitudesDeSuCuentaYNoDeLasAjenas() throws Exception {
        SesionUsuario flash = iniciarSesion("flash");
        ServiceSolicitudes servicio = solicitudes(flash);

        var recibidas = servicio.listarSolicitudesProveedor(flash.getIdActor());

        assertTrue(recibidas.size() >= 5);
        assertTrue(recibidas.stream()
                .allMatch(s -> s.getIdProveedor() == flash.getIdActor()));
    }

    @Test
    void valentinaPuedeContratarLaPropuestaSeleccionada() throws Exception {
        SesionUsuario valentina = iniciarSesion("valentina");

        ServiceContratacionServicio contratacion
                = new ServiceContratacionServicio(
                        new RepositoryContratacionServicio(), valentina);

        ConfirmacionContratacion confirmacion
                = contratacion.contratar(valentina.getIdActor(), 6);

        assertNotNull(confirmacion);
        assertEquals("Cumpleaños 15 de Sofía", confirmacion.nombreEvento());
        assertEquals(2, confirmacion.numeroVersion());
        assertEquals("CONTRATADA", texto("SELECT estado FROM Propuesta "
                + "WHERE id_propuesta = 6"));
        assertEquals("CONTRATADA", texto("SELECT estado "
                + "FROM Necesidad_Servicio_Evento WHERE id_necesidad_servicio = 3"));
    }

    private SesionUsuario iniciarSesion(String usuario) {
        SesionUsuario sesion = new ServiceAutenticacion(
                new RepositoryAutenticacion(url)).iniciarSesion(usuario, "1234");

        assertNotNull(sesion, usuario);
        return sesion;
    }

    private ServiceSolicitudes solicitudes(SesionUsuario sesion) {
        return new ServiceSolicitudes(
                new RepositorySolicitudEjecucion(),
                new RepositoryPropuestaServicio(),
                sesion);
    }

    private int contar(String sql) throws Exception {
        return Integer.parseInt(texto(sql));
    }

    private String texto(String sql) throws Exception {
        try (Connection connection = DatabaseConnection.getConnection(url);
                Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery(sql)) {

            assertTrue(result.next());
            return result.getString(1);
        }
    }
}
