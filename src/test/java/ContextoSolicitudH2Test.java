import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import entities.ContextoSolicitud;
import entities.PropuestaServicio;
import entities.SolicitudEjecucion;

/** CU-12: el proveedor ve nombres, fechas y lugar en vez de identificadores. */
class ContextoSolicitudH2Test {

    private static final String URL
            = "jdbc:h2:mem:cu12contexto;DB_CLOSE_DELAY=-1";

    @BeforeEach
    void preparar() throws Exception {
        DatosCU12Prueba.preparar(URL);
    }

    @AfterEach
    void limpiar() throws Exception {
        DatosCU12Prueba.limpiar(URL);
    }

    @Test
    void laSolicitudTraeEventoServicioYOrganizador() {
        List<SolicitudEjecucion> solicitudes = DatosCU12Prueba
                .servicioProveedor()
                .listarSolicitudesProveedor(DatosCU12Prueba.PROVEEDOR);

        assertEquals(1, solicitudes.size());
        ContextoSolicitud contexto = solicitudes.get(0).getContexto();

        assertEquals("Boda de Valentina", contexto.nombreEvento());
        assertEquals("Salón Primavera", contexto.ubicacion());
        assertEquals(80, contexto.personas());
        assertEquals(LocalDateTime.of(2026, 12, 5, 15, 0), contexto.inicioEvento());
        assertEquals(LocalDateTime.of(2026, 12, 5, 23, 0), contexto.finEvento());
        assertEquals("Decoración floral", contexto.nombreServicio());
        assertEquals("Valentina Flores", contexto.nombreOrganizador());
        assertEquals("Decoración del salón", contexto.descripcionNecesidad());
    }

    @Test
    void elOrganizadorVeLaPropuestaConProveedorServicioYEvento() {
        DatosCU12Prueba.servicioProveedor().enviarPropuesta(
                DatosCU12Prueba.PROVEEDOR,
                DatosCU12Prueba.SOLICITUD,
                new java.math.BigDecimal("1500000"),
                "Arreglos florales para 10 mesas",
                "Anticipo del 50 %");

        List<PropuestaServicio> propuestas = DatosCU12Prueba
                .servicioOrganizador()
                .listarPropuestasOrganizador(DatosCU12Prueba.ORGANIZADOR);

        assertEquals(1, propuestas.size());
        ContextoSolicitud contexto = propuestas.get(0).getContexto();

        assertEquals("Luna Decoraciones", contexto.nombreProveedor());
        assertEquals("Decoración floral", contexto.nombreServicio());
        assertEquals("Boda de Valentina", contexto.nombreEvento());
        assertEquals("Decoración del salón", contexto.descripcionNecesidad());
    }

    @Test
    void laSolicitudSigueVisibleSiFaltanDatosRelacionados() throws Exception {
        try (var connection = config.DatabaseConnection.getConnection();
                var statement = connection.createStatement()) {
            statement.execute("SET REFERENTIAL_INTEGRITY FALSE");
            statement.executeUpdate("""
                    INSERT INTO Solicitud_Ejecucion
                    (id_solicitud, id_proveedor, id_evento, id_servicio,
                     fecha_solicitud, estado)
                    VALUES (9399, 9302, 9999, 9999,
                            CURRENT_TIMESTAMP, 'PENDIENTE')
                    """);
            statement.execute("SET REFERENTIAL_INTEGRITY TRUE");
        }

        List<SolicitudEjecucion> solicitudes = DatosCU12Prueba
                .servicioProveedor()
                .listarSolicitudesProveedor(DatosCU12Prueba.PROVEEDOR);

        assertEquals(2, solicitudes.size());
        for (SolicitudEjecucion solicitud : solicitudes) {
            assertNotNull(solicitud.getContexto());
        }
    }
}
