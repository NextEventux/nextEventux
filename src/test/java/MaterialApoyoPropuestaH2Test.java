import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;

import persistencia.PropiedadSistemaTemporal;
import entities.solicitudes.PropuestaServicio;
import services.solicitudes.ServiceSolicitudes;

/** CU-12: el material de apoyo de una propuesta es opcional y queda en su versión. */
@ResourceLock(Resources.SYSTEM_PROPERTIES)
class MaterialApoyoPropuestaH2Test {

    private static final String URL
            = "jdbc:h2:mem:cu12material;DB_CLOSE_DELAY=-1";

    private ServiceSolicitudes proveedor;
    private PropiedadSistemaTemporal propiedadUrlBaseDatos;

    @BeforeEach
    void preparar() throws Exception {
        propiedadUrlBaseDatos =
                new PropiedadSistemaTemporal("nexteventux.db.url", URL);
        DatosCU12Prueba.preparar(URL);
        proveedor = DatosCU12Prueba.servicioProveedor();
    }

    @AfterEach
    void limpiar() throws Exception {
        try {
            DatosCU12Prueba.limpiar(URL);
        } finally {
            propiedadUrlBaseDatos.restaurar();
        }
    }

    private int enviar(String material) {
        return proveedor.enviarPropuesta(
                DatosCU12Prueba.PROVEEDOR,
                DatosCU12Prueba.SOLICITUD,
                new BigDecimal("1500000"),
                "Arreglos florales",
                "Anticipo del 50 %",
                material);
    }

    private List<PropuestaServicio> historial() {
        return proveedor.listarHistorialPropuestas(
                DatosCU12Prueba.PROVEEDOR, DatosCU12Prueba.SOLICITUD);
    }

    @Test
    void laPropuestaGuardaElMaterialDeApoyo() {
        enviar("https://ejemplo.com/catalogo");

        assertEquals("https://ejemplo.com/catalogo",
                historial().get(0).getMaterialApoyo());
    }

    @Test
    void elMaterialEsOpcional() {
        enviar("   ");

        assertNull(historial().get(0).getMaterialApoyo());
    }

    @Test
    void laPropuestaSinMaterialUsaElEnvioAnterior() {
        proveedor.enviarPropuesta(
                DatosCU12Prueba.PROVEEDOR,
                DatosCU12Prueba.SOLICITUD,
                new BigDecimal("900000"),
                "Centro de mesa",
                "Pago contra entrega");

        assertNull(historial().get(0).getMaterialApoyo());
    }

    @Test
    void cadaVersionConservaSuPropioMaterial() {
        enviar("https://ejemplo.com/catalogo-v1.pdf");
        enviar("https://ejemplo.com/catalogo-v2.pdf");

        List<PropuestaServicio> versiones = historial();

        assertEquals("https://ejemplo.com/catalogo-v2.pdf", versiones.get(0).getMaterialApoyo());
        assertEquals("https://ejemplo.com/catalogo-v1.pdf", versiones.get(1).getMaterialApoyo());
    }

    @Test
    void elOrganizadorVeElMaterialDeLaPropuestaVigente() {
        enviar("https://ejemplo.com/catalogo");

        List<PropuestaServicio> propuestas = DatosCU12Prueba
                .servicioOrganizador()
                .listarPropuestasOrganizador(DatosCU12Prueba.ORGANIZADOR);

        assertEquals("https://ejemplo.com/catalogo",
                propuestas.get(0).getMaterialApoyo());
    }

    @Test
    void elMaterialNoPuedeSuperarMilCaracteres() {
        assertThrows(IllegalArgumentException.class,
                () -> enviar("x".repeat(1001)));
    }

    @Test
    void elMaterialDebeSerUnEnlaceHttps() {
        assertThrows(IllegalArgumentException.class,
                () -> enviar("C:\\Users\\ana\\catalogo.pdf"));
        assertThrows(IllegalArgumentException.class,
                () -> enviar("http://ejemplo.com/catalogo"));
        assertThrows(IllegalArgumentException.class,
                () -> enviar("catalogo.pdf"));
    }
}
