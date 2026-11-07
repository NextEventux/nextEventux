package views.solicitudes;

import entities.eventos.Evento;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import entities.solicitudes.ContextoSolicitud;
import entities.solicitudes.MensajeSolicitud;

class PresentacionSolicitudTest {

    private static ContextoSolicitud contexto(
            String evento, String servicio, Integer personas) {
        return new ContextoSolicitud(
                evento,
                LocalDateTime.of(2026, 12, 5, 15, 0),
                LocalDateTime.of(2026, 12, 5, 23, 0),
                "Salón Primavera", personas, servicio,
                "Valentina Flores", "Luna Decoraciones", "Decoración");
    }

    @Test
    void losEstadosSeMuestranConTextoLegible() {
        assertEquals("En negociación",
                PresentacionSolicitud.textoEstado("EN_NEGOCIACION"));
        assertEquals("Seleccionada para contratación",
                PresentacionSolicitud.textoEstado("seleccionada"));
        assertEquals("OTRO ESTADO",
                PresentacionSolicitud.textoEstado("OTRO_ESTADO"));
        assertEquals("—", PresentacionSolicitud.textoEstado(null));
    }

    @Test
    void cadaEstadoTieneElColorQueIndicaSuSituacion() {
        assertEquals("chip-aviso", PresentacionSolicitud.estiloEstado("PENDIENTE"));
        assertEquals("chip-info", PresentacionSolicitud.estiloEstado("EN_NEGOCIACION"));
        assertEquals("chip-ok", PresentacionSolicitud.estiloEstado("CONTRATADA"));
        assertEquals("chip-error", PresentacionSolicitud.estiloEstado("RECHAZADA"));
        assertEquals("chip-neutro", PresentacionSolicitud.estiloEstado("NO_SELECCIONADA"));
        assertEquals("chip-neutro", PresentacionSolicitud.estiloEstado(null));
    }

    @Test
    void soloSeRechazaUnaSolicitudPendiente() {
        assertTrue(PresentacionSolicitud.puedeRechazar("PENDIENTE"));
        assertFalse(PresentacionSolicitud.puedeRechazar("EN_NEGOCIACION"));
        assertFalse(PresentacionSolicitud.puedeRechazar("RECHAZADA"));
    }

    @Test
    void laPropuestaSeEnviaMientrasLaNegociacionSigaAbierta() {
        assertTrue(PresentacionSolicitud.puedeEnviarPropuesta("PENDIENTE"));
        assertTrue(PresentacionSolicitud.puedeEnviarPropuesta("EN_NEGOCIACION"));
        assertFalse(PresentacionSolicitud.puedeEnviarPropuesta("CONTRATADA"));
        assertFalse(PresentacionSolicitud.puedeEnviarPropuesta("PROPUESTA_RECHAZADA"));
    }

    @Test
    void elBotonDePropuestaCambiaSiYaHayUnaVersion() {
        assertEquals("Enviar propuesta",
                PresentacionSolicitud.textoBotonPropuesta("PENDIENTE"));
        assertEquals("Enviar nueva versión",
                PresentacionSolicitud.textoBotonPropuesta("EN_NEGOCIACION"));
    }

    @Test
    void laPropuestaContratadaODescartadaEstaCerrada() {
        assertTrue(PresentacionSolicitud.propuestaCerrada("CONTRATADA"));
        assertTrue(PresentacionSolicitud.propuestaCerrada("NO_SELECCIONADA"));
        assertFalse(PresentacionSolicitud.propuestaCerrada("SELECCIONADA"));
        assertFalse(PresentacionSolicitud.propuestaCerrada("ENVIADA"));
    }

    @Test
    void sinNombreSeUsaElIdentificador() {
        ContextoSolicitud vacio = ContextoSolicitud.vacio();

        assertEquals("Evento #7", PresentacionSolicitud.nombreEvento(vacio, 7));
        assertEquals("Servicio #3", PresentacionSolicitud.nombreServicio(vacio, 3));
        assertEquals("Boda de Valentina", PresentacionSolicitud.nombreEvento(
                contexto("Boda de Valentina", "Decoración", 80), 7));
    }

