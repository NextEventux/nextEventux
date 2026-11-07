import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import entities.solicitudes.MensajeSolicitud;
import services.ServiceSolicitudes;

/** CU-12 (A2): la solicitud de ajuste queda vinculada a la versión vigente. */
class AjustePropuestaH2Test {

    private static final String URL
            = "jdbc:h2:mem:cu12ajuste;DB_CLOSE_DELAY=-1";

    private ServiceSolicitudes proveedor;
    private ServiceSolicitudes organizador;

    @BeforeEach
    void preparar() throws Exception {
        DatosCU12Prueba.preparar(URL);
        proveedor = DatosCU12Prueba.servicioProveedor();
        organizador = DatosCU12Prueba.servicioOrganizador();
    }

    @AfterEach
    void limpiar() throws Exception {
        DatosCU12Prueba.limpiar(URL);
    }

    private int enviarPropuesta(String precio) {
        return proveedor.enviarPropuesta(
                DatosCU12Prueba.PROVEEDOR,
                DatosCU12Prueba.SOLICITUD,
                new BigDecimal(precio),
                "Arreglos florales",
                "Anticipo del 50 %");
    }

    private List<MensajeSolicitud> mensajes() {
        return organizador.listarMensajes(
                DatosCU12Prueba.SOLICITUD,
                DatosCU12Prueba.USUARIO_ORGANIZADOR);
    }

    @Test
    void elAjusteQuedaVinculadoALaVersionVigente() {
        int propuesta = enviarPropuesta("1500000");

        organizador.solicitarAjuste(
                DatosCU12Prueba.ORGANIZADOR, propuesta,
                "¿Puede bajar el precio?", null);

        MensajeSolicitud ajuste = mensajes().get(0);

        assertTrue(ajuste.esSolicitudDeAjuste());
        assertEquals(propuesta, ajuste.getIdPropuesta());
        assertEquals(1, ajuste.getVersionPropuesta());
        assertEquals("¿Puede bajar el precio?", ajuste.getContenido());
    }

    @Test
    void unMensajeNormalNoEstaVinculadoAUnaPropuesta() {
        enviarPropuesta("1500000");

        organizador.enviarMensaje(
                DatosCU12Prueba.SOLICITUD,
                DatosCU12Prueba.USUARIO_ORGANIZADOR,
                "Gracias por la propuesta", null);

        MensajeSolicitud mensaje = mensajes().get(0);

        assertFalse(mensaje.esSolicitudDeAjuste());
        assertNull(mensaje.getVersionPropuesta());
    }

    @Test
    void trasUnaNuevaVersionElAjusteAnteriorConservaSuVersion() {
        int version1 = enviarPropuesta("1500000");
        organizador.solicitarAjuste(
                DatosCU12Prueba.ORGANIZADOR, version1, "Más barato", null);

        int version2 = enviarPropuesta("1300000");
        organizador.solicitarAjuste(
                DatosCU12Prueba.ORGANIZADOR, version2, "Agregar velas", null);

        List<MensajeSolicitud> mensajes = mensajes();

        assertEquals(1, mensajes.get(0).getVersionPropuesta());
        assertEquals(2, mensajes.get(1).getVersionPropuesta());
    }

    @Test
    void noSePuedePedirAjusteSobreUnaVersionQueYaNoEsLaVigente() {
        int version1 = enviarPropuesta("1500000");
        enviarPropuesta("1300000");

        assertThrows(IllegalStateException.class,
                () -> organizador.solicitarAjuste(
                        DatosCU12Prueba.ORGANIZADOR, version1, "Tarde", null));
    }

    @Test
    void noSePuedePedirAjusteSobreLaPropuestaSeleccionada() {
        int propuesta = enviarPropuesta("1500000");
        organizador.seleccionarPropuesta(
                DatosCU12Prueba.ORGANIZADOR, propuesta);

        assertThrows(IllegalStateException.class,
                () -> organizador.solicitarAjuste(
                        DatosCU12Prueba.ORGANIZADOR, propuesta, "Cambio", null));
    }

    @Test
    void elAjusteDebeExplicarQueSeQuiereCambiar() {
        int propuesta = enviarPropuesta("1500000");

        assertThrows(IllegalArgumentException.class,
                () -> organizador.solicitarAjuste(
                        DatosCU12Prueba.ORGANIZADOR, propuesta, "  ", null));
    }

    @Test
    void soloElOrganizadorPideAjustes() {
        int propuesta = enviarPropuesta("1500000");

        assertThrows(IllegalStateException.class,
                () -> proveedor.solicitarAjuste(
                        DatosCU12Prueba.PROVEEDOR, propuesta, "Cambio", null));
    }

    @Test
    void elAjusteAdmiteUnAdjuntoOpcional() {
        int propuesta = enviarPropuesta("1500000");

        organizador.solicitarAjuste(
                DatosCU12Prueba.ORGANIZADOR, propuesta,
                "Como esta foto", "/tmp/referencia.png");

        assertEquals("/tmp/referencia.png", mensajes().get(0).getAdjuntoUrl());
    }
}
