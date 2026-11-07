package views.solicitudes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import entities.eventos.EventoSolicitable;
import entities.solicitudes.NecesidadAbierta;

class SolicitudNuevaTest {

    private static final EventoSolicitable EVENTO = new EventoSolicitable(
            7, "Boda de Valentina", LocalDateTime.of(2026, 12, 5, 15, 0));

    private static String mensajeDe(
            EventoSolicitable evento, NecesidadAbierta necesidad, String texto) {
        return assertThrows(IllegalArgumentException.class,
                () -> SolicitudNueva.crear(evento, necesidad, texto))
                .getMessage();
    }

    @Test
    void conUnaNecesidadExistenteNoHaceFaltaDescripcion() {
        SolicitudNueva solicitud = SolicitudNueva.crear(
                EVENTO, new NecesidadAbierta(12, 7, "Decoración"), null);

        assertEquals(7, solicitud.idEvento());
        assertEquals(12, solicitud.idNecesidadExistente());
        assertNull(solicitud.descripcionNecesidad());
        assertFalse(solicitud.esNecesidadNueva());
    }

    @Test
    void unaNecesidadNuevaUsaLaDescripcionSinEspacios() {
        SolicitudNueva solicitud = SolicitudNueva.crear(
                EVENTO, SolicitudNueva.NECESIDAD_NUEVA, "  Flores para 10 mesas ");

        assertTrue(solicitud.esNecesidadNueva());
        assertEquals("Flores para 10 mesas", solicitud.descripcionNecesidad());
    }

    @Test
    void exigeEventoYNecesidad() {
        assertEquals("Elige el evento para el que necesitas el servicio.",
                mensajeDe(null, SolicitudNueva.NECESIDAD_NUEVA, "Flores"));
        assertEquals("Elige una necesidad o crea una nueva.",
                mensajeDe(EVENTO, null, "Flores"));
    }

    @Test
    void laNecesidadNuevaExigeDescripcionDeLongitudValida() {
        assertEquals("Describe lo que necesitas para tu evento.",
                mensajeDe(EVENTO, SolicitudNueva.NECESIDAD_NUEVA, "   "));
        assertEquals("La descripción puede tener hasta 500 caracteres.",
                mensajeDe(EVENTO, SolicitudNueva.NECESIDAD_NUEVA, "a".repeat(501)));
    }
}