    @Test
    void laFechaDelEventoUsaMesesEnMinuscula() {
        assertEquals("5 dic. 2026, 15:00 a 23:00",
                PresentacionSolicitud.fechaEvento(contexto("Boda", "Decoración", 80)));
    }

    @Test
    void lasPersonasSeEscribenEnSingularOPlural() {
        assertEquals("80 personas",
                PresentacionSolicitud.personas(contexto("Boda", "Decoración", 80)));
        assertEquals("1 persona",
                PresentacionSolicitud.personas(contexto("Boda", "Decoración", 1)));
        assertEquals("—",
                PresentacionSolicitud.personas(contexto("Boda", "Decoración", null)));
    }

    private static MensajeSolicitud mensaje(int remitente, Integer propuesta, Integer version) {
        MensajeSolicitud mensaje = new MensajeSolicitud();
        mensaje.setIdUsuarioRemitente(remitente);
        mensaje.setIdPropuesta(propuesta);
        mensaje.setVersionPropuesta(version);
        return mensaje;
    }

    @Test
    void elAutorDelMensajePropioSeMuestraComoTu() {
        assertEquals("Tú", PresentacionSolicitud.autorMensaje(
                mensaje(5, null, null), 5, "Valentina Flores"));
        assertEquals("Valentina Flores", PresentacionSolicitud.autorMensaje(
                mensaje(8, null, null), 5, "Valentina Flores"));
        assertEquals("Otro participante", PresentacionSolicitud.autorMensaje(
                mensaje(8, null, null), 5, null));
    }

    @Test
    void soloLosAjustesTienenAvisoConLaVersion() {
        assertEquals(null, PresentacionSolicitud.textoAjuste(
                mensaje(8, null, null)));
        assertEquals("Ajuste solicitado sobre la versión 2",
                PresentacionSolicitud.textoAjuste(mensaje(8, 40, 2)));
        assertEquals("Ajuste solicitado",
                PresentacionSolicitud.textoAjuste(mensaje(8, 40, null)));
    }

    @Test
    void cadaEstadoDePropuestaHabilitaSusAcciones() {
        assertTrue(PresentacionSolicitud.puedeSeleccionar("ENVIADA"));
        assertFalse(PresentacionSolicitud.puedeSeleccionar("SELECCIONADA"));

        assertTrue(PresentacionSolicitud.puedeContratar("SELECCIONADA"));
        assertFalse(PresentacionSolicitud.puedeContratar("ENVIADA"));

        assertTrue(PresentacionSolicitud.puedeAjustar("ENVIADA"));
        assertFalse(PresentacionSolicitud.puedeAjustar("SELECCIONADA"));

        assertTrue(PresentacionSolicitud.puedeRechazarPropuesta("ENVIADA"));
        assertTrue(PresentacionSolicitud.puedeRechazarPropuesta("SELECCIONADA"));
        assertFalse(PresentacionSolicitud.puedeRechazarPropuesta("CONTRATADA"));
        assertFalse(PresentacionSolicitud.puedeRechazarPropuesta("NO_SELECCIONADA"));
    }

    @Test
    void laPropuestaCerradaExplicaPorQueNoSeModifica() {
        assertEquals(
                "Esta propuesta ya fue contratada y no admite cambios.",
                PresentacionSolicitud.mensajeCerrada("CONTRATADA"));
        assertTrue(PresentacionSolicitud.mensajeCerrada("NO_SELECCIONADA")
                .contains("no seleccionada"));
    }

    @Test
    void sinNombreDeEventoOServicioSeMuestraGuion() {
        assertEquals("—", PresentacionSolicitud.nombreEvento(
                ContextoSolicitud.vacio()));
        assertEquals("—", PresentacionSolicitud.nombreServicio(
                ContextoSolicitud.vacio()));
        assertEquals("Decoración", PresentacionSolicitud.nombreServicio(
                contexto("Boda", "Decoración", 80)));
    }

    @Test
    void sinProveedorSeMuestraGuion() {
        assertEquals("—", PresentacionSolicitud.nombreProveedor(
                ContextoSolicitud.vacio()));
        assertEquals("Luna Decoraciones", PresentacionSolicitud.nombreProveedor(
                contexto("Boda", "Decoración", 80)));
    }
}
